/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;
import java.util.function.Consumer;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.ActiveContextUnbindException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class ContextObject extends AbstractSqueakObjectWithHash {
    public static final int NIL_PC_THRESHOLD = 0;
    public static final int NIL_PC_STACK_NOT_NIL_VALUE = -1;
    public static final int NIL_PC_STACK_NIL_VALUE = -2;
    private static final int INVALID_PC = Integer.MIN_VALUE;

    private static final Class<?> CONCRETE_MATERIALIZED_FRAME_CLASS = Truffle.getRuntime().createMaterializedFrame(new Object[0]).getClass();

    /**
     * To maximize GraalVM performance, this field acts as a union type transitioning between four
     * distinct states:
     * <p>
     * 1. Sender (any other object): The lightweight wrapper state. When a context is actively
     *    executing in a {@link VirtualFrame}, we avoid allocating a MaterializedFrame. Instead, we
     *    simply store its sender here until materialization is forced.
     * <p>
     * 2. {@link MaterializedFrame}: The fast-path execution state. Used for valid, suspended
     *    contexts on the heap. This allows GraalVM to execute and resume them. Note that stack
     *    slots above {@code stackPointer} are not retained.
     * <p>
     * 3. {@link NonExecutableContextState}: The slow-path fallback state. Used for newly allocated empty
     *    contexts or contexts that have been reflectively mutated with incompatible values,
     *    making them invalid Truffle frames.
     * <p>
     * 4. {@link SqueakImageChunk}: A transient bootstrap state used exclusively during image load.
     */
    private Object senderOrFrameOrState;

    public enum FrameHandling {
        /** Enumerate live objects in Truffle frames (Read-Only). */
        SCAN,
        /** Enumerate live objects and nil dead objects in Truffle frames (Read-Write). */
        SCRUB
    }

    /**
     * Fallback representation for {@link ContextObject}s whose state cannot be stored in a Truffle
     * {@link MaterializedFrame}, specifically newly allocated empty contexts ({@code Context basicNew:}
     * / {@code Context newForMethod:}) and contexts reflectively mutated with structurally
     * incompatible values (such as a non-{@link CompiledCodeObject} method or an out-of-range
     * instruction/stack pointer). The sender is not one of them: a frame can hold any object as
     * its sender, and returning to anything but a live context ends in {@code cannotReturn:}.
     * <p>
     * Any {@code null} field (or unallocated stack slot) represents an unset Squeak slot and is
     * treated as {@link NilObject#SINGLETON} on access. {@link #stackOrSize} initially stores the
     * context's variable slot count as a boxed {@link Integer} to avoid allocating an
     * {@code Object[]} on read-only operations, expanding lazily on the first stack write. Setters
     * for structural slots automatically re-materialize the context into a
     * {@link MaterializedFrame} once all invariants are satisfied.
     */
    private static final class NonExecutableContextState {
        private Object sender;
        private Object instructionPointer;
        private Object stackPointer;
        private Object method;
        private Object closureOrNil;
        private Object receiver;
        private Object stackOrSize;

        private NonExecutableContextState(final int size) {
            stackOrSize = size;
        }

        private NonExecutableContextState(final NonExecutableContextState original) {
            sender = original.sender;
            instructionPointer = original.instructionPointer;
            stackPointer = original.stackPointer;
            method = original.method;
            closureOrNil = original.closureOrNil;
            receiver = original.receiver;
            if (original.stackOrSize instanceof Object[] stack) {
                stackOrSize = stack.clone();
            } else {
                stackOrSize = original.stackOrSize;
            }
        }

        private int getSize() {
            if (stackOrSize instanceof Integer size) {
                return size;
            } else {
                return ((Object[]) stackOrSize).length;
            }
        }

        private Object[] getOrCreateStack() {
            if (stackOrSize instanceof Integer size) {
                final Object[] stack = ArrayUtils.withAll(size, NilObject.SINGLETON);
                stackOrSize = stack;
                return stack;
            }
            return (Object[]) stackOrSize;
        }

        private Object getStackValueOrNil(final int index) {
            if (stackOrSize instanceof Object[] stack && index < stack.length) {
                return resolveRawValue(stack[index]);
            }
            return NilObject.SINGLETON;
        }

        private void tracePointers(final Consumer<Object> action, final int sp) {
            action.accept(sender);
            action.accept(instructionPointer);
            action.accept(method);
            action.accept(closureOrNil);
            action.accept(receiver);
            if (stackOrSize instanceof Object[] stack) {
                final int limit = Math.min(sp, stack.length);
                for (int i = 0; i < limit; i++) {
                    action.accept(stack[i]);
                }
            }
        }
    }

    public ContextObject(final SqueakImageChunk chunk) {
        super(chunk);
        senderOrFrameOrState = chunk;
    }

    public ContextObject(final int size) {
        super();
        senderOrFrameOrState = new NonExecutableContextState(size);
        assert size == CONTEXT.SMALL_FRAMESIZE || size == CONTEXT.LARGE_FRAMESIZE || size == CONTEXT.HUGE_FRAMESIZE;
    }

    private ContextObject(final VirtualFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrState = FrameAccess.getSender(frame);
        FrameAccess.setContext(frame, this);
    }

    private ContextObject(final MaterializedFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrState = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
    }

    @TruffleBoundary
    private ContextObject(final ContextObject original) {
        super(original);
        if (original.hasNonExecutableState()) {
            setAllBooleanBits(original.getAllBooleanBits());
            senderOrFrameOrState = new NonExecutableContextState(original.getNonExecutableState());
        } else {
            if (!original.hasTruffleFrame()) {
                original.materializeFromFrames();
            }
            // Copy modified sender flag and the marked code flags after ensuring original is materialized.
            setAllBooleanBits(original.getAllBooleanBits());
            // Create shallow copy of Truffle frame.
            final MaterializedFrame originalFrame = original.getTruffleFrame();
            final FrameDescriptor frameDescriptor = originalFrame.getFrameDescriptor();
            final MaterializedFrame copyFrame = Truffle.getRuntime().createMaterializedFrame(originalFrame.getArguments().clone(), frameDescriptor);
            senderOrFrameOrState = copyFrame;
            FrameAccess.copySlotsExceptContext(originalFrame, copyFrame);
            FrameAccess.setContext(copyFrame, this);
        }
        // A newly created copy lives on the heap and is not active on the Truffle stack.
        markAsInactiveOnTruffleStack();
    }

    public static ContextObject createVirtual(final VirtualFrame frame) {
        return new ContextObject(frame);
    }

    public static ContextObject createMaterialized(final MaterializedFrame frame) {
        return new ContextObject(frame);
    }

    public static ContextObject createCopy(final ContextObject original) {
        return new ContextObject(original);
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        assert chunk.getWordSize() > CONTEXT.TEMP_FRAME_START;
        fillinAsNonExecutableState(chunk);

        // During image load, the closure shell must be explicitly filled in
        // before materialization so we can access its CompiledBlock.
        final NonExecutableContextState state = getNonExecutableState();
        if (state.closureOrNil instanceof BlockClosureObject closure &&
                        state.method instanceof CompiledCodeObject code &&
                        code.isCompiledMethod()) {
            closure.fillin(chunk.getChunk(CONTEXT.CLOSURE_OR_NIL));
        }

        // ToDo: A Context that was non-executable because its stack pointer was not an integer,
        // but was legal in all other ways, will lose the stack pointer value here.
        tryBindToFrame(true);
    }

    private void fillinAsNonExecutableState(final SqueakImageChunk chunk) {
        final int stackSize = chunk.getWordSize() - CONTEXT.TEMP_FRAME_START;
        final NonExecutableContextState state = new NonExecutableContextState(stackSize);

        // Read the exact object pointers directly from the chunk without casting
        state.sender = chunk.getPointer(CONTEXT.SENDER_OR_NIL);
        state.instructionPointer = chunk.getPointer(CONTEXT.INSTRUCTION_POINTER);
        state.stackPointer = chunk.getPointer(CONTEXT.STACKPOINTER);
        state.method = chunk.getPointer(CONTEXT.METHOD);
        state.closureOrNil = chunk.getPointer(CONTEXT.CLOSURE_OR_NIL);
        state.receiver = chunk.getPointer(CONTEXT.RECEIVER);

        // Map every variable-length stack slot from the chunk into the stack array
        final Object[] stack = state.getOrCreateStack();
        for (int i = 0; i < stackSize; i++) {
            stack[i] = chunk.getPointer(CONTEXT.TEMP_FRAME_START + i);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert senderOrFrameOrState == chunk;
        senderOrFrameOrState = state;
    }

    public CompiledCodeObject getMethodFromChunk() {
        if (senderOrFrameOrState instanceof final SqueakImageChunk chunk) {
            return (CompiledCodeObject) chunk.getPointer(CONTEXT.METHOD);
        } else {
            return getCodeObject();
        }
    }

    @Override
    public ClassObject getSqueakClass() {
        return getSqueakClass(SqueakImageContext.getSlow());
    }

    @Override
    public ClassObject getSqueakClass(final SqueakImageContext image) {
        return image.methodContextClass;
    }

    @Override
    protected AbstractSqueakObjectWithHash getForwardingPointer() {
        return this; // ContextObject cannot be forwarded
    }

    @Override
    public AbstractSqueakObjectWithHash resolveForwardingPointer() {
        return this; // ContextObject cannot be forwarded
    }

    public CompiledCodeObject getMethodOrBlock() {
        return hasClosure() ? getClosure().getCompiledBlock() : getCodeObject();
    }

    public CallTarget getResumptionCallTarget() {
        return getMethodOrBlock().getResumptionCallTarget();
    }

    private MaterializedFrame getOrCreateTruffleFrame() {
        if (!hasTruffleFrame()) {
            if (hasNonExecutableState()) {
                if (tryBindToFrame(true)) {
                    return getTruffleFrame();
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw SqueakException.create("Cannot create a frame for a non-executable context:", this);
                }
            }
            // Fallback for the lightweight sender state.
            return createDummyTruffleFrame();
        }
        return getTruffleFrame();
    }

    @TruffleBoundary
    private MaterializedFrame createDummyTruffleFrame() {
        // Method is unknown, use dummy frame instead
        final Object[] dummyArguments = FrameAccess.newWith(1);
        final CompiledCodeObject dummyMethod = SqueakImageContext.getSlow().dummyMethod;
        return installTruffleFrame(dummyArguments, dummyMethod.getFrameDescriptor(), 0, 0);
    }

    private MaterializedFrame installTruffleFrame(final Object[] frameArguments, final FrameDescriptor descriptor, final int pc, final int sp) {
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(frameArguments, descriptor);
        senderOrFrameOrState = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
        return frame;
    }

    public Object getFrameSender() {
        if (hasTruffleFrame()) {
            return FrameAccess.getSender(getTruffleFrame());
        } else if (hasNonExecutableState()) {
            return getRawFrameSender();
        } else {
            return FrameAccess.ensureValidSender(senderOrFrameOrState);
        }
    }

    @TruffleBoundary
    private Object getRawFrameSender() {
        return resolveRawValue(getNonExecutableState().sender);
    }

    public Object getSender() {
        final Object sender = getFrameSender();
        if (sender instanceof final ContextObject senderContext && !senderContext.hasTruffleFrame() && !senderContext.hasNonExecutableState()) {
            senderContext.materializeFromFrames();
        }
        return sender;
    }

    @TruffleBoundary
    public void materializeFromFrames() {
        senderOrFrameOrState = FrameAccess.findFrameForContext(this);
        setMarkedCodeFlags();
        getCodeObject().getDoesNotNeedThisContextAssumption().invalidate();
    }

    /* Context has modified sender flag */
    public boolean hasModifiedSender() {
        return isBooleanASet();
    }

    public void clearModifiedSender() {
        clearBooleanABit();
    }

    public void setModifiedSender() {
        setBooleanABit();
    }

    /* Marked code flags (implemented in object header flags). */
    private void setMarkedCodeFlags() {
        resetMarkedCodeFlags();
        if (getCodeObject().isUnwindMarked() && !hasClosure()) {
            setUnwindMarked();
        } else if (getCodeObject().isExceptionHandlerMarked()) {
            setExceptionHandlerMarked();
        }
    }

    private void resetMarkedCodeFlags() {
        clearUnwindMarked();
        clearExceptionHandlerMarked();
    }

    /**
     * Returns <code>true</code> if method is unwind-marked. In this case, the ContextObject must
     * always have a frame.
     */
    public boolean isUnwindMarked() {
        return isBooleanBSet();
    }

    private void setUnwindMarked() {
        setBooleanBBit();
    }

    private void clearUnwindMarked() {
        clearBooleanBBit();
    }

    /**
     * Returns <code>true</code> if method is exception-handler-marked. In this case, the
     * ContextObject must always have a frame.
     */
    public boolean isExceptionHandlerMarked() {
        return isBooleanCSet();
    }

    private void setExceptionHandlerMarked() {
        setBooleanCBit();
    }

    private void clearExceptionHandlerMarked() {
        clearBooleanCBit();
    }

    /**
     * Returns true if the Context might be currently executing on the Truffle stack. This acts as a
     * fast-path flag to avoid expensive {@link #isActuallyActiveOnTruffleStack()} checks. Returns false
     * if it is guaranteed to have been forced to the heap or suspended.
     * <p>
     * Note: We use inverted bit logic (0 = potentially active, 1 = inactive) to take advantage of
     * default zero-initialization. This ensures newly created Contexts safely default to requiring
     * a stack check.
     */
    public boolean isPotentiallyActiveOnTruffleStack() {
        return !isBooleanDSet();
    }

    /**
     * Caches the state indicating this Context is no longer active on the stack. Used to avoid
     * repeated Truffle frame iterations when a suspended context is modified multiple times.
     */
    public void markAsInactiveOnTruffleStack() {
        setBooleanDBit();
    }

    /**
     * Indicates that this Context has resumed execution. Resets the flag so that future external
     * modifications to this Context will trigger a proper stack check.
     */
    public void markAsPotentiallyActiveOnTruffleStack() {
        clearBooleanDBit();
    }

    /**
     * Sets the sender of a ContextObject.
     */
    public void setSender(final Object value) {
        if (!hasModifiedSender() && hasTruffleFrame() && FrameAccess.getSender(getTruffleFrame()) != value) {
            setModifiedSender();
        }
        setSenderUnsafe(value);
    }

    public void setSenderUnsafe(final Object value) {
        FrameAccess.setSender(getOrCreateTruffleFrame(), value);
    }

    public void removeSender() {
        if (hasModifiedSender()) {
            clearModifiedSender();
        }
        setSenderUnsafe(NilObject.SINGLETON);
    }

    public Object getInstructionPointer(final InlinedConditionProfile nilProfile, final Node node) {
        final int pc = FrameAccess.getInstructionPointer(getTruffleFrame());
        if (nilProfile.profile(node, pc < NIL_PC_THRESHOLD)) {
            return NilObject.SINGLETON;
        } else {
            return getCodeObject().getInitialPC() + (long) pc; // Must be a long.
        }
    }

    public int getInstructionPointerForBytecodeLoop() {
        return FrameAccess.getInstructionPointer(getTruffleFrame());
    }

    public void setInstructionPointer(final int value) {
        FrameAccess.setInstructionPointer(getTruffleFrame(), value);
    }

    public void removeInstructionPointer() {
        FrameAccess.setInstructionPointer(getTruffleFrame(), NIL_PC_STACK_NOT_NIL_VALUE);
    }

    public int getStackPointerOrZero() {
        if (hasTruffleFrame()) {
            return FrameAccess.getStackPointer(getTruffleFrame());
        } else if (hasNonExecutableState()) {
            final Object sp = resolveRawValue(getNonExecutableState().stackPointer);
            /* Like OSVM, treat anything but a valid stack pointer as zero. */
            return sp instanceof final Long l && 0 <= l && l <= getNonExecutableState().getSize() ? (int) (long) l : 0;
        } else {
            return 0;
        }
    }

    public int getStackPointer() {
        return FrameAccess.getStackPointer(getTruffleFrame());
    }

    public void setStackPointer(final int value) {
        assert 0 <= value && value <= size() : value + " not between 0 and " + size() + " in " + this;
        FrameAccess.setStackPointer(getOrCreateTruffleFrame(), value);
    }

    private boolean hasMethod() {
        return hasTruffleFrame() && getCodeObject() != null;
    }

    public CompiledCodeObject getCodeObject() {
        return FrameAccess.getCodeObject(getTruffleFrame());
    }

    public void overwriteCodeObject(final CompiledCodeObject value) {
        resetMarkedCodeFlags();
        setCodeObject(value);
    }

    public void setCodeObject(final CompiledCodeObject value) {
        createAndInstallTruffleFrame(value);
    }

    @TruffleBoundary
    private void createAndInstallTruffleFrame(final CompiledCodeObject method) {
        final Object[] frameArguments;
        final int instructionPointer;
        final int stackPointer;
        if (hasTruffleFrame()) {
            final MaterializedFrame currentFrame = getTruffleFrame();
            FrameAccess.assertSenderNotNull(currentFrame);
            FrameAccess.assertReceiverNotNull(currentFrame);

            final Object[] dummyArguments = currentFrame.getArguments();
            final int expectedArgumentSize = FrameAccess.expectedArgumentSize(method.getNumArgs());
            if (dummyArguments.length != expectedArgumentSize) {
                // Adjust arguments.
                frameArguments = Arrays.copyOf(dummyArguments, expectedArgumentSize);
            } else {
                frameArguments = currentFrame.getArguments();
            }
            assert currentFrame.getFrameDescriptor().getNumberOfSlots() > 0;
            instructionPointer = FrameAccess.getInstructionPointer(currentFrame);
            stackPointer = FrameAccess.getStackPointer(currentFrame);
        } else {
            frameArguments = FrameAccess.newWith(method.getNumArgs());
            instructionPointer = 0;
            stackPointer = method.getNumTemps();
        }
        installTruffleFrame(frameArguments, method.getFrameDescriptor(), instructionPointer, stackPointer);
    }

    public boolean isActuallyActiveOnTruffleStack() {
        if (!hasTruffleFrame()) {
            return false; // No Truffle frame means the receiver is not yet executing.
        }
        final Object result = Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (current != null && FrameAccess.isTruffleSqueakFrame(current) && this == FrameAccess.getContext(current)) {
                return this;
            }
            return null;
        });
        return result == this;
    }

    public BlockClosureObject getClosure() {
        return FrameAccess.getClosure(getTruffleFrame());
    }

    public boolean hasClosure() {
        return FrameAccess.hasClosure(getTruffleFrame());
    }

    public void removeClosure() {
        if (hasClosure()) {
            throw SqueakException.create("Not yet implemented/support");
        }
    }

    @TruffleBoundary
    public void setClosure(final BlockClosureObject value) {
        final MaterializedFrame oldFrame = getOrCreateTruffleFrame();
        final int pc = FrameAccess.getInstructionPointer(oldFrame);
        final int sp = FrameAccess.getStackPointer(oldFrame);
        // Prepare arguments
        final int numArgs = value.getNumArgs();
        final int numCopied = value.getNumCopied();
        final int expectedFrameArgumentSize = FrameAccess.expectedArgumentSize(numArgs);
        final int oldArgsLength = oldFrame.getArguments().length;
        final Object[] arguments = Arrays.copyOf(oldFrame.getArguments(), expectedFrameArgumentSize + numCopied);
        if (oldArgsLength < expectedFrameArgumentSize) {
            /*
             * If the context was materialized for its home method before the closure was set and
             * the closure takes more arguments, Arrays.copyOf pads the new argument slots with
             * null. Migrate any live values already written to the corresponding stack slots and
             * initialize the remaining argument slots to nil so atTemp() never sees Java null.
             */
            for (int i = oldArgsLength; i < expectedFrameArgumentSize; i++) {
                final int tempIndex = i - FrameAccess.getArgumentStartIndex();
                arguments[i] = tempIndex < sp ? NilObject.nullToNil(FrameAccess.getStackValue(oldFrame, tempIndex)) : NilObject.SINGLETON;
            }
        }
        System.arraycopy(value.getCopiedValues(), 0, arguments, expectedFrameArgumentSize, numCopied);
        final FrameDescriptor frameDescriptor = value.getCompiledBlock().getFrameDescriptor();
        // Create and initialize new frame
        final MaterializedFrame frame = installTruffleFrame(arguments, frameDescriptor, pc, sp);
        FrameAccess.assertSenderNotNull(frame);
        FrameAccess.assertReceiverNotNull(frame);
        FrameAccess.setClosure(frame, value);
        // Cannot use copyTo here as frame descriptors may be different
        // ToDo: This does not handle any stack slots held in auxiliarySlots.
        FrameAccess.iterateStackSlots(oldFrame, sp, slotIndex -> {
            final Object stackValue = oldFrame.getObjectStatic(slotIndex);
            if (stackValue != null) {
                frame.setObjectStatic(slotIndex, stackValue);
            }
        });
    }

    public Object getReceiver() {
        return FrameAccess.getReceiver(getTruffleFrame());
    }

    public void setReceiver(final Object value) {
        FrameAccess.setReceiver(getOrCreateTruffleFrame(), value);
    }

    /**
     * Accesses a temporary variable or stack value at the given 0-based index.
     * <p>
     * If the index falls within the range of the initial arguments/copied values, it is retrieved
     * from the frame arguments. Otherwise, it is fetched from the actual stack slots.
     *
     * @param index the 0-based index of the temporary variable to retrieve.
     * @return the object at the specified index, or {@link NilObject#SINGLETON} if null.
     */
    @TruffleBoundary
    public Object atTemp(final int index) {
        final MaterializedFrame frame = getTruffleFrame();
        final Object[] args = frame.getArguments();
        if (FrameAccess.getArgumentStartIndex() + index < args.length) {
            return args[FrameAccess.getArgumentStartIndex() + index];
        } else {
            return NilObject.nullToNil(FrameAccess.getStackValue(frame, index));
        }
    }

    /**
     * Sets a temporary variable or stack value at the given 0-based index.
     * <p>
     * This method performs a dual-write if the index falls within the range of initial arguments or
     * copied values. It updates the value in the {@code frame.getArguments()} array and
     * consistently updates the corresponding indexed stack slot in the Truffle frame.
     *
     * @param index the 0-based index of the temporary variable or stack slot to update.
     * @param value the object to store at the specified index.
     */
    @TruffleBoundary
    public void atTempPut(final int index, final Object value) {
        final MaterializedFrame frame = getOrCreateTruffleFrame();
        final Object[] args = frame.getArguments();
        if (FrameAccess.getArgumentStartIndex() + index < args.length) {
            args[FrameAccess.getArgumentStartIndex() + index] = value;
        }
        FrameAccess.setStackValue(frame, index, value);
    }

    public void terminate() {
        removeInstructionPointer();
        removeSender();
    }

    /* Context>>#isDead */
    public boolean isDead() {
        if (hasNonExecutableState()) {
            return !canBeExecuted() || FrameAccess.isDead(getTruffleFrame());
        }
        return FrameAccess.isDead(getTruffleFrame());
    }

    /**
     * Returns false if this Context is non-executable and cannot be bound to a frame either. Such a
     * Context can neither be resumed nor returned to. Like OSVM, this accepts a stack pointer that
     * is not an integer and treats it as zero.
     */
    public boolean canBeExecuted() {
        return !hasNonExecutableState() || tryBindToFrame(true);
    }

    /**
     * Returns true if this Context is alive and its sender is a valid, live Context. Senders
     * without a Truffle frame are executing JVM frames and are guaranteed to be alive. Senders with
     * a Truffle frame must not be dead.
     */
    public boolean canReturnToSender() {
        if (isDead()) {
            return false;
        }
        final Object sender = getFrameSender();
        if (sender instanceof ContextObject senderContext) {
            if (senderContext.hasNonExecutableState()) {
                return !senderContext.isDead();
            }
            return !senderContext.hasTruffleFrame() || !senderContext.isDead();
        }
        return false;
    }

    public void push(final Object value) {
        assert value != null : "Unexpected `null` value";
        final int currentStackPointer = getStackPointer();
        assert currentStackPointer <= getCodeObject().getMaxStackSize() : "curSP " + currentStackPointer + " > maxStackSize " + getCodeObject().getMaxStackSize();
        setStackPointer(currentStackPointer + 1);
        atTempPut(currentStackPointer, value);
    }

    public Object pop() {
        final int newStackPointer = getStackPointer() - 1;
        assert 0 <= newStackPointer;
        final Object value = atTemp(newStackPointer);
        assert value != null : "Unexpected `null` value";
        atTempPut(newStackPointer, NilObject.SINGLETON);
        setStackPointer(newStackPointer);
        return value;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (hasMethod()) {
            return formatContext("CTX", getCodeObject(), hasClosure());
        } else if (hasNonExecutableState() && rawAt(CONTEXT.METHOD) instanceof final CompiledCodeObject code) {
            final boolean hasClosure = rawAt(CONTEXT.CLOSURE_OR_NIL) instanceof BlockClosureObject;
            return formatContext("CTX (non-executable)", code, hasClosure);
        } else {
            return "CTX without method @" + Integer.toHexString(hashCode());
        }
    }

    private String formatContext(final String prefix, final CompiledCodeObject code, final boolean isClosure) {
        return prefix + (isClosure ? " [] in " : " ") + code + " @" + Integer.toHexString(hashCode());
    }

    @Override
    public int getNumSlots() {
        return CONTEXT.INST_SIZE + size();
    }

    @Override
    public int instsize() {
        return CONTEXT.INST_SIZE;
    }

    @Override
    public int size() {
        if (senderOrFrameOrState instanceof final NonExecutableContextState state) {
            return state.getSize();
        } else {
            if (!hasTruffleFrame()) {
                materializeFromFrames();
            }
            return getCodeObject().getSqueakContextSize();
        }
    }

    public boolean isValidInstructionPointer(final long pc) {
        final long offset = pc - getCodeObject().getInitialPC();
        return offset >= NIL_PC_THRESHOLD && offset <= Integer.MAX_VALUE;
    }

    public boolean isValidStackPointer(final long sp) {
        if (hasTruffleFrame()) {
            return sp >= 0 && sp <= FrameAccess.getNumStackSlots(getTruffleFrame());
        }
        // Without a frame, the Squeak size of the context is the limit.
        return sp >= 0 && sp <= size();
    }

    public boolean isValidTempIndex(final int tempIndex) {
        if (tempIndex < 0 || tempIndex >= getStackPointerOrZero()) {
            return false;
        }
        if (hasTruffleFrame()) {
            return tempIndex < FrameAccess.getNumStackSlots(getTruffleFrame());
        }
        return tempIndex < size();
    }

    public void become(final ContextObject other) {
        final Object otherSenderOrFrame = other.senderOrFrameOrState;
        final int otherBooleans = other.getAllBooleanBits();
        other.setFields(senderOrFrameOrState, getAllBooleanBits());
        setFields(otherSenderOrFrame, otherBooleans);
    }

    private void setFields(final Object otherSenderOrFrame, final int otherBooleanBits) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        senderOrFrameOrState = otherSenderOrFrame;
        setAllBooleanBits(otherBooleanBits);
    }

    public Object[] getReceiverAndNArguments() {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        final int numArgs = hasClosure() ? getClosure().getNumArgs() : getCodeObject().getNumArgs();
        return getReceiverAndNArguments(numArgs);
    }

    private Object[] getReceiverAndNArguments(final int numArgs) {
        final Object[] arguments = new Object[1 + numArgs];
        arguments[0] = getReceiver();
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = atTemp(i);
        }
        return arguments;
    }

    /**
     * Since {@link MaterializedFrame} is an interface, the Graal compiler needs help to find the
     * concrete class, and which concrete implementation is used depends on the GraalVM edition (CE
     * vs. EE). This in turn means that the concrete class can be cached statically and injected via
     * {@link CompilerDirectives#castExact(Object, Class)}.
     */
    public MaterializedFrame getTruffleFrame() {
        return (MaterializedFrame) CompilerDirectives.castExact(senderOrFrameOrState, CONCRETE_MATERIALIZED_FRAME_CLASS);
    }

    public boolean hasTruffleFrame() {
        return senderOrFrameOrState instanceof MaterializedFrame;
    }

    public void setTruffleFrame(final MaterializedFrame frame) {
        assert !hasTruffleFrame();
        senderOrFrameOrState = frame;
    }

    /**
     * Accessing the non-executable state.
     */

    public boolean hasNonExecutableState() {
        return senderOrFrameOrState instanceof NonExecutableContextState;
    }

    private NonExecutableContextState getNonExecutableState() {
        assert hasNonExecutableState();
        return (NonExecutableContextState) senderOrFrameOrState;
    }

    @TruffleBoundary
    private boolean tryBindToFrame(final boolean forced) {
        final NonExecutableContextState state = (NonExecutableContextState) senderOrFrameOrState;

        final Object methodObj = resolveRawValue(state.method);
        final Object closureOrNil = resolveRawValue(state.closureOrNil);
        final Object senderObj = resolveRawValue(state.sender);

        if (!hasBindableStructure(methodObj, closureOrNil)) {
            return false;
        }

        final CompiledCodeObject code = (CompiledCodeObject) methodObj;

        // Resolve method and closure
        final BlockClosureObject closure;
        final CompiledCodeObject methodOrBlock;
        final int numArgs;

        if (closureOrNil == NilObject.SINGLETON) {
            closure = null;
            methodOrBlock = code;
            numArgs = code.getNumArgs();
        } else {
            closure = (BlockClosureObject) closureOrNil;
            methodOrBlock = closure.isAFullBlockClosure() ? code : closure.getCompiledBlock();
            numArgs = closure.getNumArgs() + closure.getNumCopied();
        }

        // Validate instruction pointer bounds before allocating frame objects
        final int relativePC = determineInstructionPointer(resolveRawValue(state.instructionPointer), methodOrBlock);
        if (relativePC == INVALID_PC) {
            return false;
        }

        // Evaluate stack pointer bounds before allocating or installing the frame
        final int numStackSlots = methodOrBlock.getFrameDescriptor().getNumberOfSlots() - FrameAccess.getStackStart();
        final int sp = determineStackPointer(resolveRawValue(state.stackPointer), forced, numStackSlots);
        if (sp < 0) {
            return false;
        }

        // Allocate and apply state
        final Object[] receiverAndArgs = gatherReceiverAndArgs(state, numArgs);
        final Object[] frameArguments = FrameAccess.newWith(senderObj, closure, receiverAndArgs);
        installTruffleFrame(frameArguments, methodOrBlock.getFrameDescriptor(), relativePC, sp);
        restoreStack(state, sp);

        return true;
    }

    private static boolean hasBindableStructure(final Object method, final Object closureOrNil) {
        if (!(method instanceof CompiledCodeObject)) {
            return false; // Cannot materialize without a structurally valid method
        }
        return closureOrNil == NilObject.SINGLETON || closureOrNil instanceof BlockClosureObject; // Structurally invalid closure otherwise
    }

    private static Object[] gatherReceiverAndArgs(final NonExecutableContextState state, final int numArgs) {
        final Object[] receiverAndArgs = new Object[1 + numArgs];
        receiverAndArgs[0] = resolveRawValue(state.receiver);
        for (int i = 0; i < numArgs; i++) {
            receiverAndArgs[1 + i] = state.getStackValueOrNil(i);
        }
        return receiverAndArgs;
    }

    private static int determineInstructionPointer(final Object pc, final CompiledCodeObject methodOrBlock) {
        if (pc == NilObject.SINGLETON) {
            return NIL_PC_STACK_NOT_NIL_VALUE;
        } else if (pc instanceof Long l) {
            final long offset = l - methodOrBlock.getInitialPC();
            if (offset < NIL_PC_THRESHOLD || offset > Integer.MAX_VALUE) {
                return INVALID_PC; // Stay non-executable if PC cannot be represented in a Truffle frame
            }
            return (int) offset;
        }
        return INVALID_PC;
    }

    private static int determineStackPointer(final Object stackPointer, final boolean forced, final int numStackSlots) {
        if (stackPointer instanceof Long l) {
            if (l < 0 || l > numStackSlots) {
                // ToDo: Alternatively, we could force SP to zero when forced
                return -1; // Refuse to materialize if SP out of bounds
            }
            return MiscUtils.toIntExact(l);
        } else if (forced) {
            return 0; // OSVM interprets non-integers as zero for execution/loading
        }
        return -1; // Stay non-executable
    }

    private void restoreStack(final NonExecutableContextState state, final int sp) {
        /*
         * Only live stack slots in [0, sp) are copied; any values written above sp while non-executable
         * form are discarded upon materialization (Context>>#stackp: nils those slots anyway).
         */
        for (int i = 0; i < sp; i++) {
            atTempPut(i, state.getStackValueOrNil(i));
        }
    }

    /**
     * Gracefully degrades a MaterializedFrame into a NonExecutableContextState.
     * This is triggered when Squeak reflection attempts to write structurally
     * invalid types (e.g., Strings instead of CompiledCodeObjects) into critical slots.
     */
    public void unbindFromFrame() {
        assert hasTruffleFrame();

        // ToDo: Ensure primitives only operate on heap-based Contexts.
        if (isPotentiallyActiveOnTruffleStack() && isActuallyActiveOnTruffleStack()) {
            CompilerDirectives.transferToInterpreter();
            throw new ActiveContextUnbindException();
        }

        // Extract all state from the Truffle frame using the existing slow-path getters
        final int sp = getStackPointer();
        final NonExecutableContextState state = new NonExecutableContextState(getCodeObject().getSqueakContextSize());
        state.sender = getSender();
        state.instructionPointer = getInstructionPointer(InlinedConditionProfile.getUncached(), null);
        state.stackPointer = (long) sp;
        state.method = getCodeObject();
        state.closureOrNil = NilObject.nullToNil(getClosure());
        state.receiver = getReceiver();

        // Populate the stack array with the Truffle frame's stack
        final Object[] stack = state.getOrCreateStack();
        for (int i = 0; i < sp; i++) {
            stack[i] = atTemp(i);
        }

        // Sever the connection to the Truffle frame
        senderOrFrameOrState = state;
        resetMarkedCodeFlags();
    }

    private static Object resolveRawValue(final Object value) {
        return value == null ? NilObject.SINGLETON : AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(value);
    }

    /**
     * Reads the slot at the given 0-based index from the non-executable state. Slots at or above
     * the stack pointer read as {@code nil}.
     */
    @TruffleBoundary
    public Object rawAt(final long index) {
        final NonExecutableContextState state = getNonExecutableState();
        if (index == CONTEXT.SENDER_OR_NIL) {
            return resolveRawValue(state.sender);
        } else if (index == CONTEXT.INSTRUCTION_POINTER) {
            return resolveRawValue(state.instructionPointer);
        } else if (index == CONTEXT.STACKPOINTER) {
            return resolveRawValue(state.stackPointer);
        } else if (index == CONTEXT.METHOD) {
            return resolveRawValue(state.method);
        } else if (index == CONTEXT.CLOSURE_OR_NIL) {
            return resolveRawValue(state.closureOrNil);
        } else if (index == CONTEXT.RECEIVER) {
            return resolveRawValue(state.receiver);
        } else {
            final int tempIndex = (int) (index - CONTEXT.TEMP_FRAME_START);
            return isValidTempIndex(tempIndex) ? state.getStackValueOrNil(tempIndex) : NilObject.SINGLETON;
        }
    }

    /**
     * Writes the slot at the given 0-based index of the non-executable state. Writes to slots at or
     * above the stack pointer are ignored. Afterwards, the context is bound to a frame again if a
     * structural slot changed and all slots are valid now.
     */
    @TruffleBoundary
    public void rawAtPut(final long index, final Object value) {
        final NonExecutableContextState state = getNonExecutableState();
        if (index == CONTEXT.SENDER_OR_NIL) {
            state.sender = value;
        } else if (index == CONTEXT.INSTRUCTION_POINTER) {
            state.instructionPointer = value;
        } else if (index == CONTEXT.STACKPOINTER) {
            state.stackPointer = value;
        } else if (index == CONTEXT.METHOD) {
            state.method = value;
        } else if (index == CONTEXT.CLOSURE_OR_NIL) {
            state.closureOrNil = value;
        } else if (index == CONTEXT.RECEIVER) {
            state.receiver = value;
            return; // Cannot make the context executable.
        } else {
            final int tempIndex = (int) (index - CONTEXT.TEMP_FRAME_START);
            if (isValidTempIndex(tempIndex)) {
                state.getOrCreateStack()[tempIndex] = value;
            }
            return; // Cannot make the context executable.
        }
        tryBindToFrame(false);
    }

    /**
     * Implements {@code Context>>#stackp:} for the non-executable state: slots that become
     * accessible are set to {@code nil}.
     */
    @TruffleBoundary
    public void rawStackPointerPut(final int newStackPointer) {
        final NonExecutableContextState state = getNonExecutableState();
        final Object[] stack = state.getOrCreateStack();
        for (int i = getStackPointerOrZero(); i < newStackPointer; i++) {
            stack[i] = NilObject.SINGLETON;
        }
        state.stackPointer = (long) newStackPointer;
        tryBindToFrame(false);
    }

    @TruffleBoundary
    public boolean pointsTo(final Object thang) {
        if (hasNonExecutableState()) {
            final int stackPointer = getStackPointerOrZero();
            for (int i = CONTEXT.SENDER_OR_NIL; i < CONTEXT.TEMP_FRAME_START + stackPointer; i++) {
                final Object value = rawAt(i);
                if (value == thang || (value instanceof Long && thang.equals(value))) {
                    return true;
                }
            }
            return false;
        }
        if (!hasTruffleFrame()) {
            materializeFromFrames();
        }
        final int stackPointer = getStackPointer();
        if (getFrameSender() == thang ||
                        thang.equals(getInstructionPointer(InlinedConditionProfile.getUncached(), null)) ||
                        thang.equals((long) stackPointer) ||
                        getCodeObject() == thang ||
                        (hasClosure() ? getClosure() : NilObject.SINGLETON) == thang ||
                        getReceiver() == thang) {
            return true;
        }
        for (int i = 0; i < stackPointer; i++) {
            if (atTemp(i) == thang) {
                return true;
            }
        }
        return false;
    }

    private static Object migrate(final UnmodifiableEconomicMap<Object, Object> fromToMap, final Object value) {
        return value != null ? fromToMap.get(value) : null;
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        if (hasTruffleFrame()) {
            /*
             * Migrate arguments, stack slots, and sender in place on initialFrame BEFORE
             * overwriteCodeObject() or setClosure(), because both allocate and install a new
             * MaterializedFrame (copying state from the current frame). Resolve migratedCode and
             * migratedClosure from initialFrame upfront so both are read before any frame
             * replacement occurs.
             */
            final MaterializedFrame initialFrame = getTruffleFrame();
            final Object[] arguments = initialFrame.getArguments();
            for (int i = FrameAccess.getReceiverStartIndex(); i < arguments.length; i++) {
                final Object migratedValue = migrate(fromToMap, arguments[i]);
                if (migratedValue != null) {
                    arguments[i] = migratedValue;
                }
            }
            FrameAccess.iterateStackObjectsWithReplacement(initialFrame, true, stackValue -> migrate(fromToMap, stackValue));
            final Object migratedSender = migrate(fromToMap, FrameAccess.getSender(initialFrame));
            if (migratedSender != null) {
                setSender(migratedSender);
            }
            final Object migratedCode = migrate(fromToMap, FrameAccess.getCodeObject(initialFrame));
            final Object migratedClosure = migrate(fromToMap, FrameAccess.getClosure(initialFrame));
            if (migratedCode instanceof final CompiledCodeObject o) {
                overwriteCodeObject(o);
            }
            if (migratedClosure instanceof final BlockClosureObject o) {
                setClosure(o);
            }
        } else if (hasNonExecutableState()) {
            /* Intentionally skips stack pointer since it can only be nil or a non-negative integer. */
            final NonExecutableContextState state = getNonExecutableState();
            boolean structuralSlotChanged = false;
            final Object migratedSender = migrate(fromToMap, state.sender);
            if (migratedSender != null) {
                state.sender = migratedSender;
                structuralSlotChanged = true;
            }
            final Object migratedInstructionPointer = migrate(fromToMap, state.instructionPointer);
            if (migratedInstructionPointer != null) {
                state.instructionPointer = migratedInstructionPointer;
                structuralSlotChanged = true;
            }
            final Object migratedMethod = migrate(fromToMap, state.method);
            if (migratedMethod != null) {
                state.method = migratedMethod;
                structuralSlotChanged = true;
            }
            final Object migratedClosureOrNil = migrate(fromToMap, state.closureOrNil);
            if (migratedClosureOrNil != null) {
                state.closureOrNil = migratedClosureOrNil;
                structuralSlotChanged = true;
            }
            final Object migratedReceiver = migrate(fromToMap, state.receiver);
            if (migratedReceiver != null) {
                state.receiver = migratedReceiver;
            }
            if (state.stackOrSize instanceof Object[] stack) {
                final int limit = Math.min(getStackPointerOrZero(), stack.length);
                for (int i = 0; i < limit; i++) {
                    final Object migratedStackValue = migrate(fromToMap, stack[i]);
                    if (migratedStackValue != null) {
                        stack[i] = migratedStackValue;
                    }
                }
                Arrays.fill(stack, limit, stack.length, null);
            }
            if (structuralSlotChanged) {
                tryBindToFrame(false);
            }
        } else if (senderOrFrameOrState != null) {
            // Migrate the lightweight sender state
            final Object migratedSender = migrate(fromToMap, senderOrFrameOrState);
            if (migratedSender != null) {
                senderOrFrameOrState = migratedSender;
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        if (hasTruffleFrame()) {
            final MaterializedFrame frame = getTruffleFrame();
            tracer.addIfUnmarked(FrameAccess.getCodeObject(frame));
            tracer.addAllIfUnmarked(frame.getArguments());
            FrameAccess.iterateStackObjects(frame, tracer.frameHandling, tracer::addIfUnmarked);
        } else if (hasNonExecutableState()) {
            getNonExecutableState().tracePointers(tracer::addIfUnmarked, getStackPointerOrZero());
        } else {
            tracer.addIfUnmarked(senderOrFrameOrState);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (hasNonExecutableState()) {
            getNonExecutableState().tracePointers(writer::traceIfNecessary, getStackPointerOrZero());
        } else {
            if (!hasTruffleFrame()) {
                materializeFromFrames();
            }
            final MaterializedFrame frame = getTruffleFrame();
            getSender(); /* May materialize sender. */
            writer.traceIfNecessary(FrameAccess.getCodeObject(frame));
            writer.traceAllIfNecessary(frame.getArguments());
            FrameAccess.iterateStackObjects(frame, FrameHandling.SCRUB, writer::traceIfNecessary);
        }
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ContextObject must have slots:", this);
        }

        if (hasNonExecutableState()) {
            final NonExecutableContextState state = getNonExecutableState();
            writer.writeObject(resolveRawValue(state.sender));
            writer.writeObject(resolveRawValue(state.instructionPointer));
            writer.writeObject(resolveRawValue(state.stackPointer));
            writer.writeObject(resolveRawValue(state.method));
            writer.writeObject(resolveRawValue(state.closureOrNil));
            writer.writeObject(resolveRawValue(state.receiver));

            final int contextSize = state.getSize();
            final int sp = getStackPointerOrZero();
            for (int i = 0; i < contextSize; i++) {
                writer.writeObject(i < sp ? state.getStackValueOrNil(i) : NilObject.SINGLETON);
            }
        } else {
            if (!hasTruffleFrame()) {
                materializeFromFrames();
            }
            writer.writeObject(getSender());
            writer.writeObject(getInstructionPointer(InlinedConditionProfile.getUncached(), null));
            writer.writeSmallInteger(getStackPointer());
            writer.writeObject(getCodeObject());
            writer.writeObject(NilObject.nullToNil(getClosure()));
            final MaterializedFrame frame = getTruffleFrame();
            final Object[] args = frame.getArguments();
            final int numArgs = FrameAccess.getNumArguments(frame);
            // Write receiver and arguments
            for (int i = 0; i < 1 + numArgs; i++) {
                writer.writeObject(args[FrameAccess.getReceiverStartIndex() + i]);
            }
            // Write stack values from frame slots
            final int numSlots = FrameAccess.getNumStackSlots(frame);
            for (int i = numArgs; i < numSlots; i++) {
                final Object stackValue = FrameAccess.getStackValue(frame, i);
                if (stackValue == null) {
                    writer.writeNil();
                } else {
                    writer.writeObject(stackValue);
                }
            }
            final int contextSize = getCodeObject().getSqueakContextSize();
            for (int i = numSlots; i < contextSize; i++) {
                writer.writeNil();
            }
        }
    }
}

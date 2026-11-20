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

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.ActiveContextDematerializationException;
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
     * 1. {@link AbstractSqueakObject} (Sender): The lightweight wrapper state. When a context is
     *    actively executing in a {@link VirtualFrame}, we avoid allocating a MaterializedFrame.
     *    Instead, we simply store its sender here until materialization is forced.
     * <p>
     * 2. {@link MaterializedFrame}: The fast-path execution state. Used for valid, suspended
     *    contexts on the heap. This allows GraalVM to execute and resume them. Note that stack
     *    slots above {@code stackPointer} are not retained.
     * <p>
     * 3. {@link ContextProxy}: The slow-path fallback state. Used for newly allocated empty
     *    contexts or contexts that have been reflectively mutated with incompatible values,
     *    making them invalid Truffle frames.
     * <p>
     * 4. {@link SqueakImageChunk}: A transient bootstrap state used exclusively during image load.
     */
    private Object senderOrFrameOrProxy;

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
     * incompatible values (such as a non-{@link CompiledCodeObject} method, an out-of-range
     * instruction/stack pointer, or an immediate/non-{@link AbstractSqueakObject} sender).
     * <p>
     * Any {@code null} field (or unallocated stack slot) represents an unset Squeak slot and is
     * treated as {@link NilObject#SINGLETON} on access. {@link #stackOrSize} initially stores the
     * context's variable slot count as a boxed {@link Integer} to avoid allocating an
     * {@code Object[]} on read-only operations, expanding lazily on the first stack write. Setters
     * for structural slots automatically re-materialize the context into a
     * {@link MaterializedFrame} once all invariants are satisfied.
     */
    private static final class ContextProxy {
        private Object sender;
        private Object instructionPointer;
        private Object stackPointer;
        private Object method;
        private Object closureOrNil;
        private Object receiver;
        private Object stackOrSize;

        private ContextProxy(final int size) {
            stackOrSize = size;
        }

        private ContextProxy(final ContextProxy original) {
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
                return resolveProxyValue(stack[index]);
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
        senderOrFrameOrProxy = chunk;
    }

    public ContextObject(final int size) {
        super();
        senderOrFrameOrProxy = new ContextProxy(size);
        assert size == CONTEXT.SMALL_FRAMESIZE || size == CONTEXT.LARGE_FRAMESIZE || size == CONTEXT.HUGE_FRAMESIZE;
    }

    private ContextObject(final VirtualFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrProxy = FrameAccess.getSender(frame);
        FrameAccess.setContext(frame, this);
    }

    private ContextObject(final MaterializedFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrProxy = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
    }

    @TruffleBoundary
    private ContextObject(final ContextObject original) {
        super(original);
        if (original.hasContextProxy()) {
            setAllBooleanBits(original.getAllBooleanBits());
            senderOrFrameOrProxy = new ContextProxy(original.getProxy());
        } else {
            if (!original.hasTruffleFrame()) {
                assert original.senderOrFrameOrProxy instanceof AbstractSqueakObject;
                original.materializeFromFrames();
            }
            // Copy modified sender flag and the marked code flags after ensuring original is materialized.
            setAllBooleanBits(original.getAllBooleanBits());
            // Create shallow copy of Truffle frame.
            final MaterializedFrame originalFrame = original.getTruffleFrame();
            final FrameDescriptor frameDescriptor = originalFrame.getFrameDescriptor();
            final MaterializedFrame copyFrame = Truffle.getRuntime().createMaterializedFrame(originalFrame.getArguments().clone(), frameDescriptor);
            senderOrFrameOrProxy = copyFrame;
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
        fillinAsProxy(chunk);

        // During image load, the closure shell must be explicitly filled in
        // before materialization so we can access its CompiledBlock.
        final ContextProxy proxy = getProxy();
        if (proxy.closureOrNil instanceof BlockClosureObject closure &&
                        proxy.method instanceof CompiledCodeObject code &&
                        code.isCompiledMethod()) {
            closure.fillin(chunk.getChunk(CONTEXT.CLOSURE_OR_NIL));
        }

        // ToDo: A Context that was a proxy because its stack pointer was not an integer,
        // but was legal in all other ways, will lose the stack pointer value here.
        materializeFromProxy(true);
    }

    private void fillinAsProxy(final SqueakImageChunk chunk) {
        final int stackSize = chunk.getWordSize() - CONTEXT.TEMP_FRAME_START;
        final ContextProxy proxy = new ContextProxy(stackSize);

        // Read the exact object pointers directly from the chunk without casting
        proxy.sender = chunk.getPointer(CONTEXT.SENDER_OR_NIL);
        proxy.instructionPointer = chunk.getPointer(CONTEXT.INSTRUCTION_POINTER);
        proxy.stackPointer = chunk.getPointer(CONTEXT.STACKPOINTER);
        proxy.method = chunk.getPointer(CONTEXT.METHOD);
        proxy.closureOrNil = chunk.getPointer(CONTEXT.CLOSURE_OR_NIL);
        proxy.receiver = chunk.getPointer(CONTEXT.RECEIVER);

        // Map every variable-length stack slot from the chunk into the proxy array
        final Object[] stack = proxy.getOrCreateStack();
        for (int i = 0; i < stackSize; i++) {
            stack[i] = chunk.getPointer(CONTEXT.TEMP_FRAME_START + i);
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert senderOrFrameOrProxy == chunk;
        senderOrFrameOrProxy = proxy;
    }

    public CompiledCodeObject getMethodFromChunk() {
        if (senderOrFrameOrProxy instanceof final SqueakImageChunk chunk) {
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
            if (hasContextProxy()) {
                if (materializeFromProxy(true)) {
                    return getTruffleFrame();
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw SqueakException.create("Cannot materialize a structurally invalid ContextProxy!");
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
        senderOrFrameOrProxy = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
        return frame;
    }

    public AbstractSqueakObject getFrameSender() {
        if (hasTruffleFrame()) {
            return FrameAccess.getSender(getTruffleFrame());
        } else if (senderOrFrameOrProxy instanceof AbstractSqueakObject o) {
            return o;
        } else {
            return getFrameSenderFallback();
        }
    }

    @TruffleBoundary
    private AbstractSqueakObject getFrameSenderFallback() {
        if (hasContextProxy()) {
            return resolveProxyValue(getProxy().sender) instanceof AbstractSqueakObject o ? o : NilObject.SINGLETON;
        }
        throw CompilerDirectives.shouldNotReachHere("Unexpected senderOrFrameOrProxy state: " + senderOrFrameOrProxy);
    }

    public AbstractSqueakObject getSender() {
        final AbstractSqueakObject sender;
        if (hasTruffleFrame()) {
            sender = FrameAccess.getSender(getTruffleFrame());
        } else if (senderOrFrameOrProxy instanceof ContextProxy proxy) {
            sender = resolveProxyValue(proxy.sender) instanceof AbstractSqueakObject o ? o : NilObject.SINGLETON;
        } else {
            sender = (AbstractSqueakObject) senderOrFrameOrProxy;
        }

        if (sender instanceof final ContextObject senderContext && !senderContext.hasTruffleFrame() && !senderContext.hasContextProxy()) {
            senderContext.materializeFromFrames();
        }
        return sender;
    }

    @TruffleBoundary
    public void materializeFromFrames() {
        senderOrFrameOrProxy = FrameAccess.findFrameForContext(this);
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
    public void setSender(final AbstractSqueakObject value) {
        if (!hasModifiedSender() && hasTruffleFrame() && FrameAccess.getSender(getTruffleFrame()) != value) {
            setModifiedSender();
        }
        setSenderUnsafe(value);
    }

    public void setSenderUnsafe(final AbstractSqueakObject value) {
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
        } else if (hasContextProxy()) {
            final Object sp = getProxyStackPointer();
            return sp instanceof Long l && 0 <= l && l <= Integer.MAX_VALUE ? (int) (long) l : 0;
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
        return FrameAccess.isDead(getTruffleFrame());
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
        final AbstractSqueakObject sender = getFrameSender();
        if (sender instanceof ContextObject senderContext) {
            if (senderContext.hasContextProxy()) {
                return false;
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
        } else if (hasContextProxy() && getProxyMethod() instanceof CompiledCodeObject code) {
            final boolean hasClosure = getProxyClosureOrNil() instanceof BlockClosureObject;
            return formatContext("CTX (proxy)", code, hasClosure);
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
        if (senderOrFrameOrProxy instanceof final ContextProxy proxy) {
            return proxy.getSize();
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
        // If it is a proxy, physical size limits apply.
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
        final Object otherSenderOrFrame = other.senderOrFrameOrProxy;
        final int otherBooleans = other.getAllBooleanBits();
        other.setFields(senderOrFrameOrProxy, getAllBooleanBits());
        setFields(otherSenderOrFrame, otherBooleans);
    }

    private void setFields(final Object otherSenderOrFrame, final int otherBooleanBits) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        senderOrFrameOrProxy = otherSenderOrFrame;
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
        return (MaterializedFrame) CompilerDirectives.castExact(senderOrFrameOrProxy, CONCRETE_MATERIALIZED_FRAME_CLASS);
    }

    public boolean hasTruffleFrame() {
        return senderOrFrameOrProxy instanceof MaterializedFrame;
    }

    public void setTruffleFrame(final MaterializedFrame frame) {
        assert !hasTruffleFrame();
        senderOrFrameOrProxy = frame;
    }

    /**
     *  Proxy Accessing.
     */

    public boolean hasContextProxy() {
        return senderOrFrameOrProxy instanceof ContextProxy;
    }

    private ContextProxy getProxy() {
        assert hasContextProxy();
        return (ContextProxy) senderOrFrameOrProxy;
    }

    @TruffleBoundary
    private boolean materializeFromProxy(final boolean forced) {
        final ContextProxy proxy = (ContextProxy) senderOrFrameOrProxy;

        final Object methodObj = resolveProxyValue(proxy.method);
        final Object closureOrNil = resolveProxyValue(proxy.closureOrNil);
        final Object senderObj = resolveProxyValue(proxy.sender);

        if (!hasValidProxyStructure(methodObj, closureOrNil, senderObj)) {
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
        final int relativePC = determineInstructionPointer(resolveProxyValue(proxy.instructionPointer), methodOrBlock);
        if (relativePC == INVALID_PC) {
            return false;
        }

        // Evaluate stack pointer bounds before allocating or installing the frame
        final int numStackSlots = methodOrBlock.getFrameDescriptor().getNumberOfSlots() - FrameAccess.getStackStart();
        final int sp = determineStackPointer(resolveProxyValue(proxy.stackPointer), forced, numStackSlots);
        if (sp < 0) {
            return false;
        }

        // Allocate and apply state
        final Object[] receiverAndArgs = gatherReceiverAndArgs(proxy, numArgs);
        final Object[] frameArguments = FrameAccess.newWith((AbstractSqueakObject) senderObj, closure, receiverAndArgs);
        installTruffleFrame(frameArguments, methodOrBlock.getFrameDescriptor(), relativePC, sp);
        restoreStack(proxy, sp);

        return true;
    }

    private static boolean hasValidProxyStructure(final Object method, final Object closureOrNil, final Object sender) {
        if (!(method instanceof CompiledCodeObject)) {
            return false; // Cannot materialize without a structurally valid method
        }
        if (closureOrNil != NilObject.SINGLETON && !(closureOrNil instanceof BlockClosureObject)) {
            return false; // Structurally invalid closure
        }
        return sender instanceof AbstractSqueakObject; // Structurally invalid sender (e.g., primitive Long)
    }

    private static Object[] gatherReceiverAndArgs(final ContextProxy proxy, final int numArgs) {
        final Object[] receiverAndArgs = new Object[1 + numArgs];
        receiverAndArgs[0] = resolveProxyValue(proxy.receiver);
        for (int i = 0; i < numArgs; i++) {
            receiverAndArgs[1 + i] = proxy.getStackValueOrNil(i);
        }
        return receiverAndArgs;
    }

    private static int determineInstructionPointer(final Object pc, final CompiledCodeObject methodOrBlock) {
        if (pc == NilObject.SINGLETON) {
            return NIL_PC_STACK_NOT_NIL_VALUE;
        } else if (pc instanceof Long l) {
            final long offset = l - methodOrBlock.getInitialPC();
            if (offset < NIL_PC_THRESHOLD || offset > Integer.MAX_VALUE) {
                return INVALID_PC; // Keep as proxy if PC cannot be represented in a Truffle frame
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
        return -1; // Stay a proxy
    }

    private void restoreStack(final ContextProxy proxy, final int sp) {
        /*
         * Only live stack slots in [0, sp) are copied; any values written above sp while in proxy
         * form are discarded upon materialization (Context>>#stackp: nils those slots anyway).
         */
        for (int i = 0; i < sp; i++) {
            atTempPut(i, proxy.getStackValueOrNil(i));
        }
    }

    /**
     * Gracefully degrades a MaterializedFrame into a ContextProxy.
     * This is triggered when Squeak reflection attempts to write structurally
     * invalid types (e.g., Strings instead of CompiledCodeObjects) into critical slots.
     */
    public void dematerializeToProxy() {
        assert hasTruffleFrame();

        // ToDo: Ensure primitives only operate on heap-based Contexts.
        if (isPotentiallyActiveOnTruffleStack() && isActuallyActiveOnTruffleStack()) {
            CompilerDirectives.transferToInterpreter();
            throw new ActiveContextDematerializationException();
        }

        // Extract all state from the Truffle frame using the existing slow-path getters
        final int sp = getStackPointer();
        final ContextProxy proxy = new ContextProxy(getCodeObject().getSqueakContextSize());
        proxy.sender = getSender();
        proxy.instructionPointer = getInstructionPointer(InlinedConditionProfile.getUncached(), null);
        proxy.stackPointer = (long) sp;
        proxy.method = getCodeObject();
        proxy.closureOrNil = NilObject.nullToNil(getClosure());
        proxy.receiver = getReceiver();

        // Populate the proxy's array with the Truffle frame's stack
        final Object[] stack = proxy.getOrCreateStack();
        for (int i = 0; i < sp; i++) {
            stack[i] = atTemp(i);
        }

        // Sever the connection to the Truffle frame
        senderOrFrameOrProxy = proxy;
        resetMarkedCodeFlags();
    }

    private static Object resolveProxyValue(final Object value) {
        return value == null ? NilObject.SINGLETON : AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(value);
    }

    public Object getProxySender() {
        return resolveProxyValue(getProxy().sender);
    }

    public Object getProxyInstructionPointer() {
        return resolveProxyValue(getProxy().instructionPointer);
    }

    public Object getProxyStackPointer() {
        return resolveProxyValue(getProxy().stackPointer);
    }

    public Object getProxyMethod() {
        return resolveProxyValue(getProxy().method);
    }

    public Object getProxyClosureOrNil() {
        return resolveProxyValue(getProxy().closureOrNil);
    }

    public Object getProxyReceiver() {
        return resolveProxyValue(getProxy().receiver);
    }

    public Object getProxyTemp(final int index) {
        assert index >= 0 : "Temp index cannot be negative: " + index;
        return getProxy().getStackValueOrNil(index);
    }

    public void setProxySender(final Object value) {
        getProxy().sender = value;
        materializeFromProxy(false);
    }

    public void setProxyInstructionPointer(final Object value) {
        getProxy().instructionPointer = value;
        materializeFromProxy(false);
    }

    public void setProxyStackPointer(final Object value) {
        getProxy().stackPointer = value;
        materializeFromProxy(false);
    }

    public void setProxyMethod(final Object value) {
        getProxy().method = value;
        materializeFromProxy(false);
    }

    public void setProxyClosureOrNil(final Object value) {
        getProxy().closureOrNil = value;
        materializeFromProxy(false);
    }

    public void setProxyReceiver(final Object value) {
        /* Setting the receiver cannot result in a proxy becoming materializable. */
        getProxy().receiver = value;
    }

    public void setProxyTemp(final int index, final Object value) {
        /* Setting a value on the stack cannot result in a proxy becoming materializable. */
        assert index >= 0 : "Temp index cannot be negative: " + index;
        final Object[] stack = getProxy().getOrCreateStack();
        if (index < stack.length) {
            stack[index] = value;
        }
    }

    @TruffleBoundary
    public boolean pointsTo(final Object thang) {
        if (hasContextProxy()) {
            final int stackPointer = getStackPointerOrZero();
            if (getProxySender() == thang ||
                            thang.equals(getProxyInstructionPointer()) ||
                            thang.equals(getProxyStackPointer()) ||
                            getProxyMethod() == thang ||
                            getProxyClosureOrNil() == thang ||
                            getProxyReceiver() == thang) {
                return true;
            }
            final ContextProxy proxy = getProxy();
            for (int i = 0; i < stackPointer; i++) {
                if (proxy.getStackValueOrNil(i) == thang) {
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
            if (migrate(fromToMap, FrameAccess.getSender(initialFrame)) instanceof final AbstractSqueakObject o) {
                setSender(o);
            }
            final Object migratedCode = migrate(fromToMap, FrameAccess.getCodeObject(initialFrame));
            final Object migratedClosure = migrate(fromToMap, FrameAccess.getClosure(initialFrame));
            if (migratedCode instanceof final CompiledCodeObject o) {
                overwriteCodeObject(o);
            }
            if (migratedClosure instanceof final BlockClosureObject o) {
                setClosure(o);
            }
        } else if (hasContextProxy()) {
            /* Intentionally skips stack pointer since it can only be nil or a non-negative integer. */
            final ContextProxy proxy = getProxy();
            boolean structuralSlotChanged = false;
            final Object migratedSender = migrate(fromToMap, proxy.sender);
            if (migratedSender != null) {
                proxy.sender = migratedSender;
                structuralSlotChanged = true;
            }
            final Object migratedInstructionPointer = migrate(fromToMap, proxy.instructionPointer);
            if (migratedInstructionPointer != null) {
                proxy.instructionPointer = migratedInstructionPointer;
                structuralSlotChanged = true;
            }
            final Object migratedMethod = migrate(fromToMap, proxy.method);
            if (migratedMethod != null) {
                proxy.method = migratedMethod;
                structuralSlotChanged = true;
            }
            final Object migratedClosureOrNil = migrate(fromToMap, proxy.closureOrNil);
            if (migratedClosureOrNil != null) {
                proxy.closureOrNil = migratedClosureOrNil;
                structuralSlotChanged = true;
            }
            final Object migratedReceiver = migrate(fromToMap, proxy.receiver);
            if (migratedReceiver != null) {
                proxy.receiver = migratedReceiver;
            }
            if (proxy.stackOrSize instanceof Object[] stack) {
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
                materializeFromProxy(false);
            }
        } else if (senderOrFrameOrProxy != null) {
            // Migrate the lightweight sender state
            if (migrate(fromToMap, senderOrFrameOrProxy) instanceof final AbstractSqueakObject o) {
                senderOrFrameOrProxy = o;
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
        } else if (hasContextProxy()) {
            getProxy().tracePointers(tracer::addIfUnmarked, getStackPointerOrZero());
        } else {
            tracer.addIfUnmarked(senderOrFrameOrProxy);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (hasContextProxy()) {
            getProxy().tracePointers(writer::traceIfNecessary, getStackPointerOrZero());
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

        if (hasContextProxy()) {
            final ContextProxy proxy = getProxy();
            writer.writeObject(resolveProxyValue(proxy.sender));
            writer.writeObject(resolveProxyValue(proxy.instructionPointer));
            writer.writeObject(resolveProxyValue(proxy.stackPointer));
            writer.writeObject(resolveProxyValue(proxy.method));
            writer.writeObject(resolveProxyValue(proxy.closureOrNil));
            writer.writeObject(resolveProxyValue(proxy.receiver));

            final int contextSize = proxy.getSize();
            final int sp = getStackPointerOrZero();
            for (int i = 0; i < contextSize; i++) {
                writer.writeObject(i < sp ? proxy.getStackValueOrNil(i) : NilObject.SINGLETON);
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

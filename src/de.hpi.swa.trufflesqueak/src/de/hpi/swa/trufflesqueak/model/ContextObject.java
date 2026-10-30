/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

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
     *    contexts on the heap. This allows GraalVM to execute and resume them.
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

    public ContextObject(final VirtualFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrProxy = FrameAccess.getSender(frame);
        FrameAccess.setContext(frame, this);
    }

    public ContextObject(final MaterializedFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrProxy = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
    }

    @TruffleBoundary
    public ContextObject(final ContextObject original) {
        super(original);
        // Copy modified sender flag and the marked code flags.
        setAllBooleanBits(original.getAllBooleanBits());
        // Create shallow copy of Truffle frame
        final FrameDescriptor frameDescriptor = FrameAccess.getCodeObject(original.getTruffleFrame()).getFrameDescriptor();
        senderOrFrameOrProxy = Truffle.getRuntime().createMaterializedFrame(original.getTruffleFrame().getArguments().clone(), frameDescriptor);
        FrameAccess.copyAllSlots(original.getTruffleFrame(), getTruffleFrame());
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

    public CallTarget getCallTarget() {
        return getMethodOrBlock().getResumptionCallTarget(this);
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
            senderOrFrameOrProxy = createTruffleFrame(this);
            setMarkedCodeFlags();
        }
        return getTruffleFrame();
    }

    @TruffleBoundary
    private static MaterializedFrame createTruffleFrame(final ContextObject context) {
        // Method is unknown, use dummy frame instead
        final Object[] dummyArguments = FrameAccess.newWith(1);
        final CompiledCodeObject dummyMethod = SqueakImageContext.getSlow().dummyMethod;
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(dummyArguments, dummyMethod.getFrameDescriptor());
        FrameAccess.setContext(truffleFrame, context);
        FrameAccess.setInstructionPointer(truffleFrame, 0);
        FrameAccess.setStackPointer(truffleFrame, 0);
        return truffleFrame;
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
            final Object proxySender = getProxy().sender;
            return proxySender instanceof AbstractSqueakObject o ? o : NilObject.SINGLETON;
        }
        return (AbstractSqueakObject) senderOrFrameOrProxy;
    }

    public AbstractSqueakObject getSender() {
        final AbstractSqueakObject sender;
        if (hasTruffleFrame()) {
            sender = FrameAccess.getSender(getTruffleFrame());
        } else if (senderOrFrameOrProxy instanceof ContextProxy proxy) {
            sender = (AbstractSqueakObject) NilObject.nullToNil(proxy.sender);
        } else {
            sender = (AbstractSqueakObject) senderOrFrameOrProxy;
        }

        if (sender instanceof final ContextObject senderContext && !senderContext.hasTruffleFrame()) {
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
            return sp instanceof Long l ? MiscUtils.toIntExact(l) : 0;
        } else {
            return 0;
        }
    }

    public int getStackPointer() {
        return FrameAccess.getStackPointer(getTruffleFrame());
    }

    public void setStackPointer(final int value) {
        assert 0 <= value && value <= size() : value + " not between 0 and " + getCodeObject().getSqueakContextSize() + " in " + this;
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
        senderOrFrameOrProxy = createTruffleFrame(value);
        setMarkedCodeFlags();
    }

    @TruffleBoundary
    private MaterializedFrame createTruffleFrame(final CompiledCodeObject method) {
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
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, method.getFrameDescriptor());
        FrameAccess.setContext(truffleFrame, this);
        FrameAccess.setInstructionPointer(truffleFrame, instructionPointer);
        FrameAccess.setStackPointer(truffleFrame, stackPointer);
        return truffleFrame;
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
        final Object[] arguments = Arrays.copyOf(oldFrame.getArguments(), expectedFrameArgumentSize + numCopied);
        System.arraycopy(value.getCopiedValues(), 0, arguments, expectedFrameArgumentSize, numCopied);
        final FrameDescriptor frameDescriptor = value.getCompiledBlock().getFrameDescriptor();
        // Create and initialize new frame
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(arguments, frameDescriptor);
        senderOrFrameOrProxy = frame;
        setMarkedCodeFlags();
        FrameAccess.assertSenderNotNull(frame);
        FrameAccess.assertReceiverNotNull(frame);
        FrameAccess.setContext(frame, this);
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
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
            if (hasClosure()) {
                return "CTX [] in " + getCodeObject() + " @" + Integer.toHexString(hashCode());
            } else {
                return "CTX " + getCodeObject() + " @" + Integer.toHexString(hashCode());
            }
        } else {
            return "CTX without method @" + Integer.toHexString(hashCode());
        }
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
            return getCodeObject().getSqueakContextSize();
        }
    }

    public boolean isValidStackPointer(final long sp) {
        if (hasTruffleFrame()) {
            return sp >= 0 && sp <= FrameAccess.getNumStackSlots(getTruffleFrame());
        }
        // If it is a proxy, physical size limits apply.
        return sp >= 0 && sp <= size();
    }

    public boolean isValidTempIndex(final int tempIndex) {
        if (hasTruffleFrame()) {
            return tempIndex >= 0 && tempIndex < FrameAccess.getNumStackSlots(getTruffleFrame());
        }
        return tempIndex >= 0 && tempIndex < size();
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

        if (!hasValidProxyStructure(proxy)) {
            return false;
        }

        final CompiledCodeObject code = (CompiledCodeObject) proxy.method;
        final Object closureOrNil = NilObject.nullToNil(proxy.closureOrNil);

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

        // Gather receiver and arguments
        final Object[] proxyStack = proxy.getOrCreateStack();
        final Object[] receiverAndArgs = gatherReceiverAndArgs(proxy, numArgs, proxyStack);

        // Allocate Truffle Frame
        final AbstractSqueakObject sender = (AbstractSqueakObject) NilObject.nullToNil(proxy.sender);
        final Object[] frameArguments = FrameAccess.newWith(sender, closure, receiverAndArgs);
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(frameArguments, methodOrBlock.getFrameDescriptor());

        // Evaluate bounds
        final int sp = determineStackPointer(proxy, forced, FrameAccess.getNumStackSlots(frame));
        if (sp < 0) {
            return false;
        }

        // Apply state
        senderOrFrameOrProxy = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
        restorePointersAndStack(proxy, methodOrBlock, sp, proxyStack);

        return true;
    }

    private static boolean hasValidProxyStructure(final ContextProxy proxy) {
        if (!(proxy.method instanceof CompiledCodeObject)) {
            return false; // Cannot materialize without a structurally valid method
        }
        final Object closureOrNil = NilObject.nullToNil(proxy.closureOrNil);
        if (closureOrNil != NilObject.SINGLETON && !(closureOrNil instanceof BlockClosureObject)) {
            return false; // Structurally invalid closure
        }
        final Object pc = NilObject.nullToNil(proxy.instructionPointer);
        if (pc != NilObject.SINGLETON && !(pc instanceof Long)) {
            return false; // Structurally invalid instruction pointer
        }
        final Object sender = NilObject.nullToNil(proxy.sender);
        return sender instanceof AbstractSqueakObject; // Structurally invalid sender (e.g., primitive Long)
    }

    private static Object[] gatherReceiverAndArgs(final ContextProxy proxy, final int numArgs, final Object[] proxyStack) {
        final Object[] receiverAndArgs = new Object[1 + numArgs];
        receiverAndArgs[0] = NilObject.nullToNil(proxy.receiver);

        final int limit = Math.min(numArgs, proxyStack.length);

        // Map values that exist within the proxyStack bounds
        for (int i = 0; i < limit; i++) {
            receiverAndArgs[1 + i] = NilObject.nullToNil(proxyStack[i]);
        }

        // Pad any remaining arguments with NilObject
        for (int i = limit; i < numArgs; i++) {
            receiverAndArgs[1 + i] = NilObject.SINGLETON;
        }

        return receiverAndArgs;
    }

    private static int determineStackPointer(final ContextProxy proxy, final boolean forced, final int numStackSlots) {
        if (proxy.stackPointer instanceof Long l) {
            final int sp = MiscUtils.toIntExact(l);
            if (sp < 0 || sp > numStackSlots) {
                // ToDo: Alternatively, we could force SP to zero when forced
                return -1; // Refuse to materialize if SP out of bounds
            }
            return sp;
        } else if (forced) {
            return 0; // OSVM interprets non-integers as zero for execution/loading
        }
        return -1; // Stay a proxy
    }

    private void restorePointersAndStack(final ContextProxy proxy, final CompiledCodeObject methodOrBlock, final int sp, final Object[] proxyStack) {
        final Object pc = NilObject.nullToNil(proxy.instructionPointer);
        if (pc == NilObject.SINGLETON) {
            removeInstructionPointer();
        } else if (pc instanceof Long l) {
            setInstructionPointer(MiscUtils.toIntExact(l) - methodOrBlock.getInitialPC());
        }

        setStackPointer(sp);

        final int limit = Math.min(sp, proxyStack.length);

        // Restore stack temps that exist within the proxyStack bounds
        for (int i = 0; i < limit; i++) {
            atTempPut(i, NilObject.nullToNil(proxyStack[i]));
        }

        // Pad any remaining stack pointers with NilObject
        for (int i = limit; i < sp; i++) {
            atTempPut(i, NilObject.SINGLETON);
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
            throw SqueakException.create("Fatal: Cannot structurally degrade a Context that is currently active on the Truffle execution stack.");
        }

        // Extract all state from the Truffle frame using the existing slow-path getters
        final int sp = getStackPointer();
        final ContextProxy proxy = new ContextProxy(getCodeObject().getSqueakContextSize());
        proxy.sender = getSender();
        proxy.instructionPointer = getInstructionPointer(InlinedConditionProfile.getUncached(), null);
        proxy.stackPointer = (long) sp;
        proxy.method = getCodeObject();
        proxy.closureOrNil = getClosure();
        proxy.receiver = getReceiver();

        // Populate the proxy's array with the Truffle frame's stack
        final Object[] stack = proxy.getOrCreateStack();
        for (int i = 0; i < sp; i++) {
            stack[i] = atTemp(i);
        }

        // Sever the connection to the Truffle frame
        senderOrFrameOrProxy = proxy;
    }

    public Object getProxySender() {
        return NilObject.nullToNil(getProxy().sender);
    }

    public Object getProxyInstructionPointer() {
        return NilObject.nullToNil(getProxy().instructionPointer);
    }

    public Object getProxyStackPointer() {
        return NilObject.nullToNil(getProxy().stackPointer);
    }

    public Object getProxyMethod() {
        return NilObject.nullToNil(getProxy().method);
    }

    public Object getProxyClosureOrNil() {
        return NilObject.nullToNil(getProxy().closureOrNil);
    }

    public Object getProxyReceiver() {
        return NilObject.nullToNil(getProxy().receiver);
    }

    public Object getProxyTemp(final int index) {
        assert index >= 0 : "Temp index cannot be negative: " + index;
        final Object[] stack = getProxy().getOrCreateStack();
        return index < stack.length ? NilObject.nullToNil(stack[index]) : NilObject.SINGLETON;
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
        getProxy().receiver = value;
    }

    public void setProxyTemp(final int index, final Object value) {
        assert index >= 0 : "Temp index cannot be negative: " + index;
        final Object[] stack = getProxy().getOrCreateStack();
        if (index < stack.length) {
            stack[index] = value;
        }
    }

    @TruffleBoundary
    public boolean pointsTo(final Object thang) {
        // TODO: make sure this works correctly
        if (hasTruffleFrame()) {
            final int stackPointer = getStackPointer();
            if (getSender() == thang || thang.equals(getInstructionPointer(InlinedConditionProfile.getUncached(), null)) || thang.equals(stackPointer) || getCodeObject() == thang ||
                            getClosure() == thang ||
                            getReceiver() == thang) {
                return true;
            }
            for (int i = 0; i < stackPointer; i++) {
                if (atTemp(i) == thang) {
                    return true;
                }
            }
        } else if (hasContextProxy()) {
            final ContextProxy proxy = getProxy();
            if (proxy.sender == thang || thang.equals(proxy.instructionPointer) || thang.equals(proxy.stackPointer) || proxy.method == thang ||
                            proxy.closureOrNil == thang ||
                            proxy.receiver == thang) {
                return true;
            }
            if (proxy.stackOrSize instanceof Object[] stack) {
                final int sp = getStackPointerOrZero();
                final int limit = Math.min(sp, stack.length);
                for (int i = 0; i < limit; i++) {
                    if (stack[i] == thang) {
                        return true;
                    }
                }
            }
        } else if (senderOrFrameOrProxy == thang) {
            return true; // Lightweight sender state
        }
        return false;
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        if (hasTruffleFrame()) {
            final MaterializedFrame frame = getTruffleFrame();
            final CompiledCodeObject compiledCodeObject = FrameAccess.getCodeObject(frame);
            if (compiledCodeObject != null && fromToMap.get(compiledCodeObject) instanceof final CompiledCodeObject o) {
                overwriteCodeObject(o);
            }
            final AbstractSqueakObject sender = FrameAccess.getSender(frame);
            if (sender != null && fromToMap.get(sender) instanceof final ContextObject o) {
                setSender(o);
            }
            final Object closure = FrameAccess.getClosure(frame);
            if (closure != null && fromToMap.get(closure) instanceof final BlockClosureObject o) {
                setClosure(o);
            }
            final Object[] arguments = frame.getArguments();
            for (int i = FrameAccess.getReceiverStartIndex(); i < arguments.length; i++) {
                final Object argument = arguments[i];
                if (argument != null) {
                    final Object migratedValue = fromToMap.get(argument);
                    if (migratedValue != null) {
                        arguments[i] = migratedValue;
                    }
                }
            }
            FrameAccess.iterateStackObjectsWithReplacement(frame, true, stackValue -> {
                if (stackValue != null) {
                    return fromToMap.get(stackValue);
                } else {
                    return null;
                }
            });
        } else if (hasContextProxy()) {
            final ContextProxy proxy = getProxy();
            if (proxy.sender != null) {
                final Object migrated = fromToMap.get(proxy.sender);
                if (migrated != null) {
                    proxy.sender = migrated;
                }
            }
            if (proxy.instructionPointer != null) {
                final Object migrated = fromToMap.get(proxy.instructionPointer);
                if (migrated != null) {
                    proxy.instructionPointer = migrated;
                }
            }
            if (proxy.stackPointer != null) {
                final Object migrated = fromToMap.get(proxy.stackPointer);
                if (migrated != null) {
                    proxy.stackPointer = migrated;
                }
            }
            if (proxy.method != null) {
                final Object migrated = fromToMap.get(proxy.method);
                if (migrated != null) {
                    proxy.method = migrated;
                }
            }
            if (proxy.closureOrNil != null) {
                final Object migrated = fromToMap.get(proxy.closureOrNil);
                if (migrated != null) {
                    proxy.closureOrNil = migrated;
                }
            }
            if (proxy.receiver != null) {
                final Object migrated = fromToMap.get(proxy.receiver);
                if (migrated != null) {
                    proxy.receiver = migrated;
                }
            }
            if (proxy.stackOrSize instanceof Object[] stack) {
                for (int i = 0; i < stack.length; i++) {
                    final Object stackVal = stack[i];
                    if (stackVal != null) {
                        final Object migrated = fromToMap.get(stackVal);
                        if (migrated != null) {
                            stack[i] = migrated;
                        }
                    }
                }
            }
        } else if (senderOrFrameOrProxy != null) {
            // Migrate the lightweight sender state
            final Object migrated = fromToMap.get(senderOrFrameOrProxy);
            if (migrated != null) {
                senderOrFrameOrProxy = migrated;
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
            final ContextProxy proxy = getProxy();
            tracer.addIfUnmarked(proxy.sender);
            tracer.addIfUnmarked(proxy.instructionPointer);
            tracer.addIfUnmarked(proxy.method);
            tracer.addIfUnmarked(proxy.closureOrNil);
            tracer.addIfUnmarked(proxy.receiver);
            if (proxy.stackOrSize instanceof Object[] stack) {
                final int sp = Math.min(getStackPointerOrZero(), stack.length);
                for (int i = 0; i < sp; i++) {
                    tracer.addIfUnmarked(stack[i]);
                }
            }
        } else {
            tracer.addIfUnmarked(senderOrFrameOrProxy);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (hasTruffleFrame()) {
            final MaterializedFrame frame = getTruffleFrame();
            getSender(); /* May materialize sender. */
            writer.traceIfNecessary(FrameAccess.getCodeObject(frame));
            writer.traceAllIfNecessary(frame.getArguments());
            FrameAccess.iterateStackObjects(frame, FrameHandling.SCRUB, writer::traceIfNecessary);
        } else if (hasContextProxy()) {
            final ContextProxy proxy = (ContextProxy) senderOrFrameOrProxy;
            writer.traceIfNecessary(proxy.sender);
            writer.traceIfNecessary(proxy.instructionPointer);
            writer.traceIfNecessary(proxy.method);
            writer.traceIfNecessary(proxy.closureOrNil);
            writer.traceIfNecessary(proxy.receiver);
            if (proxy.stackOrSize instanceof Object[] stack) {
                final int sp = Math.min(getStackPointerOrZero(), stack.length);
                for (int i = 0; i < sp; i++) {
                    writer.traceIfNecessary(stack[i]);
                }
            }
        } else if (senderOrFrameOrProxy instanceof AbstractSqueakObject o) {
            writer.traceIfNecessary(o);
        }
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ContextObject must have slots:", this);
        }

        if (hasTruffleFrame()) {
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
        } else if (hasContextProxy()) {
            final ContextProxy proxy = (ContextProxy) senderOrFrameOrProxy;
            writer.writeObject(NilObject.nullToNil(proxy.sender));
            writer.writeObject(NilObject.nullToNil(proxy.instructionPointer));
            writer.writeObject(NilObject.nullToNil(proxy.stackPointer));
            writer.writeObject(NilObject.nullToNil(proxy.method));
            writer.writeObject(NilObject.nullToNil(proxy.closureOrNil));
            writer.writeObject(NilObject.nullToNil(proxy.receiver));

            final Object[] stack = proxy.stackOrSize instanceof Object[] s ? s : null;
            final int contextSize = proxy.getSize();
            final int sp = getStackPointerOrZero();

            for (int i = 0; i < contextSize; i++) {
                if (i < sp && stack != null && stack[i] != null) {
                    writer.writeObject(stack[i]);
                } else {
                    writer.writeNil();
                }
            }
        } else {
            writer.writeObject(getSender());
            writer.writeNil();
            writer.writeNil();
            writer.writeNil();
            writer.writeNil();
            writer.writeNil();
            final int contextSize = size();
            for (int i = 0; i < contextSize; i++) {
                writer.writeNil();
            }
        }
    }
}

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
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class ContextObject extends AbstractSqueakObjectWithHash {
    public static final int NIL_PC_THRESHOLD = 0;
    public static final int NIL_PC_STACK_NOT_NIL_VALUE = -1;
    public static final int NIL_PC_STACK_NIL_VALUE = -2;

    private static final Class<?> CONCRETE_MATERIALIZED_FRAME_CLASS = Truffle.getRuntime().createMaterializedFrame(new Object[0]).getClass();

    private Object senderOrFrameOrSize;

    public ContextObject(final SqueakImageChunk chunk) {
        super(chunk);
        senderOrFrameOrSize = chunk;
    }

    public ContextObject(final int size) {
        super();
        senderOrFrameOrSize = size;
        assert size == CONTEXT.SMALL_FRAMESIZE || size == CONTEXT.LARGE_FRAMESIZE || size == CONTEXT.HUGE_FRAMESIZE;
    }

    public ContextObject(final VirtualFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrSize = FrameAccess.getSender(frame);
        FrameAccess.setContext(frame, this);
    }

    public ContextObject(final MaterializedFrame frame) {
        super();
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrSize = frame;
        setMarkedCodeFlags();
        FrameAccess.setContext(frame, this);
    }

    public ContextObject(final CompiledCodeObject code, final MaterializedFrame frame) {
        super();
        CompilerAsserts.partialEvaluationConstant(code);
        FrameAccess.assertSenderNotNull(frame);
        senderOrFrameOrSize = frame;
        setMarkedCodeFlags(code);
        FrameAccess.setContext(frame, this);
    }

    @TruffleBoundary
    public ContextObject(final ContextObject original) {
        super(original);
        // Copy modified sender flag and the marked code flags.
        setAllBooleanBits(original.getAllBooleanBits());
        // Create shallow copy of Truffle frame
        final FrameDescriptor frameDescriptor = FrameAccess.getCodeObject(original.getTruffleFrame()).getFrameDescriptor();
        senderOrFrameOrSize = Truffle.getRuntime().createMaterializedFrame(original.getTruffleFrame().getArguments().clone(), frameDescriptor);
        FrameAccess.copyAllSlots(original.getTruffleFrame(), getTruffleFrame());
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        assert chunk.getWordSize() > CONTEXT.TEMP_FRAME_START;
        final CompiledCodeObject code = (CompiledCodeObject) chunk.getPointer(CONTEXT.METHOD);
        final AbstractSqueakObject sender = (AbstractSqueakObject) chunk.getPointer(CONTEXT.SENDER_OR_NIL);
        assert sender != null : "sender should not be null";
        final Object closureOrNil = chunk.getPointer(CONTEXT.CLOSURE_OR_NIL);
        final BlockClosureObject closure;
        final int numArgs;
        final CompiledCodeObject methodOrBlock;
        if (closureOrNil == NilObject.SINGLETON) {
            closure = null;
            methodOrBlock = code;
            numArgs = code.getNumArgs();
        } else {
            closure = (BlockClosureObject) closureOrNil;
            numArgs = closure.getNumArgs() + closure.getNumCopied();
            if (code.isCompiledMethod()) {
                closure.fillin(chunk.getChunk(CONTEXT.CLOSURE_OR_NIL));
                methodOrBlock = closure.getCompiledBlock();
            } else {
                assert closure.isAFullBlockClosure();
                methodOrBlock = code;
            }
        }
        final int endArguments = CONTEXT.TEMP_FRAME_START + numArgs;
        final Object[] arguments = chunk.getPointers(CONTEXT.RECEIVER, endArguments);
        final Object[] frameArguments = FrameAccess.newWith(sender, closure, arguments);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert senderOrFrameOrSize == chunk;
        senderOrFrameOrSize = Truffle.getRuntime().createMaterializedFrame(frameArguments, methodOrBlock.getFrameDescriptor());
        setMarkedCodeFlags();
        FrameAccess.setContext(getTruffleFrame(), this);
        final Object pc = chunk.getPointer(CONTEXT.INSTRUCTION_POINTER);
        if (pc == NilObject.SINGLETON) {
            removeInstructionPointer();
        } else {
            setInstructionPointer(MiscUtils.toIntExact((long) pc) - methodOrBlock.getInitialPC());
        }
        final int stackPointer = MiscUtils.toIntExact((long) chunk.getPointer(CONTEXT.STACKPOINTER));
        setStackPointer(stackPointer);
        for (int i = 0; i < stackPointer; i++) {
            atTempPut(i, chunk.getPointer(CONTEXT.TEMP_FRAME_START + i));
        }
    }

    public CompiledCodeObject getMethodFromChunk() {
        if (senderOrFrameOrSize instanceof final SqueakImageChunk chunk) {
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
            senderOrFrameOrSize = createTruffleFrame(this);
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
        } else {
            return (AbstractSqueakObject) senderOrFrameOrSize;
        }
    }

    public AbstractSqueakObject getSender() {
        final AbstractSqueakObject sender = getFrameSender();
        if (sender instanceof final ContextObject senderContext && !senderContext.hasTruffleFrame()) {
            senderContext.materializeFromFrames();
        }
        return sender;
    }

    @TruffleBoundary
    public void materializeFromFrames() {
        senderOrFrameOrSize = FrameAccess.findFrameForContext(this);
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
        setMarkedCodeFlags(getCodeObject());
    }

    private void setMarkedCodeFlags(final CompiledCodeObject code) {
        if (code.isUnwindMarked() && !hasClosure()) {
            setUnwindMarked();
        } else if (code.isExceptionHandlerMarked()) {
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

    public void setCodeObject(final CompiledCodeObject value) {
        senderOrFrameOrSize = createTruffleFrame(this, getTruffleFrame(), value);
        setMarkedCodeFlags();
    }

    public void overwriteCodeObject(final CompiledCodeObject value) {
        resetMarkedCodeFlags();
        setCodeObject(value);
    }

    @TruffleBoundary
    private static MaterializedFrame createTruffleFrame(final ContextObject context, final MaterializedFrame currentFrame, final CompiledCodeObject method) {
        final Object[] frameArguments;
        final int instructionPointer;
        final int stackPointer;
        if (currentFrame != null) {
            assert FrameAccess.getSender(currentFrame) != null : "Sender should not be null";
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
        FrameAccess.setContext(truffleFrame, context);
        FrameAccess.setInstructionPointer(truffleFrame, instructionPointer);
        FrameAccess.setStackPointer(truffleFrame, stackPointer);
        return truffleFrame;
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
        senderOrFrameOrSize = frame;
        setMarkedCodeFlags();
        FrameAccess.assertSenderNotNull(frame);
        FrameAccess.assertReceiverNotNull(frame);
        FrameAccess.setContext(frame, this);
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
        FrameAccess.setClosure(frame, value);
        // Cannot use copyTo here as frame descriptors may be different
        // ToDo: This does not handle any stack slots held in auxiliarySlots.
        FrameAccess.iterateStackSlots(oldFrame, slotIndex -> {
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

    public boolean canBeReturnedTo() {
        return !isDead() && getFrameSender() != NilObject.SINGLETON;
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
        return CONTEXT.INST_SIZE + getCodeObject().getSqueakContextSize();
    }

    @Override
    public int instsize() {
        return CONTEXT.INST_SIZE;
    }

    @Override
    public int size() {
        if (senderOrFrameOrSize instanceof final Integer size) {
            return size;
        } else {
            return getCodeObject().getSqueakContextSize();
        }
    }

    public void become(final ContextObject other) {
        final Object otherSenderOrFrame = other.senderOrFrameOrSize;
        final int otherBooleans = other.getAllBooleanBits();
        other.setFields(senderOrFrameOrSize, getAllBooleanBits());
        setFields(otherSenderOrFrame, otherBooleans);
    }

    private void setFields(final Object otherSenderOrFrame, final int otherBooleanBits) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        senderOrFrameOrSize = otherSenderOrFrame;
        setAllBooleanBits(otherBooleanBits);
    }

    public Object[] getReceiverAndNArguments() {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        return getReceiverAndNArguments(getCodeObject().getNumArgs());
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
        return (MaterializedFrame) CompilerDirectives.castExact(senderOrFrameOrSize, CONCRETE_MATERIALIZED_FRAME_CLASS);
    }

    public boolean hasTruffleFrame() {
        return senderOrFrameOrSize instanceof MaterializedFrame;
    }

    public void setTruffleFrame(final MaterializedFrame frame) {
        assert !hasTruffleFrame();
        senderOrFrameOrSize = frame;
    }

    // The context represents primitive call which needs to be skipped when unwinding call stack.
    public boolean isPrimitiveContext() {
        return !hasClosure() && getCodeObject().hasPrimitive() && getInstructionPointerForBytecodeLoop() == 0;
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
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        if (hasTruffleFrame()) {
            final MaterializedFrame frame = getTruffleFrame();
            tracer.addIfUnmarked(FrameAccess.getCodeObject(frame));
            tracer.addAllIfUnmarked(frame.getArguments());
            FrameAccess.iterateStackObjects(frame, true, tracer::addIfUnmarked);
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
            FrameAccess.iterateStackObjects(frame, true, writer::traceIfNecessary);
        }
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ContextObject must have slots:", this);
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
        // Write nil values for remaining stack values
        final int contextSize = getCodeObject().getSqueakContextSize();
        for (int i = numSlots; i < contextSize; i++) {
            writer.writeNil();
        }
        assert FrameAccess.hasUnusedAuxiliarySlots(frame) : "Auxiliary slots are used but not (yet) persisted";
    }
}

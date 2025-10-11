/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.image.SqueakImageReader;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class ContextObject extends AbstractSqueakObjectWithClassAndHash {
    public static final int NIL_PC_THRESHOLD = 0;
    public static final int NIL_PC_STACK_NOT_NIL_VALUE = -1;
    public static final int NIL_PC_STACK_NIL_VALUE = -2;

    private static final Class<?> CONCRETE_MATERIALIZED_FRAME_CLASS = Truffle.getRuntime().createMaterializedFrame(new Object[0]).getClass();

    private AbstractSqueakObject senderContextOrNil;
    private MaterializedFrame truffleFrame;
    private int size;
    private boolean hasModifiedSender;
    private boolean escaped;

    private ContextObject(final long header, final SqueakImageContext image) {
        super(header, image.methodContextClass);
        truffleFrame = null;
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        super(image, image.methodContextClass);
        truffleFrame = null;
        this.size = size;
    }

    private ContextObject(final SqueakImageContext image, final VirtualFrame frame) {
        super(image, image.methodContextClass);
        this.senderContextOrNil = FrameAccess.getSender(frame);
        this.truffleFrame = null;
        this.size = FrameAccess.getCodeObject(frame).getSqueakContextSize();
    }

    private ContextObject(final SqueakImageContext image, final MaterializedFrame truffleFrame, final int size) {
        super(image, image.methodContextClass);
        assert FrameAccess.getSender(truffleFrame) != null;
        assert FrameAccess.getContext(truffleFrame) == null;
        assert FrameAccess.getCodeObject(truffleFrame).getSqueakContextSize() == size;
        this.senderContextOrNil = FrameAccess.getSender(truffleFrame);
        this.truffleFrame = truffleFrame;
        this.size = size;
    }

    @TruffleBoundary
    private ContextObject(final ContextObject original) {
        super(original);

        hasModifiedSender = original.hasModifiedSender();
        senderContextOrNil = original.senderContextOrNil;
        escaped = original.escaped;
        size = original.size;
        // Create shallow copy of Truffle frame
        final FrameDescriptor frameDescriptor = FrameAccess.getCodeObject(original.truffleFrame).getFrameDescriptor();
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(original.truffleFrame.getArguments().clone(), frameDescriptor);
        FrameAccess.copyAllSlots(original.truffleFrame, truffleFrame);
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    public static ContextObject createWithHeader(final SqueakImageContext image, final long header) {
        return new ContextObject(header, image);
    }

    public static ContextObject create(final SqueakImageContext image, final VirtualFrame frame) {
        final ContextObject context = new ContextObject(image, frame);
        FrameAccess.setContext(frame, context);
        return context;
    }

    public static ContextObject create(final SqueakImageContext image, final FrameInstance frameInstance) {
        final Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
        return create(image, frame.materialize(), FrameAccess.getCodeObject(frame));
    }

    public static ContextObject create(final SqueakImageContext image, final MaterializedFrame frame, final CompiledCodeObject blockOrMethod) {
        final ContextObject context = new ContextObject(image, frame, blockOrMethod.getSqueakContextSize());
        FrameAccess.setContext(frame, context);
        return context;
    }

    /**
     * {@link ContextObject}s are filled in at a later stage by a
     * {@link SqueakImageReader#fillInContextObjects}.
     */
    @Override
    public void fillin(final SqueakImageChunk chunk) {
        // Do nothing.
    }

    public void fillinContext(final SqueakImageChunk chunk) {
        size = chunk.getWordSize();
        assert size > CONTEXT.TEMP_FRAME_START;
        final CompiledCodeObject method = (CompiledCodeObject) chunk.getPointer(CONTEXT.METHOD);
        final AbstractSqueakObject sender = (AbstractSqueakObject) chunk.getPointer(CONTEXT.SENDER_OR_NIL);
        assert sender != null : "sender should not be null";
        final Object closureOrNil = chunk.getPointer(CONTEXT.CLOSURE_OR_NIL);
        final BlockClosureObject closure;
        final int numArgs;
        final CompiledCodeObject methodOrBlock;
        if (closureOrNil == NilObject.SINGLETON) {
            closure = null;
            methodOrBlock = method;
            numArgs = method.getNumArgs();
        } else {
            closure = (BlockClosureObject) closureOrNil;
            numArgs = (int) (closure.getNumArgs() + closure.getNumCopied());
            if (method.isCompiledMethod()) {
                methodOrBlock = closure.getCompiledBlock(method);
            } else { // FullBlockClosure
                assert !closure.isABlockClosure(chunk.getImage()) && !method.isCompiledMethod();
                methodOrBlock = method;
            }
        }
        final int endArguments = CONTEXT.TEMP_FRAME_START + numArgs;
        final Object[] arguments = chunk.getPointers(CONTEXT.RECEIVER, endArguments);
        final Object[] frameArguments = FrameAccess.newWith(sender, closure, arguments);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, methodOrBlock.getFrameDescriptor());
        FrameAccess.setContext(truffleFrame, this);
        final Object pc = chunk.getPointer(CONTEXT.INSTRUCTION_POINTER);
        if (pc == NilObject.SINGLETON) {
            removeInstructionPointer();
        } else {
            setInstructionPointer(MiscUtils.toIntExact((long) pc));
        }
        final int stackPointer = MiscUtils.toIntExact((long) chunk.getPointer(CONTEXT.STACKPOINTER));
        setStackPointer(stackPointer);
        for (int i = 0; i < stackPointer; i++) {
            atTempPut(i, chunk.getPointer(CONTEXT.TEMP_FRAME_START + i));
        }
    }

    public CallTarget getCallTarget() {
        return getCodeObject().getResumptionCallTarget(this);
    }

    private MaterializedFrame getOrCreateTruffleFrame() {
        if (truffleFrame == null) {
            truffleFrame = createTruffleFrame(this);
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
        FrameAccess.setStackPointer(truffleFrame, 1);
        return truffleFrame;
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
            instructionPointer = method.getInitialPC();
            stackPointer = method.getNumTemps();
        }
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, method.getFrameDescriptor());
        FrameAccess.setContext(truffleFrame, context);
        FrameAccess.setInstructionPointer(truffleFrame, instructionPointer);
        FrameAccess.setStackPointer(truffleFrame, stackPointer);
        return truffleFrame;
    }

    public AbstractSqueakObject getFrameSender() {
        if (hasTruffleFrame()) {
            return FrameAccess.getSender(truffleFrame);
        } else {
            return senderContextOrNil;
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
        truffleFrame = FrameAccess.findFrameForContext(this);
// assert getCodeObject().getDoesNotNeedThisContextAssumption().isValid();
        getCodeObject().getDoesNotNeedThisContextAssumption().invalidate();
    }

    public AbstractSqueakObject getMaterializedSender() {
        return FrameAccess.getSender(getTruffleFrame());
    }

    public boolean isUnwindMarked() {
        return hasTruffleFrame() && !hasClosure() && getCodeObject().isUnwindMarked();
    }

    public boolean isExceptionHandlerMarked() {
        return hasTruffleFrame() && getCodeObject().isExceptionHandlerMarked();
    }

    /**
     * Sets the sender of a ContextObject.
     */
    public void setSender(final ContextObject value) {
        if (truffleFrame != null) {
            final Object sender = FrameAccess.getSender(getTruffleFrame());
            if (!hasModifiedSender && sender != value) {
                hasModifiedSender = true;
            }
        }
        setSenderUnsafe(value);
    }

    public void setNilSender() {
        if (truffleFrame != null) {
            final Object sender = FrameAccess.getSender(getTruffleFrame());
            if (!hasModifiedSender && sender != NilObject.SINGLETON) {
                hasModifiedSender = true;
            }
        }
        setSenderUnsafe(NilObject.SINGLETON);
    }

    public void setSenderUnsafe(final AbstractSqueakObject value) {
        senderContextOrNil = value;
        FrameAccess.setSender(getOrCreateTruffleFrame(), value);
    }

    public void removeSender() {
        if (hasModifiedSender) {
            hasModifiedSender = false;
        }
        setSenderUnsafe(NilObject.SINGLETON);
    }

    public Object getInstructionPointer(final InlinedConditionProfile nilProfile, final Node node) {
        final int pc = FrameAccess.getInstructionPointer(getTruffleFrame());
        if (nilProfile.profile(node, pc < NIL_PC_THRESHOLD)) {
            return NilObject.SINGLETON;
        } else {
            return (long) pc; // Must be a long.
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
        assert 0 <= value && value <= getCodeObject().getSqueakContextSize() : value + " not between 0 and " + getCodeObject().getSqueakContextSize() + " in " + this;
        FrameAccess.setStackPointer(getOrCreateTruffleFrame(), value);
    }

    private boolean hasMethod() {
        return hasTruffleFrame() && getCodeObject() != null;
    }

    public CompiledCodeObject getCodeObject() {
        return FrameAccess.getCodeObject(getTruffleFrame());
    }

    public void setCodeObject(final CompiledCodeObject value) {
        truffleFrame = createTruffleFrame(this, truffleFrame, value);
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
        final int numArgs = (int) value.getNumArgs();
        final int numCopied = value.getNumCopied();
        final int expectedFrameArgumentSize = FrameAccess.expectedArgumentSize(numArgs);
        final Object[] arguments = Arrays.copyOf(oldFrame.getArguments(), expectedFrameArgumentSize + numCopied);
        System.arraycopy(value.getCopiedValues(), 0, arguments, expectedFrameArgumentSize, numCopied);
        final FrameDescriptor frameDescriptor = value.getCompiledBlock().getFrameDescriptor();
        // Create and initialize new frame
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(arguments, frameDescriptor);
        FrameAccess.assertSenderNotNull(truffleFrame);
        FrameAccess.assertReceiverNotNull(truffleFrame);
        FrameAccess.setContext(truffleFrame, this);
        FrameAccess.setInstructionPointer(truffleFrame, pc);
        FrameAccess.setStackPointer(truffleFrame, sp);
        FrameAccess.setClosure(truffleFrame, value);
        // Cannot use copyTo here as frame descriptors may be different
        // ToDo: This does not handle any stack slots held in auxiliarySlots.
        FrameAccess.iterateStackSlots(oldFrame, slotIndex -> {
            final Object stackValue = oldFrame.getValue(slotIndex);
            if (stackValue != null) {
                FrameAccess.setSlot(truffleFrame, slotIndex, stackValue);
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
        final Object[] args = truffleFrame.getArguments();
        if (FrameAccess.getArgumentStartIndex() + index < args.length) {
            return args[FrameAccess.getArgumentStartIndex() + index];
        } else {
            return NilObject.nullToNil(FrameAccess.getSlotValue(truffleFrame, FrameAccess.toStackSlotIndex(truffleFrame, index)));
        }
    }

    @TruffleBoundary
    public void atTempPut(final int index, final Object value) {
        final Object[] args = getOrCreateTruffleFrame().getArguments();
        if (FrameAccess.getArgumentStartIndex() + index < args.length) {
            args[FrameAccess.getArgumentStartIndex() + index] = value;
        } else {
            FrameAccess.setStackSlot(truffleFrame, index, value);
        }
    }

    public void terminate() {
        removeInstructionPointer();
        removeSender();
        hasModifiedSender = false;
    }

    /* Context>>#isDead */
    public boolean isDead() {
        return FrameAccess.isDead(getTruffleFrame());
    }

    public boolean canBeReturnedTo() {
        return !isDead() && getFrameSender() != NilObject.SINGLETON;
    }

    public ContextObject shallowCopy() {
        return new ContextObject(this);
    }

    public boolean hasEscaped() {
        return escaped;
    }

    public void markEscaped() {
        escaped = true;
    }

    public void clearModifiedSender() {
        hasModifiedSender = false;
    }

    public boolean hasModifiedSender() {
        return hasModifiedSender;
    }

    public void push(final Object value) {
        assert value != null : "Unexpected `null` value";
        final int currentStackPointer = getStackPointer();
        assert currentStackPointer < CONTEXT.MAX_STACK_SIZE;
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
        return size;
    }

    public int getStackSize() {
        return getCodeObject().getSqueakContextSize();
    }

    public void become(final ContextObject other) {
        final MaterializedFrame otherTruffleFrame = other.truffleFrame;
        final int otherSize = other.size;
        final boolean otherHasModifiedSender = other.hasModifiedSender;
        final boolean otherEscaped = other.escaped;
        other.setFields(truffleFrame, size, hasModifiedSender, escaped);
        setFields(otherTruffleFrame, otherSize, otherHasModifiedSender, otherEscaped);
    }

    private void setFields(final MaterializedFrame otherTruffleFrame, final int otherSize, final boolean otherHasModifiedSender,
                    final boolean otherEscaped) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = otherTruffleFrame;
        size = otherSize;
        hasModifiedSender = otherHasModifiedSender;
        escaped = otherEscaped;
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
        return (MaterializedFrame) CompilerDirectives.castExact(truffleFrame, CONCRETE_MATERIALIZED_FRAME_CLASS);
    }

    public boolean hasTruffleFrame() {
        return truffleFrame != null;
    }

    public void setTruffleFrame(final MaterializedFrame frame) {
        assert truffleFrame == null;
        truffleFrame = frame;
    }

    // The context represents primitive call which needs to be skipped when unwinding call stack.
    public boolean isPrimitiveContext() {
        return !hasClosure() && getCodeObject().hasPrimitive() && getInstructionPointerForBytecodeLoop() == getCodeObject().getInitialPC();
    }

    @TruffleBoundary
    public boolean pointsTo(final Object thang) {
        // TODO: make sure this works correctly
        if (truffleFrame != null) {
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
        super.pointersBecomeOneWay(fromToMap);
        if (hasTruffleFrame()) {
            final CompiledCodeObject compiledCodeObject = FrameAccess.getCodeObject(truffleFrame);
            if (compiledCodeObject != null && fromToMap.get(compiledCodeObject) instanceof final CompiledCodeObject o) {
                setCodeObject(o);
            }
            final Object sender = FrameAccess.getSender(truffleFrame);
            if (sender != null && fromToMap.get(sender) instanceof final ContextObject o) {
                setSender(o);
            }
            final Object closure = FrameAccess.getClosure(truffleFrame);
            if (closure != null && fromToMap.get(closure) instanceof final BlockClosureObject o) {
                setClosure(o);
            }
            final Object[] arguments = truffleFrame.getArguments();
            for (int i = FrameAccess.getReceiverStartIndex(); i < arguments.length; i++) {
                final Object argument = arguments[i];
                if (argument != null) {
                    final Object migratedValue = fromToMap.get(argument);
                    if (migratedValue != null) {
                        arguments[i] = migratedValue;
                    }
                }
            }
            FrameAccess.iterateStackObjectsWithReplacement(truffleFrame, true, stackValue -> {
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
            tracer.addIfUnmarked(FrameAccess.getCodeObject(truffleFrame));
            tracer.addAllIfUnmarked(truffleFrame.getArguments());
            FrameAccess.iterateStackObjects(truffleFrame, true, tracer::addIfUnmarked);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (hasTruffleFrame()) {
            getSender(); /* May materialize sender. */
            writer.traceIfNecessary(FrameAccess.getCodeObject(truffleFrame));
            writer.traceAllIfNecessary(truffleFrame.getArguments());
            FrameAccess.iterateStackObjects(truffleFrame, true, writer::traceIfNecessary);
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
        final Object[] args = truffleFrame.getArguments();
        final int numArgs = FrameAccess.getNumArguments(truffleFrame);
        // Write receiver and arguments
        for (int i = 0; i < 1 + numArgs; i++) {
            writer.writeObject(args[FrameAccess.getReceiverStartIndex() + i]);
        }
        // Write remaining stack values
        final int numSlots = FrameAccess.getNumStackSlots(truffleFrame);
        final int contextSize = getCodeObject().getSqueakContextSize();
        for (int i = numArgs; i < contextSize; i++) {
            if (i < numSlots) { // stack value stored in frame slot
                final int slotIndex = FrameAccess.toStackSlotIndex(truffleFrame, i);
                final Object stackValue = truffleFrame.getValue(slotIndex);
                if (stackValue == null) {
                    writer.writeNil();
                } else {
                    writer.writeObject(stackValue);
                }
            } else { // beyond max stack depth stack values are nil
                writer.writeNil();
            }
        }
        assert FrameAccess.hasUnusedAuxiliarySlots(truffleFrame) : "Auxiliary slots are used but not (yet) persisted";
    }
}

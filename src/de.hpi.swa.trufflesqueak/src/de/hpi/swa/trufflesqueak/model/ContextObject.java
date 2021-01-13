/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageReader;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class ContextObject extends AbstractSqueakObjectWithClassAndHash {
    public static final int NIL_PC_VALUE = -1;
    private static final Class<?> CONCRETE_MATERIALIZED_FRAME_CLASS = Truffle.getRuntime().createMaterializedFrame(new Object[0]).getClass();

    private MaterializedFrame truffleFrame;
    private CompiledCodeObject methodOrBlock; // Code object holding truffleFrame's frame descriptor
    private PointersObject process;
    private int size;
    private boolean hasModifiedSender;
    private boolean escaped;

    private ContextObject(final SqueakImageContext image, final long hash) {
        super(image, hash, image.methodContextClass);
        truffleFrame = null;
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        super(image, image.methodContextClass);
        truffleFrame = null;
        this.size = size;
    }

    private ContextObject(final SqueakImageContext image, final MaterializedFrame truffleFrame, final int size) {
        super(image, image.methodContextClass);
        assert FrameAccess.getSender(truffleFrame) != null;
        assert FrameAccess.findContext(truffleFrame) == null;
        assert FrameAccess.getCodeObject(truffleFrame).getSqueakContextSize() == size;
        this.truffleFrame = truffleFrame;
        methodOrBlock = FrameAccess.getMethodOrBlock(truffleFrame);
        this.size = size;
    }

    @TruffleBoundary
    private ContextObject(final ContextObject original) {
        super(original);
        methodOrBlock = original.methodOrBlock;
        hasModifiedSender = original.hasModifiedSender();
        escaped = original.escaped;
        size = original.size;
        // Create shallow copy of Truffle frame
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(original.truffleFrame.getArguments().clone(), methodOrBlock.getFrameDescriptor());
        // Copy frame slot values
        FrameAccess.initializeMarker(truffleFrame, methodOrBlock);
        FrameAccess.setContext(truffleFrame, methodOrBlock, this);
        FrameAccess.setInstructionPointer(truffleFrame, methodOrBlock, FrameAccess.getInstructionPointer(original.truffleFrame, methodOrBlock));
        FrameAccess.setStackPointer(truffleFrame, methodOrBlock, FrameAccess.getStackPointer(original.truffleFrame, methodOrBlock));
        // Copy stack
        FrameAccess.iterateStackSlots(truffleFrame, slot -> {
            final Object value = original.truffleFrame.getValue(slot);
            if (value != null) {
                FrameAccess.setStackSlot(truffleFrame, slot, value);
            }
        });
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    public static ContextObject createWithHash(final SqueakImageContext image, final long hash) {
        return new ContextObject(image, hash);
    }

    public static ContextObject create(final SqueakImageContext image, final FrameInstance frameInstance) {
        final Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
        return create(image, frame.materialize(), FrameAccess.getCodeObject(frame));
    }

    public static ContextObject create(final SqueakImageContext image, final MaterializedFrame frame, final CompiledCodeObject blockOrMethod) {
        final ContextObject context = new ContextObject(image, frame, blockOrMethod.getSqueakContextSize());
        FrameAccess.setContext(frame, blockOrMethod, context);
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
        final Object[] pointers = chunk.getPointers();
        size = pointers.length;
        assert size > CONTEXT.TEMP_FRAME_START;
        final CompiledCodeObject method = (CompiledCodeObject) pointers[CONTEXT.METHOD];
        final AbstractSqueakObject sender = (AbstractSqueakObject) pointers[CONTEXT.SENDER_OR_NIL];
        assert sender != null : "sender should not be null";
        final Object closureOrNil = pointers[CONTEXT.CLOSURE_OR_NIL];
        final BlockClosureObject closure;
        final int numArgs;
        if (closureOrNil == NilObject.SINGLETON) {
            closure = null;
            methodOrBlock = method;
            numArgs = method.getNumArgs();
        } else {
            if (method.isCompiledMethod()) {
                closure = (BlockClosureObject) closureOrNil;
                methodOrBlock = closure.getCompiledBlock(method);
                numArgs = (int) (closure.getNumArgs() + closure.getNumCopied());
            } else { // FullBlockClosure
                closure = (BlockClosureObject) closureOrNil;
                assert !closure.isABlockClosure() && !method.isCompiledMethod();
                methodOrBlock = method;
                numArgs = method.getNumArgs();
            }
        }
        final int endArguments = CONTEXT.TEMP_FRAME_START + numArgs;
        final Object[] arguments = Arrays.copyOfRange(pointers, CONTEXT.RECEIVER, endArguments);
        final Object[] frameArguments = FrameAccess.newWith(methodOrBlock, sender, closure, arguments);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, methodOrBlock.getFrameDescriptor());
        FrameAccess.initializeMarker(truffleFrame, methodOrBlock);
        FrameAccess.setContext(truffleFrame, methodOrBlock, this);
        final Object pc = pointers[CONTEXT.INSTRUCTION_POINTER];
        if (pc == NilObject.SINGLETON) {
            removeInstructionPointer();
        } else {
            setInstructionPointer(MiscUtils.toIntExact((long) pc));
        }
        final int stackPointer = MiscUtils.toIntExact((long) pointers[CONTEXT.STACKPOINTER]);
        setStackPointer(stackPointer);
        for (int i = 0; i < stackPointer; i++) {
            atTempPut(i, pointers[CONTEXT.TEMP_FRAME_START + i]);
        }
    }

    public CallTarget getCallTarget() {
        return methodOrBlock.getResumptionCallTarget(this);
    }

    public CompiledCodeObject getMethodOrBlock() {
        return methodOrBlock;
    }

    public MaterializedFrame getOrCreateTruffleFrame() {
        if (truffleFrame == null) {
            truffleFrame = createTruffleFrame(this);
            methodOrBlock = FrameAccess.getMethodOrBlock(truffleFrame);
        }
        return getTruffleFrame();
    }

    private MaterializedFrame getOrCreateTruffleFrame(final CompiledCodeObject method) {
        if (truffleFrame == null || FrameAccess.getCodeObject(getTruffleFrame()) == null) {
            truffleFrame = createTruffleFrame(this, truffleFrame, method);
            methodOrBlock = method;
        }
        return getTruffleFrame();
    }

    @TruffleBoundary
    private static MaterializedFrame createTruffleFrame(final ContextObject context) {
        // Method is unknown, use dummy frame instead
        final Object[] dummyArguments = FrameAccess.newDummyWith(null, NilObject.SINGLETON, null, new Object[2]);
        final CompiledCodeObject dummyMethod = SqueakLanguage.getContext().dummyMethod;
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(dummyArguments, dummyMethod.getFrameDescriptor());
        FrameAccess.setContext(truffleFrame, dummyMethod, context);
        FrameAccess.setInstructionPointer(truffleFrame, dummyMethod, 0);
        FrameAccess.setStackPointer(truffleFrame, dummyMethod, 1);
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
            assert currentFrame.getFrameDescriptor().getSize() > 0;
            instructionPointer = FrameAccess.getInstructionPointer(currentFrame, method);
            stackPointer = FrameAccess.getStackPointer(currentFrame, method);
        } else {
            // Receiver plus arguments.
            final Object[] squeakArguments = new Object[1 + method.getNumArgs()];
            frameArguments = FrameAccess.newDummyWith(method, NilObject.SINGLETON, null, squeakArguments);
            instructionPointer = method.getInitialPC();
            stackPointer = method.getNumTemps();
        }
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, method.getFrameDescriptor());
        FrameAccess.initializeMarker(truffleFrame, method);
        FrameAccess.setContext(truffleFrame, method, context);
        FrameAccess.setInstructionPointer(truffleFrame, method, instructionPointer);
        FrameAccess.setStackPointer(truffleFrame, method, stackPointer);
        return truffleFrame;
    }

    @TruffleBoundary
    private static MaterializedFrame createTruffleFrame(final ContextObject context, final MaterializedFrame currentFrame, final BlockClosureObject closure) {
        final Object[] frameArguments;
        final CompiledCodeObject block = closure.getCompiledBlock();
        final int numArgsAndCopied = block.getNumArgs() + closure.getNumCopied();
        final int instructionPointer;
        final int stackPointer;
        if (currentFrame != null) {
            // FIXME: Assuming here this context is not active, add check?
            assert FrameAccess.getSender(currentFrame) != null : "Sender should not be null";

            final Object[] dummyArguments = currentFrame.getArguments();
            final int expectedArgumentSize = FrameAccess.expectedArgumentSize(numArgsAndCopied);
            if (dummyArguments.length != expectedArgumentSize) {
                // Adjust arguments.
                frameArguments = Arrays.copyOf(dummyArguments, expectedArgumentSize);
            } else {
                frameArguments = currentFrame.getArguments();
            }
            if (currentFrame.getFrameDescriptor().getSize() > 0) {
                instructionPointer = FrameAccess.getInstructionPointer(currentFrame, block);
                stackPointer = FrameAccess.getStackPointer(currentFrame, block);
            } else { // Frame slots unknown, so initialize PC and SP;
                instructionPointer = block.getInitialPC();
                stackPointer = numArgsAndCopied;
            }
        } else {
            // Receiver plus arguments.
            final Object[] squeakArguments = new Object[1 + numArgsAndCopied];
            frameArguments = FrameAccess.newDummyWith(block, NilObject.SINGLETON, closure, squeakArguments);
            instructionPointer = block.getInitialPC();
            stackPointer = numArgsAndCopied;
        }
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, block.getFrameDescriptor());
        FrameAccess.assertSenderNotNull(truffleFrame);
        FrameAccess.assertReceiverNotNull(truffleFrame);
        FrameAccess.initializeMarker(truffleFrame, block);
        FrameAccess.setContext(truffleFrame, block, context);
        FrameAccess.setInstructionPointer(truffleFrame, block, instructionPointer);
        FrameAccess.setStackPointer(truffleFrame, block, stackPointer);
        return truffleFrame;
    }

    public Object getFrameSender() {
        return FrameAccess.getSender(getTruffleFrame());
    }

    public AbstractSqueakObject getSender() {
        final Object value = FrameAccess.getSender(getTruffleFrame());
        if (value instanceof FrameMarker) {
            getCodeObject().getDoesNotNeedSenderAssumption().invalidate("Sender requested");
            final ContextObject previousContext = ((FrameMarker) value).getMaterializedContext();
            if (process != null && !(getReceiver() instanceof ContextObject)) {
                previousContext.setProcess(process);
            }
            FrameAccess.setSender(getTruffleFrame(), previousContext);
            return previousContext;
        } else {
            return (AbstractSqueakObject) value;
        }
    }

    // should only be used when sender is not nil
    public ContextObject getNotNilSender() {
        return (ContextObject) getSender();
    }

    public boolean hasMaterializedSender() {
        return !(FrameAccess.getSender(getTruffleFrame()) instanceof FrameMarker);
    }

    public AbstractSqueakObject getMaterializedSender() {
        return (AbstractSqueakObject) FrameAccess.getSender(getTruffleFrame());
    }

    /**
     * Sets the sender of a ContextObject.
     */
    public void setSender(final ContextObject value) {
        if (truffleFrame != null) {
            final Object sender = FrameAccess.getSender(getTruffleFrame());
            if (!hasModifiedSender && sender != value && sender != value.getFrameMarker()) {
                hasModifiedSender = true;
            }
        }
        setSenderUnsafe(value);
    }

    public void setSenderUnsafe(final AbstractSqueakObject value) {
        FrameAccess.setSender(getOrCreateTruffleFrame(), value);
    }

    public void removeSender() {
        if (hasModifiedSender) {
            hasModifiedSender = false;
        }
        FrameAccess.setSender(getOrCreateTruffleFrame(), NilObject.SINGLETON);
    }

    public Object getInstructionPointer(final ConditionProfile nilProfile) {
        final int pc = FrameAccess.getInstructionPointer(getTruffleFrame(), methodOrBlock);
        if (nilProfile.profile(pc == NIL_PC_VALUE)) {
            return NilObject.SINGLETON;
        } else {
            return (long) pc; // Must be a long.
        }
    }

    public int getInstructionPointerForBytecodeLoop() {
        return FrameAccess.getInstructionPointer(getTruffleFrame(), methodOrBlock);
    }

    public void setInstructionPointer(final int value) {
        FrameAccess.setInstructionPointer(getTruffleFrame(), methodOrBlock, value);
    }

    public void removeInstructionPointer() {
        FrameAccess.setInstructionPointer(getTruffleFrame(), methodOrBlock, NIL_PC_VALUE);
    }

    public int getStackPointer() {
        return FrameAccess.getStackPointer(getTruffleFrame(), methodOrBlock);
    }

    public void setStackPointer(final int value) {
        assert 0 <= value && value <= getCodeObject().getSqueakContextSize() : value + " not between 0 and " + getCodeObject().getSqueakContextSize() + " in " + toString();
        FrameAccess.setStackPointer(getOrCreateTruffleFrame(), methodOrBlock, value);
    }

    private boolean hasMethod() {
        return hasTruffleFrame() && getCodeObject() != null;
    }

    public CompiledCodeObject getCodeObject() {
        return FrameAccess.getCodeObject(getTruffleFrame());
    }

    public void setCodeObject(final CompiledCodeObject value) {
        FrameAccess.setCodeObject(getOrCreateTruffleFrame(value), value);
    }

    public BlockClosureObject getClosure() {
        return FrameAccess.getClosure(getTruffleFrame());
    }

    public boolean hasClosure() {
        return FrameAccess.getClosure(getTruffleFrame()) != null;
    }

    public void removeClosure() {
        if (getClosure() != null) {
            throw SqueakException.create("Not yet implemented/support");
        }
    }

    @TruffleBoundary
    public void setClosure(final BlockClosureObject value) {
        final MaterializedFrame oldFrame = getOrCreateTruffleFrame();
        final CompiledCodeObject code = FrameAccess.getMethodOrBlock(oldFrame);
        final int pc = FrameAccess.getInstructionPointer(oldFrame, code);
        final int sp = FrameAccess.getStackPointer(oldFrame, code);
        final CompiledCodeObject block = value.getCompiledBlock();
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(oldFrame.getArguments(), block.getFrameDescriptor());
        methodOrBlock = block;
        FrameAccess.assertSenderNotNull(truffleFrame);
        FrameAccess.assertReceiverNotNull(truffleFrame);
        FrameAccess.initializeMarker(truffleFrame, block);
        FrameAccess.setContext(truffleFrame, block, this);
        FrameAccess.setInstructionPointer(truffleFrame, block, pc);
        FrameAccess.setStackPointer(truffleFrame, block, sp);
        FrameAccess.setClosure(truffleFrame, value);
        FrameAccess.iterateStackSlots(truffleFrame, slot -> {
            final Object stackValue = oldFrame.getValue(slot);
            if (stackValue != null) {
                FrameAccess.setStackSlot(truffleFrame, slot, stackValue);
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
            return NilObject.nullToNil(truffleFrame.getValue(FrameAccess.findOrAddStackSlot(truffleFrame, index)));
        }
    }

    @TruffleBoundary
    public void atTempPut(final int index, final Object value) {
        final Object[] args = getOrCreateTruffleFrame().getArguments();
        if (FrameAccess.getArgumentStartIndex() + index < args.length) {
            args[FrameAccess.getArgumentStartIndex() + index] = value;
        } else {
            FrameAccess.setStackSlot(truffleFrame, FrameAccess.findOrAddStackSlot(truffleFrame, index), value);
        }
    }

    public void terminate() {
        removeInstructionPointer();
        removeSender();
    }

    public boolean isTerminated() {
        return getInstructionPointerForBytecodeLoop() < 0 && getFrameSender() == NilObject.SINGLETON;
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
            final BlockClosureObject closure = getClosure();
            if (closure != null) {
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
        final CompiledCodeObject otherMethodOrBlock = other.methodOrBlock;
        final int otherSize = other.size;
        final boolean otherHasModifiedSender = other.hasModifiedSender;
        final boolean otherEscaped = other.escaped;
        other.setFields(truffleFrame, methodOrBlock, size, hasModifiedSender, escaped);
        setFields(otherTruffleFrame, otherMethodOrBlock, otherSize, otherHasModifiedSender, otherEscaped);
    }

    private void setFields(final MaterializedFrame otherTruffleFrame, final CompiledCodeObject otherMethodOrBlock, final int otherSize, final boolean otherHasModifiedSender,
                    final boolean otherEscaped) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = otherTruffleFrame;
        methodOrBlock = otherMethodOrBlock;
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

    public void transferTo(final SqueakImageContext image, final PointersObject newProcess, final AbstractPointersObjectReadNode readNode, final AbstractPointersObjectWriteNode writeNode,
                    final GetActiveProcessNode getActiveProcessNode) {
        // Record a process to be awakened on the next interpreter cycle.
        final PointersObject scheduler = image.getScheduler();
        assert newProcess != getActiveProcessNode.execute() : "trying to switch to already active process";
        // overwritten in next line.
        final PointersObject oldProcess = getActiveProcessNode.execute();
        writeNode.execute(scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        writeNode.execute(oldProcess, PROCESS.SUSPENDED_CONTEXT, this);
        writeNode.executeNil(newProcess, PROCESS.LIST);
        final ContextObject newActiveContext = (ContextObject) readNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        newActiveContext.setProcess(newProcess);
        writeNode.executeNil(newProcess, PROCESS.SUSPENDED_CONTEXT);
        if (CompilerDirectives.isPartialEvaluationConstant(newActiveContext)) {
            throw ProcessSwitch.create(newActiveContext);
        } else {
            // Avoid further PE if newActiveContext is not a PE constant.
            throw ProcessSwitch.createWithBoundary(newActiveContext);
        }
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

    public FrameMarker getFrameMarker() {
        return FrameAccess.getMarker(getTruffleFrame(), methodOrBlock);
    }

    // The context represents primitive call which needs to be skipped when unwinding call stack.
    public boolean isPrimitiveContext() {
        return !hasClosure() && getCodeObject().hasPrimitive() && getInstructionPointerForBytecodeLoop() <= CallPrimitiveNode.NUM_BYTECODES;
    }

    @TruffleBoundary
    public boolean pointsTo(final Object thang) {
        // TODO: make sure this works correctly
        if (truffleFrame != null) {
            final int stackPointer = getStackPointer();
            if (getSender() == thang || thang.equals(getInstructionPointer(ConditionProfile.getUncached())) || thang.equals(stackPointer) || getCodeObject() == thang || getClosure() == thang ||
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
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        if (hasTruffleFrame()) {
            for (int i = 0; i < from.length; i++) {
                final Object fromPointer = from[i];
                final Object toPointer = to[i];
                if (fromPointer == getFrameSender() && toPointer instanceof ContextObject) {
                    setSender((ContextObject) toPointer);
                }
                if (fromPointer == getCodeObject() && toPointer instanceof CompiledCodeObject) {
                    setCodeObject((CompiledCodeObject) toPointer);
                }
                if (fromPointer == getClosure() && toPointer instanceof BlockClosureObject) {
                    setClosure((BlockClosureObject) toPointer);
                }
                if (fromPointer == getReceiver()) {
                    setReceiver(toPointer);
                }

                final Object[] arguments = truffleFrame.getArguments();
                for (int j = FrameAccess.getArgumentStartIndex(); j < arguments.length; j++) {
                    final Object argument = arguments[j];
                    if (argument == fromPointer) {
                        arguments[j] = toPointer;
                    }
                }

                assert methodOrBlock != fromPointer : "Code should change and with it the frame descriptor";
                assert process != fromPointer : "Process should change";
                FrameAccess.iterateStackSlots(truffleFrame, slot -> {
                    if (truffleFrame.isObject(slot)) {
                        final Object stackValue = FrameUtil.getObjectSafe(truffleFrame, slot);
                        if (fromPointer == stackValue) {
                            truffleFrame.setObject(slot, toPointer);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        if (hasTruffleFrame()) {
            tracer.addIfUnmarked(getFrameSender());
            tracer.addIfUnmarked(getCodeObject());
            tracer.addIfUnmarked(getClosure());
            tracer.addIfUnmarked(getReceiver());
            for (final Object arg : truffleFrame.getArguments()) {
                tracer.addIfUnmarked(arg);
            }
            FrameAccess.iterateStackSlots(truffleFrame, slot -> {
                if (truffleFrame.isObject(slot)) {
                    tracer.addIfUnmarked(FrameUtil.getObjectSafe(truffleFrame, slot));
                }
            });
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        if (hasTruffleFrame()) {
            writer.traceIfNecessary(getSender()); /* May materialize sender. */
            writer.traceIfNecessary(getCodeObject());
            writer.traceIfNecessary(getClosure());
            writer.traceIfNecessary(getReceiver());
            for (final Object arg : truffleFrame.getArguments()) {
                writer.traceIfNecessary(arg);
            }
            FrameAccess.iterateStackSlots(truffleFrame, slot -> {
                if (truffleFrame.isObject(slot)) {
                    writer.traceIfNecessary(FrameUtil.getObjectSafe(truffleFrame, slot));
                }
            });
        }
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ContextObject must have slots:", this);
        }
        writer.writeObject(getSender());
        writer.writeObject(getInstructionPointer(ConditionProfile.getUncached()));
        writer.writeSmallInteger(getStackPointer());
        writer.writeObject(getCodeObject());
        writer.writeObject(NilObject.nullToNil(getClosure()));
        final Object[] args = truffleFrame.getArguments();
        final int numArgs = FrameAccess.getNumArguments(truffleFrame);
        // Write receiver and arguments
        for (int i = 0; i < 1 + numArgs; i++) {
            writer.writeObject(args[FrameAccess.getReceiverStartIndex() + i]);
        }
        // Write remaining stack items
        for (int i = numArgs; i < getCodeObject().getSqueakContextSize(); i++) {
            final FrameSlot slot = FrameAccess.findStackSlot(truffleFrame, i);
            if (slot == null) {
                writer.writeNil();
            } else {
                final Object stackValue = truffleFrame.getValue(slot);
                if (stackValue == null) {
                    writer.writeNil();
                } else {
                    writer.writeObject(stackValue);
                }
            }
        }
    }

    public PointersObject getProcess() {
        return process;
    }

    public void setProcess(final PointersObject process) {
        assert process != null && (this.process == null || this.process == process);
        this.process = process;
    }
}

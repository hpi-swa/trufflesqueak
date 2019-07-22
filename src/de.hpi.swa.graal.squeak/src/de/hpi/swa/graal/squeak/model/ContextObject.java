package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReader;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class ContextObject extends AbstractSqueakObjectWithHash {
    @CompilationFinal private MaterializedFrame truffleFrame;
    @CompilationFinal private int size;
    private boolean hasModifiedSender = false;
    private boolean escaped = false;

    private ContextObject(final SqueakImageContext image, final long hash) {
        super(image, hash);
        truffleFrame = null;
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        super(image);
        truffleFrame = null;
        this.size = size;
    }

    private ContextObject(final Frame frame, final CompiledCodeObject blockOrMethod) {
        super(blockOrMethod.image);
        assert FrameAccess.getSender(frame) != null;
        assert FrameAccess.getContext(frame, blockOrMethod) == null;
        truffleFrame = frame.materialize();
        FrameAccess.setContext(truffleFrame, blockOrMethod, this);
        size = blockOrMethod.getSqueakContextSize();
    }

    private ContextObject(final ContextObject original) {
        super(original.image);
        final CompiledCodeObject code = FrameAccess.getBlockOrMethod(original.truffleFrame);
        hasModifiedSender = original.hasModifiedSender();
        escaped = original.escaped;
        size = original.size;
        // Create shallow copy of Truffle frame
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(original.truffleFrame.getArguments(), code.getFrameDescriptor());
        // Copy frame slot values
        FrameAccess.setMarker(truffleFrame, code, FrameAccess.getMarker(original.truffleFrame, code));
        FrameAccess.setContext(truffleFrame, code, this);
        FrameAccess.setInstructionPointer(truffleFrame, code, FrameAccess.getInstructionPointer(original.truffleFrame, code));
        FrameAccess.setStackPointer(truffleFrame, code, FrameAccess.getStackPointer(original.truffleFrame, code));
        // Copy stack
        final int numStackSlots = code.getNumStackSlots();
        for (int i = 0; i < numStackSlots; i++) {
            final FrameSlot slot = code.getStackSlot(i);
            final Object value = original.truffleFrame.getValue(slot);
            if (value != null) {
                FrameAccess.setStackSlot(truffleFrame, slot, value);
            } else {
                break; // This and all following slots are not in use.
            }
        }
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    public static ContextObject createWithHash(final SqueakImageContext image, final long hash) {
        return new ContextObject(image, hash);
    }

    public static ContextObject create(final FrameInstance frameInstance) {
        final Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
        return create(frame, FrameAccess.getBlockOrMethod(frame));
    }

    public static ContextObject create(final Frame frame, final CompiledCodeObject blockOrMethod) {
        return new ContextObject(frame, blockOrMethod);
    }

    @Override
    public ClassObject getSqueakClass() {
        return image.methodContextClass;
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
        final CompiledMethodObject method = (CompiledMethodObject) pointers[CONTEXT.METHOD];
        final AbstractSqueakObject sender = (AbstractSqueakObject) pointers[CONTEXT.SENDER_OR_NIL];
        assert sender != null : "sender should not be null";
        final Object closureOrNil = pointers[CONTEXT.CLOSURE_OR_NIL];
        final BlockClosureObject closure;
        final CompiledCodeObject code;
        if (closureOrNil == NilObject.SINGLETON) {
            closure = null;
            code = method;
        } else {
            closure = (BlockClosureObject) closureOrNil;
            code = closure.getCompiledBlock(method);
        }
        final int endArguments = CONTEXT.RECEIVER + 1 + method.getNumArgsAndCopied();
        final Object[] arguments = Arrays.copyOfRange(pointers, CONTEXT.RECEIVER, endArguments);
        final Object[] frameArguments = FrameAccess.newWith(method, sender, closure, arguments);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, code.getFrameDescriptor());
        FrameAccess.initializeMarker(truffleFrame, code);
        FrameAccess.setContext(truffleFrame, code, this);
        atput0(CONTEXT.INSTRUCTION_POINTER, pointers[CONTEXT.INSTRUCTION_POINTER]);
        atput0(CONTEXT.STACKPOINTER, pointers[CONTEXT.STACKPOINTER]);
        for (int i = CONTEXT.TEMP_FRAME_START; i < pointers.length; i++) {
            atput0(i, pointers[i]);
        }
    }

    /** Turns a ContextObject back into an array of pointers (fillIn reversed). */
    public Object[] asPointers() {
        assert hasTruffleFrame();
        final Object[] pointers = new Object[size];
        for (int i = 0; i < size; i++) {
            pointers[i] = at0(i);
        }
        return pointers;
    }

    private Object at0(final long longIndex) {
        assert longIndex >= 0;
        final int index = (int) longIndex;
        switch (index) {
            case CONTEXT.SENDER_OR_NIL:
                return getSender();
            case CONTEXT.INSTRUCTION_POINTER:
                final int pc = getInstructionPointer();
                return pc < 0 ? NilObject.SINGLETON : (long) pc;  // Must return a long here.
            case CONTEXT.STACKPOINTER:
                return (long) getStackPointer(); // Must return a long here.
            case CONTEXT.METHOD:
                return getMethod();
            case CONTEXT.CLOSURE_OR_NIL:
                return NilObject.nullToNil(getClosure());
            case CONTEXT.RECEIVER:
                return getReceiver();
            default:
                return atTemp(index - CONTEXT.TEMP_FRAME_START);
        }
    }

    public void atput0(final long longIndex, final Object value) {
        assert longIndex >= 0 && value != null;
        final int index = (int) longIndex;
        assert value != null : "null indicates a problem";
        switch (index) {
            case CONTEXT.SENDER_OR_NIL:
                if (value == NilObject.SINGLETON) {
                    removeSender();
                } else {
                    setSender((ContextObject) value);
                }
                break;
            case CONTEXT.INSTRUCTION_POINTER:
                /**
                 * TODO: Adjust control flow when pc of active context is changed. For this, an
                 * exception could be used to unwind Truffle frames until the target frame is found.
                 * However, this exception should only be thrown when the context object is actually
                 * active. So it might need to be necessary to extend ContextObjects with an
                 * `isActive` field to avoid the use of iterateFrames.
                 */
                setInstructionPointer(value == NilObject.SINGLETON ? -1 : (int) (long) value);
                break;
            case CONTEXT.STACKPOINTER:
                setStackPointer((int) (long) value);
                break;
            case CONTEXT.METHOD:
                setMethod((CompiledMethodObject) value);
                break;
            case CONTEXT.CLOSURE_OR_NIL:
                setClosure(value == NilObject.SINGLETON ? null : (BlockClosureObject) value);
                break;
            case CONTEXT.RECEIVER:
                setReceiver(value);
                break;
            default:
                atTempPut(index - CONTEXT.TEMP_FRAME_START, value);
                break;
        }
    }

    public MaterializedFrame getOrCreateTruffleFrame() {
        if (truffleFrame == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // Method is unknown, use dummy frame instead
            final int guessedArgumentSize = size > CONTEXT.LARGE_FRAMESIZE ? size - CONTEXT.LARGE_FRAMESIZE : size - CONTEXT.SMALL_FRAMESIZE;
            final Object[] dummyArguments = FrameAccess.newDummyWith(null, NilObject.SINGLETON, null, new Object[guessedArgumentSize]);
            truffleFrame = Truffle.getRuntime().createMaterializedFrame(dummyArguments);
        }
        return truffleFrame;
    }

    private MaterializedFrame getOrCreateTruffleFrame(final CompiledMethodObject method) {
        if (truffleFrame == null || FrameAccess.getMethod(truffleFrame) == null) {
            final Object[] frameArguments;
            final int instructionPointer;
            final int stackPointer;
            if (truffleFrame != null) {
                assert FrameAccess.getSender(truffleFrame) != null : "Sender should not be null";

                final Object[] dummyArguments = truffleFrame.getArguments();
                final int expectedArgumentSize = FrameAccess.expectedArgumentSize(method.getNumArgsAndCopied());
                assert dummyArguments.length >= expectedArgumentSize : "Unexpected argument size, maybe dummy frame had wrong size?";
                FrameAccess.assertReceiverNotNull(truffleFrame);
                frameArguments = truffleFrame.getArguments();
                if (truffleFrame.getFrameDescriptor().getSize() > 0) {
                    instructionPointer = FrameAccess.getInstructionPointer(truffleFrame, method);
                    stackPointer = FrameAccess.getStackPointer(truffleFrame, method);
                } else { // Frame slots unknown, so initialize PC and SP.
                    instructionPointer = 0;
                    stackPointer = 0;
                }
            } else {
                // Receiver plus arguments.
                final Object[] squeakArguments = new Object[1 + method.getNumArgsAndCopied()];
                frameArguments = FrameAccess.newDummyWith(method, NilObject.SINGLETON, null, squeakArguments);
                instructionPointer = 0;
                stackPointer = 0;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, method.getFrameDescriptor());
            FrameAccess.initializeMarker(truffleFrame, method);
            FrameAccess.setContext(truffleFrame, method, this);
            FrameAccess.setInstructionPointer(truffleFrame, method, instructionPointer);
            FrameAccess.setStackPointer(truffleFrame, method, stackPointer);
        }
        return truffleFrame;
    }

    private MaterializedFrame getOrCreateTruffleFrame(final BlockClosureObject closure) {
        if (truffleFrame == null || FrameAccess.getClosure(truffleFrame) != closure) {
            final Object[] frameArguments;
            final CompiledBlockObject compiledBlock = closure.getCompiledBlock();
            final int instructionPointer;
            final int stackPointer;
            if (truffleFrame != null) {
                // FIXME: Assuming here this context is not active, add check?
                assert FrameAccess.getSender(truffleFrame) != null : "Sender should not be null";

                final Object[] dummyArguments = truffleFrame.getArguments();
                final int expectedArgumentSize = FrameAccess.expectedArgumentSize(compiledBlock.getNumArgsAndCopied());
                assert dummyArguments.length >= expectedArgumentSize : "Unexpected argument size, maybe dummy frame had wrong size?";
                if (dummyArguments.length > expectedArgumentSize) {
                    // Trim arguments.
                    frameArguments = Arrays.copyOfRange(dummyArguments, 0, expectedArgumentSize);
                } else {
                    frameArguments = truffleFrame.getArguments();
                }
                if (truffleFrame.getFrameDescriptor().getSize() > 0) {
                    instructionPointer = FrameAccess.getInstructionPointer(truffleFrame, compiledBlock);
                    stackPointer = FrameAccess.getStackPointer(truffleFrame, compiledBlock);
                } else { // Frame slots unknown, so initialize PC and SP;
                    instructionPointer = 0;
                    stackPointer = 0;
                }
            } else {
                // Receiver plus arguments.
                final Object[] squeakArguments = new Object[1 + compiledBlock.getNumArgsAndCopied()];
                frameArguments = FrameAccess.newDummyWith(null, NilObject.SINGLETON, null, squeakArguments);
                instructionPointer = 0;
                stackPointer = 0;
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArguments, compiledBlock.getFrameDescriptor());
            FrameAccess.assertSenderNotNull(truffleFrame);
            FrameAccess.assertReceiverNotNull(truffleFrame);
            FrameAccess.initializeMarker(truffleFrame, compiledBlock);
            FrameAccess.setContext(truffleFrame, compiledBlock, this);
            FrameAccess.setInstructionPointer(truffleFrame, compiledBlock, instructionPointer);
            FrameAccess.setStackPointer(truffleFrame, compiledBlock, stackPointer);
        }
        return truffleFrame;
    }

    public Object getFrameSender() {
        return FrameAccess.getSender(truffleFrame);
    }

    public AbstractSqueakObject getSender() {
        final Object value = FrameAccess.getSender(truffleFrame);
        if (value instanceof FrameMarker) {
            getBlockOrMethod().getDoesNotNeedSenderAssumption().invalidate("Sender requested");
            return ((FrameMarker) value).getMaterializedContext();
        } else {
            return (AbstractSqueakObject) value;
        }
    }

    // should only be used when sender is not nil
    public ContextObject getNotNilSender() {
        return (ContextObject) getSender();
    }

    /**
     * Sets the sender of a ContextObject.
     */
    public void setSender(final ContextObject value) {
        if (!hasModifiedSender && truffleFrame != null && FrameAccess.getSender(truffleFrame) != value.getFrameMarker()) {
            hasModifiedSender = true;
        }
        FrameAccess.setSender(getOrCreateTruffleFrame(), value);
    }

    public void removeSender() {
        if (hasModifiedSender) {
            hasModifiedSender = false;
        }
        FrameAccess.setSender(getOrCreateTruffleFrame(), NilObject.SINGLETON);
    }

    public int getInstructionPointer() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            final CompiledBlockObject block = closure.getCompiledBlock();
            return FrameAccess.getInstructionPointer(truffleFrame, block) + block.getInitialPC();
        } else {
            final CompiledMethodObject method = getMethod();
            return FrameAccess.getInstructionPointer(truffleFrame, method) + method.getInitialPC();
        }
    }

    public int getInstructionPointerForBytecodeLoop() {
        return FrameAccess.getInstructionPointer(truffleFrame, getBlockOrMethod());
    }

    public void setInstructionPointer(final int value) {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            final CompiledBlockObject block = closure.getCompiledBlock();
            FrameAccess.setInstructionPointer(truffleFrame, block, value - block.getInitialPC());
        } else {
            final CompiledMethodObject method = getMethod();
            FrameAccess.setInstructionPointer(truffleFrame, method, value - method.getInitialPC());
        }
    }

    public int getStackPointer() {
        return FrameAccess.getStackPointer(truffleFrame, getBlockOrMethod());
    }

    public void setStackPointer(final int value) {
        assert 0 <= value && value <= getBlockOrMethod().getSqueakContextSize() : value + " not between 0 and " + getBlockOrMethod().getSqueakContextSize() + " in " + toString();
        FrameAccess.setStackPointer(getOrCreateTruffleFrame(), getBlockOrMethod(), value);
    }

    private boolean hasMethod() {
        return hasTruffleFrame() && getMethod() != null;
    }

    public CompiledMethodObject getMethod() {
        return FrameAccess.getMethod(truffleFrame);
    }

    public void setMethod(final CompiledMethodObject value) {
        FrameAccess.setMethod(getOrCreateTruffleFrame(value), value);
    }

    public BlockClosureObject getClosure() {
        return FrameAccess.getClosure(truffleFrame);
    }

    public boolean hasClosure() {
        return FrameAccess.getClosure(truffleFrame) != null;
    }

    public void setClosure(final BlockClosureObject value) {
        FrameAccess.setClosure(getOrCreateTruffleFrame(value), value);
    }

    public CompiledCodeObject getBlockOrMethod() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            return closure.getCompiledBlock();
        } else {
            return getMethod();
        }
    }

    public Object getReceiver() {
        return FrameAccess.getReceiver(truffleFrame);
    }

    public void setReceiver(final Object value) {
        FrameAccess.setReceiver(getOrCreateTruffleFrame(), value);
    }

    public Object atTemp(final int index) {
        final CompiledCodeObject blockOrMethod = getBlockOrMethod();
        assert index < blockOrMethod.getNumStackSlots() : "Invalid context stack access at #" + index;
        return NilObject.nullToNil(truffleFrame.getValue(blockOrMethod.getStackSlot(index)));
    }

    public void atTempPut(final int index, final Object value) {
        FrameAccess.setArgumentIfInRange(getOrCreateTruffleFrame(), index, value);
        final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(truffleFrame);
        assert index < blockOrMethod.getNumStackSlots() : "Invalid context stack access at #" + index;
        final FrameSlot frameSlot = blockOrMethod.getStackSlot(index);
        FrameAccess.setStackSlot(truffleFrame, frameSlot, value);
    }

    public void terminate() {
        // Remove pc and sender.
        atput0(CONTEXT.INSTRUCTION_POINTER, NilObject.SINGLETON);
        atput0(CONTEXT.SENDER_OR_NIL, NilObject.SINGLETON);
    }

    public boolean isTerminated() {
        return getInstructionPointerForBytecodeLoop() < 0 && getSender() == NilObject.SINGLETON;
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
        assert value != null;
        final int currentStackPointer = getStackPointer();
        assert currentStackPointer < CONTEXT.MAX_STACK_SIZE;
        setStackPointer(currentStackPointer + 1);
        atTempPut(currentStackPointer, value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (hasMethod()) {
            final BlockClosureObject closure = getClosure();
            if (closure != null) {
                return "CTX [] in " + getMethod() + " #" + hashCode();
            } else {
                return "CTX " + getMethod() + " #" + hashCode();
            }
        } else {
            return "CTX without method" + " #" + hashCode();
        }
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
        return getBlockOrMethod().getSqueakContextSize();
    }

    public void become(final ContextObject other) {
        final MaterializedFrame otherTruffleFrame = other.truffleFrame;
        final int otherSize = other.size;
        final boolean otherHasModifiedSender = other.hasModifiedSender;
        final boolean otherEscaped = other.escaped;
        other.setFields(truffleFrame, size, hasModifiedSender, escaped);
        setFields(otherTruffleFrame, otherSize, otherHasModifiedSender, otherEscaped);
    }

    private void setFields(final MaterializedFrame otherTruffleFrame, final int otherSize, final boolean otherHasModifiedSender, final boolean otherEscaped) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleFrame = otherTruffleFrame;
        size = otherSize;
        hasModifiedSender = otherHasModifiedSender;
        escaped = otherEscaped;
    }

    private Object[] getReceiverAndNArguments(final int numArgs) {
        final Object[] arguments = new Object[1 + numArgs];
        arguments[0] = getReceiver();
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = atTemp(i);
        }
        return arguments;
    }

    public void transferTo(final PointersObject newProcess) {
        // Record a process to be awakened on the next interpreter cycle.
        final PointersObject scheduler = image.getScheduler();
        // assert newProcess != image.getActiveProcess() : "trying to switch to already active
        // process";
        final PointersObject currentProcess = image.getActiveProcess(); // overwritten in next line.
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        currentProcess.atput0(PROCESS.SUSPENDED_CONTEXT, this);
        final ContextObject newActiveContext = (ContextObject) newProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        newProcess.atput0(PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
        if (CompilerDirectives.isPartialEvaluationConstant(newActiveContext)) {
            throw ProcessSwitch.create(newActiveContext);
        } else {
            // Avoid further PE if newActiveContext is not a PE constant.
            throw ProcessSwitch.createWithBoundary(newActiveContext);
        }
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        ContextObject current = this;
        while (current != null) {
            final CompiledCodeObject code = current.getBlockOrMethod();
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments(code.getNumArgsAndCopied());
            image.getOutput().println(MiscUtils.format("%s #(%s) [%s]", current, ArrayUtils.toJoinedString(", ", rcvrAndArgs), current.getFrameMarker()));
            final Object sender = current.getSender();
            if (sender == NilObject.SINGLETON) {
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }

    public MaterializedFrame getTruffleFrame() {
        return truffleFrame;
    }

    public boolean hasTruffleFrame() {
        return truffleFrame != null;
    }

    public boolean hasMaterializedSender() {
        return !(FrameAccess.getSender(truffleFrame) instanceof FrameMarker);
    }

    public FrameMarker getFrameMarker() {
        return FrameAccess.getMarker(truffleFrame);
    }

    // The context represents primitive call which needs to be skipped when unwinding call stack.
    public boolean isPrimitiveContext() {
        return !hasClosure() && getMethod().hasPrimitive() && getInstructionPointerForBytecodeLoop() <= CallPrimitiveNode.NUM_BYTECODES;
    }

    public boolean pointsTo(final Object thang) {
        // TODO: make sure this works correctly
        if (truffleFrame != null) {
            for (int i = 0; i < size(); i++) {
                if (at0(i) == thang) {
                    return true;
                }
            }
        }
        return false;
    }
}

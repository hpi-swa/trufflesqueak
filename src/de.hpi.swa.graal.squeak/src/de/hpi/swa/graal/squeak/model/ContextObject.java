package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ContextObject extends AbstractPointersObject {
    @CompilationFinal private MaterializedFrame truffleFrame;
    @CompilationFinal private FrameMarker frameMarker;
    private boolean hasModifiedSender = false;
    private boolean isDirty = false;
    private boolean escaped = false;

    public static ContextObject createWithHash(final SqueakImageContext image, final long hash) {
        return new ContextObject(image, hash);
    }

    private ContextObject(final SqueakImageContext image, final long hash) {
        super(image, hash, image.methodContextClass);
        isDirty = true;
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        super(image, image.methodContextClass);
        isDirty = true;
        /*
         * Size of pointers array is too small, because method is unknown, so we cannot call
         * CompiledCodeObject.getNumArgsAndCopied() here. Add 8 additional slots for now.
         */
        setPointersUnsafe(new Object[CONTEXT.TEMP_FRAME_START + size + 8]);
    }

    public static ContextObject create(final SqueakImageContext image, final int size, final MaterializedFrame frame, final FrameMarker frameMarker) {
        return new ContextObject(image, size, frame, frameMarker);
    }

    private ContextObject(final SqueakImageContext image, final int size, final MaterializedFrame frame, final FrameMarker frameMarker) {
        this(image, size);
        isDirty = false;
        truffleFrame = frame;
        this.frameMarker = frameMarker;
    }

    public ContextObject(final ContextObject original) {
        super(original.image, original.image.methodContextClass);
        setPointers(original.getPointers().clone());
        truffleFrame = original.truffleFrame;
        hasModifiedSender = original.hasModifiedSender;
    }

    public void terminate() {
        // remove pc and sender without flagging as dirty
        setPointer(CONTEXT.INSTRUCTION_POINTER, image.nil);
        setPointer(CONTEXT.SENDER_OR_NIL, image.nil);
    }

    public boolean isTerminated() {
        return getPointer(CONTEXT.INSTRUCTION_POINTER) == image.nil &&
                        getPointer(CONTEXT.SENDER_OR_NIL) == image.nil;
    }

    public Object at0(final long longIndex) {
        assert longIndex >= 0;
        final int index = (int) longIndex;
        if (isDirty) {
            if (index == CONTEXT.SENDER_OR_NIL) {
                return getSender(); // sender might need to be reconstructed
            }
            final Object result = getPointer(index);
            if (result != null) {
                return result;
            }
            if (truffleFrame == null) {
                return image.nil;
            }
        }
        switch (index) {
            case CONTEXT.SENDER_OR_NIL:
                return getSender();
            case CONTEXT.INSTRUCTION_POINTER:
                final int pc = getFramePC();
                if (pc < 0) {
                    return image.nil;
                } else {
                    final CompiledCodeObject blockOrMethod = getMethod();
                    final int initalPC;
                    if (blockOrMethod instanceof CompiledBlockObject) {
                        initalPC = ((CompiledBlockObject) blockOrMethod).getInitialPC();
                    } else {
                        initalPC = ((CompiledMethodObject) blockOrMethod).getInitialPC();
                    }
                    return (long) (initalPC + pc);
                }
            case CONTEXT.STACKPOINTER:
                return (long) FrameUtil.getIntSafe(truffleFrame, getMethod().stackPointerSlot) + 1;
            case CONTEXT.METHOD:
                return FrameAccess.getMethod(truffleFrame);
            case CONTEXT.CLOSURE_OR_NIL:
                final BlockClosureObject closure = FrameAccess.getClosure(truffleFrame);
                return closure == null ? image.nil : closure;
            case CONTEXT.RECEIVER:
                return FrameAccess.getReceiver(truffleFrame);
            default:
                return truffleFrame.getValue(getMethod().getStackSlot(index - CONTEXT.TEMP_FRAME_START));
        }
    }

    public void atput0(final long index, final Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            image.printVerbose("Sender of", this, " set to", value);
            hasModifiedSender = true;
        }
        if (!isDirty) {
            isDirty = true;
        }
        assert value != null : "null indicates a problem";
        if (index >= size()) { // Ensure context's pointers array is big enough.
            /*
             * When a new context is created, its method might be unknown. And since arguments are
             * stored in the pointers array as well, it is unknown how big the pointers array needs
             * to be in those cases. See comment in `ContextObject(image, size)`.
             */
            image.printToStdErr("Growing context pointers from", size(), "to", index + 8, "-", this);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setPointersUnsafe(Arrays.copyOf(getPointers(), (int) index + 8));
        }
        setPointer((int) index, value);
    }

    public CompiledCodeObject getClosureOrMethod() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            return closure.getCompiledBlock();
        }
        return getMethod();
    }

    private boolean hasMethod() {
        return at0(CONTEXT.METHOD) instanceof CompiledCodeObject;
    }

    public CompiledCodeObject getMethod() {
        return (CompiledCodeObject) at0(CONTEXT.METHOD);
    }

    public AbstractSqueakObject shallowCopy() {
        return new ContextObject(this);
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean hasEscaped() {
        return escaped;
    }

    public void markEscaped() {
        this.escaped = true;
    }

    public boolean hasModifiedSender() {
        return hasModifiedSender;
    }

    public boolean hasVirtualSender() {
        return getPointer(CONTEXT.SENDER_OR_NIL) instanceof FrameMarker;
    }

    private int getFramePC() {
        return FrameUtil.getIntSafe(truffleFrame, getMethod().instructionPointerSlot);
    }

    private boolean truffleFrameMarkedAsTerminated() {
        return truffleFrame != null && getFramePC() < 0;
    }

    public AbstractSqueakObject getSender() {
        final Object sender = getPointer(CONTEXT.SENDER_OR_NIL);
        if (sender instanceof ContextObject) {
            if (truffleFrameMarkedAsTerminated()) {
                setSender(image.nil);
                return image.nil;
            }
            return (AbstractSqueakObject) sender;
        } else if (sender instanceof NilObject) {
            return (AbstractSqueakObject) sender;
        }
        final AbstractSqueakObject actualSender;
        final Object senderOrMarker = truffleFrame.getArguments()[FrameAccess.SENDER_OR_SENDER_MARKER];
        if (senderOrMarker instanceof FrameMarker) {
            final Frame frame = FrameAccess.findFrameForMarker((FrameMarker) senderOrMarker);
            if (frame == null) {
                throw new SqueakException("Unable to find frame for marker:", senderOrMarker);
            }
            actualSender = getOrCreateContextFor(frame.materialize());
            assert actualSender != null;
        } else {
            actualSender = (AbstractSqueakObject) senderOrMarker;
        }
        setSender(actualSender);
        return actualSender;
    }

    private static ContextObject getOrCreateContextFor(final MaterializedFrame frame) {
        final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        if (contextOrMarker instanceof FrameMarker) {
            final CompiledCodeObject method = (CompiledCodeObject) frame.getArguments()[FrameAccess.METHOD];
            final ContextObject context = ContextObject.create(method.image, method.sqContextSize(), frame, (FrameMarker) contextOrMarker);
            frame.setObject(method.thisContextOrMarkerSlot, context);
            return context;
        } else {
            return (ContextObject) contextOrMarker;
        }
    }

    // should only be used when sender is not nil
    public ContextObject getNotNilSender() {
        return (ContextObject) getSender();
    }

    /*
     * Set sender without flagging context as dirty.
     */
    public void setSender(final Object sender) {
        setPointer(CONTEXT.SENDER_OR_NIL, sender);
    }

    public void push(final Object value) {
        assert value != null;
        final long newSP = getStackPointer() + 1;
        assert newSP <= CONTEXT.MAX_STACK_SIZE;
        atStackPut(newSP, value);
        setStackPointer(newSP);
    }

    public long getInstructionPointer() {
        return (long) at0(CONTEXT.INSTRUCTION_POINTER);
    }

    public void setInstructionPointer(final long newPC) {
        assert newPC >= 0;
        atput0(CONTEXT.INSTRUCTION_POINTER, newPC);
    }

    public long getStackPointer() {
        return (long) at0(CONTEXT.STACKPOINTER);
    }

    public void setStackPointer(final long newSP) {
        assert 0 <= newSP && newSP <= CONTEXT.MAX_STACK_SIZE;
        atput0(CONTEXT.STACKPOINTER, newSP);
    }

    @Override
    public String toString() {
        if (hasMethod()) {
            final BlockClosureObject closure = getClosure();
            if (closure != null) {
                return "CTX [] in " + getMethod();
            } else {
                return "CTX " + getMethod();
            }
        } else {
            return "CTX without method";
        }
    }

    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(final int offset) {
        return atStack(getStackPointer() - offset);
    }

    public Object getReceiver() {
        return at0(CONTEXT.RECEIVER);
    }

    public Object atTemp(final long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START + argumentIndex);
    }

    public void atTempPut(final long argumentIndex, final Object value) {
        atput0(CONTEXT.TEMP_FRAME_START + argumentIndex, value);
    }

    public Object atStack(final long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex);
    }

    public void atStackPut(final long argumentIndex, final Object value) {
        atput0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex, value);
    }

    public int getStackSize() {
        return size() - CONTEXT.TEMP_FRAME_START;
    }

    public void become(final ContextObject other) {
        becomeOtherClass(other);
        final Object[] otherPointers = other.getPointers();
        other.setPointers(this.getPointers());
        setPointers(otherPointers);
    }

    public BlockClosureObject getClosure() {
        final Object closureOrNil = at0(CONTEXT.CLOSURE_OR_NIL);
        return closureOrNil == image.nil ? null : (BlockClosureObject) closureOrNil;
    }

    public boolean isUnwindContext() {
        return getMethod().isUnwindMarked();
    }

    public Object[] getReceiverAndNArguments(final int numArgs) {
        final Object[] arguments = new Object[1 + numArgs];
        arguments[0] = getReceiver();
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = atTemp(i);
        }
        return arguments;
    }

    public MaterializedFrame getTruffleFrame(final int numArgs) {
        if (!isDirty && truffleFrame != null) {
            return truffleFrame;
        }
        final CompiledCodeObject closureOrMethod = getClosureOrMethod();
        final AbstractSqueakObject sender = getSender();
        final BlockClosureObject closure = getClosure();
        final Object[] frameArgs = getReceiverAndNArguments(numArgs);
        final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(FrameAccess.newWith(closureOrMethod, sender, closure, frameArgs), getMethod().getFrameDescriptor());
        frame.setObject(closureOrMethod.thisContextOrMarkerSlot, this);
        return frame;
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        ContextObject current = this;
        while (true) {
            final CompiledCodeObject code = current.getClosureOrMethod();
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments(code.getNumArgsAndCopied());
            final String[] argumentStrings = new String[rcvrAndArgs.length];
            for (int i = 0; i < rcvrAndArgs.length; i++) {
                argumentStrings[i] = rcvrAndArgs[i].toString();
            }
            image.getOutput().println(String.format("%s #(%s)", current, String.join(", ", argumentStrings)));
            final AbstractSqueakObject sender = current.getSender();
            if (sender == image.nil) {
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
        return getPointer(CONTEXT.SENDER_OR_NIL) != null || truffleFrame.getArguments()[FrameAccess.SENDER_OR_SENDER_MARKER] instanceof ContextObject;
    }

    public FrameMarker getFrameMarker() {
        return frameMarker;
    }
}

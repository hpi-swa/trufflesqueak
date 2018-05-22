package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ContextObject extends AbstractSqueakObject {
    @CompilationFinal(dimensions = 1) protected Object[] pointers;
    @CompilationFinal private FrameMarker frameMarker;
    @CompilationFinal private boolean isDirty;

    public static ContextObject create(final SqueakImageContext image) {
        return new ContextObject(image);
    }

    private ContextObject(final SqueakImageContext image) {
        super(image, image.methodContextClass);
    }

    public static ContextObject create(final SqueakImageContext image, final int size) {
        return new ContextObject(image, size);
    }

    private ContextObject(final SqueakImageContext image, final int size) {
        this(image);
        this.pointers = ArrayUtils.withAll(CONTEXT.TEMP_FRAME_START + size, image.nil);
    }

    public static ContextObject create(final SqueakImageContext image, final int size, final FrameMarker frameMarker) {
        return new ContextObject(image, size, frameMarker);
    }

    private ContextObject(final SqueakImageContext image, final int size, final FrameMarker frameMarker) {
        this(image, size);
        this.frameMarker = frameMarker;
    }

    public ContextObject(final ContextObject original) {
        super(original.image, original.image.methodContextClass);
        pointers = original.pointers.clone();
        frameMarker = original.frameMarker;
        isDirty = original.isDirty;
    }

    public void terminate() {
        atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
        setSender(image.nil); // remove sender without flagging dirty
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    public Object at0(final long index) {
        assert index >= 0;
        if (index == CONTEXT.SENDER_OR_NIL) {
            return getSender(); // sender might need to be reconstructed
        }
        return pointers[(int) index];
    }

    public void atput0(final long index, final Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            image.traceVerbose("Sender of " + toString() + " set to " + value);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isDirty = true;
        }
        assert value != null; // null indicates a problem
        pointers[(int) index] = value;
    }

    public CompiledCodeObject getClosureOrMethod() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            return closure.getCompiledBlock();
        }
        return getMethod();
    }

    public boolean hasMethod() {
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

    public boolean hasVirtualSender() {
        return pointers[CONTEXT.SENDER_OR_NIL] instanceof FrameMarker;
    }

    public AbstractSqueakObject getSender() {
        final Object sender = pointers[CONTEXT.SENDER_OR_NIL];
        if (sender instanceof ContextObject) {
            return (AbstractSqueakObject) sender;
        } else if (sender instanceof NilObject) {
            return (AbstractSqueakObject) sender;
        } else {
            CompilerDirectives.transferToInterpreter();
            assert sender instanceof FrameMarker;
            final Frame frame = FrameAccess.findFrameForMarker((FrameMarker) sender);
            assert frame != null : "Frame for context to reconstruct does not exist anymore";
            final AbstractSqueakObject reconstructedSender = GetOrCreateContextNode.getOrCreate(frame);
            assert reconstructedSender != null;
            setSender(reconstructedSender);
            return reconstructedSender;
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
        pointers[CONTEXT.SENDER_OR_NIL] = sender;
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

    public int size() {
        return pointers.length;
    }

    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
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

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof ContextObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers2 = ((ContextObject) other).pointers;
        ((ContextObject) other).pointers = this.pointers;
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            // skip sender (for performance), pc, and sp
            for (int j = CONTEXT.METHOD; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                        ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
    }

    public BlockClosureObject getClosure() {
        final Object closureOrNil = at0(CONTEXT.CLOSURE_OR_NIL);
        return closureOrNil == image.nil ? null : (BlockClosureObject) closureOrNil;
    }

    public FrameMarker getFrameMarker() {
        return frameMarker;
    }

    public void setFrameMarker(final FrameMarker frameMarker) {
        this.frameMarker = frameMarker;
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

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        ContextObject current = this;
        int numArgsAndCopiedValues;
        while (true) {
            final CompiledCodeObject code = current.getClosureOrMethod();
            if (code instanceof CompiledBlockObject) {
                numArgsAndCopiedValues = ((CompiledBlockObject) code).getNumArgs() + ((CompiledBlockObject) code).getNumCopiedValues();
            } else {
                numArgsAndCopiedValues = ((CompiledMethodObject) code).getNumArgs();
            }
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments(numArgsAndCopiedValues);
            final String[] argumentStrings = new String[rcvrAndArgs.length];
            for (int i = 0; i < rcvrAndArgs.length; i++) {
                argumentStrings[i] = rcvrAndArgs[i].toString();
            }
            image.getOutput().println(String.format("%s #(%s)", current, String.join(", ", argumentStrings)));
            final AbstractSqueakObject sender = current.getSender();
            if (sender.isNil()) {
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }
}

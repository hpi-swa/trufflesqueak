package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class ContextObject extends AbstractPointersObject {
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
    public Object at0(final long index) {
        assert index >= 0;
        if (index == CONTEXT.SENDER_OR_NIL) {
            return getSender(); // sender might need to be reconstructed
        }
        return super.at0(index);
    }

    @Override
    public void atput0(final long index, final Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            image.traceVerbose("Sender of " + toString() + " set to " + value);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isDirty = true;
        }
        super.atput0(index, value);
    }

    public CompiledCodeObject getCodeObject() {
        final BlockClosureObject closure = getClosure();
        if (closure != null) {
            return closure.getCompiledBlock();
        }
        return getMethod();
    }

    public CompiledCodeObject getMethod() {
        return (CompiledCodeObject) at0(CONTEXT.METHOD);
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new ContextObject(this);
    }

    public Object[] getReceiverAndArguments() {
        final int numArgs = getCodeObject().getNumArgsAndCopiedValues();
        final Object[] arguments = new Object[1 + numArgs];
        arguments[0] = getReceiver();
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = atTemp(i);
        }
        return arguments;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean hasVirtualSender() {
        return super.at0(CONTEXT.SENDER_OR_NIL) instanceof FrameMarker;
    }

    public BaseSqueakObject getSender() {
        final Object sender = super.at0(CONTEXT.SENDER_OR_NIL);
        if (sender instanceof ContextObject) {
            return (BaseSqueakObject) sender;
        } else if (sender instanceof NilObject) {
            return (BaseSqueakObject) sender;
        } else {
            CompilerDirectives.transferToInterpreter();
            assert sender instanceof FrameMarker;
            final Frame frame = FrameAccess.findFrameForMarker((FrameMarker) sender);
            assert frame != null : "Frame for context to reconstruct does not exist anymore";
            final BaseSqueakObject reconstructedSender = GetOrCreateContextNode.getOrCreate(frame);
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
        super.atput0(CONTEXT.SENDER_OR_NIL, sender);
    }

    public void push(final Object value) {
        assert value != null;
        final long newSP = getStackPointer() + 1;
        assert newSP <= CONTEXT.MAX_STACK_SIZE;
        atStackPut(newSP, value);
        setStackPointer(newSP);
    }

    public long getInstructionPointer() {
        final CompiledCodeObject code = getCodeObject();
        return decodeSqPC((long) at0(CONTEXT.INSTRUCTION_POINTER), code);
    }

    public void setInstructionPointer(final long newPC) {
        final long encodedPC = encodeSqPC(newPC, getCodeObject());
        assert encodedPC >= 0;
        atput0(CONTEXT.INSTRUCTION_POINTER, encodedPC);
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
        if (at0(CONTEXT.METHOD) == image.nil) {
            return "CTX without method";
        } else {
            final BlockClosureObject closure = getClosure();
            if (closure != null) {
                return "CTX [] in " + getMethod();
            } else {
                return "CTX " + getMethod();
            }
        }
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(final int offset) {
        return atStack(getStackPointer() - offset);
    }

    public Object pop() {
        final long sp = getStackPointer();
        if (sp > 0) {
            setStackPointer(sp - 1);
        }
        return atStackAndClear(sp);
    }

    public Object[] popNReversed(final int numPop) {
        final long sp = getStackPointer();
        assert sp - numPop >= 0;
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStackAndClear(sp - i);
        }
        setStackPointer(sp - numPop);
        return result;
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

    public Object atStackAndClear(final long argumentIndex) {
        final Object value = atStack(argumentIndex);
// CompiledCodeObject code = getMethod();
// if (argumentIndex > 1 + code.getNumArgsAndCopiedValues() + code.getNumTemps()) {
// only nil out stack values, not receiver, args, or temps
// atStackPut(argumentIndex, image.nil);
// }
        return value;
    }

    @Override
    public final void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            // skip sender (for performance), pc, and sp
            for (int j = CONTEXT.METHOD; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                        ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
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

    /*
     * pc is offset by the initial pc
     */
    public static long encodeSqPC(final long pc, final CompiledCodeObject code) {
        return pc + code.getInitialPC() + code.getOffset();
    }

    public static long decodeSqPC(final long pc, final CompiledCodeObject code) {
        return pc - code.getInitialPC() - code.getOffset();
    }

    public boolean isUnwindContext() {
        return getMethod().isUnwindMarked();
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        ContextObject current = this;
        while (true) {
            final Object[] rcvrAndArgs = current.getReceiverAndArguments();
            final String[] argumentStrings = new String[rcvrAndArgs.length];
            for (int i = 0; i < rcvrAndArgs.length; i++) {
                argumentStrings[i] = rcvrAndArgs[i].toString();
            }
            image.getOutput().println(String.format("%s #(%s)", current, String.join(", ", argumentStrings)));
            final BaseSqueakObject sender = current.getSender();
            if (sender.isNil()) {
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }
}

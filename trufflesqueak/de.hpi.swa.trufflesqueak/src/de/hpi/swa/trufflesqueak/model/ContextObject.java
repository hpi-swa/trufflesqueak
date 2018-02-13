package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class ContextObject extends AbstractPointersObject {
    @CompilationFinal private FrameMarker frameMarker;
    private boolean isDirty;

    public static ContextObject create(SqueakImageContext image) {
        return new ContextObject(image);
    }

    private ContextObject(SqueakImageContext image) {
        super(image, image.methodContextClass);
    }

    public static ContextObject create(SqueakImageContext image, int size) {
        return new ContextObject(image, size);
    }

    private ContextObject(SqueakImageContext image, int size) {
        this(image);
        pointers = new Object[CONTEXT.TEMP_FRAME_START + size];
        Arrays.fill(pointers, image.nil); // initialize all with nil
    }

    public static ContextObject create(SqueakImageContext image, int size, FrameMarker frameMarker) {
        return new ContextObject(image, size, frameMarker);
    }

    private ContextObject(SqueakImageContext image, int size, FrameMarker frameMarker) {
        this(image, size);
        this.frameMarker = frameMarker;
    }

    private ContextObject(ContextObject original) {
        super(original.image);
        pointers = original.pointers;
    }

    public void terminate() {
// atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
// setSender(image.nil); // remove sender
    }

    @Override
    public Object at0(long index) {
        assert index >= 0;
        if (index == CONTEXT.SENDER_OR_NIL) {
            return getSender(); // sender might need to be reconstructed
        }
        return super.at0(index);
    }

    @Override
    public void atput0(long index, Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            isDirty = true;
        }
        super.atput0(index, value);
    }

    public CompiledCodeObject getCodeObject() {
        BlockClosureObject closure = getClosure();
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
        int numArgs = getCodeObject().getNumArgsAndCopiedValues();
        Object[] arguments = new Object[1 + numArgs];
        BlockClosureObject closure = getClosure();
        arguments[0] = closure != null ? closure.getReceiver() : at0(CONTEXT.RECEIVER);
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = at0(CONTEXT.TEMP_FRAME_START + i);
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
        Object sender = super.at0(CONTEXT.SENDER_OR_NIL);
        if (sender instanceof ContextObject) {
            return (BaseSqueakObject) sender;
        } else if (sender instanceof NilObject) {
            return (BaseSqueakObject) sender;
        } else {
            CompilerDirectives.transferToInterpreter();
            assert sender instanceof FrameMarker;
            Frame frame = FrameAccess.findFrameForMarker((FrameMarker) sender);
            BaseSqueakObject reconstructedSender;
            if (frame == null) {
                reconstructedSender = image.nil; // no frame found, so sender must be nil
            } else {
                reconstructedSender = GetOrCreateContextNode.getOrCreate(frame);
                assert reconstructedSender != null;
            }
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
    public void setSender(Object sender) {
        super.atput0(CONTEXT.SENDER_OR_NIL, sender);
    }

    public void push(Object value) {
        assert value != null;
        long newSP = getStackPointer() + 1;
        assert newSP <= CONTEXT.MAX_STACK_SIZE;
        atStackPut(newSP, value);
        setStackPointer(newSP);
    }

    public long getInstructionPointer() {
        CompiledCodeObject code = getCodeObject();
        return decodeSqPC((long) at0(CONTEXT.INSTRUCTION_POINTER), code);
    }

    public void setInstructionPointer(long newPC) {
        long encodedPC = encodeSqPC(newPC, getCodeObject());
        assert encodedPC >= 0;
        atput0(CONTEXT.INSTRUCTION_POINTER, encodedPC);
    }

    public long getStackPointer() {
        return (long) at0(CONTEXT.STACKPOINTER);
    }

    public void setStackPointer(long newSP) {
        assert 0 <= newSP && newSP <= CONTEXT.MAX_STACK_SIZE;
        atput0(CONTEXT.STACKPOINTER, newSP);
    }

    @Override
    public String toString() {
        return "Context for " + getMethod();
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(int offset) {
        return atStack(getStackPointer() - offset);
    }

    public Object pop() {
        long sp = getStackPointer();
        if (sp > 0) {
            setStackPointer(sp - 1);
        }
        return atStack(sp);
    }

    public Object[] popNReversed(int numPop) {
        long sp = getStackPointer();
        assert sp - numPop >= 0;
        Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = atStack(sp - i);
        }
        setStackPointer(sp - numPop);
        return result;
    }

    public Object getReceiver() {
        return at0(CONTEXT.RECEIVER);
    }

    public Object atTemp(long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START + argumentIndex);
    }

    public void atTempPut(long argumentIndex, Object value) {
        atput0(CONTEXT.TEMP_FRAME_START + argumentIndex, value);
    }

    public Object atStack(long argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex);
    }

    public void atStackPut(long argumentIndex, Object value) {
        atput0(CONTEXT.TEMP_FRAME_START - 1 + argumentIndex, value);
    }

    public BlockClosureObject getClosure() {
        Object closureOrNil = at0(CONTEXT.CLOSURE_OR_NIL);
        return closureOrNil == image.nil ? null : (BlockClosureObject) closureOrNil;
    }

    public FrameMarker getFrameMarker() {
        return frameMarker;
    }

    public void setFrameMarker(FrameMarker frameMarker) {
        this.frameMarker = frameMarker;
    }

    /*
     * pc is offset by the initial pc
     */
    public static long encodeSqPC(long pc, CompiledCodeObject code) {
        return pc + code.getInitialPC();
    }

    public static long decodeSqPC(long pc, CompiledCodeObject code) {
        return pc - code.getInitialPC();
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
            image.getOutput().println(current.toString());
            BaseSqueakObject sender = current.getSender();
            if (sender == image.nil) {
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }
}

package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class ContextObject extends AbstractPointersObject {
    @CompilationFinal private FrameMarker frameMarker;
    @Child private GetOrCreateContextNode createContextNode = GetOrCreateContextNode.create();
    private boolean isDirty;

    public static ContextObject create(SqueakImageContext img) {
        return new ContextObject(img);
    }

    private ContextObject(SqueakImageContext img) {
        super(img);
    }

    public static ContextObject create(SqueakImageContext img, int size) {
        return new ContextObject(img, size);
    }

    private ContextObject(SqueakImageContext img, int size) {
        this(img);
        pointers = new Object[CONTEXT.TEMP_FRAME_START + size];
        Arrays.fill(pointers, img.nil); // initialize all with nil
    }

    public static ContextObject create(SqueakImageContext img, int size, FrameMarker frameMarker) {
        return new ContextObject(img, size, frameMarker);
    }

    private ContextObject(SqueakImageContext img, int size, FrameMarker frameMarker) {
        this(img, size);
        this.frameMarker = frameMarker;
    }

    private ContextObject(ContextObject original) {
        super(original.image);
        pointers = original.pointers;
    }

    @Override
    public ClassObject getSqClass() {
        return image.methodContextClass;
    }

    public void terminate() {
        atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
        setSender(image.nil); // remove sender
    }

    @Override
    public Object at0(int index) {
        assert index >= 0;
        if (index == CONTEXT.SENDER_OR_NIL) {
            return getSender(); // sender might need to be reconstructed
        }
        return super.at0(index);
    }

    @Override
    public void atput0(int index, Object value) {
        assert index >= 0 && value != null;
        if (index == CONTEXT.SENDER_OR_NIL) {
            isDirty = true;
        }
        super.atput0(index, value);
    }

    @Override
    public int instsize() {
        return CONTEXT.TEMP_FRAME_START;
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
        if (sender instanceof ContextObject || sender instanceof NilObject) {
            return (BaseSqueakObject) sender;
        } else {
            assert sender instanceof FrameMarker;
            Frame frame = FrameAccess.findFrameForMarker((FrameMarker) sender);
            ContextObject reconstructedSender = createContextNode.executeGet(frame);
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
    public void setSender(Object sender) {
        super.atput0(CONTEXT.SENDER_OR_NIL, sender);
    }

    public void push(Object value) {
        assert value != null;
        int newSP = stackPointer() + 1;
        atput0(newSP, value);
        setStackPointer(newSP);
    }

    public int instructionPointer() {
        return decodeSqPC((int) at0(CONTEXT.INSTRUCTION_POINTER), getCodeObject());
    }

    private int stackPointer() {
        return decodeSqueakStackPointer((int) at0(CONTEXT.STACKPOINTER));
    }

    private void setStackPointer(int newSP) {
        int encodedSP = toSqueakStackPointer(newSP);
        assert encodedSP >= -1;
        atput0(CONTEXT.STACKPOINTER, encodedSP);
    }

    @Override
    public String toString() {
        return String.format("Context for %s", at0(CONTEXT.METHOD));
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(int offset) {
        return at0(stackPointer() - offset);
    }

    public Object pop() {
        int sp = stackPointer();
        setStackPointer(sp - 1);
        return at0(sp);
    }

    public Object[] popNReversed(int numPop) {
        int sp = stackPointer();
        assert sp - numPop >= 0;
        Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = at0(sp - i);
        }
        setStackPointer(sp - numPop);
        return result;
    }

    public Object getReceiver() {
        return at0(CONTEXT.RECEIVER);
    }

    public Object atTemp(int argumentIndex) {
        return at0(CONTEXT.TEMP_FRAME_START + argumentIndex);
    }

    public void atTempPut(int argumentIndex, Object value) {
        atput0(CONTEXT.TEMP_FRAME_START + argumentIndex, value);
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
    public static int encodeSqPC(int pc, CompiledCodeObject code) {
        return pc + code.getInitialPC();
    }

    public static int decodeSqPC(int pc, CompiledCodeObject code) {
        return pc - code.getInitialPC();
    }

    /*
     * sp is offset by CONTEXT.TEMP_FRAME_START, -1 for zero-based addressing
     */
    public static int toSqueakStackPointer(int sp) {
        return sp - (CONTEXT.TEMP_FRAME_START - 1);
    }

    public static int decodeSqueakStackPointer(int sp) {
        return sp + (CONTEXT.TEMP_FRAME_START - 1);
    }
}

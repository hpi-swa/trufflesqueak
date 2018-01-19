package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class ContextObject extends AbstractPointersObject {
    @CompilationFinal private FrameDescriptor frameDescriptor;
    @CompilationFinal private FrameMarker frameMarker;
    private boolean isDirty;

    public static ContextObject create(SqueakImageContext img, Frame virtualFrame) {
        MaterializedFrame frame = virtualFrame.materialize();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameSlot thisContextSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT);
        ContextObject contextObject = (ContextObject) FrameUtil.getObjectSafe(frame, thisContextSlot);
        if (contextObject == null) {
            contextObject = new ContextObject(img, frame);
            // do not attach ReadOnlyContextObject to thisContextSlot to avoid becoming non-virtualized
        }
        return contextObject;
    }

    public static ContextObject create(SqueakImageContext img) {
        return new ContextObject(img);
    }

    public static ContextObject create(SqueakImageContext img, int size) {
        return new ContextObject(img, size);
    }

    private ContextObject(SqueakImageContext img, Frame frame) {
        super(img);
        frameDescriptor = frame.getFrameDescriptor();
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        ContextObject sender = FrameAccess.getSender(frame);
        FrameSlot stackPointerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.STACK_POINTER);

        pointers = new Object[CONTEXT.TEMP_FRAME_START + method.frameSize()];
        setSender(sender == null ? image.nil : sender);
        atput0(CONTEXT.INSTRUCTION_POINTER, method.getInitialPC());
        atput0(CONTEXT.STACKPOINTER, FrameUtil.getIntSafe(frame, stackPointerSlot));
        atput0(CONTEXT.METHOD, method);
        atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? image.nil : closure);
        atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));
    }

    private ContextObject(SqueakImageContext img) {
        super(img);
    }

    private ContextObject(SqueakImageContext img, int size) {
        super(img);
        pointers = new Object[CONTEXT.TEMP_FRAME_START + size];
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
    public void atput0(int index, Object value) {
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
        if (closure != null) {
            arguments[0] = closure.getReceiver();
        } else {
            arguments[0] = at0(CONTEXT.RECEIVER);
        }
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = at0(CONTEXT.TEMP_FRAME_START + i);
        }
        return arguments;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public ContextObject getSender() {
        Object sender = at0(CONTEXT.SENDER_OR_NIL);
        if (sender instanceof ContextObject) {
            return (ContextObject) sender;
        } else if (sender instanceof NilObject) {
            return null;
        } else if (sender == null) { // null indicates virtual frame, reconstructing contexts...
            Frame frame = FrameAccess.currentMaterializableFrame();
            ContextObject reconstructedSender = FrameAccess.findSenderForMarker(frame, getCodeObject().markerSlot, image);
            if (reconstructedSender == null) {
                throw new RuntimeException("Unable to find sender");
            }
            setSender(reconstructedSender);
            return reconstructedSender;
        }
        throw new RuntimeException("Unexpected sender: " + sender);
    }

    /*
     * Set sender without flagging context as dirty.
     */
    public void setSender(Object sender) {
        super.atput0(CONTEXT.SENDER_OR_NIL, sender);
    }

    public void push(Object value) {
        assert value != null;
        int sp = stackPointer();
        atput0(sp, value);
        setStackPointer(sp + 1);
    }

    private int stackPointer() {
        return CONTEXT.TEMP_FRAME_START + (int) at0(CONTEXT.STACKPOINTER) - 1;
    }

    private void setStackPointer(int newSP) {
        int encodedSP = newSP + 1 - CONTEXT.TEMP_FRAME_START;
        assert encodedSP >= 0;
        atput0(CONTEXT.STACKPOINTER, encodedSP);
    }

    @Override
    public String toString() {
        return String.format("Context for %s", at0(CONTEXT.METHOD));
    }

    public boolean hasSameMethodObject(ContextObject obj) {
        return getMethod().equals(obj.getMethod());
    }

    public Object top() {
        return peek(0);
    }

    public Object peek(int offset) {
        return at0(stackPointer() - 1 - offset);
    }

    public Object pop() {
        int newSP = stackPointer() - 1;
        setStackPointer(newSP);
        return at0(newSP);
    }

    public Object[] popNReversed(int numPop) {
        int sp = stackPointer();
        assert sp - numPop >= -1;
        Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[numPop - 1 - i] = at0(sp - 1 - i);
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
}

package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class MethodContextObject extends BaseSqueakObject {
    @Child private BlockActivationNode dispatch = BlockActivationNodeGen.create();
    @CompilationFinal private ActualContextObject actualContext;
    private boolean isDirty;

    public static MethodContextObject createReadOnlyContextObject(SqueakImageContext img, Frame virtualFrame) {
        MaterializedFrame frame = virtualFrame.materialize();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameSlot thisContextSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT);
        MethodContextObject contextObject = (MethodContextObject) FrameUtil.getObjectSafe(frame, thisContextSlot);
        if (contextObject == null) {
            contextObject = new MethodContextObject(img, new ReadOnlyContextObject(img, frame));
            frame.setObject(thisContextSlot, contextObject);
        }
        return contextObject;
    }

    public static MethodContextObject createWriteableContextObject(SqueakImageContext img) {
        return new MethodContextObject(img, new WriteableContextObject(img));
    }

    public static MethodContextObject createWriteableContextObject(SqueakImageContext img, int size) {
        return new MethodContextObject(img, new WriteableContextObject(img, size));
    }

    private MethodContextObject(SqueakImageContext img, ActualContextObject context) {
        super(img);
        actualContext = context;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        assert actualContext instanceof WriteableContextObject;
        actualContext.fillin(chunk);
    }

    @Override
    public ClassObject getSqClass() {
        return image.methodContextClass;
    }

    @Override
    public Object at0(int index) {
        return actualContext.at0(index);
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
        try {
            actualContext.atContextPut0(index, value);
        } catch (NonVirtualContextModification e) {
            beWriteable();
            actualContext.atput0(index, value);
        }
    }

    private void beWriteable() {
        if (actualContext instanceof ReadOnlyContextObject) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            actualContext = new WriteableContextObject(image, (ReadOnlyContextObject) actualContext);
        }
    }

    @Override
    public int size() {
        return actualContext.size();
    }

    @Override
    public int instsize() {
        // the receiver is part of the "variable part", because it is on the stack in Squeak
        return CONTEXT.TEMP_FRAME_START - 1;
    }

    public CompiledCodeObject getCodeObject() {
        Object closure = at0(CONTEXT.CLOSURE_OR_NIL);
        if (closure instanceof BlockClosureObject) {
            return ((BlockClosureObject) closure).getCompiledBlock();
        }
        return (CompiledCodeObject) at0(CONTEXT.METHOD);
    }

    public Object getFrameMarker() {
        return actualContext.getFrameMarker();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return actualContext.shallowCopy();
    }

    public Object[] getReceiverAndArguments() {
        int numArgs = getCodeObject().getNumArgsAndCopiedValues();
        Object[] arguments = new Object[1 + numArgs];
        Object method = at0(CONTEXT.METHOD);
        if (method instanceof BlockClosureObject) {
            arguments[0] = ((BlockClosureObject) method).getReceiver();
        } else {
            arguments[0] = actualContext.at0(CONTEXT.TEMP_FRAME_START);
        }
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = actualContext.at0(CONTEXT.TEMP_FRAME_START + 1 + i);
        }
        return arguments;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public MethodContextObject getSender() {
        Object sender = actualContext.at0(CONTEXT.SENDER_OR_NIL);
        if (sender instanceof MethodContextObject) {
            return (MethodContextObject) sender;
        } else if (sender instanceof NilObject) {
            return null;
        }
        throw new RuntimeException("Unexpected sender: " + sender);
    }

    /*
     * Set sender without flagging context as dirty.
     */
    public void setSender(Object sender) {
        actualContext.atput0(CONTEXT.SENDER_OR_NIL, sender);
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
        return actualContext.toString();
    }

    public boolean hasSameMethodObject(MethodContextObject obj) {
        return actualContext.at0(CONTEXT.METHOD).equals(obj.actualContext.at0(CONTEXT.METHOD));
    }

    public Object top() {
        return at0(stackPointer() - 1);
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
        return closureOrNil.equals(image.nil) ? null : (BlockClosureObject) closureOrNil;
    }
}

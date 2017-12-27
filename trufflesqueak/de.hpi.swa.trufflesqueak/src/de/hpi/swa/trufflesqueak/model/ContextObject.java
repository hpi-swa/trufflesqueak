package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.util.Constants.BLOCK_CONTEXT;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class ContextObject extends BaseSqueakObject {
    private ActualContextObject actualContext;

    private ContextObject(SqueakImageContext img, ActualContextObject context) {
        super(img);
        actualContext = context;
    }

    public static ContextObject createReadOnlyContextObject(SqueakImageContext img, Frame virtualFrame) {
        MaterializedFrame frame = virtualFrame.materialize();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameSlot selfSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.SELF);
        Object contextObject = FrameUtil.getObjectSafe(frame, selfSlot);
        if (contextObject instanceof ContextObject) {
            return (ContextObject) contextObject;
        } else if (contextObject instanceof NilObject) {
            ContextObject newContextObject = new ContextObject(img, new ReadOnlyContextObject(img, frame));
            frame.setObject(selfSlot, newContextObject);
            return newContextObject;
        }
        throw new RuntimeException("Unexpected state");
    }

    public static ContextObject createWriteableContextObject(SqueakImageContext img) {
        return new ContextObject(img, new WriteableContextObject(img));
    }

    public static ContextObject createNewContextObject(CompiledCodeObject code) {
        ContextObject context = createWriteableContextObject(code.image);
        context.initializePointers(CONTEXT.TEMP_FRAME_START);
        context.atput0(CONTEXT.INSTRUCTION_POINTER, code.getBytecodeOffset() + 1);
        context.atput0(CONTEXT.STACKPOINTER, 0);
        context.atput0(CONTEXT.METHOD, code);
        return context;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        assert actualContext instanceof WriteableContextObject;
        actualContext.fillin(chunk);
    }

    public void initializePointers(int size) {
        assert actualContext instanceof WriteableContextObject;
        ((WriteableContextObject) actualContext).initializePointers(size);
    }

    @Override
    public ClassObject getSqClass() {
        return image.methodContextClass;
    }

    @Override
    public Object at0(int l) {
        return actualContext.at0(l);
    }

    @Override
    public void atput0(int idx, Object object) {
        try {
            actualContext.atContextPut0(idx, object);
        } catch (NonVirtualContextModification e) {
            beWriteable();
            actualContext.atput0(idx, object);
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

    public void step() {
        beWriteable();
    }

    public CompiledCodeObject getCodeObject() {
        Object method = at0(CONTEXT.METHOD);
        if (method instanceof Integer) { // if the method field is an integer, activeContex is a block context
            ContextObject homeContext = (ContextObject) at0(BLOCK_CONTEXT.HOME);
            return (CompiledCodeObject) homeContext.at0(CONTEXT.METHOD);
        } else {
            return (CompiledCodeObject) method;
        }
    }

    public Object getFrameMarker() {
        return actualContext.getFrameMarker();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return actualContext.shallowCopy();
    }
}

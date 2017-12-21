package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
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
        return ContextPartConstants.TEMP_FRAME_START - 1;
    }

    public void step() {
        beWriteable();
        throw new RuntimeException("stepping in context not implemented yet");
    }

    public Object getFrameMarker() {
        return actualContext.getFrameMarker();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return actualContext.shallowCopy();
    }
}

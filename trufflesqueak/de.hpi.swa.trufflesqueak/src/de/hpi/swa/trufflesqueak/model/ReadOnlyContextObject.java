package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.util.Chunk;

/**
 * A ReadOnlyContextObject is backed by a MaterializedFrame. Even though the frame is materialized,
 * we cannot allow all modifications, because most modifications that make sense to do manipulate
 * the sender chain or properties of the Squeak execution state. These cannot be adequately
 * represented in an execution.
 */
public class ReadOnlyContextObject extends BaseSqueakObject implements ActualContextObject {
    private final MaterializedFrame frame;
    private final FrameSlot methodSlot;
    private final FrameSlot closureSlot;
    private final FrameSlot rcvrSlot;
    private final FrameSlot markerSlot;
    private final FrameDescriptor frameDescriptor;
    private final int pc;
    private final int stackPointer;
    private Object sender;

    public ReadOnlyContextObject(SqueakImageContext img, MaterializedFrame materializedFrame) {
        super(img);
        frame = materializedFrame;
        frameDescriptor = frame.getFrameDescriptor();
        methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
        markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
        closureSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.CLOSURE);
        rcvrSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.RECEIVER);
        pc = 0;
        stackPointer = getMethod().getNumStackSlots(); // we assume that at this point, we have our
                                                       // temps and args initialized
    }

    @Override
    public Object at0(int i) {
        switch (i) {
            case ContextPartConstants.SENDER:
                return getSender();
            case ContextPartConstants.PC:
                return pc;
            case ContextPartConstants.SP:
                return stackPointer + 1;
            case ContextPartConstants.METHOD:
                return getMethod();
            case ContextPartConstants.CLOSURE:
                return getClosure();
            case ContextPartConstants.RECEIVER:
                return getReceiver();
            default:
                return getTemp(i - ContextPartConstants.TEMP_FRAME_START);
        }
    }

    private Object getTemp(int i) {
        if (i < 0) {
            return null;
        }
        if (i < stackPointer) {
            FrameSlot frameSlot = frameDescriptor.findFrameSlot(i);
            if (frameSlot.getKind().equals(FrameSlotKind.Boolean)) {
                return FrameUtil.getBooleanSafe(frame, frameSlot);
            } else if (frameSlot.getKind().equals(FrameSlotKind.Double)) {
                return FrameUtil.getDoubleSafe(frame, frameSlot);
            } else if (frameSlot.getKind().equals(FrameSlotKind.Int)) {
                return FrameUtil.getIntSafe(frame, frameSlot);
            } else if (frameSlot.getKind().equals(FrameSlotKind.Long)) {
                return FrameUtil.getLongSafe(frame, frameSlot);
            } else if (frameSlot.getKind().equals(FrameSlotKind.Illegal)) {
                return null;
            } else if (frameSlot.getKind().equals(FrameSlotKind.Object)) {
                return FrameUtil.getObjectSafe(frame, frameSlot);
            } else {
                throw new RuntimeException("unexpected frame slot kind");
            }
        }
        return null;
    }

    private void setTemp(int i, Object o) throws NonVirtualContextModification {
        if (i >= 0 && i < stackPointer) {
            FrameSlot frameSlot = frameDescriptor.findFrameSlot(i);
            frameSlot.setKind(FrameSlotKind.Object);
            frame.setObject(frameSlot, o);
        }
        throw new NonVirtualContextModification();
    }

    private Object getReceiver() {
        return FrameUtil.getObjectSafe(frame, rcvrSlot);
    }

    private Object getClosure() {
        return FrameUtil.getObjectSafe(frame, closureSlot);
    }

    private CompiledCodeObject getMethod() {
        return (CompiledCodeObject) FrameUtil.getObjectSafe(frame, methodSlot);
    }

    private Object getSender() {
        if (sender == null) {
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                boolean foundMyself = false;
                Object marker = FrameUtil.getObjectSafe(frame, markerSlot);

                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    FrameDescriptor currentFD = current.getFrameDescriptor();
                    FrameSlot currentMarkerSlot = currentFD.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                    if (foundMyself) {
                        sender = ContextObject.createReadOnlyContextObject(image, current);
                        return sender;
                    } else if (marker == FrameUtil.getObjectSafe(current, currentMarkerSlot)) {
                        foundMyself = true;
                    }
                    return null;
                }
            });
        }
        return sender;
    }

    public void atContextPut0(int i, Object obj) throws NonVirtualContextModification {
        switch (i) {
            case ContextPartConstants.RECEIVER:
                frame.setObject(rcvrSlot, obj);
                break;
            default:
                setTemp(i - ContextPartConstants.TEMP_FRAME_START, obj);
        }
    }

    @Override
    public void fillin(Chunk chunk) {
        throw new RuntimeException("should not implement");
    }

    @Override
    public ClassObject getSqClass() {
        return null;
    }

    @Override
    public int size() {
        return stackPointer + ContextPartConstants.TEMP_FRAME_START;
    }

    @Override
    public int instsize() {
        return ContextPartConstants.RECEIVER;
    }

    @Override
    public void atput0(int idx, Object object) {
        throw new RuntimeException("should not implement");
    }

    public Object getFrameMarker() {
        return FrameUtil.getObjectSafe(frame, markerSlot);
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new WriteableContextObject(image, this);
    }
}

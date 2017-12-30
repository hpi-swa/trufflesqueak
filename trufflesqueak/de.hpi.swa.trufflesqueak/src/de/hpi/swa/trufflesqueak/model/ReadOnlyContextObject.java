package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
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
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

/**
 * A ReadOnlyContextObject is backed by a MaterializedFrame. Even though the frame is materialized,
 * we cannot allow all modifications, because most modifications that make sense to do manipulate
 * the sender chain or properties of the Squeak execution state. These cannot be adequately
 * represented in an execution.
 */
public class ReadOnlyContextObject extends BaseSqueakObject implements ActualContextObject {
    @CompilationFinal private final MaterializedFrame frame;
    @CompilationFinal private final FrameSlot methodSlot;
    @CompilationFinal private final FrameSlot closureSlot;
    @CompilationFinal private final FrameSlot markerSlot;
    @CompilationFinal private final FrameDescriptor frameDescriptor;
    @CompilationFinal private final int stackPointer;
    @CompilationFinal private Object receiver;
    @CompilationFinal private Object sender;

    public ReadOnlyContextObject(SqueakImageContext img, MaterializedFrame materializedFrame) {
        super(img);
        frame = materializedFrame;
        frameDescriptor = frame.getFrameDescriptor();
        methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
        markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
        closureSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.CLOSURE);
        receiver = frame.getArguments()[0];
        stackPointer = getMethod().getNumTemps() - 1;
    }

    @Override
    public Object at0(int i) {
        switch (i) {
            case CONTEXT.SENDER:
                return getSender();
            case CONTEXT.INSTRUCTION_POINTER:
                return -1;
            case CONTEXT.STACKPOINTER:
                return stackPointer;
            case CONTEXT.METHOD:
                return getMethod();
            case CONTEXT.CLOSURE:
                return getClosure();
            case CONTEXT.RECEIVER:
                return receiver;
            default:
                return getTemp(i - CONTEXT.TEMP_FRAME_START);
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

    private Object getClosure() {
        return FrameUtil.getObjectSafe(frame, closureSlot);
    }

    private CompiledCodeObject getMethod() {
        return (CompiledCodeObject) FrameUtil.getObjectSafe(frame, methodSlot);
    }

    private Object getSender() {
        if (sender == null) {
            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                boolean foundMyself = false;
                Object marker = FrameUtil.getObjectSafe(frame, markerSlot);

                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
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
            if (sender == null) {
                throw new RuntimeException("Unable to find sender");
            }
        }
        return sender;
    }

    public void atContextPut0(int i, Object obj) throws NonVirtualContextModification {
        switch (i) {
            case CONTEXT.RECEIVER:
                receiver = obj;
                break;
            default:
                setTemp(i - CONTEXT.TEMP_FRAME_START, obj);
        }
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        throw new RuntimeException("should not implement");
    }

    @Override
    public ClassObject getSqClass() {
        return null;
    }

    @Override
    public int size() {
        return stackPointer + CONTEXT.TEMP_FRAME_START;
    }

    @Override
    public int instsize() {
        return CONTEXT.RECEIVER;
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

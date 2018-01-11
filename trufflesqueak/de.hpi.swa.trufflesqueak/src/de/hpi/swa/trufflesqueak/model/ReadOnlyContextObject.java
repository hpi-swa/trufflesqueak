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
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.FrameAccess;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

/**
 * A ReadOnlyContextObject is backed by a MaterializedFrame. Even though the frame is materialized,
 * we cannot allow all modifications, because most modifications that make sense to do manipulate
 * the sender chain or properties of the Squeak execution state. These cannot be adequately
 * represented in an execution.
 */
public class ReadOnlyContextObject extends BaseSqueakObject implements ActualContextObject {
    @CompilationFinal private final MaterializedFrame frame;
    @CompilationFinal private final FrameSlot stackPointerSlot;
    @CompilationFinal private final FrameSlot markerSlot;
    @CompilationFinal private final CompiledCodeObject method;
    @CompilationFinal private final FrameDescriptor frameDescriptor;
    @CompilationFinal private Object receiver;
    @CompilationFinal private MethodContextObject sender;
    @CompilationFinal private int pc = -1;
    @CompilationFinal private BlockClosureObject closure;

    public ReadOnlyContextObject(SqueakImageContext img, MaterializedFrame materializedFrame) {
        super(img);
        frame = materializedFrame;
        frameDescriptor = frame.getFrameDescriptor();
        markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
        method = FrameAccess.getMethod(frame);
        closure = FrameAccess.getClosure(frame);
        stackPointerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.STACK_POINTER);
        receiver = frame.getArguments()[0];
    }

    @Override
    public Object at0(int i) {
        switch (i) {
            case CONTEXT.SENDER_OR_NIL:
                return getSender();
            case CONTEXT.INSTRUCTION_POINTER:
                return getPC();
            case CONTEXT.STACKPOINTER:
                return getStackPointer();
            case CONTEXT.METHOD:
                return method;
            case CONTEXT.CLOSURE_OR_NIL:
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
        if (i < getStackPointer()) {
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
        if (i >= 0 && i < getStackPointer()) {
            FrameSlot frameSlot = frameDescriptor.findFrameSlot(i);
            frameSlot.setKind(FrameSlotKind.Object);
            frame.setObject(frameSlot, o);
        }
        throw new NonVirtualContextModification();
    }

    private Object getClosure() {
        return FrameAccess.getClosure(frame);
    }

    private int getPC() {
        if (pc == -1) { // TODO: is this still needed?
            pc = method.getBytecodeOffset() + 1;
        }
        return pc;
    }

    private int getStackPointer() {
        return FrameUtil.getIntSafe(frame, stackPointerSlot);
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
                        sender = MethodContextObject.createReadOnlyContextObject(image, current);
                        return sender;
                    } else if (marker == FrameUtil.getObjectSafe(current, currentMarkerSlot)) {
                        foundMyself = true;
                    }
                    return null;
                }
            });
            if (sender == null) {
                throw new RuntimeException("Unable to find sender"); // this might need to return nil to indicate that no sender was found
            }
        }
        return sender;
    }

    public void atContextPut0(int i, Object obj) throws NonVirtualContextModification {
        switch (i) {
            case CONTEXT.RECEIVER:
                receiver = obj;
                break;
            case CONTEXT.SENDER_OR_NIL:
                sender = (MethodContextObject) obj;
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
        return getStackPointer() + CONTEXT.TEMP_FRAME_START;
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

    @Override
    public String toString() {
        return String.format("Readonly context for %s", at0(CONTEXT.METHOD));
    }
}

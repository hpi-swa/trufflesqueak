package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class FrameAccess {
    /**
     * Our frame:
     *
     * <pre>
     * CompiledCodeObject
     * ClosureOrNull
     * Receiver
     * Args*
     * CopiedValues*
     * </pre>
     */
    public static final int METHOD = 0;
    public static final int SENDER_OR_NULL = 1;
    public static final int CLOSURE_OR_NULL = 2;
    public static final int RECEIVER = 3;
    public static final int RCVR_AND_ARGS_START = 3;
    public static final int TEMP_START = 4;

    public static CompiledCodeObject getMethod(Frame frame) {
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public static MethodContextObject getSender(Frame frame) {
        return (MethodContextObject) frame.getArguments()[SENDER_OR_NULL];
    }

    public static BlockClosureObject getClosure(Frame frame) {
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static Object getReceiver(Frame frame) {
        return frame.getArguments()[RECEIVER];
    }

    public static Object getArg(Frame frame, int idx) {
        return frame.getArguments()[idx + TEMP_START];
    }

    public static Object[] newWith(CompiledCodeObject code, MethodContextObject sender, BlockClosureObject closure, Object[] frameArgs) {
        Object[] arguments = new Object[RCVR_AND_ARGS_START + frameArgs.length];
        arguments[METHOD] = code;
        arguments[SENDER_OR_NULL] = sender;
        arguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < frameArgs.length; i++) {
            arguments[RCVR_AND_ARGS_START + i] = frameArgs[i];
        }
        return arguments;
    }

    public static MethodContextObject findContextForMarker(FrameMarker frameMarker, SqueakImageContext image) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MethodContextObject>() {
            @Override
            public MethodContextObject visitFrame(FrameInstance frameInstance) {
                Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
                FrameSlot markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                Object marker = FrameUtil.getObjectSafe(frame, markerSlot);
                if (marker == frameMarker) {
                    return MethodContextObject.createReadOnlyContextObject(image, frame);
                }
                return null;
            }
        });
    }

    public static MethodContextObject findSenderForMarker(FrameMarker frameMarker, SqueakImageContext image) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MethodContextObject>() {
            boolean foundMyself = false;

            @Override
            public MethodContextObject visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                FrameDescriptor currentFD = current.getFrameDescriptor();
                FrameSlot currentMarkerSlot = currentFD.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                if (foundMyself) {
                    return MethodContextObject.createReadOnlyContextObject(image, current);
                } else if (frameMarker == FrameUtil.getObjectSafe(current, currentMarkerSlot)) {
                    foundMyself = true;
                }
                return null;
            }
        });
    }
}

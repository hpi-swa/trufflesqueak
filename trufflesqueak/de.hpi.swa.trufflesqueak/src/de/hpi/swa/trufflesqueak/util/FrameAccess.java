package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class FrameAccess {
    /**
     * TruffleSqueak frame:
     *
     * <pre>
     * CompiledCodeObject
     * SenderOrNull
     * ClosureOrNull
     * Receiver
     * Arguments*
     * CopiedValues*
     * </pre>
     */
    public static final int METHOD = 0;
    public static final int SENDER_OR_SENDER_MARKER = 1;
    public static final int CLOSURE_OR_NULL = 2;
    public static final int RECEIVER = 3;
    public static final int RCVR_AND_ARGS_START = 3;

    public static CompiledCodeObject getMethod(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public static Object getSender(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[SENDER_OR_SENDER_MARKER];
    }

    public static BlockClosureObject getClosure(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static Object getReceiver(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[RECEIVER];
    }

    public static Object[] getArguments(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        int index = 0;
        Object[] arguments = new Object[frame.getArguments().length - RCVR_AND_ARGS_START];
        for (Object argument : frame.getArguments()) {
            if (index >= RCVR_AND_ARGS_START) {
                arguments[index - RCVR_AND_ARGS_START] = argument;
            }
            index++;
        }
        return arguments;
    }

    public static Object getContextOrMarker(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            return frame.getObject(getMethod(frame).thisContextOrMarkerSlot);
        } catch (FrameSlotTypeException e) {
            throw new SqueakException("thisContextOrMarkerSlot should never be invalid");
        }
    }

    @ExplodeLoop
    public static Object[] newWith(CompiledCodeObject code, Object sender, BlockClosureObject closure, Object[] frameArgs) {
        Object[] arguments = new Object[RCVR_AND_ARGS_START + frameArgs.length];
        arguments[METHOD] = code;
        arguments[SENDER_OR_SENDER_MARKER] = sender;
        arguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < frameArgs.length; i++) {
            arguments[RCVR_AND_ARGS_START + i] = frameArgs[i];
        }
        return arguments;
    }

    @TruffleBoundary
    public static Frame findFrameForMarker(FrameMarker frameMarker) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getArguments().length < RCVR_AND_ARGS_START) {
                    return null;
                }
                FrameDescriptor frameDescriptor = current.getFrameDescriptor();
                FrameSlot contextOrMarkerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER);
                if (frameMarker == FrameUtil.getObjectSafe(current, contextOrMarkerSlot)) {
                    return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                }
                return null;
            }
        });
    }
}

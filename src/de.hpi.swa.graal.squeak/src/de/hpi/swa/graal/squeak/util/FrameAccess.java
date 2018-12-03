package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;

public class FrameAccess {
    /**
     * GraalSqueak frame arguments.
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
    public static final int ARGUMENTS_START = 4;

    /**
     * GraalSqueak frame slots.
     *
     * <pre>
     * thisContextOrMarker
     * instructionPointer
     * stackPointer
     * stack*
     * </pre>
     */

    public static final CompiledCodeObject getMethod(final Frame frame) {
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public static final Object getSender(final Frame frame) {
        return frame.getArguments()[SENDER_OR_SENDER_MARKER];
    }

    public static final BlockClosureObject getClosure(final Frame frame) {
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static final Object getReceiver(final Frame frame) {
        return frame.getArguments()[RECEIVER];
    }

    public static final Object getArgument(final Frame frame, final int index) {
        return frame.getArguments()[RECEIVER + index];
    }

    public static final Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), RECEIVER, frame.getArguments().length);
    }

    public static final Object getContextOrMarker(final Frame frame) {
        return FrameUtil.getObjectSafe(frame, getMethod(frame).thisContextOrMarkerSlot);
    }

    public static final boolean isGraalSqueakFrame(final Frame frame) {
        final Object[] arguments = frame.getArguments();
        return arguments.length >= RECEIVER && arguments[METHOD] instanceof CompiledCodeObject;
    }

    public static final Object[] newWith(final CompiledCodeObject code, final Object sender, final BlockClosureObject closure, final Object[] frameArgs) {
        final Object[] arguments = new Object[RECEIVER + frameArgs.length];
        arguments[METHOD] = code;
        arguments[SENDER_OR_SENDER_MARKER] = sender;
        arguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < frameArgs.length; i++) {
            arguments[RECEIVER + i] = frameArgs[i];
        }
        return arguments;
    }

    @TruffleBoundary
    public static final Frame findFrameForMarker(final FrameMarker frameMarker) {
        CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            @Override
            public Frame visitFrame(final FrameInstance frameInstance) {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (!isGraalSqueakFrame(current)) {
                    return null;
                }
                final Object contextOrMarker = getContextOrMarker(current);
                if (frameMarker == contextOrMarker) {
                    return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                }
                return null;
            }
        });
    }
}

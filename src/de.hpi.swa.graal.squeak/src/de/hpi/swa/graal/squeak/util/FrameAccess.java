package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
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
    @CompilationFinal public static final int METHOD = 0;
    @CompilationFinal public static final int SENDER_OR_SENDER_MARKER = 1;
    @CompilationFinal public static final int CLOSURE_OR_NULL = 2;
    @CompilationFinal public static final int RECEIVER = 3;
    @CompilationFinal public static final int ARGUMENTS_START = 4;

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
    @CompilationFinal public static final int CONTEXT_OR_MARKER = 0;

    public static final CompiledCodeObject getMethod(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public static final Object getSender(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[SENDER_OR_SENDER_MARKER];
    }

    public static final BlockClosureObject getClosure(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static final Object getReceiver(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[RECEIVER];
    }

    public static final Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), RECEIVER, frame.getArguments().length);
    }

    public static final Object getContextOrMarker(final Frame frame, final FrameSlot contextOrMarkerSlot) {
        try {
            return frame.getObject(contextOrMarkerSlot);
        } catch (FrameSlotTypeException e) {
            throw new SqueakException("thisContextOrMarkerSlot should never be invalid");
        }
    }

    public static final Object getContextOrMarker(final Frame frame) {
        // TODO: should not be used
        return getContextOrMarker(frame, getContextOrMarkerSlot(frame));
    }

    public static final FrameSlot getContextOrMarkerSlot(final Frame frame) {
        return frame.getFrameDescriptor().getSlots().get(CONTEXT_OR_MARKER);
    }

    @TruffleBoundary
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
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            @Override
            public Frame visitFrame(final FrameInstance frameInstance) {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getFrameDescriptor().getSize() <= 0) {
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

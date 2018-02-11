package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public class FrameAccess {
    /**
     * TruffleSqueak frame arguments:
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
    @CompilationFinal public static final int RCVR_AND_ARGS_START = 3;

    /**
     * TruffleSqueak frame slots:
     *
     * <pre>
     * thisContextOrMarker
     * instructionPointer
     * stackPointer
     * stack*
     * </pre>
     */
    @CompilationFinal public static final int CONTEXT_OR_MARKER = 0;

    public final static CompiledCodeObject getMethod(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public final static Object getSender(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[SENDER_OR_SENDER_MARKER];
    }

    public final static BlockClosureObject getClosure(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public final static Object getReceiver(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getArguments()[RECEIVER];
    }

    public final static Object[] getArguments(Frame frame) {
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

    public final static Object getContextOrMarker(Frame frame) {
        try {
            return frame.getObject(frame.getFrameDescriptor().getSlots().get(CONTEXT_OR_MARKER));
        } catch (FrameSlotTypeException e) {
            throw new SqueakException("thisContextOrMarkerSlot should never be invalid");
        }
    }

    public final static Object[] newWith(CompiledCodeObject code, Object sender, BlockClosureObject closure, Object[] frameArgs) {
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
    public final static Frame findFrameForMarker(FrameMarker frameMarker) {
        return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
            @Override
            public Frame visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getFrameDescriptor().getSize() <= 0) {
                    return null;
                }
                Object contextOrMarker = getContextOrMarker(current);
                if (isMatchingMarker(frameMarker, contextOrMarker)) {
                    return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
                }
                return null;
            }
        });
    }

    public final static boolean isMatchingMarker(FrameMarker frameMarker, Object contextOrMarker) {
        return frameMarker == contextOrMarker || (contextOrMarker instanceof ContextObject && frameMarker == ((ContextObject) contextOrMarker).getFrameMarker());
    }
}

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;

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
}

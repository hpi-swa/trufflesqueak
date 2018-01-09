package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

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
    public static final int CLOSURE_OR_NULL = 1;
    public static final int RECEIVER = 2;
    public static final int RCVR_AND_ARGS_START = 2;
    public static final int TEMP_START = 3;

    public static CompiledCodeObject getMethod(VirtualFrame frame) {
        return (CompiledCodeObject) frame.getArguments()[METHOD];
    }

    public static BlockClosureObject getClosure(VirtualFrame frame) {
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static Object getReceiver(VirtualFrame frame) {
        return frame.getArguments()[RECEIVER];
    }

    public static Object getArg(VirtualFrame frame, int idx) {
        return frame.getArguments()[idx + TEMP_START];
    }

    public static Object[] newWith(CompiledCodeObject code, Object closure, Object[] frameArgs) {
        assert closure != code.image.nil;
        Object[] arguments = new Object[RCVR_AND_ARGS_START + frameArgs.length];
        arguments[METHOD] = code;
        arguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < frameArgs.length; i++) {
            arguments[RCVR_AND_ARGS_START + i] = frameArgs[i];
        }
        return arguments;
    }
}

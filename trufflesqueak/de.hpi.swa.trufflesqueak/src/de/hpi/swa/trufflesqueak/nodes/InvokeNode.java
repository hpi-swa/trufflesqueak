package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class InvokeNode extends Node {

    public static InvokeNode create() {
        return InvokeNodeGen.create();
    }

    public abstract Object executeInvoke(CompiledCodeObject method, Object[] arguments);

    @Specialization(guards = "code.getCallTarget() == callTarget", limit = "1")
    protected Object doInvoke(CompiledCodeObject code, Object[] arguments,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") RootCallTarget callTarget,
                    @Cached("create(callTarget)") DirectCallNode callNode) {
        Object[] frameArgs = new Object[FrameAccess.RCVR_AND_ARGS_START + arguments.length]; // arguments array already contains receiver
        frameArgs[FrameAccess.METHOD] = code;
        frameArgs[FrameAccess.CLOSURE_OR_NULL] = null;
        System.arraycopy(arguments, 0, frameArgs, FrameAccess.RCVR_AND_ARGS_START, arguments.length);
        return callNode.call(arguments);
    }
}

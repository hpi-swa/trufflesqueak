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
    protected Object doInvoke(@SuppressWarnings("unused") CompiledCodeObject code, Object[] arguments,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") RootCallTarget callTarget,
                    @Cached("create(callTarget)") DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doInvoke")
    protected Object doIndirect(CompiledCodeObject code, Object[] arguments) {
        RootCallTarget callTarget = code.getCallTarget();
        DirectCallNode callNode = DirectCallNode.create(callTarget);
        return callNode.call(arguments);
    }
}

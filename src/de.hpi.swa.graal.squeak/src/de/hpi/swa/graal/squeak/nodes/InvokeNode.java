package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class InvokeNode extends Node {

    public static InvokeNode create() {
        return InvokeNodeGen.create();
    }

    public abstract Object executeInvoke(CompiledCodeObject method, Object[] arguments);

    @Specialization(guards = "code.getCallTarget() == callTarget", limit = "1")
    protected static final Object doInvoke(@SuppressWarnings("unused") final CompiledCodeObject code, final Object[] arguments,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") final RootCallTarget callTarget,
                    @SuppressWarnings("unused") @Cached("code.getSplitCallTarget()") final RootCallTarget splitCallTarget,
                    @Cached("create(splitCallTarget)") final DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doInvoke")
    protected static final Object doIndirect(final CompiledCodeObject code, final Object[] arguments,
                    @Cached("create()") final IndirectCallNode callNode) {
        return callNode.call(code.getCallTarget(), arguments);
    }
}

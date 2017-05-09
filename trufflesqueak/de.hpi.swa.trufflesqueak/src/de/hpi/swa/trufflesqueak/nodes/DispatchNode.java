package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class DispatchNode extends Node {
    public abstract Object executeDispatch(Object method, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"method == cachedMethod"}, assumptions = {"callTargetStable"})
    protected static Object doDirect(CompiledMethodObject method, Object[] arguments,
                    @Cached("method") CompiledMethodObject cachedMethod,
                    @Cached("method.getCallTarget()") RootCallTarget cachedTarget,
                    @Cached("method.getCallTargetStable()") Assumption callTargetStable,
                    @Cached("create(cachedTarget)") DirectCallNode callNode) {
        return callNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(CompiledCodeObject method, Object[] arguments,
                    @Cached("create()") IndirectCallNode callNode) {
        return callNode.call(method.getCallTarget(), arguments);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object fail(Object method, Object[] arguments) {
        throw new RuntimeException("failed to lookup generic selector object on generic class");
    }
}

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class DispatchNode extends Node {

    public static DispatchNode create() {
        return DispatchNodeGen.create();
    }

    public abstract Object executeDispatch(Object method, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"method == cachedMethod"}, assumptions = {"callTargetStable"})
    protected Object doDirect(CompiledCodeObject method, Object[] arguments,
                    @Cached("method") CompiledCodeObject cachedMethod,
                    @Cached("create()") InvokeNode invokeNode,
                    @Cached("method.getCallTargetStable()") Assumption callTargetStable) {
        return invokeNode.executeInvoke(cachedMethod, arguments);
    }

    @Specialization(replaces = "doDirect")
    protected Object doIndirect(CompiledCodeObject method, Object[] arguments,
                    @Cached("create()") InvokeNode invokeNode) {
        return invokeNode.executeInvoke(method, arguments);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fail(Object method, Object[] arguments) {
        throw new RuntimeException("failed to lookup generic selector object on generic class");
    }
}

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class InvokeNode extends Node {
    public static int callDepth = 0;
    private final static int CALL_DEPTH_LIMIT = 400;

    public static InvokeNode create() {
        return InvokeNodeGen.create();
    }

    public abstract Object executeInvoke(CompiledCodeObject method, Object[] arguments);

    @Specialization(guards = "code.getCallTarget() == callTarget", limit = "1")
    protected Object doInvoke(@SuppressWarnings("unused") CompiledCodeObject code, Object[] arguments,
                    @SuppressWarnings("unused") @Cached("code.getCallTarget()") RootCallTarget callTarget,
                    @Cached("create(callTarget)") DirectCallNode callNode) {
        incrementAndCheckCallDepth(arguments[0]);
        try {
            return callNode.call(arguments);
        } finally {
            callDepth--;
        }
    }

    @Specialization(replaces = "doInvoke")
    protected Object doIndirect(CompiledCodeObject code, Object[] arguments,
                    @Cached("create()") IndirectCallNode callNode) {
        incrementAndCheckCallDepth(arguments[0]);
        try {
            return callNode.call(code.getCallTarget(), arguments);
        } finally {
            callDepth--;
        }
    }

    private static void incrementAndCheckCallDepth(Object code) {
        callDepth++;
        if (callDepth >= CALL_DEPTH_LIMIT) {
            ((CompiledCodeObject) code).image.printSqStackTrace();
            throw new SqueakException("Avoiding StackOverflowError, callDepth has reached " + CALL_DEPTH_LIMIT + ".");
        }
    }
}

package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNodeWithCode {

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        return HandlePrimitiveFailedNodeGen.create(code);
    }

    public HandlePrimitiveFailedNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract void executeHandle(VirtualFrame frame, PrimitiveFailed e);

    /*
     * Look up error symbol in error table and push it to stack. The fallback code pops the error
     * symbol into the corresponding temporary variable.
     */
    @Specialization(guards = "followedByExtendedStore(code)")
    protected final void doHandle(final VirtualFrame frame, final PrimitiveFailed e,
                    @Cached("create(code)") final StackPushNode pushNode) {
        pushNode.executeWrite(frame, code.image.primitiveErrorTable.at0(e.getReasonCode()));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!followedByExtendedStore(code)")
    protected static final void doNothing(final PrimitiveFailed e) {
        // nothing to do
    }

    protected static final boolean followedByExtendedStore(final CompiledCodeObject codeObject) {
        // fourth bytecode indicates extended store after callPrimitive
        return codeObject.getBytes()[3] == (byte) 0x81;
    }
}

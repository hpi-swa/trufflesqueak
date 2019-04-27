package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNodeWithCode {

    protected HandlePrimitiveFailedNode(final CompiledCodeObject code) {
        super(code);
    }

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        return HandlePrimitiveFailedNodeGen.create(code);
    }

    public abstract void executeHandle(VirtualFrame frame, PrimitiveFailed e);

    /*
     * Look up error symbol in error table and push it to stack. The fallback code pops the error
     * symbol into the corresponding temporary variable. See
     * StackInterpreter>>#getErrorObjectFromPrimFailCode for more information.
     */
    @Specialization(guards = {"followedByExtendedStore(code)", "e.getReasonCode() < sizeNode.execute(code.image.primitiveErrorTable)"}, limit = "1")
    protected final void doHandleWithLookup(final VirtualFrame frame, final PrimitiveFailed e,
                    @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                    @Cached("create(code)") final StackPushNode pushNode,
                    @Cached final ArrayObjectReadNode readNode) {
        pushNode.executeWrite(frame, readNode.execute(code.image.primitiveErrorTable, e.getReasonCode()));
    }

    @Specialization(guards = {"followedByExtendedStore(code)", "e.getReasonCode() >= sizeNode.execute(code.image.primitiveErrorTable)"}, limit = "1")
    protected static final void doHandleRawValue(final VirtualFrame frame, final PrimitiveFailed e,
                    @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                    @Cached("create(code)") final StackPushNode pushNode) {
        pushNode.executeWrite(frame, e.getReasonCode());
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

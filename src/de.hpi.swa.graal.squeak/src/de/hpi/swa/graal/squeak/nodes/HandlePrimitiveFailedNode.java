package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPushNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNodeWithCode {

    protected HandlePrimitiveFailedNode(final CompiledCodeObject code) {
        super(code);
    }

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        return HandlePrimitiveFailedNodeGen.create(code);
    }

    public abstract void executeHandle(VirtualFrame frame, int reasonCode);

    /*
     * Look up error symbol in error table and push it to stack. The fallback code pops the error
     * symbol into the corresponding temporary variable. See
     * StackInterpreter>>#getErrorObjectFromPrimFailCode for more information.
     */
    @Specialization(guards = {"followedByExtendedStore(code)", "reasonCode < code.image.primitiveErrorTable.getObjectLength()"})
    protected final void doHandleWithLookup(final VirtualFrame frame, final int reasonCode,
                    @Cached("create(code)") final FrameStackPushNode pushNode) {
        pushNode.execute(frame, code.image.primitiveErrorTable.getObjectStorage()[reasonCode]);
    }

    @Specialization(guards = {"followedByExtendedStore(code)", "reasonCode >= code.image.primitiveErrorTable.getObjectLength()"})
    protected static final void doHandleRawValue(final VirtualFrame frame, final int reasonCode,
                    @Cached("create(code)") final FrameStackPushNode pushNode) {
        pushNode.execute(frame, reasonCode);
    }

    @Specialization(guards = "!followedByExtendedStore(code)")
    protected static final void doNothing(@SuppressWarnings("unused") final int reasonCode) {
        // nothing to do
    }

    protected static final boolean followedByExtendedStore(final CompiledCodeObject codeObject) {
        // fourth bytecode indicates extended store after callPrimitive
        return codeObject.getBytes()[3] == (byte) 0x81;
    }
}

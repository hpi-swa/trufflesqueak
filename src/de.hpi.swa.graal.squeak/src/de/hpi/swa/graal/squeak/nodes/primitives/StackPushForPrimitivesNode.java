package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

/**
 * This node pushes values onto the stack and is intended to be used in
 * {@link AbstractPrimitiveNode}. Unlike {@link FrameStackWriteNode}, it does not need a
 * {@link CompiledCodeObject} on creation, but uses the one attached to the provided
 * {@link VirtualFrame}. This is necessary to ensure correct {@link FrameDescriptor} ownership
 * during eager primitive calls.
 */
@NodeInfo(cost = NodeCost.NONE)
@ImportStatic(FrameAccess.class)
public abstract class StackPushForPrimitivesNode extends AbstractNode {

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization
    public static final void executeWrite(final VirtualFrame frame, final Object value,
                    @SuppressWarnings("unused") @Cached("getBlockOrMethod(frame)") final CompiledCodeObject codeObject,
                    @Cached("create(codeObject)") final FrameStackWriteNode writeNode) {
        assert value != null;
        writeNode.executePush(frame, value);
    }
}

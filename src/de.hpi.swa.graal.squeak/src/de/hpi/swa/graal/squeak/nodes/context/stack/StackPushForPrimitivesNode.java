package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

/**
 * This node pushes values onto the stack and is intended to be used in
 * {@link AbstractPrimitiveNode}. Unlike {@link StackPushNode}, it does not need a
 * {@link CompiledCodeObject} on creation, but uses the one attached to the provided
 * {@link VirtualFrame}. This is necessary to ensure correct {@link FrameDescriptor} ownership
 * during eager primitive calls.
 */
@ImportStatic(FrameAccess.class)
public abstract class StackPushForPrimitivesNode extends Node {
    public static StackPushForPrimitivesNode create() {
        return StackPushForPrimitivesNodeGen.create();
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization
    protected static final void doWrite(final VirtualFrame frame, final Object value,
                    @Cached("getBlockOrMethod(frame)") final CompiledCodeObject codeObject,
                    @Cached("create(codeObject)") final FrameStackWriteNode writeNode) {
        assert value != null;
        final int currentStackPointer = FrameUtil.getIntSafe(frame, codeObject.getStackPointerSlot());
        frame.setInt(codeObject.getStackPointerSlot(), currentStackPointer + 1);
        writeNode.execute(frame, currentStackPointer, value);
    }
}

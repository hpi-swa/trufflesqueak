package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

@NodeInfo(cost = NodeCost.NONE)
@ImportStatic(CONTEXT.class)
public abstract class FrameStackWriteNode extends AbstractNodeWithCode {

    public static FrameStackWriteNode create(final CompiledCodeObject code) {
        return FrameStackWriteNodeGen.create(code);
    }

    protected FrameStackWriteNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract void execute(Frame frame, int stackIndex, Object value);

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex", "code == cacheCode"}, limit = "MAX_STACK_SIZE")
    protected static final void doWrite(final VirtualFrame frame, final int index, final Object value,
                    @Cached("index") final int cachedIndex,
                    @Cached("code") final CompiledCodeObject cacheCode,
                    @Cached("create(cacheCode.getStackSlot(index))") final FrameSlotWriteNode writeNode) {
        writeNode.executeWrite(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doWrite")
    protected static final void doFail(final Frame frame, final int stackIndex, final Object value) {
        throw new SqueakException("Unexpected failure in FrameStackWriteNode");
    }
}

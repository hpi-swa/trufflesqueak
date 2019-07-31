package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(CONTEXT.class)
public abstract class FrameStackWriteNode extends AbstractNodeWithCode {

    protected FrameStackWriteNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackWriteNode create(final CompiledCodeObject code) {
        return FrameStackWriteNodeGen.create(code);
    }

    public final void executePush(final VirtualFrame frame, final Object value) {
        assert value != null : "Unexpected `null` value";
        final int currentStackPointer = FrameAccess.getStackPointer(frame, code);
        FrameAccess.setStackPointer(frame, code, currentStackPointer + 1);
        execute(frame, currentStackPointer, value);
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
}

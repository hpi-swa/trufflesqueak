package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

@ImportStatic(CONTEXT.class)
public abstract class FrameStackClearNode extends AbstractNodeWithCode {

    public static FrameStackClearNode create(final CompiledCodeObject code) {
        return FrameStackClearNodeGen.create(code);
    }

    protected FrameStackClearNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract void execute(Frame frame, int stackIndex);

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "MAX_STACK_SIZE")
    protected static final void doClear(final Frame frame, final int index,
                    @Cached("index") final int cachedIndex,
                    @Cached("code.getStackSlot(index)") final FrameSlot slot,
                    @Cached("create(slot)") final FrameSlotClearNode clearNode) {
        clearNode.executeClear(frame);
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doClear")
    protected static final void doFail(final Frame frame, final int stackIndex) {
        throw new SqueakException("Unexpected failure in FrameStackClearNode");
    }
}

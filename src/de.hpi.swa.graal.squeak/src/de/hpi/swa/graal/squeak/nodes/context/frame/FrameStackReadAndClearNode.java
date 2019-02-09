package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

@NodeInfo(cost = NodeCost.NONE)
@ImportStatic(CONTEXT.class)
public abstract class FrameStackReadAndClearNode extends AbstractNodeWithCode {

    protected FrameStackReadAndClearNode(final CompiledCodeObject code) {
        super(code);
    }

    public static FrameStackReadAndClearNode create(final CompiledCodeObject code) {
        return FrameStackReadAndClearNodeGen.create(code);
    }

    public abstract Object execute(Frame frame, int stackIndex);

    @SuppressWarnings("unused")
    @Specialization(guards = {"index == cachedIndex"}, limit = "MAX_STACK_SIZE")
    protected static final Object doClear(final Frame frame, final int index,
                    @Cached("index") final int cachedIndex,
                    @Cached("code.getStackSlot(index)") final FrameSlot slot,
                    @Cached("mustClear(index)") final boolean clear,
                    @Cached("create(slot)") final FrameSlotReadAndClearNode clearNode) {
        return clearNode.executeReadAndClear(frame, clear);
    }

    protected final boolean mustClear(final int index) {
        // Only clear stack values, not receiver, arguments, or temporary variables.
        return index >= code.getNumArgsAndCopied() + code.getNumTemps();
    }

    @SuppressWarnings("unused")
    @Specialization(replaces = "doClear")
    protected static final void doFail(final Frame frame, final int stackIndex) {
        throw SqueakException.create("Unexpected failure in FrameStackClearNode");
    }
}

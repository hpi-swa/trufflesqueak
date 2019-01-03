package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackClearNode;

public abstract class AbstractStackPopNode extends AbstractStackNode {
    @Child private FrameStackClearNode clearNode;

    public AbstractStackPopNode(final CompiledCodeObject code) {
        super(code);
        clearNode = FrameStackClearNode.create(code);
    }

    protected final Object atStackAndClear(final VirtualFrame frame, final int index) {
        final Object value = readNode.execute(frame, index);
        assert value != null;
        if (index >= code.getNumArgsAndCopied() + code.getNumTemps()) {
            // Only clear stack values, not receiver, arguments, or temporary variables.
            clearNode.execute(frame, index); // TODO: merge clearNode with readNode?
        }
        return value;
    }
}

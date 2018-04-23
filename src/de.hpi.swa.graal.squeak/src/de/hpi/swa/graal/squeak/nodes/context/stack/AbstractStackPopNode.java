package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class AbstractStackPopNode extends AbstractStackNode {
    @Child protected FrameStackWriteNode writeNode = FrameStackWriteNode.create();

    public AbstractStackPopNode(final CompiledCodeObject code) {
        super(code);
    }

    protected final Object atStackAndClear(final VirtualFrame frame, final int index) {
        final Object value = readNode.execute(frame, index);
        if (index >= 1 + code.getNumArgsAndCopiedValues() + code.getNumTemps()) {
            // only nil out stack values, not receiver, arguments, or temporary variables
            writeNode.execute(frame, index, code.image.nil);
        }
        return value;
    }
}

package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public abstract class AbstractStackPopNode extends AbstractStackNode {
    @Child protected FrameStackWriteNode writeNode = FrameStackWriteNode.create();

    public AbstractStackPopNode(CompiledCodeObject code) {
        super(code);
    }

    protected final Object atStackAndClear(final VirtualFrame frame, final int index) {
        Object value = readNode.execute(frame, index);
        if (index > 1 + code.getNumTemps()) { // do not modify receiver and temps
            writeNode.execute(frame, index, code.image.nil);
        }
        return value;
    }
}

package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;

public abstract class TemporaryReadNode extends SqueakNodeWithCode {
    @Child private FrameSlotReadNode readNode;

    protected TemporaryReadNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        readNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
    }

    public static TemporaryReadNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryReadNodeGen.create(code, tempIndex);
    }

    @Specialization
    public final Object doRead(final VirtualFrame frame) {
        return readNode.executeRead(frame);
    }
}

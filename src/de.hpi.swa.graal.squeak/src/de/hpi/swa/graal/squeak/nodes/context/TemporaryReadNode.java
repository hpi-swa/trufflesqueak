package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;

public abstract class TemporaryReadNode extends SqueakNodeWithCode {
    private final int tempIndex;
    @Child private FrameSlotReadNode readNode;

    public static SqueakNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryReadNodeGen.create(code, tempIndex);
    }

    protected TemporaryReadNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        this.tempIndex = tempIndex;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public final Object doReadVirtualized(final VirtualFrame frame) {
        return getReadNode().executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    public final Object doRead(final VirtualFrame frame) {
        return getContext(frame).atTemp(tempIndex);
    }

    private FrameSlotReadNode getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert tempIndex >= 0;
            readNode = insert(FrameSlotReadNode.create(code, code.getStackSlot(tempIndex)));
        }
        return readNode;
    }
}

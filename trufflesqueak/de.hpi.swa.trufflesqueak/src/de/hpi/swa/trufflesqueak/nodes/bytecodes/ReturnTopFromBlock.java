package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class ReturnTopFromBlock extends SqueakBytecodeNode {
    @Child FrameSlotReadNode top;

    public ReturnTopFromBlock(CompiledMethodObject cm, int idx) {
        super(cm, idx);
        top = FrameSlotReadNode.top(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn, NonLocalReturn {
        throw new LocalReturn(top.executeGeneric(frame));
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class ReturnTopFromBlockNode extends ReturnNode {
    @Child protected PopStackNode popNode;

    public ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
        super(code, index);
        popNode = new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new LocalReturn(popNode.execute(frame));
    }

    @Override
    public String toString() {
        return "blockReturn";
    }
}
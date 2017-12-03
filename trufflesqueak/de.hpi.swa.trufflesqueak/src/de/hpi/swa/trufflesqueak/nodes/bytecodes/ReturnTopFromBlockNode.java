package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ReturnTopFromBlockNode extends ReturnNode {
    @Child SqueakNode valueNode;

    public ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
        super(code, index);
        valueNode = new PopNode(code, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(valueNode.executeGeneric(frame));
    }
}
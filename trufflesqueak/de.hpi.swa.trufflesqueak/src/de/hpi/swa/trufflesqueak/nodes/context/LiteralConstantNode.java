package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class LiteralConstantNode extends SqueakNode {
    @Child private SqueakNode literalNode;

    public LiteralConstantNode(CompiledCodeObject code, int literalIndex) {
        super();
        literalNode = new MethodLiteralNode(code, literalIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literalNode.executeGeneric(frame);
    }
}
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class LiteralConstantNode extends SqueakNodeWithCode {
    @Child private SqueakNode literalNode;

    public LiteralConstantNode(CompiledCodeObject code, int literalIdx) {
        super(code);
        literalNode = new MethodLiteralNode(code, literalIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literalNode.executeGeneric(frame);
    }
}
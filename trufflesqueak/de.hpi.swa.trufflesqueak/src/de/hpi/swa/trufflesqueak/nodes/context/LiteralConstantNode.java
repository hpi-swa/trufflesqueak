package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class LiteralConstantNode extends SqueakNode {
    @Child private SqueakNode literalNode;

    public LiteralConstantNode(final CompiledCodeObject code, final long literalIndex) {
        super();
        literalNode = new MethodLiteralNode(code, literalIndex);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        return literalNode.executeRead(frame);
    }
}

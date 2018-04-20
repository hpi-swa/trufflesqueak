package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

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

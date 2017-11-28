package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class LiteralConstantNode extends SqueakBytecodeNode {
    @Child SqueakNode literalNode;

    public LiteralConstantNode(CompiledCodeObject method, int idx, int literalIdx) {
        super(method, idx);
        literalNode = new MethodLiteralNode(method, literalIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literalNode.executeGeneric(frame);
    }
}
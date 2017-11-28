package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;

public class PushLiteralConstantNode extends SqueakBytecodeNode {
    @Child SqueakNode literalNode;

    public PushLiteralConstantNode(CompiledCodeObject code, int idx, int literalIdx) {
        super(code, idx);
        literalNode = new MethodLiteralNode(code, literalIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, literalNode.executeGeneric(frame));
    }
}

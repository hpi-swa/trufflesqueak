package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;

public class PushLiteralConstantNode extends SqueakBytecodeNode {
    @Child SqueakNode literalNode;
    private final int literalIndex;

    public PushLiteralConstantNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        literalNode = new MethodLiteralNode(code, literalIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, literalNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushConstant: " + code.getLiteral(literalIndex).toString();
    }
}

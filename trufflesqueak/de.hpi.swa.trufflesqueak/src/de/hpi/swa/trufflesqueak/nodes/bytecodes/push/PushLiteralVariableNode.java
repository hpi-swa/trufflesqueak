package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushLiteralVariableNode extends SqueakBytecodeNode {
    @Child public SqueakNode valueNode;
    private final int literalIndex;

    public PushLiteralVariableNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, index, literalIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, valueNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushLit: " + literalIndex;
    }
}

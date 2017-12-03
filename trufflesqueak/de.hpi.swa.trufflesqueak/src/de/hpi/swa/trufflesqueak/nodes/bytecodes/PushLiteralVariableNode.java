package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushLiteralVariableNode extends SqueakBytecodeNode {
    @Child public SqueakNode valueNode;

    public PushLiteralVariableNode(CompiledCodeObject code, int index, int literalIndex) {
        super(code, index);
        valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, index, literalIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, valueNode.executeGeneric(frame));
    }
}

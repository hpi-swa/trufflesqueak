package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushLiteralVariableNode extends AbstractPushNode {
    @Child private ObjectAtNode valueNode;
    @CompilationFinal private final int literalIndex;

    public PushLiteralVariableNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, literalIndex));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, valueNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushLit: " + literalIndex;
    }
}

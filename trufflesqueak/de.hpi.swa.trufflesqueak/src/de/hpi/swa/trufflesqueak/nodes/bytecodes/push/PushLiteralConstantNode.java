package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;

public class PushLiteralConstantNode extends AbstractPushNode {
    @Child private SqueakNode literalNode;
    @CompilationFinal private final int literalIndex;

    public PushLiteralConstantNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        literalNode = new MethodLiteralNode(code, literalIndex);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, literalNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushConstant: " + code.getLiteral(literalIndex).toString();
    }
}

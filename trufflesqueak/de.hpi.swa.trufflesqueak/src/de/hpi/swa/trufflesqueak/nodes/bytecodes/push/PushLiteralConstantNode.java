package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushLiteralConstantNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private SqueakNode literalNode;
    @CompilationFinal private final int literalIndex;

    public PushLiteralConstantNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        pushNode = new PushStackNode(code);
        literalNode = new MethodLiteralNode(code, literalIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pushNode.executeWrite(frame, literalNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushConstant: " + code.getLiteral(literalIndex).toString();
    }
}

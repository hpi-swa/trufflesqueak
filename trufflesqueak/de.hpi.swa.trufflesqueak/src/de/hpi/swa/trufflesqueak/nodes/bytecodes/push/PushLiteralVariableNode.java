package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushLiteralVariableNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private SqueakNode valueNode;
    @CompilationFinal private final int literalIndex;

    public PushLiteralVariableNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex) {
        super(code, index, numBytecodes);
        this.literalIndex = literalIndex;
        pushNode = new PushStackNode(code);
        valueNode = ObjectAtNode.create(1, new LiteralConstantNode(code, index, literalIndex));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pushNode.executeWrite(frame, valueNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushLit: " + literalIndex;
    }
}

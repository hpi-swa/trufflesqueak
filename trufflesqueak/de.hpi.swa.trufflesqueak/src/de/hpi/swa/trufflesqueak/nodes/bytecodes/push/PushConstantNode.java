package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushConstantNode extends SqueakBytecodeNode {
    @CompilationFinal private final Object constant;
    @Child private PushStackNode pushNode;

    public PushConstantNode(CompiledCodeObject code, int index, Object obj) {
        super(code, index);
        constant = obj;
        pushNode = new PushStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pushNode.executeWrite(frame, constant);
    }

    @Override
    public String toString() {
        return "pushConstant: " + constant.toString();
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PushConstantNode extends AbstractPushNode {
    @CompilationFinal private final Object constant;

    public PushConstantNode(CompiledCodeObject code, int index, Object obj) {
        super(code, index);
        constant = obj;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, constant);
    }

    @Override
    public String toString() {
        return "pushConstant: " + constant.toString();
    }
}

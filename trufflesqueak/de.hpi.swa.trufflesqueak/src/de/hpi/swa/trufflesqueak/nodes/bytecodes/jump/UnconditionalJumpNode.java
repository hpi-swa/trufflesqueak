package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class UnconditionalJumpNode extends AbstractJump {

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
        super(code, index, numBytecodes, shortJumpOffset(bytecode));
    }

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter) {
        super(code, index, numBytecodes, longUnconditionalJumpOffset(bytecode, parameter));
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        return successorIndex + offset;
    }

    @Override
    public String toString() {
        return "jumpTo: " + offset;
    }
}

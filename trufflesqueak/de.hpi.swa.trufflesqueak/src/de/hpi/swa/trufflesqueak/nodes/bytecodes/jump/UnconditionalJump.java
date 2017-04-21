package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class UnconditionalJump extends AbstractJump {
    private final int offset;

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode) {
        super(cm, idx);
        offset = shortJumpOffset(bytecode);
    }

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode, int parameter) {
        super(cm, idx);
        offset = longJumpOffset(bytecode, parameter);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("not used");
    }

    @Override
    public int stepBytecode(VirtualFrame frame) {
        return getIndex() + offset + 1;
    }
}
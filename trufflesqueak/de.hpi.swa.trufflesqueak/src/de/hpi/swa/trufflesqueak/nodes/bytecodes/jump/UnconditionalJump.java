package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;

public class UnconditionalJump extends UnknownBytecodeNode {
    protected final int offset;

    public UnconditionalJump(CompiledCodeObject cm, int idx, int bytecode) {
        super(cm, idx, bytecode);
        offset = AbstractJump.shortJumpOffset(bytecode);
    }

    public UnconditionalJump(CompiledCodeObject cm, int idx, int bytecode, int parameter) {
        super(cm, idx, bytecode);
        offset = AbstractJump.longUnconditionalJumpOffset(bytecode, parameter);
    }
}

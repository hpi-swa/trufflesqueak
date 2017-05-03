package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;

public class UnconditionalJump extends UnknownBytecodeNode {
    protected final int offset;

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode) {
        super(cm, idx, bytecode);
        offset = AbstractJump.shortJumpOffset(bytecode);
    }

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode, int parameter) {
        super(cm, idx, bytecode);
        offset = AbstractJump.longUnconditionalJumpOffset(bytecode, parameter);
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;

public class UnconditionalJump extends UnknownBytecodeNode {
    protected final int offset;

    public UnconditionalJump(CompiledCodeObject code, int index, int bytecode) {
        super(code, index, bytecode);
        offset = AbstractJump.shortJumpOffset(bytecode);
    }

    public UnconditionalJump(CompiledCodeObject code, int index, int bytecode, int parameter) {
        super(code, index, bytecode);
        offset = AbstractJump.longUnconditionalJumpOffset(bytecode, parameter);
    }
}

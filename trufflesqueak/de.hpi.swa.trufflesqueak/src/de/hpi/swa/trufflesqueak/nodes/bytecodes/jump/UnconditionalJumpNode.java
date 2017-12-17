package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;

public class UnconditionalJumpNode extends AbstractBytecodeNode {
    @CompilationFinal protected final int offset;

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
        super(code, index, numBytecodes);
        this.offset = shortJumpOffset(bytecode);
    }

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter) {
        super(code, index, numBytecodes);
        this.offset = longJumpOffset(bytecode, parameter);
    }

    protected int shortJumpOffset(int bytecode) {
        return (bytecode & 7) + 1;
    }

    protected int longJumpOffset(int bytecode, int parameter) {
        return (((bytecode & 7) - 4) << 8) + parameter;
    }

    public int getJumpSuccessor() {
        return index + numBytecodes + offset;
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        return getJumpSuccessor();
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new RuntimeException("Jumps cannot be executed like other bytecode nodes");
    }

    @Override
    public String toString() {
        return "jumpTo: " + offset;
    }
}
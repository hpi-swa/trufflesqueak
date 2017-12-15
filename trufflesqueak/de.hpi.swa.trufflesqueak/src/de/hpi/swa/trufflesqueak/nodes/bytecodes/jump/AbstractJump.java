package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public abstract class AbstractJump extends SqueakBytecodeNode {
    @CompilationFinal protected final int offset;

    protected static int shortJumpOffset(int code) {
        return (code & 7) + 1;
    }

    protected static int longJumpOffset(int code, int param) {
        return ((code & 3) << 8) + param;
    }

    static int longUnconditionalJumpOffset(int bc, int param) {
        return (((bc & 7) - 4) << 8) + param;
    }

    public AbstractJump(CompiledCodeObject code, int index, int numBytecodes, int offset) {
        super(code, index, numBytecodes);
        this.offset = offset;
    }

    @Override
    public abstract int executeInt(VirtualFrame frame);

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Jumps cannot be executed");
    }
}
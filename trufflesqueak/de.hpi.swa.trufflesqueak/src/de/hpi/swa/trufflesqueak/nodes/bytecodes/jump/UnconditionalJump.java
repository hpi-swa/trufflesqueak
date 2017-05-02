package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import java.util.Vector;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class UnconditionalJump extends AbstractJump {
    private final int offset;

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode) {
        super(cm, idx);
        offset = shortJumpOffset(bytecode);
    }

    public UnconditionalJump(CompiledMethodObject cm, int idx, int bytecode, int parameter) {
        super(cm, idx);
        // +1 because our index is before the parameter byte
        offset = longUnconditionalJumpOffset(bytecode, parameter) + 1;
    }

    static int longUnconditionalJumpOffset(int bc, int param) {
        return (((bc & 7) - 4) << 8) + param;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("should never happen");
    }

    @Override
    public int getJump() {
        assert offset < 0;
        return offset;
    }

    @Override
    public SqueakBytecodeNode decompileFrom(Vector<SqueakBytecodeNode> sequence) {
        throw new RuntimeException("not used, the unconditional jump back is used as a marker for the forward jump at the head of this loop");
    }
}

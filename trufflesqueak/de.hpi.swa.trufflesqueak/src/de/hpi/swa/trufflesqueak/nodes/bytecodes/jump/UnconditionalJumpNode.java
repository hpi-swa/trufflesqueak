package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class UnconditionalJumpNode extends SqueakBytecodeNode {

    private int offset;

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode) {
        super(code, index, numBytecodes);
        offset = AbstractJump.shortJumpOffset(bytecode);
    }

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int numBytecodes, int bytecode, int parameter) {
        super(code, index, numBytecodes);
        offset = AbstractJump.longUnconditionalJumpOffset(bytecode, parameter);
    }

    @Override
    public int executeInt(VirtualFrame frame) {
        return successorIndex + offset;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("ConditionalJumps cannot be executed");
    }

    @Override
    public String toString() {
        return "jumpTo: " + offset;
    }
}

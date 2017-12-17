package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class UnknownBytecodeNode extends AbstractBytecodeNode {
    private final int bytecode;

    public UnknownBytecodeNode(CompiledCodeObject code, int index, int numBytecodes, int bc) {
        super(code, index, numBytecodes);
        bytecode = bc;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new RuntimeException("Unknown/uninterpreted bytecode " + bytecode);
    }

    @Override
    public String toString() {
        return String.format("unknown: %02X", bytecode);
    }
}

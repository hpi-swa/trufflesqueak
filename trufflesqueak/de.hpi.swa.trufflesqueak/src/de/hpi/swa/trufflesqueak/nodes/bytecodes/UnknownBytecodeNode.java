package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class UnknownBytecodeNode extends SqueakBytecodeNode {
    private final int bytecode;

    public UnknownBytecodeNode(CompiledCodeObject code, int index, int bc) {
        super(code, index);
        bytecode = bc;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Unknown/uninterpreted bytecode " + bytecode);
    }
}

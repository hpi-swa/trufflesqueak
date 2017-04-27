package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class UnknownBytecode extends SqueakBytecodeNode {
    private final int bytecode;

    public UnknownBytecode(CompiledMethodObject cm, int idx, int bc) {
        super(cm, idx);
        bytecode = bc;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Unknown bytecode " + bytecode);
    }
}

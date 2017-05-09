package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class UnknownBytecodeNode extends SqueakBytecodeNode {
    private final int bytecode;

    public UnknownBytecodeNode(CompiledCodeObject method, int idx, int bc) {
        super(method, idx);
        bytecode = bc;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Unknown/uninterpreted bytecode " + bytecode);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        throw new RuntimeException("Should not be interpreted");
    }
}

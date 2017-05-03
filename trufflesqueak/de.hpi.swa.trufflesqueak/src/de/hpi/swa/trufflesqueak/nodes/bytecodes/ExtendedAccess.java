package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public abstract class ExtendedAccess extends SqueakBytecodeNode {
    final int bytecode;
    @Child SqueakNode actualNode;

    public ExtendedAccess(CompiledMethodObject cm, int index, int i) {
        super(cm, index);
        bytecode = i;
    }

    abstract protected SqueakNode createActualNode(int idx, int type, Stack<SqueakNode> stack);

    protected static byte extractIndex(int i) {
        return (byte) (i & 63);
    }

    protected static byte extractType(int i) {
        return (byte) ((i >> 6) & 3);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return actualNode.executeGeneric(frame);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        actualNode = createActualNode(extractIndex(bytecode), extractType(bytecode), stack);
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public abstract class ReturnNode extends SqueakBytecodeNode {
    @Child SqueakNode valueNode;

    public ReturnNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        statements.push(this);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new LocalReturn(valueNode.executeGeneric(frame));
    }
}

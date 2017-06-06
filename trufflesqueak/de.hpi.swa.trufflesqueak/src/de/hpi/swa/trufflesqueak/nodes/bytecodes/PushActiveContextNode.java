package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushActiveContextNode extends SqueakBytecodeNode {

    public PushActiveContextNode(CompiledCodeObject method, int idx) {
        super(method, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return ContextObject.createReadOnlyContextObject(method.image, frame);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        stack.push(this);
    }

    @Override
    public void accept(PrettyPrintVisitor b) {
        b.visit(this);
    }
}

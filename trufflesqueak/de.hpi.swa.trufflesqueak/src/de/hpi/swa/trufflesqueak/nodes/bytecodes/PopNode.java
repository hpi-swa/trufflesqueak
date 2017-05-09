package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

/**
 * The Pop node is also used to mark a case statement. When we find one on the stack already, we
 * just pop it, otherwise, we'll take the last thing on the stack and make it into a statement for
 * its effect.
 */
public class PopNode extends UnknownBytecodeNode {
    public PopNode(CompiledCodeObject method, int idx) {
        super(method, idx, -1);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        if (stack.empty()) {
            return;
        } else {
            if (stack.peek() instanceof PopNode) {
                stack.pop();
            } else {
                sequence.push(stack.pop());
            }
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("not an executable node");
    }

}

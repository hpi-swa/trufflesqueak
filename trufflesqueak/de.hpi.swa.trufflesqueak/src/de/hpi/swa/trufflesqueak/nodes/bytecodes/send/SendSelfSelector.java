package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReceiverNode;

public class SendSelfSelector extends AbstractSend {
    public SendSelfSelector(CompiledCodeObject method, int idx, Object selector, int numArgs) {
        super(method, idx, selector, numArgs);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        super.interpretOn(stack, sequence);
        stack.push(receiverNode); // for this send, the receiver wasn't pushed on the stack, leave
                                  // what was there before
        receiverNode = new ReceiverNode(method, index);
    }
}

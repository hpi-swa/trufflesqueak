package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;

public class SendSelfSelector extends AbstractSend {
    public SendSelfSelector(CompiledMethodObject cm, int idx, BaseSqueakObject selector, int numArgs) {
        super(cm, idx, selector, numArgs);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        super.interpretOn(stack, sequence);
        stack.push(receiverNode); // for this send, the receiver wasn't pushed on the stack, leave
                                  // what was there before
        receiverNode = FrameSlotReadNode.receiver(method);
    }
}

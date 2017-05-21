package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class CascadedSend extends AbstractSend {
    private final boolean isLast;

    public CascadedSend(CompiledCodeObject method, int idx, SqueakNode receiver, Object selector, SqueakNode[] argNodes, SqueakNode[] cascadedSends) {
        super(method, idx, selector, argNodes);
        isLast = true;
        receiverNode = sendToCascade(receiver, cascadedSends);
    }

    private CascadedSend(CompiledCodeObject method, int idx, SqueakNode receiver, Object selector, SqueakNode[] argNodes) {
        super(method, idx, selector, argNodes);
        isLast = false;
        receiverNode = receiver;
    }

    private SqueakNode sendToCascade(SqueakNode receiver, SqueakNode[] cascadedSends) {
        SqueakNode lastReceiver = receiver;
        for (SqueakNode node : cascadedSends) {
            AbstractSend send = (AbstractSend) node;
            lastReceiver = new CascadedSend(method, index, lastReceiver, send.selector, send.argumentNodes);
        }
        return lastReceiver;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        throw new RuntimeException("not used");
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object receiver = receiverNode.executeGeneric(frame);
        Object sendResult = executeSend(frame, receiver);
        if (isLast) {
            // we're the last, return the message result
            return sendResult;
        } else {
            return receiver;
        }
    }
}

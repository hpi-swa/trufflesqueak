package de.hpi.swa.trufflesqueak.test;

import java.util.Iterator;

import org.junit.Test;

import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.BytecodeSequenceNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelectorNode;

public class TestDecoder extends TestSqueak {
    @Test
    public void testIfNil() {
        // (1 ifNil: [true]) class
        // pushConstant: 1, dup, pushConstant: nil, send: ==, jumpFalse: 24, pop, pushConstant:
        // true, send: class, pop, returnSelf
        int[] bytes = {0x76, 0x88, 0x73, 0xc6, 0x99, 0x87, 0x71, 0xc7, 0x87, 0x78};
        CompiledCodeObject cm = makeMethod(bytes);
        BytecodeSequenceNode bytecodeSequence = cm.getBytecodeNode();
        Iterator<Node> childIterator = bytecodeSequence.getChildren().iterator();
        assertSame(PushConstantNode.class, childIterator.next().getClass());
        assertSame(DupNode.class, childIterator.next().getClass());
        assertSame(PushConstantNode.class, childIterator.next().getClass());

        SendSelectorNode send = (SendSelectorNode) childIterator.next();
        assertSame(image.equivalent, send.selector);

        assertSame(ConditionalJumpNode.class, childIterator.next().getClass());
        assertSame(PopNode.class, childIterator.next().getClass());
        assertSame(PushConstantNode.class, childIterator.next().getClass());

        send = (SendSelectorNode) childIterator.next();
        assertSame(image.klass, send.selector);

        assertSame(PopNode.class, childIterator.next().getClass());
        assertSame(ReturnReceiverNode.class, childIterator.next().getClass());
        assertFalse(childIterator.hasNext());
    }

    @Test
    public void testIfNotNil() {
        // (1 ifNotNil: [true]) class
        // pushConstant: 1, pushConstant: nil, send: ==, jumpFalse: 23, pushConstant: nil, jumpTo:
        // 24, pushConstant: true, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x73, 0xc6, 0x99, 0x73, 0x90, 0x71, 0xc7, 0x87, 0x78);
        BytecodeSequenceNode bytecodeSequence = cm.getBytecodeNode();
// assertEquals(bytecodeSequence.length, 2);
// assertSame(bytecodeSequence[0].getClass(), SendSelector.class);
// assertSame(bytecodeSequence[1].getClass(), ReturnReceiverNode.class);
// SendSelector send = (SendSelector) bytecodeSequence[0];
// assertSame(send.selector, image.klass);
// assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    @Test
    public void testIfNotNilDo() {
        // (1 ifNotNil: [:o | o class]) class
        // pushConstant: 1, storeIntoTemp: 0, pushConstant: nil, send: ==, jumpFalse: 25,
        // pushConstant: nil, jumpTo: 27, pushTemp: 0, send: class, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x81, 0x40, 0x73, 0xc6, 0x99, 0x73, 0x91, 0x10, 0xc7, 0xc7, 0x87, 0x78);
        BytecodeSequenceNode bytecodeSequence = cm.getBytecodeNode();
// assertEquals(bytecodeSequence.length, 2);
// assertSame(bytecodeSequence[0].getClass(), SendSelector.class);
// assertSame(bytecodeSequence[1].getClass(), ReturnReceiverNode.class);
// SendSelector send = (SendSelector) bytecodeSequence[0];
// assertSame(send.selector, image.klass);
// assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    @Test
    public void testCascade() {
        // 1 value; size; class
        // pushConstant: 1, dup, send: value, pop, dup, send: size, pop, send: class, pop,
        // returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x88, 0xc9, 0x87, 0x88, 0xc2, 0x87, 0xc7, 0x87, 0x78);
        BytecodeSequenceNode bytecodeSequence = cm.getBytecodeNode();
// assertEquals(bytecodeSequence.length, 2);
// assertSame(bytecodeSequence[0].getClass(), CascadedSend.class);
// assertSame(bytecodeSequence[1].getClass(), ReturnReceiverNode.class);
// CascadedSend send = (CascadedSend) bytecodeSequence[0];
// assertSame(send.selector, image.klass);
// assertSame(send.receiverNode.getClass(), CascadedSend.class);
// send = (CascadedSend) send.receiverNode;
// assertSame(send.selector, image.size_);
// assertSame(send.receiverNode.getClass(), CascadedSend.class);
// send = (CascadedSend) send.receiverNode;
// assertSame(send.selector, image.value);
// assertSame(send.receiverNode.getClass(), ConstantNode.class);
    }

}

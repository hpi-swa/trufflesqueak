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
		// pushConstant: 1, dup, pushConstant: nil, send: ==, jumpFalse: 24, pop,
		// pushConstant: true, send: class, pop, returnSelf
		int[] bytes = { 0x76, 0x88, 0x73, 0xc6, 0x99, 0x87, 0x71, 0xc7, 0x87, 0x78 };
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
}

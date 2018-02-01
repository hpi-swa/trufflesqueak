package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.PopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class SqueakBytecodeDecoderTest extends AbstractSqueakTestCase {
    @Test
    public void testIfNil() {
        // (1 ifNil: [true]) class
        // pushConstant: 1, dup, pushConstant: nil, send: ==, jumpFalse: 24, pop,
        // pushConstant: true, send: class, pop, returnSelf
        int[] bytes = {0x76, 0x88, 0x73, 0xc6, 0x99, 0x87, 0x71, 0xc7, 0x87, 0x78};
        CompiledCodeObject code = makeMethod(bytes);
        AbstractBytecodeNode[] bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        assertEquals(bytes.length, bytecodeNodes.length);
        assertSame(PushConstantNode.class, bytecodeNodes[0].getClass());
        assertSame(DupNode.class, bytecodeNodes[1].getClass());
        assertSame(PushConstantNode.class, bytecodeNodes[2].getClass());

        assertEquals("send: " + image.equivalent, bytecodeNodes[3].toString());

        assertSame(ConditionalJumpNode.class, bytecodeNodes[4].getClass());
        assertSame(PopNode.class, bytecodeNodes[5].getClass());
        assertSame(PushConstantNode.class, bytecodeNodes[6].getClass());

        assertEquals("send: " + image.klass, bytecodeNodes[7].toString());

        assertSame(PopNode.class, bytecodeNodes[8].getClass());
        assertTrue(ReturnReceiverNode.class.isAssignableFrom(bytecodeNodes[9].getClass()));
    }
}

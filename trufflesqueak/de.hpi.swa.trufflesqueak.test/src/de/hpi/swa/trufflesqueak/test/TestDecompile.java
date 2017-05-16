package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfNilCheck;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfThenNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.CascadedSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;

public class TestDecompile extends TestSqueak {
    @Test
    public void testIfNil() {
        // (1 ifNil: [true]) class
        // pushConstant: 1, dup, pushConstant: nil, send: ==, jumpFalse: 24, pop, pushConstant:
        // true, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x88, 0x73, 0xc6, 0x99, 0x87, 0x71, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfNilCheck.class);
    }

    @Test
    public void testIfNotNil() {
        // (1 ifNotNil: [true]) class
        // pushConstant: 1, pushConstant: nil, send: ==, jumpFalse: 23, pushConstant: nil, jumpTo:
        // 24, pushConstant: true, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x73, 0xc6, 0x99, 0x73, 0x90, 0x71, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    @Test
    public void testIfNotNilDo() {
        // (1 ifNotNil: [:o | o class]) class
        // pushConstant: 1, storeIntoTemp: 0, pushConstant: nil, send: ==, jumpFalse: 25,
        // pushConstant: nil, jumpTo: 27, pushTemp: 0, send: class, send: class, pop, returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x81, 0x40, 0x73, 0xc6, 0x99, 0x73, 0x91, 0x10, 0xc7, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), SendSelector.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        SendSelector send = (SendSelector) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), IfThenNode.class);
    }

    @Test
    public void testCascade() {
        // 1 value; size; class
        // pushConstant: 1, dup, send: value, pop, dup, send: size, pop, send: class, pop,
        // returnSelf
        CompiledCodeObject cm = makeMethod(0x76, 0x88, 0xc9, 0x87, 0x88, 0xc2, 0x87, 0xc7, 0x87, 0x78);
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
        assertSame(bytecodeAST[0].getClass(), CascadedSend.class);
        assertSame(bytecodeAST[1].getClass(), ReturnReceiverNode.class);
        CascadedSend send = (CascadedSend) bytecodeAST[0];
        assertSame(send.selector, image.klass);
        assertSame(send.receiverNode.getClass(), CascadedSend.class);
        send = (CascadedSend) send.receiverNode;
        assertSame(send.selector, image.size_);
        assertSame(send.receiverNode.getClass(), CascadedSend.class);
        send = (CascadedSend) send.receiverNode;
        assertSame(send.selector, image.value);
        assertSame(send.receiverNode.getClass(), ConstantNode.class);
    }

    @Test
    public void testNestedClosure() {
        CompiledCodeObject cm = makeMethod(0x11, 0x43, 0xd2, 0xe1, 0x8f, 0x01, 0x00, 0x0c,
                        0x10, 0x24, 0xe1, 0x8f, 0x01, 0x00, 0x03,
                        0x10, 0xd5, 0x7d,
                        0xe0, 0x7d,
                        0xe0, 0x68, 0x78);
        // tokens := (aString findTokens: Character cr) collect: [:line| (line findTokens: ',')
        // collect: [:t| t asInteger]].
        // pushTemp: 1, pushLit: Character, send: cr, send: findTokens:, closureNumCopied: 0
        // numArgs: 1 bytes 45 to 56
        // pushTemp: 0, pushConstant: ',', send: findTokens:, closureNumCopied: 0 numArgs: 1 bytes
        // 52 to 54
        // pushTemp: 0, send: asInteger, blockReturn
        // send: collect:, blockReturn
        // send: collect:, popIntoTemp: 0, returnSelf
        SqueakNode[] bytecodeAST = cm.getBytecodeAST();
        assertEquals(bytecodeAST.length, 2);
    }
}

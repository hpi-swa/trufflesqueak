package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.PopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class SqueakMiscellaneousTest extends AbstractSqueakTestCaseWithDummyImage {
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

    @Test
    public void testSource() {
        Object[] literals = new Object[]{14548994L, image.nil, image.nil}; // header with numTemp=55
        CompiledCodeObject code = makeMethod(literals, 0x70, 0x68, 0x10, 0x8F, 0x10, 0x00, 0x02, 0x10, 0x7D, 0xC9, 0x7C);
        CharSequence source = CompiledCodeObjectPrinter.getString(code);
        //@formatter:off
        assertEquals(
            "1 <70> self\n" +
            "2 <68> popIntoTemp: 0\n" +
            "3 <10> pushTemp: 0\n" +
            "4 <8F 10 00 02> closureNumCopied: 1 numArgs: 0 bytes 7 to 9\n" +
            "5  <10> pushTemp: 0\n" +
            "6  <7D> blockReturn\n" +
            "7 <C9> send: value\n" +
            "8 <7C> returnTop", source);
        //@formatter:on
    }

    @Test
    public void testSourceAllBytecodes() {
        Object[] literals = new Object[]{17104899L, 21, 42, 63};
        CompiledCodeObject code = makeMethod(literals,
        //@formatter:off
            15, 31, 63, 95, 96, 97, 98, 99, 103, 111, 112, 113, 114, 115, 116,
            117, 118, 119, 120, 121, 122, 123, 124, 126, 127,
            128, 31,
            129, 31,
            130, 31,
            131, 31,
            132, 31, 63,
            133, 31,
            134, 31,
            135,
            136,
            137,
            138, 31,
            139, 31, 0,
            140, 31, 63,
            141, 31, 63,
            142, 31, 63,
            143, 31, 63, 127,
            151,
            159,
            167, 31,
            171, 31,
            175, 31,
            125,
            176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188,
            189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201,
            202, 203, 204, 205, 206, 207, 208, 225, 242
        );
        CharSequence source = CompiledCodeObjectPrinter.getString(code);
        assertEquals(
            "1 <0F> pushRcvr: 15\n" +
            "2 <1F> pushTemp: 15\n" +
            "3 <3F> pushConstant: 17104899\n" +
            "4 <5F> pushLit: 31\n" +
            "5 <60> popIntoRcvr: 0\n" +
            "6 <61> popIntoRcvr: 1\n" +
            "7 <62> popIntoRcvr: 2\n" +
            "8 <63> popIntoRcvr: 3\n" +
            "9 <67> popIntoRcvr: 7\n" +
            "10 <6F> popIntoTemp: 7\n" +
            "11 <70> self\n" +
            "12 <71> pushConstant: true\n" +
            "13 <72> pushConstant: false\n" +
            "14 <73> pushConstant: nil\n" +
            "15 <74> pushConstant: -1\n" +
            "16 <75> pushConstant: 0\n" +
            "17 <76> pushConstant: 1\n" +
            "18 <77> pushConstant: 2\n" +
            "19 <78> returnSelf\n" +
            "20 <79> return: true\n" +
            "21 <7A> return: false\n" +
            "22 <7B> return: nil\n" +
            "23 <7C> returnTop\n" +
            "24 <7E> unknown: 126\n" +
            "25 <7F> unknown: 127\n" +
            "26 <80 1F> pushRcvr: 31\n" +
            "27 <81 1F> storeIntoRcvr: 31\n" +
            "28 <82 1F> popIntoRcvr: 31\n" +
            "29 <83 1F> send: 17104899\n" +
            "30 <84 1F 3F> send: 17104899\n" +
            "31 <85 1F> sendSuper: 17104899\n" +
            "32 <86 1F> send: 17104899\n" +
            "33 <87> pop\n" +
            "34 <88> dup\n" +
            "35 <89> pushThisContext:\n" +
            "36 <8A 1F> push: (Array new: 31)\n" +
            "37 <8B 1F 00> callPrimitive: 31\n" +
            "38 <8C 1F 3F> pushTemp: 31 inVectorAt: 63\n" +
            "39 <8D 1F 3F> storeIntoTemp: 31 inVectorAt: 63\n" +
            "40 <8E 1F 3F> popIntoTemp: 31 inVectorAt: 63\n" +
            "41 <8F 1F 3F 7F> closureNumCopied: 1 numArgs: 15 bytes 61 to 16316\n" +
            "42  <97> jumpTo: 8\n" +
            "43  <9F> jumpFalse: 8\n" +
            "44  <A7 1F> jumpTo: 799\n" +
            "45  <AB 1F> jumpTrue: 799\n" +
            "46  <AF 1F> jumpFalse: 799\n" +
            "47  <7D> blockReturn\n" +
            "48 <B0> send: plus\n" +
            "49 <B1> send: minus\n" +
            "50 <B2> send: lt\n" +
            "51 <B3> send: gt\n" +
            "52 <B4> send: le\n" +
            "53 <B5> send: ge\n" +
            "54 <B6> send: eq\n" +
            "55 <B7> send: ne\n" +
            "56 <B8> send: times\n" +
            "57 <B9> send: divide\n" +
            "58 <BA> send: modulo\n" +
            "59 <BB> send: pointAt\n" +
            "60 <BC> send: bitShift\n" +
            "61 <BD> send: floorDivide\n" +
            "62 <BE> send: bitAnd\n" +
            "63 <BF> send: bitOr\n" +
            "64 <C0> send: at\n" +
            "65 <C1> send: atput\n" +
            "66 <C2> send: size\n" +
            "67 <C3> send: next\n" +
            "68 <C4> send: nextPut\n" +
            "69 <C5> send: atEnd\n" +
            "70 <C6> send: equivalent\n" +
            "71 <C7> send: klass\n" +
            "72 <C8> send: blockCopy\n" +
            "73 <C9> send: value\n" +
            "74 <CA> send: valueWithArg\n" +
            "75 <CB> send: do\n" +
            "76 <CC> send: new\n" +
            "77 <CD> send: newWithArg\n" +
            "78 <CE> send: x\n" +
            "79 <CF> send: y\n" +
            "80 <D0> send: 21\n" +
            "81 <E1> send: 42\n" +
            "82 <F2> send: 63", source);
        //@formatter:on
    }

    @Test
    public void testFloatDecoding() {
        SqueakImageChunk chunk = newFloatChunk();
        chunk.data().add(0);
        chunk.data().add(1072693248);
        assertEquals(1.0, chunk.asObject());

        chunk = newFloatChunk();
        chunk.data().add((int) 2482401462L);
        chunk.data().add(1065322751);
        assertEquals(0.007699011184197404, chunk.asObject());

        chunk = newFloatChunk();
        chunk.data().add(876402988);
        chunk.data().add(1075010976);
        assertEquals(4.841431442464721, chunk.asObject());
    }

    private static SqueakImageChunk newFloatChunk() {
        SqueakImageChunk chunk = new SqueakImageChunk(
                        null,
                        image,
                        2, // 2 words
                        10, // float format, 32-bit words without padding word
                        34, // classid of BoxedFloat64
                        3833906, // identityHash for 1.0
                        0 // position
        );
        chunk.setSqClass(image.floatClass);
        return chunk;
    }
}

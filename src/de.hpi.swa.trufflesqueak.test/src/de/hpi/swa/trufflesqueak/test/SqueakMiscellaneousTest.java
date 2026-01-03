/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
import de.hpi.swa.trufflesqueak.image.SqueakImageReader;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.interpreter.DecoderV3PlusClosures;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@SuppressWarnings("static-method")
public final class SqueakMiscellaneousTest extends AbstractSqueakTestCaseWithDummyImage {
    private static final String ALL_BYTECODES_EXPECTED_RESULT = String.join("\n", "1 <8B 1F 00> callPrimitive: 31",
                    "2 <1F> pushTemp: 15",
                    "3 <20> pushConstant: 'someSelector'",
                    "4 <43> pushLitVar: #UndefinedObject => UndefinedObject",
                    "5 <60> popIntoRcvr: 0",
                    "6 <61> popIntoRcvr: 1",
                    "7 <62> popIntoRcvr: 2",
                    "8 <63> popIntoRcvr: 3",
                    "9 <67> popIntoRcvr: 7",
                    "10 <6F> popIntoTemp: 7",
                    "11 <70> self",
                    "12 <71> pushConstant: true",
                    "13 <72> pushConstant: false",
                    "14 <73> pushConstant: nil",
                    "15 <74> pushConstant: -1",
                    "16 <75> pushConstant: 0",
                    "17 <76> pushConstant: 1",
                    "18 <77> pushConstant: 2",
                    "19 <78> returnSelf",
                    "20 <79> return: true",
                    "21 <7A> return: false",
                    "22 <7B> return: nil",
                    "23 <7C> returnTop",
                    "24 <7E> unknown: 126",
                    "25 <7F> unknown: 127",
                    "26 <80 1F> pushRcvr: 31",
                    "27 <81 1F> storeIntoRcvr: 31",
                    "28 <82 1F> popIntoRcvr: 31",
                    "29 <83 20> send: 'someSelector'",
                    "30 <84 1F 01> send: 'someOtherSelector'",
                    "31 <85 20> sendSuper: 'someSelector'",
                    "32 <86 01> send: 'someOtherSelector'",
                    "33 <87> pop",
                    "34 <88> dup",
                    "35 <89> pushThisContext:",
                    "36 <8A 1F> push: (Array new: 31)",
                    "37 <0F> pushRcvr: 15",
                    "38 <8C 1F 37> pushTemp: 31 inVectorAt: 55",
                    "39 <8D 1F 37> storeIntoTemp: 31 inVectorAt: 55",
                    "40 <8E 1F 37> popIntoTemp: 31 inVectorAt: 55",
                    "41 <8F 1F 3F 7F> closureNumCopied: 1 numArgs: 15 bytes 61 to 16316",
                    "42  <97> jumpTo: 8",
                    "43  <9F> jumpFalse: 8",
                    "44  <A7 1F> jumpTo: 799",
                    "45  <AB 1F> jumpTrue: 799",
                    "46  <AF 1F> jumpFalse: 799",
                    "47  <7D> blockReturn",
                    "48 <B0> send: +",
                    "49 <B1> send: -",
                    "50 <B2> send: <",
                    "51 <B3> send: >",
                    "52 <B4> send: <=",
                    "53 <B5> send: >=",
                    "54 <B6> send: =",
                    "55 <B7> send: ~=",
                    "56 <B8> send: *",
                    "57 <B9> send: /",
                    "58 <BA> send: \\",
                    "59 <BB> send: @",
                    "60 <BC> send: bitShift:",
                    "61 <BD> send: //",
                    "62 <BE> send: bitAnd:",
                    "63 <BF> send: bitOr:",
                    "64 <C0> send: at:",
                    "65 <C1> send: at:put:",
                    "66 <C2> send: size",
                    "67 <C3> send: next",
                    "68 <C4> send: nextPut:",
                    "69 <C5> send: atEnd",
                    "70 <C6> send: ==",
                    "71 <C7> send: class",
                    "72 <C8> send: ~~",
                    "73 <C9> send: value",
                    "74 <CA> send: value:",
                    "75 <CB> send: do:",
                    "76 <CC> send: new",
                    "77 <CD> send: new:",
                    "78 <CE> send: x",
                    "79 <CF> send: y",
                    "80 <D0> send: 'someSelector'",
                    "81 <E1> send: 'someOtherSelector'",
                    "82 <F0> send: 'someSelector'");

    @Test
    public void testSource() {
        final int header = 14548994; // header with numTemp=55
        final Object[] literals = {NilObject.SINGLETON, NilObject.SINGLETON};
        final CompiledCodeObject code = makeMethod(header, literals, 0x70, 0x68, 0x10, 0x8F, 0x10, 0x00, 0x02, 0x10, 0x7D, 0xC9, 0x7C);
        final CharSequence source = DecoderV3PlusClosures.SINGLETON.decodeToString(code);
        assertEquals(String.join("\n",
                        "1 <70> self",
                        "2 <68> popIntoTemp: 0",
                        "3 <10> pushTemp: 0",
                        "4 <8F 10 00 02> closureNumCopied: 1 numArgs: 0 bytes 7 to 9",
                        "5  <10> pushTemp: 0",
                        "6  <7D> blockReturn",
                        "7 <C9> send: value",
                        "8 <7C> returnTop"), source);
    }

    @Test
    public void testSourceAllBytecodes() {
        final Object[] literals = {image.asByteString("someSelector"), image.asByteString("someOtherSelector"), 63, nilClassBinding};
        final long header = makeHeader(0, 0, literals.length, false, true);
        final CompiledCodeObject code = makeMethod(header, literals,
                        139, 31, 0,
                        31, 32, 67, 96, 97, 98, 99, 103, 111, 112, 113, 114, 115, 116,
                        117, 118, 119, 120, 121, 122, 123, 124, 126, 127,
                        128, 31,
                        129, 31,
                        130, 31,
                        131, 32,
                        132, 31, 1,
                        133, 32,
                        134, 1,
                        135,
                        136,
                        137,
                        138, 31,
                        15,
                        140, 31, CONTEXT.LARGE_FRAMESIZE - 1,
                        141, 31, CONTEXT.LARGE_FRAMESIZE - 1,
                        142, 31, CONTEXT.LARGE_FRAMESIZE - 1,
                        143, 31, 63, 127,
                        151,
                        159,
                        167, 31,
                        171, 31,
                        175, 31,
                        125,
                        176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188,
                        189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201,
                        202, 203, 204, 205, 206, 207, 208, 225, 240);
        final CharSequence source = DecoderV3PlusClosures.SINGLETON.decodeToString(code);
        assertEquals(ALL_BYTECODES_EXPECTED_RESULT, source);
    }

    @Test
    public void testFloatDecoding() {
        SqueakImageChunk chunk = newFloatChunk(UnsafeUtils.toBytes(new int[]{0, 1072693248}));
        assertEquals(1.0, (double) chunk.asObject(), 0);

        chunk = newFloatChunk(UnsafeUtils.toBytes(new int[]{(int) 2482401462L, 1065322751}));
        assertEquals(0.007699011184197404, (double) chunk.asObject(), 0);

        chunk = newFloatChunk(UnsafeUtils.toBytes(new int[]{876402988, 1075010976}));
        assertEquals(4.841431442464721, (double) chunk.asObject(), 0);

        chunk = newFloatChunk(UnsafeUtils.toBytes(new int[]{0, (int) 4294443008L}));
        final Object nan = chunk.asObject();
        assertTrue(nan instanceof final FloatObject o && o.isNaN());
    }

    private static SqueakImageChunk newFloatChunk(final byte[] data) {
        final SqueakImageChunk chunk = new SqueakImageChunk(
                        new SqueakImageReader(image),
                        ObjectHeader.getHeader(0, 3833906, 10, 34),
                        0, // position
                        data // 2 words
        );
        chunk.setSqueakClass(image.floatClass);
        return chunk;
    }
}

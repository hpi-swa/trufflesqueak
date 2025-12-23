/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

public final class DecoderSistaV1 extends AbstractDecoder {
    public static final DecoderSistaV1 SINGLETON = new DecoderSistaV1();
    private static final byte SP_NIL_TAG = -42;

    private DecoderSistaV1() {
    }

    @Override
    public boolean hasStoreIntoTemp1AfterCallPrimitive(final CompiledCodeObject code) {
        final byte[] bytes = code.getBytes();
        return Byte.toUnsignedInt(bytes[3]) == 245;
    }

    @Override
    protected String decodeBytecodeToString(final CompiledCodeObject code, final int b, final int index) {
        final byte[] bytecode = code.getBytes();
        return switch (b) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> "pushRcvr: " + (b & 15);
            case 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 -> "pushLitVar: " + code.getLiteral(b & 15);
            case 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63 -> "pushConstant: " + code.getLiteral(b & 31);
            case 64, 65, 66, 67, 68, 69, 70, 71 -> "pushTemp: " + (b & 7);
            case 72, 73, 74, 75 -> "pushTemp: " + (b & 3) + 8;
            case 76 -> "self";
            case 77 -> "pushConstant: true";
            case 78 -> "pushConstant: false";
            case 79 -> "pushConstant: nil";
            case 80 -> "pushConstant: 0";
            case 81 -> "pushConstant: 1";
            case 82 -> "pushThisContext:";
            case 83 -> "dup";
            case 84, 85, 86, 87 -> "unknown";
            case 88 -> "returnSelf";
            case 89 -> "return: true";
            case 90 -> "return: false";
            case 91 -> "return: nil";
            case 92 -> "returnTop";
            case 93, 94 -> "blockReturn";
            case 95 -> "nop";
            case 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127 //
                -> "send: " + code.getSqueakClass().getImage().getSpecialSelector(b - 96).asStringUnsafe();
            case 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, //
                159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175 //
                -> "send: " + code.getLiteral(b & 0xF);
            case 176, 177, 178, 179, 180, 181, 182, 183 -> "jumpTo: ";
            case 184, 185, 186, 187, 188, 189, 190, 191 -> "jumpTrue: ";
            case 192, 193, 194, 195, 196, 197, 198, 199 -> "jumpFalse: ";
            case 200, 201, 202, 203, 204, 205, 206, 207 -> "popIntoRcvr: " + (b & 7);
            case 208, 209, 210, 211, 212, 213, 214, 215 -> "popIntoTemp: " + (b & 7);
            case 216 -> "pop";
            case 217 -> "send: " + code.getSqueakClass().getImage().getSpecialSelector(SPECIAL_OBJECT.SELECTOR_SISTA_TRAP).asStringUnsafe();
            case 218, 219, 220, 221, 222, 223 -> "unknown";
            case 224 -> "extA";
            case 225 -> "extB";
            case 226 -> "pushRcvr: ";
            case 227 -> "pushLitVar: ";
            case 228 -> "pushConstant: ";
            case 229 -> "pushTemp: ";
            case 230 -> "unknown";
            case 231 -> "push: (Array new: " + (bytecode[index + 1] & 127) + ")";
            case 232 -> "pushConstant: ";
            case 233 -> "pushConstant: $";
            case 234 -> "send: ";
            case 235 -> "sendSuper: ";
            case 236 -> "unknown";
            case 237 -> "jumpTo: ";
            case 238 -> "jumpTrue: ";
            case 239 -> "jumpFalse: ";
            case 240 -> "popIntoRcvr: ";
            case 241 -> "popIntoLit: ";
            case 242 -> "popIntoTemp: ";
            case 243 -> "storeIntoRcvr: ";
            case 244 -> "storeIntoLit: ";
            case 245 -> "storeIntoTemp: ";
            case 246, 247 -> "unknown";
            case 248 -> {
                final int j = bytecode[index + 2] & 31;
                final int primitiveIndex = Byte.toUnsignedInt(bytecode[index + 1]) + j;
                yield "callPrimitive: " + primitiveIndex;
            }
            case 249 -> "pushFullClosure: (self literalAt: ?) numCopied: ? numArgs: ?";
            case 250 -> "closureNumCopied: ? numArgs: ? bytes ? to ?";
            case 251 -> "pushTemp: inVectorAt: ";
            case 252 -> "storeTemp: inVectorAt: ";
            case 253 -> "popTemp: inVectorAt: ";
            case 254, 255 -> "unknown";
            default -> throw SqueakException.create("Unknown bytecode:", b);
        };
    }

    @Override
    public int pcPreviousTo(final CompiledCodeObject code, final int pc) {
        int currentPC = 0;
        assert currentPC < pc;
        int previousPC = -1;
        while (currentPC < pc) {
            previousPC = currentPC;
            currentPC += decodeNextPCDelta(code, currentPC, false);
        }
        assert previousPC > 0;
        return previousPC;
    }

    @Override
    protected int decodeNumBytes(final CompiledCodeObject code, final int index) {
        return decodeNumBytes(code, index, 0, false);
    }

    private static int decodeNumBytes(final CompiledCodeObject code, final int index, final int extB, final boolean skipOverBlocks) {
        final int b = Byte.toUnsignedInt(code.getBytes()[index]);
        if (b <= 223) {
            return 1;
        } else if (b <= 247) {
            return 2;
        } else if (b == 250 && skipOverBlocks) {
            final int blockSize = Byte.toUnsignedInt(code.getBytes()[index + 2]) + (extB << 8);
            return 3 + blockSize;
        } else {
            return 3;
        }
    }

    private record DecodedExtension(int offset, int extA, int extB) {
    }

    private static DecodedExtension decodeExtension(final CompiledCodeObject code, final int index) {
        return decodeExtension(code.getBytes(), index);
    }

    private static DecodedExtension decodeExtension(final byte[] bc, final int index) {
        int b = Byte.toUnsignedInt(bc[index]);
        int offset = 0;
        int extA = 0;
        int extB = 0;
        while (b == 0xE0 || b == 0xE1) {
            final int byteValue = Byte.toUnsignedInt(bc[index + offset + 1]);
            if (b == 0xE0) {
                extA = (extA << 8) | byteValue;
            } else {
                extB = (extB == 0 && byteValue > 127) ? byteValue - 256 : (extB << 8) | byteValue;
            }
            offset += 2;
            b = Byte.toUnsignedInt(bc[index + offset]);
        }
        return new DecodedExtension(offset, extA, extB);
    }

    private static int decodeNextPCDelta(final CompiledCodeObject code, final int index, final boolean skipOverBlocks) {
        final DecodedExtension extension = decodeExtension(code, index);
        return decodeNextPCDelta(code, index, extension, skipOverBlocks);
    }

    private static int decodeNextPCDelta(final CompiledCodeObject code, final int index, final DecodedExtension extension, final boolean skipOverBlocks) {
        return extension.offset + decodeNumBytes(code, index + extension.offset, extension.extB, skipOverBlocks);
    }

    @Override
    public ShadowBlockParams decodeShadowBlock(final CompiledCodeObject code, final int shadowBlockIndex) {
        /**
         * From EncoderForSistaV1 >> pcOfBlockCreationBytecodeForBlockStartingAt: startpc in: method
         *
         * <pre>
         *
         *  224   11100000 aaaaaaaa           Extend A (Ext A = Ext A prev * 256 + Ext A)
         *  225   11100001 bbbbbbbb           Extend B (Ext B = Ext B prev * 256 + Ext B)
         *  250   11111010 eeiiikkk jjjjjjjj  Push Closure Num Copied iii (+ExtA//16*8)
         *                                                 Num Args kkk (+ ExtA\\16*8)
         *                                                 BlockSize jjjjjjjj (+ExtB*256).
         *                                                 ee = num extensions
         * </pre>
         */
        final byte[] bc = code.getBytes();

        final int numExtensions = Byte.toUnsignedInt(bc[shadowBlockIndex - 2]) >> 6;
        final int index = shadowBlockIndex - 3 - (numExtensions * 2);
        final DecodedExtension extension = decodeExtension(bc, index);

        final int byteA = Byte.toUnsignedInt(bc[index + extension.offset + 1]);
        final int byteB = Byte.toUnsignedInt(bc[index + extension.offset + 2]);

        final int numArgs = (byteA & 7) + Math.floorMod(extension.extA, 16) * 8;
        final int numCopied = (byteA >> 3 & 0x7) + Math.floorDiv(extension.extA, 16) * 8;
        final int blockSize = (extension.extB << 8) | byteB;

        return new ShadowBlockParams(numArgs, numCopied, blockSize);
    }

    /**
     * The implementation is derived from StackDepthFinder. Note that the Squeak compiler no longer
     * allows dead code (at least the one for SistaV1), which simplifies the implementation.
     */
    @Override
    public int determineMaxNumStackSlots(final CompiledCodeObject code, final int initialPC, final int maxPC) {
        final byte[] bc = code.getBytes();
        final int[] joins = new int[maxPC];
        Arrays.fill(joins, SP_NIL_TAG);
        int index = initialPC;
        int currentStackPointer = 0; // initial SP
        int maxStackPointer = currentStackPointer;
        final int contextSize = code.getSqueakContextSize();
        while (index < maxPC) {
            final DecodedExtension extension = decodeExtension(bc, index);
            joins[index] = currentStackPointer;
            currentStackPointer = decodeStackPointer(code, bc, index, extension, currentStackPointer, joins);
            assert 0 <= currentStackPointer && currentStackPointer <= contextSize : "Stack pointer out of range: " + currentStackPointer + " (Context size: " + contextSize + ")";
            maxStackPointer = Math.max(maxStackPointer, currentStackPointer);
            index += decodeNextPCDelta(code, index, extension, true);
        }
        assert 0 <= maxStackPointer && maxStackPointer <= contextSize : "Stack pointer out of range: " + maxStackPointer + " (Context size: " + contextSize + ")";
        return maxStackPointer;
    }

    private static int decodeStackPointer(final CompiledCodeObject code, final byte[] bc, final int index, final DecodedExtension extension, final int sp, final int[] joins) {
        CompilerAsserts.neverPartOfCompilation();

        final int indexWithExt = index + extension.offset;
        final int b = Byte.toUnsignedInt(bc[indexWithExt]);

        return switch (b) {
            case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, //
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, //
                0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, //
                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, //
                0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, //
                0x50, 0x51 //
                -> sp + 1;
            case 0x52 -> {
                if (extension.extB == 0) {
                    yield sp + 1;
                } else {
                    throw SqueakException.create("Not a bytecode:", b);
                }
            }
            case 0x53 -> sp + 1;
            case 0x54, 0x55, 0x56, 0x57 -> throw SqueakException.create("Not a bytecode:", b);
            case 0x58, 0x59, 0x5A, 0x5B -> resetStackAfterBranchOrReturn(joins, index + 1, sp + 0);
            case 0x5C -> resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);
            case 0x5D -> resetStackAfterBranchOrReturn(joins, index + 1, sp + 0);
            case 0x5E -> {
                if (extension.extA == 0) {
                    yield resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);
                } else {
                    throw SqueakException.create("Not a bytecode:", b);
                }
            }
            case 0x5F -> sp;
            case 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, //
                0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F -> {
                final int numArguments = code.getSqueakClass().getImage().getSpecialSelectorNumArgs(b - 96);
                yield sp - numArguments;
            }
            case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F -> sp + 0;
            case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> sp - 1;
            case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF -> sp - 2;
            case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp + 0, delta);
            }
            case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp - 1, delta);
            }
            case 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF, 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> sp - 1;
            /* Check joins first for '(SqueakSSL class >> #ensureSampleCert) detailedSymbolic'. */
            case 0xD8 -> joins[index] == SP_NIL_TAG ? sp - 1 : Math.max(0, sp - 1);
            case 0xD9 -> sp - 1;
            case 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF -> throw SqueakException.create("Not a bytecode:", b);
            case 0xE0, 0xE1 -> throw SqueakException.create("Caller error:", b);
            case 0xE2, 0xE3, 0xE4, 0xE5 -> sp + 1;
            case 0xE6 -> throw SqueakException.create("Not a bytecode:", b);
            case 0xE7 -> {
                final byte param = bc[indexWithExt + 1];
                final int arraySize = param & 127;
                yield sp + 1 - (param < 0 ? arraySize : 0);
            }
            case 0xE8, 0xE9 -> sp + 1;
            case 0xEA -> {
                final int byte1 = Byte.toUnsignedInt(bc[indexWithExt + 1]);
                final int numArgs = (byte1 & 7) + (extension.extB << 3);
                yield sp - numArgs;
            }
            case 0xEB -> {
                int extBValue = extension.extB;
                if (extBValue >= 64) {
                    extBValue = extBValue & 63;
                }
                final int byte1 = Byte.toUnsignedInt(bc[indexWithExt + 1]);
                final int numArgs = (byte1 & 7) + (extBValue << 3);
                yield sp - numArgs;
            }
            case 0xEC -> throw SqueakException.create("Not a bytecode:", b);
            case 0xED -> {
                final int delta = InterpreterSistaV1Node.calculateLongExtendedOffset(bc[indexWithExt + 1], extension.extB);
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp + 0, delta);
            }
            case 0xEE, 0xEF -> {
                final int delta = InterpreterSistaV1Node.calculateLongExtendedOffset(bc[indexWithExt + 1], extension.extB);
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp - 1, delta);
            }
            case 0xF0, 0xF1, 0xF2 -> sp - 1;
            case 0xF3, 0xF4, 0xF5 -> sp + 0;
            case 0xF6, 0xF7 -> throw SqueakException.create("Not a bytecode:", b);
            case 0xF8 -> {
                final int i = Byte.toUnsignedInt(bc[indexWithExt + 1]);
                final int j = bc[indexWithExt + 2] & 31;
                final int primitiveIndex = i + (j << 8);
                assert 1 <= primitiveIndex && primitiveIndex < 32767 : "primitiveIndex out of range";
                if (primitiveIndex < 1000) {
                    yield sp + 0;
                }
                throw SqueakException.create("Not yet implemented for inline prim");
            }
            case 0xF9 -> {
                final byte byteB = bc[indexWithExt + 2];
                final int numCopied = Byte.toUnsignedInt(byteB) & 63;
                final boolean receiverOnStack = (byteB >> 7 & 1) == 1;
                yield sp + (receiverOnStack ? -1 : 0) - numCopied + 1;
            }
            case 0xFA -> {
                final byte byteA = bc[indexWithExt + 1];
                final int numCopied = (Byte.toUnsignedInt(byteA) >> 3 & 0x7) + Math.floorDiv(extension.extA, 16) * 8;
                yield sp + 1 - numCopied;
            }
            case 0xFB -> sp + 1;
            case 0xFC -> sp + 0;
            case 0xFD -> sp - 1;
            case 0xFE, 0xFF -> throw SqueakException.create("Not a bytecode:", b);

            default -> throw new AssertionError("Unknown bytecode");
        };
    }

    private static int jumpAndResetStackAfterBranchOrReturn(final int[] joins, final int pc, final int sp, final int delta) {
        if (delta < 0) {
            assert joins[pc + delta] == sp : "bad join";
        } else {
            joins[pc + delta] = sp;
        }
        return resetStackAfterBranchOrReturn(joins, pc, sp);
    }

    private static int resetStackAfterBranchOrReturn(final int[] joins, final int pc, final int sp) {
        if (pc < joins.length) {
            final int spAtPC = joins[pc];
            if (spAtPC == SP_NIL_TAG) {
                return sp;
            } else {
                return spAtPC;
            }
        } else {
            return sp;
        }
    }
}

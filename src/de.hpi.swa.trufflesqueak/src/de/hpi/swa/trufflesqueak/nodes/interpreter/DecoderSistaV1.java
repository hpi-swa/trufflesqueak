/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

public final class DecoderSistaV1 extends AbstractDecoder {
    public static final DecoderSistaV1 SINGLETON = new DecoderSistaV1();

    private static final byte[] BYTECODE_DELTAS = new byte[256];

    private DecoderSistaV1() {
    }

    /*
     * Split the implementation of determineMaxNumStackSlots into a fast path (bytecodes that do not
     * need an extension or additional bytes to determine the stack pointer change) decoded by a
     * table lookup, and a slow path, decoded using a switch.
     */
    static {
        // Default everything to "Requires Switch Logic"
        Arrays.fill(BYTECODE_DELTAS, NEEDS_SWITCH);

        // Mark the extensions.
        BYTECODE_DELTAS[0xE0] = NEEDS_EXTENSION;
        BYTECODE_DELTAS[0xE1] = NEEDS_EXTENSION;

        // Mark the special sends.
        Arrays.fill(BYTECODE_DELTAS, 0x60, 0x80, NEEDS_SPECIAL_SELECTORS);

        // Map the "Fast Path" (Fixed Deltas)

        // sp + 1
        Arrays.fill(BYTECODE_DELTAS, 0x00, 0x52, (byte) 1);
        BYTECODE_DELTAS[0x53] = 1;
        Arrays.fill(BYTECODE_DELTAS, 0xE2, 0xE6, (byte) 1);
        BYTECODE_DELTAS[0xE8] = 1;
        BYTECODE_DELTAS[0xE9] = 1;
        BYTECODE_DELTAS[0xFB] = 1;

        // sp + 0
        BYTECODE_DELTAS[0x5F] = 0;
        Arrays.fill(BYTECODE_DELTAS, 0x80, 0x90, (byte) 0);
        Arrays.fill(BYTECODE_DELTAS, 0xF3, 0xF6, (byte) 0);
        BYTECODE_DELTAS[0xFC] = 0;

        // sp - 1
        Arrays.fill(BYTECODE_DELTAS, 0x90, 0xA0, (byte) -1);
        Arrays.fill(BYTECODE_DELTAS, 0xC8, 0xD8, (byte) -1);
        BYTECODE_DELTAS[0xD9] = -1;
        Arrays.fill(BYTECODE_DELTAS, 0xF0, 0xF3, (byte) -1);
        BYTECODE_DELTAS[0xFD] = -1;

        // sp - 2
        Arrays.fill(BYTECODE_DELTAS, 0xA0, 0xB0, (byte) -2);
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
            case 72, 73, 74, 75 -> "pushTemp: " + ((b & 3) + 8);
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
                final int primitiveIndex = Byte.toUnsignedInt(bytecode[index + 1]) + (j << 8);
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
            currentPC += decodeNextPCDelta(code, currentPC, decodeExtension(code, currentPC), false);
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

    private static int decodeNoExtensionNumBytes(final int b) {
        if (b <= 223) {
            return 1;
        } else if (b <= 247) {
            return 2;
        } else {
            return 3;
        }
    }

    private record DecodedExtension(int offset, int extA, int extB) {
        private static final DecodedExtension DEFAULT = new DecodedExtension(0, 0, 0);
    }

    private static DecodedExtension decodeExtension(final CompiledCodeObject code, final int index) {
        return decodeExtension(code.getBytes(), index);
    }

    private static DecodedExtension decodeExtension(final byte[] bc, final int index) {
        int b = Byte.toUnsignedInt(bc[index]);
        int offset = 0;

        // Track unified state
        long extBA = 0;

        while (b == 0xE0 || b == 0xE1) {
            final int byteValue = Byte.toUnsignedInt(bc[index + offset + 1]);
            if (b == 0xE0) {
                extBA = InterpreterSistaV1Node.computeExtA(extBA, byteValue);
            } else {
                extBA = InterpreterSistaV1Node.computeExtB(extBA, byteValue);
            }
            offset += 2;
            b = Byte.toUnsignedInt(bc[index + offset]);
        }

        return new DecodedExtension(offset, (int) extBA, (int) (extBA >> 32));
    }

    private static int decodeNextPCDelta(final CompiledCodeObject code, final int index, final DecodedExtension extension, final boolean skipOverBlocks) {
        return extension.offset + decodeNumBytes(code, index + extension.offset, extension.extB, skipOverBlocks);
    }

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
    @Override
    public ShadowBlockParams decodeShadowBlock(final CompiledCodeObject code, final int shadowBlockIndex) {
        final byte[] bc = code.getBytes();

        final int numExtensions = Byte.toUnsignedInt(bc[shadowBlockIndex - 2]) >> 6;
        final int index = shadowBlockIndex - 3 - (numExtensions * 2);
        final DecodedExtension extension = decodeExtension(bc, index);

        final int byteA = Byte.toUnsignedInt(bc[index + extension.offset + 1]);
        final int byteB = Byte.toUnsignedInt(bc[index + extension.offset + 2]);

        final int numArgs = (byteA & 0x07) + (extension.extA & 0x0F) * 8;
        final int numCopied = (byteA >> 3 & 0x07) + (extension.extA >> 4) * 8;
        final int blockSize = (extension.extB << 8) | byteB;

        return new ShadowBlockParams(numArgs, numCopied, blockSize);
    }

    /**
     * The implementation is derived from StackDepthFinder. Note that the Squeak compiler no longer
     * allows dead code (at least the one for SistaV1), which simplifies the implementation.
     */
    @Override
    public int determineMaxNumStackSlots(final CompiledCodeObject code, final int initialPC, final int maxPC, final int initialSP) {
        final SqueakImageContext image = code.getSqueakClass().getImage();
        final int contextSize = code.getSqueakContextSize();
        final byte[] joins = new byte[maxPC];
        final byte[] bc = code.getBytes();

        // Use a biased stack pointer to avoid filling the joins array.
        int currentStackPointer = SP_BIAS;
        int maxStackPointer = currentStackPointer;

        int index = initialPC;
        while (index < maxPC) {
            joins[index] = (byte) currentStackPointer;

            final int b = Byte.toUnsignedInt(bc[index]);
            final int delta = BYTECODE_DELTAS[b];
            if (delta > NEEDS_SPECIAL_SELECTORS) {
                // Fast path: stack offset determined by single bytecode only.
                currentStackPointer += delta;
                index += decodeNoExtensionNumBytes(b);
            } else if (delta == NEEDS_SPECIAL_SELECTORS) {
                // Fast path: stack offset determined by single bytecode and special selectors
                // table.
                currentStackPointer -= image.getSpecialSelectorNumArgs(b - 96);
                index += decodeNoExtensionNumBytes(b);
            } else {
                // Slow path: stack offset determined by extension and/or multiple bytes.
                final DecodedExtension extension = (delta == NEEDS_EXTENSION)
                                ? decodeExtension(bc, index)
                                : DecodedExtension.DEFAULT;
                currentStackPointer = decodeStackPointer(bc, index, extension, currentStackPointer, joins);
                index += decodeNextPCDelta(code, index, extension, true);
            }

            if (currentStackPointer > maxStackPointer) {
                maxStackPointer = currentStackPointer;
            }
        }

        final int finalMaxStackPointer = maxStackPointer - SP_BIAS + initialSP;
        assert initialSP <= finalMaxStackPointer && finalMaxStackPointer <= contextSize : "Stack pointer out of range: " + finalMaxStackPointer + " (Context size: " + contextSize + ")";

        // Leave space for a push that could occur during unwind handling or process/context
        // manipulation. For example, Context class>>contextEnsure: and ContextPart>>unwindAndStop:
        return finalMaxStackPointer < contextSize ? finalMaxStackPointer + 1 : finalMaxStackPointer;
    }

    private static int decodeStackPointer(final byte[] bc, final int index, final DecodedExtension extension, final int sp, final byte[] joins) {
        CompilerAsserts.neverPartOfCompilation();

        final int indexWithExt = index + extension.offset;
        final int b = Byte.toUnsignedInt(bc[indexWithExt]);

        return switch (b) {
            case 0x52 -> {
                if (extension.extB == 0) {
                    yield sp + 1;
                } else {
                    throw SqueakException.create("Not a bytecode:", b);
                }
            }

            case 0x54, 0x55, 0x56, 0x57 -> throw SqueakException.create("Not a bytecode:", b);
            case 0x58, 0x59, 0x5A, 0x5B -> resetStackAfterBranchOrReturn(joins, index + 1, sp);
            case 0x5C -> resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);
            case 0x5D -> resetStackAfterBranchOrReturn(joins, index + 1, sp);
            case 0x5E -> {
                if (extension.extA == 0) {
                    yield resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);
                } else {
                    throw SqueakException.create("Not a bytecode:", b);
                }
            }

            case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp, delta);
            }
            case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp - 1, delta);
            }

            /* Check joins first for '(SqueakSSL class >> #ensureSampleCert) detailedSymbolic'. */
            case 0xD8 -> Byte.toUnsignedInt(joins[index]) == SP_NIL_TAG ? sp - 1 : Math.max(SP_BIAS, sp - 1);

            case 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF -> throw SqueakException.create("Not a bytecode:", b);

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
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp, delta);
            }
            case 0xEE, 0xEF -> {
                final int delta = InterpreterSistaV1Node.calculateLongExtendedOffset(bc[indexWithExt + 1], extension.extB);
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp - 1, delta);
            }

            case 0xF0, 0xF1, 0xF2 -> sp - 1;
            case 0xF3, 0xF4, 0xF5 -> sp;

            case 0xF6, 0xF7 -> throw SqueakException.create("Not a bytecode:", b);
            case 0xF8 -> {
                final int i = Byte.toUnsignedInt(bc[indexWithExt + 1]);
                final int j = bc[indexWithExt + 2] & 31;
                final int primitiveIndex = i + (j << 8);
                assert 1 <= primitiveIndex && primitiveIndex < 32767 : "primitiveIndex out of range";
                if (primitiveIndex < 1000) {
                    yield sp;
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

            case 0xFE, 0xFF -> throw SqueakException.create("Not a bytecode:", b);

            default -> {
                if (BYTECODE_DELTAS[b] <= NEEDS_SWITCH) {
                    throw new AssertionError("Unknown bytecode");
                } else {
                    throw SqueakException.create("Caller error:", b);
                }
            }
        };
    }

    /**
     * Computes, for every backward jump, the temporaries that are dead at the jump target (the loop
     * header), i.e., written before being read on every path from there. The result maps the pc of
     * each backward jump bytecode (without extensions) to a bit mask over temporary indices 0-63, or
     * is {@code null} if there are no such temporaries or the bytecode is not understood. Bytecodes
     * of embedded blocks run in their own frames and are skipped. {@code pushThisContext} makes all
     * temporaries live.
     */
    public static long[] deadTempsAtBackwardJumps(final CompiledCodeObject code, final int startPC, final int endPC, final int firstTemp, final int numTemps) {
        CompilerAsserts.neverPartOfCompilation();
        if (firstTemp >= Math.min(numTemps, Long.SIZE)) {
            return null;
        }
        final byte[] bc = code.getBytes();
        final long tempsMask = (Math.min(numTemps, Long.SIZE) == Long.SIZE ? -1L : (1L << Math.min(numTemps, Long.SIZE)) - 1) & -(1L << firstTemp);
        final int[] instructionAt = new int[endPC + 1];
        Arrays.fill(instructionAt, -1);
        int count = 0;
        final int[] starts = new int[endPC];
        final int[] opPCs = new int[endPC];
        final int[] fallthroughs = new int[endPC];
        final int[] targets = new int[endPC];
        final long[] uses = new long[endPC];
        final long[] defs = new long[endPC];
        boolean hasBackwardJump = false;
        int index = startPC;
        while (index < endPC) {
            final DecodedExtension extension = decodeExtension(bc, index);
            final int opPC = index + extension.offset;
            if (opPC >= endPC) {
                return null;
            }
            final int b = Byte.toUnsignedInt(bc[opPC]);
            final int next = index + decodeNextPCDelta(code, index, extension, true);
            int fallthrough = next;
            int target = -1;
            long use = 0;
            long def = 0;
            switch (b) {
                case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> use = bit(b & 7);
                case 0x48, 0x49, 0x4A, 0x4B -> use = bit((b & 3) + 8);
                case 0x52 -> use = -1L; // pushThisContext
                case 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E -> fallthrough = -1; // returns
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> {
                    target = next + AbstractInterpreterNode.calculateShortOffset(b);
                    fallthrough = -1;
                }
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> target = next + AbstractInterpreterNode.calculateShortOffset(b);
                case 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> def = bit(b & 7);
                case 0xE5 -> use = bit(Byte.toUnsignedInt(bc[opPC + 1]));
                case 0xED -> {
                    final int delta = InterpreterSistaV1Node.calculateLongExtendedOffset(bc[opPC + 1], extension.extB);
                    target = next + delta;
                    fallthrough = -1;
                    hasBackwardJump |= delta < 0;
                }
                case 0xEE, 0xEF -> target = next + Byte.toUnsignedInt(bc[opPC + 1]) + (extension.extB << 8);
                case 0xF2, 0xF5 -> def = bit(Byte.toUnsignedInt(bc[opPC + 1]));
                case 0xFB, 0xFC, 0xFD -> use = bit(Byte.toUnsignedInt(bc[opPC + 2])); // temp vector
                case 0x54, 0x55, 0x56, 0x57, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF, 0xE6, 0xEC, 0xF6, 0xF7, 0xFE, 0xFF -> {
                    return null; // not a bytecode
                }
                default -> {
                    // no temporary access, falls through
                }
            }
            if (target >= endPC || target < -1 || fallthrough > endPC) {
                return null;
            }
            instructionAt[index] = count;
            starts[count] = index;
            opPCs[count] = opPC;
            fallthroughs[count] = fallthrough;
            targets[count] = target;
            uses[count] = use;
            defs[count] = def;
            count++;
            index = next;
        }
        if (!hasBackwardJump) {
            return null;
        }
        // Backward liveness, iterated to a fixpoint (loops).
        final long[] liveIn = new long[count];
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = count - 1; i >= 0; i--) {
                long liveOut = 0;
                if (fallthroughs[i] >= 0 && fallthroughs[i] < endPC) {
                    final int succ = instructionAt[fallthroughs[i]];
                    if (succ < 0) {
                        return null;
                    }
                    liveOut |= liveIn[succ];
                }
                if (targets[i] >= 0) {
                    final int succ = instructionAt[targets[i]];
                    if (succ < 0) {
                        return null;
                    }
                    liveOut |= liveIn[succ];
                }
                final long in = uses[i] | (liveOut & ~defs[i]);
                if (in != liveIn[i]) {
                    liveIn[i] = in;
                    changed = true;
                }
            }
        }
        long[] result = null;
        for (int i = 0; i < count; i++) {
            if (targets[i] >= 0 && targets[i] <= starts[i] && fallthroughs[i] == -1) {
                final long dead = ~liveIn[instructionAt[targets[i]]] & tempsMask;
                if (dead != 0) {
                    if (result == null) {
                        result = new long[endPC];
                    }
                    result[opPCs[i]] = dead;
                }
            }
        }
        return result;
    }

    private static long bit(final int tempIndex) {
        return tempIndex < Long.SIZE ? 1L << tempIndex : 0L;
    }

    private static int jumpAndResetStackAfterBranchOrReturn(final byte[] joins, final int pc, final int sp, final int delta) {
        if (delta < 0) {
            assert Byte.toUnsignedInt(joins[pc + delta]) == sp : "bad join";
        } else {
            joins[pc + delta] = (byte) sp;
        }
        return resetStackAfterBranchOrReturn(joins, pc, sp);
    }

    private static int resetStackAfterBranchOrReturn(final byte[] joins, final int pc, final int sp) {
        if (pc < joins.length) {
            final int spAtPC = Byte.toUnsignedInt(joins[pc]);
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

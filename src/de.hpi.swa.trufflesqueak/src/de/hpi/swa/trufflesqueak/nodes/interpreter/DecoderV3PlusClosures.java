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

public final class DecoderV3PlusClosures extends AbstractDecoder {
    public static final DecoderV3PlusClosures SINGLETON = new DecoderV3PlusClosures();

    private static final byte[] BYTECODE_DELTAS = new byte[256];

    private static final byte DYNAMIC_LENGTH = 4;
    private static final byte[] BYTECODE_LENGTHS = new byte[256];

    private DecoderV3PlusClosures() {
    }

    /*
     * Split the implementation of determineMaxNumStackSlots into a fast path (bytecodes that do not
     * need additional bytes to determine the stack pointer change) decoded by a table lookup, and a
     * slow path, decoded using a switch.
     */
    static {
        // Default everything to "Requires Switch Logic"
        Arrays.fill(BYTECODE_DELTAS, NEEDS_SWITCH);

        // 0-95: Push (sp + 1)
        Arrays.fill(BYTECODE_DELTAS, 0, 96, (byte) 1);

        // 96-111: Pop (sp - 1)
        Arrays.fill(BYTECODE_DELTAS, 96, 112, (byte) -1);

        // 112-119: Push Constant (sp + 1)
        Arrays.fill(BYTECODE_DELTAS, 112, 120, (byte) 1);

        // 128: Push, 129: Store (0), 130: Pop (-1)
        BYTECODE_DELTAS[128] = 1;
        BYTECODE_DELTAS[129] = 0;
        BYTECODE_DELTAS[130] = -1;

        // 135: Pop (-1), 136-137: Push (+1)
        BYTECODE_DELTAS[135] = -1;
        BYTECODE_DELTAS[136] = 1;
        BYTECODE_DELTAS[137] = 1;

        // 139: Store (0), 140: Push (+1), 141: Store (0), 142: Pop (-1)
        BYTECODE_DELTAS[139] = 0;
        BYTECODE_DELTAS[140] = 1;
        BYTECODE_DELTAS[141] = 0;
        BYTECODE_DELTAS[142] = -1;

        // 176-207: Special Selectors (Variable delta based on Image)
        Arrays.fill(BYTECODE_DELTAS, 176, 208, NEEDS_SPECIAL_SELECTORS);

        // 208-223: Send 0 args (delta 0)
        Arrays.fill(BYTECODE_DELTAS, 208, 224, (byte) 0);

        // 224-239: Send 1 arg (delta -1)
        Arrays.fill(BYTECODE_DELTAS, 224, 240, (byte) -1);

        // 240-255: Send 2 args (delta -2)
        Arrays.fill(BYTECODE_DELTAS, 240, 256, (byte) -2);
    }

    static {
        // Default to 1 byte (covers 0-127, 135-137, 144-159, 176-255)
        Arrays.fill(BYTECODE_LENGTHS, (byte) 1);

        // 2-byte instructions
        Arrays.fill(BYTECODE_LENGTHS, 128, 132, (byte) 2);
        Arrays.fill(BYTECODE_LENGTHS, 133, 135, (byte) 2);
        BYTECODE_LENGTHS[138] = 2;
        Arrays.fill(BYTECODE_LENGTHS, 160, 176, (byte) 2);

        // 3-byte instructions
        BYTECODE_LENGTHS[132] = 3;
        Arrays.fill(BYTECODE_LENGTHS, 139, 143, (byte) 3);

        // 4-byte / Dynamic (Push Closure)
        BYTECODE_LENGTHS[143] = DYNAMIC_LENGTH;
    }

    private static int decodeNumBytes(final int b) {
        return BYTECODE_LENGTHS[b];
    }

    @Override
    public boolean hasStoreIntoTemp1AfterCallPrimitive(final CompiledCodeObject code) {
        final byte[] bytes = code.getBytes();
        return Byte.toUnsignedInt(bytes[3]) == 129 && (Byte.toUnsignedInt(bytes[4]) >> 6 & 3) == 1;
    }

    @Override
    protected String decodeBytecodeToString(final CompiledCodeObject code, final int b0, final int index) {
        final byte[] bytecode = code.getBytes();
        return switch (b0) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> "pushRcvr: " + (b0 & 15);
            case 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 -> "pushTemp: " + (b0 & 15);
            case 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63 -> "pushConstant: " + code.getLiteral(b0 & 31);
            case 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95 -> "pushLitVar: " + code.getLiteral(b0 & 31);
            case 96, 97, 98, 99, 100, 101, 102, 103 -> "popIntoRcvr: " + (b0 & 7);
            case 104, 105, 106, 107, 108, 109, 110, 111 -> "popIntoTemp: " + (b0 & 7);
            case 112 -> "self";
            case 113 -> "pushConstant: true";
            case 114 -> "pushConstant: false";
            case 115 -> "pushConstant: nil";
            case 116 -> "pushConstant: -1";
            case 117 -> "pushConstant: 0";
            case 118 -> "pushConstant: 1";
            case 119 -> "pushConstant: 2";
            case 120 -> "returnSelf";
            case 121 -> "return: true";
            case 122 -> "return: false";
            case 123 -> "return: nil";
            case 124 -> "returnTop";
            case 125 -> "blockReturn";
            case 126, 127 -> "unknown: " + b0;
            case 128 -> {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = InterpreterV3PlusClosuresNode.variableIndex(b1);
                yield switch (InterpreterV3PlusClosuresNode.variableType(b1)) {
                    case 0 -> "pushRcvr: " + variableIndex;
                    case 1 -> "pushTemp: " + variableIndex;
                    case 2 -> "pushConstant: " + code.getLiteral(variableIndex);
                    case 3 -> "pushLit: " + variableIndex;
                    default -> throw SqueakException.create("unexpected type for ExtendedPush");
                };
            }
            case 129 -> {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = InterpreterV3PlusClosuresNode.variableIndex(b1);
                yield switch (InterpreterV3PlusClosuresNode.variableType(b1)) {
                    case 0 -> "storeIntoRcvr: " + variableIndex;
                    case 1 -> "storeIntoTemp: " + variableIndex;
                    case 2 -> "unknown: " + b1;
                    case 3 -> "storeIntoLit: " + variableIndex;
                    default -> throw SqueakException.create("illegal ExtendedStore bytecode");
                };
            }
            case 130 -> {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = InterpreterV3PlusClosuresNode.variableIndex(b1);
                yield switch (InterpreterV3PlusClosuresNode.variableType(b1)) {
                    case 0 -> "popIntoRcvr: " + variableIndex;
                    case 1 -> "popIntoTemp: " + variableIndex;
                    case 2 -> "unknown: " + b1;
                    case 3 -> "popIntoLit: " + variableIndex;
                    default -> throw SqueakException.create("illegal ExtendedStore bytecode");
                };
            }
            case 131 -> "send: " + code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 31);
            case 132 -> {
                final int b1 = Byte.toUnsignedInt(bytecode[index + 1]);
                final int b2 = Byte.toUnsignedInt(bytecode[index + 2]);
                yield switch (b1 >> 5) {
                    case 0 -> "send: " + code.getLiteral(b2);
                    case 1 -> "sendSuper: " + code.getLiteral(b2 & 31);
                    case 2 -> "pushRcvr: " + b2;
                    case 3 -> "pushConstant: " + code.getLiteral(b2);
                    case 4 -> "pushLit: " + b2;
                    case 5 -> "storeIntoRcvr: " + b2;
                    case 6 -> "popIntoRcvr: " + b2;
                    case 7 -> "storeIntoLit: " + b2;
                    default -> "unknown: " + b1;
                };
            }
            case 133 -> "sendSuper: " + code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 31);
            case 134 -> "send: " + code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 63);
            case 135 -> "pop";
            case 136 -> "dup";
            case 137 -> "pushThisContext:";
            case 138 -> "push: (Array new: " + (Byte.toUnsignedInt(bytecode[index + 1]) & 127) + ")";
            case 139 -> "callPrimitive: " + (Byte.toUnsignedInt(bytecode[index + 1]) + (Byte.toUnsignedInt(bytecode[index + 2]) << 8));
            case 140 -> "pushTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 141 -> "storeIntoTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 142 -> "popIntoTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 143 -> {
                final byte b1 = bytecode[index + 1];
                final int start = index + 4;
                final int end = start + (Byte.toUnsignedInt(bytecode[index + 2]) << 8 | Byte.toUnsignedInt(bytecode[index + 3]));
                yield "closureNumCopied: " + (b1 >> 4 & 0xF) + " numArgs: " + (b1 & 0xF) + " bytes " + start + " to " + end;
            }
            case 144, 145, 146, 147, 148, 149, 150, 151 -> "jumpTo: " + ((b0 & 7) + 1);
            case 152, 153, 154, 155, 156, 157, 158, 159 -> "jumpFalse: " + ((b0 & 7) + 1);
            case 160, 161, 162, 163, 164, 165, 166, 167 -> "jumpTo: " + (((b0 & 7) - 4 << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 168, 169, 170, 171 -> "jumpTrue: " + (((b0 & 3) << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 172, 173, 174, 175 -> "jumpFalse: " + (((b0 & 3) << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207 //
                -> "send: " + code.getSqueakClass().getImage().getSpecialSelector(b0 - 176).asStringUnsafe();
            case 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, //
                240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255 //
                -> "send: " + code.getLiteral(b0 & 0xF);
            default -> throw SqueakException.create("Unknown bytecode:", b0);
        };
    }

    @Override
    public int pcPreviousTo(final CompiledCodeObject code, final int pc) {
        final int initialPC = 0;
        int currentPC = initialPC;
        assert currentPC < pc;
        int previousPC = -1;
        while (currentPC < pc) {
            previousPC = currentPC;
            currentPC += decodeNumBytes(code, currentPC - initialPC);
        }
        assert previousPC > 0;
        return previousPC;
    }

    @Override
    protected int decodeNumBytes(final CompiledCodeObject code, final int index) {
        return decodeNumBytes(Byte.toUnsignedInt(code.getBytes()[index]));
    }

    private static int decodeNumBytesSkipOverBlocks(final byte[] bc, final int index) {
        final int numBytes = decodeNumBytes(Byte.toUnsignedInt(bc[index]));
        if (numBytes == DYNAMIC_LENGTH) {
            final int blockSize = (Byte.toUnsignedInt(bc[index + 2]) << 8 | Byte.toUnsignedInt(bc[index + 3]));
            return numBytes + blockSize;
        } else {
            return numBytes;
        }
    }

    @Override
    public ShadowBlockParams decodeShadowBlock(final CompiledCodeObject code, final int shadowBlockIndex) {
        final int index = shadowBlockIndex - 4;
        final byte[] bc = code.getBytes();
        final int numArgsNumCopied = Byte.toUnsignedInt(bc[index + 1]);
        final int numArgs = numArgsNumCopied & 0xF;
        final int numCopied = numArgsNumCopied >> 4 & 0xF;
        final int blockSize = (Byte.toUnsignedInt(bc[index + 2]) << 8) | Byte.toUnsignedInt(bc[index + 3]);
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
                index += decodeNumBytes(b);
            } else if (delta == NEEDS_SPECIAL_SELECTORS) {
                // Fast path: stack offset determined by single bytecode and special selectors
                // table.
                currentStackPointer -= image.getSpecialSelectorNumArgs(b - 176);
                index += decodeNumBytes(b);
            } else {
                // Slow path: stack offset determined by multiple bytes.
                currentStackPointer = decodeStackPointer(bc, index, currentStackPointer, joins);
                index += decodeNumBytesSkipOverBlocks(bc, index);
            }

            if (currentStackPointer > maxStackPointer) {
                maxStackPointer = currentStackPointer;
            }
        }

        final int finalMaxStackPointer = maxStackPointer - SP_BIAS + initialSP;
        assert initialSP <= finalMaxStackPointer && finalMaxStackPointer <= contextSize : "Stack pointer out of range: " + finalMaxStackPointer + " (Context size: " + contextSize + ")";
        return finalMaxStackPointer;
    }

    private static int decodeStackPointer(final byte[] bc, final int index, final int sp, final byte[] joins) {
        CompilerAsserts.neverPartOfCompilation();

        final int b = Byte.toUnsignedInt(bc[index]);

        return switch (b) {
            case 120, 121, 122, 123 -> resetStackAfterBranchOrReturn(joins, index + 1, sp);
            case 124, 125 -> resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);

            case 131 -> {
                final int numArguments = Byte.toUnsignedInt(bc[index + 1]) >> 5;
                yield sp - numArguments;
            }
            case 132 -> {
                final int b1 = Byte.toUnsignedInt(bc[index + 1]);
                final int b2 = Byte.toUnsignedInt(bc[index + 2]);
                final int numArguments = b1 & 31;
                yield switch (b1 >> 5) {
                    case 0, 1 -> sp - numArguments;
                    case 2, 3, 4 -> sp + 1;
                    case 5 -> sp;
                    case 6 -> sp - 1;
                    case 7 -> sp;
                    default -> throw SqueakException.create("Not a bytecode sequence:", b, " ", b1, " ", b2);
                };
            }
            case 133 -> {
                final int numArguments = Byte.toUnsignedInt(bc[index + 1]) >> 5;
                yield sp - numArguments;
            }
            case 134 -> {
                final int numArguments = Byte.toUnsignedInt(bc[index + 1]) >> 6;
                yield sp - numArguments;
            }

            case 138 -> {
                final byte param = bc[index + 1];
                final int arraySize = param & 127;
                yield sp + 1 - (param < 0 ? arraySize : 0);
            }

            case 143 -> {
                final byte numArgsNumCopied = bc[index + 1];
                final int numCopied = numArgsNumCopied >> 4 & 0xF;
                yield sp + 1 - numCopied;
            }
            case 144, 145, 146, 147, 148, 149, 150, 151 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp, delta);
            }
            case 152, 153, 154, 155, 156, 157, 158, 159 -> {
                final int delta = AbstractInterpreterNode.calculateShortOffset(b);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp - 1, delta);
            }
            case 160, 161, 162, 163, 164, 165, 166, 167 -> {
                final int delta = ((b & 7) - 4 << 8) + Byte.toUnsignedInt(bc[index + 1]);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 2, sp, delta);
            }
            case 168, 169, 170, 171, 172, 173, 174, 175 -> {
                final int delta = ((b & 3) << 8) + Byte.toUnsignedInt(bc[index + 1]);
                yield jumpAndResetStackAfterBranchOrReturn(joins, index + 2, sp - 1, delta);
            }

            default -> {
                if (BYTECODE_DELTAS[b] <= NEEDS_SWITCH) {
                    throw new AssertionError("Unknown bytecode");
                } else {
                    throw SqueakException.create("Caller error:", b);
                }
            }
        };
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

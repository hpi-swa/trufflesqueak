/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.interpreter.InterpreterNewsqueakV4Node.BC;

/**
 * Decoder for the NewsqueakV4 bytecode set, the secondary bytecode set of Newspeak images. See
 * EncoderForNewsqueakV4 and StackInterpreter class>>initializeBytecodeTableForNewspeakV4.
 */
public final class DecoderNewsqueakV4 extends AbstractDecoder {
    public static final DecoderNewsqueakV4 SINGLETON = new DecoderNewsqueakV4();

    private DecoderNewsqueakV4() {
    }

    private record DecodedExtension(int offset, int extA, int extB) {
    }

    static DecodedExtension decodeExtension(final byte[] bc, final int index) {
        int b = Byte.toUnsignedInt(bc[index]);
        int offset = 0;
        int extA = 0;
        int extB = 0;
        int numExtB = 0;
        while (b == BC.EXT_A || b == BC.EXT_B) {
            final int byteValue = Byte.toUnsignedInt(bc[index + offset + 1]);
            if (b == BC.EXT_A) {
                extA = InterpreterNewsqueakV4Node.computeExtA(extA, byteValue);
            } else {
                extB = InterpreterNewsqueakV4Node.computeExtB(extB, numExtB++, byteValue);
            }
            offset += 2;
            b = Byte.toUnsignedInt(bc[index + offset]);
        }
        return new DecodedExtension(offset, extA, extB);
    }

    private static int decodeNumBytes(final byte[] bc, final int index, final int extB, final boolean skipOverBlocks) {
        final int b = Byte.toUnsignedInt(bc[index]);
        if (b <= 223) {
            return 1;
        } else if (b <= 248) {
            return 2;
        } else if (b == BC.EXT_PUSH_CLOSURE && skipOverBlocks) {
            final int blockSize = Byte.toUnsignedInt(bc[index + 2]) + (extB << 8);
            return 3 + blockSize;
        } else {
            return 3;
        }
    }

    @Override
    protected int decodeNumBytes(final CompiledCodeObject code, final int index) {
        return decodeNumBytes(code.getBytes(), index, 0, false);
    }

    @Override
    public boolean hasStoreIntoTemp1AfterCallPrimitive(final CompiledCodeObject code) {
        final byte[] bytes = code.getBytes();
        return bytes.length > 3 && Byte.toUnsignedInt(bytes[0]) == BC.CALL_PRIMITIVE && Byte.toUnsignedInt(bytes[3]) == BC.LONG_STORE_TEMPORARY_VARIABLE;
    }

    @Override
    public int pcPreviousTo(final CompiledCodeObject code, final int pc) {
        final byte[] bc = code.getBytes();
        int currentPC = 0;
        assert currentPC < pc;
        int previousPC = -1;
        while (currentPC < pc) {
            previousPC = currentPC;
            final DecodedExtension extension = decodeExtension(bc, currentPC);
            currentPC += extension.offset + decodeNumBytes(bc, currentPC + extension.offset, extension.extB, false);
        }
        assert previousPC > 0;
        return previousPC;
    }

    /**
     * <pre>
     *  224   11100000 aaaaaaaa           Extend A (Ext A = Ext A prev * 256 + Ext A)
     *  225   11100001 sbbbbbbb           Extend B (Ext B = Ext B prev * 256 + Ext B)
     *  253   11111101 eeiiikkk jjjjjjjj  Push Closure Num Copied iii (+ExtA//16*8)
     *                                                 Num Args kkk (+ ExtA\\16*8)
     *                                                 BlockSize jjjjjjjj (+ExtB*256).
     *                                                 ee = num extensions
     * </pre>
     */
    @Override
    public ShadowBlockParams decodeShadowBlock(final CompiledCodeObject code, final int shadowBlockIndex) {
        final byte[] bc = code.getBytes();
        final int numExtensions = Byte.toUnsignedInt(bc[shadowBlockIndex - 2]) >> 6;
        final int index = shadowBlockIndex - 3 - numExtensions * 2;
        final DecodedExtension extension = decodeExtension(bc, index);
        assert Byte.toUnsignedInt(bc[index + extension.offset]) == BC.EXT_PUSH_CLOSURE;
        final int byteA = Byte.toUnsignedInt(bc[index + extension.offset + 1]);
        final int byteB = Byte.toUnsignedInt(bc[index + extension.offset + 2]);
        final int numArgs = (byteA & 0x07) + (extension.extA & 0x0F) * 8;
        final int numCopied = (byteA >> 3 & 0x07) + (extension.extA >> 4) * 8;
        final int blockSize = extension.extB << 8 | byteB;
        return new ShadowBlockParams(numArgs, numCopied, blockSize);
    }

    /** The implementation is derived from StackDepthFinder. */
    @Override
    public int determineMaxNumStackSlots(final CompiledCodeObject code, final int initialPC, final int maxPC, final int initialSP) {
        CompilerAsserts.neverPartOfCompilation();
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
            final DecodedExtension extension = decodeExtension(bc, index);
            currentStackPointer = decodeStackPointer(image, bc, index, extension, currentStackPointer, joins);
            index += extension.offset + decodeNumBytes(bc, index + extension.offset, extension.extB, true);
            if (currentStackPointer > maxStackPointer) {
                maxStackPointer = currentStackPointer;
            }
        }

        final int finalMaxStackPointer = maxStackPointer - SP_BIAS + initialSP;
        assert initialSP <= finalMaxStackPointer && finalMaxStackPointer <= contextSize : "Stack pointer out of range: " + finalMaxStackPointer + " (Context size: " + contextSize + ")";

        // Leave space for a push that could occur during unwind handling or process/context
        // manipulation.
        return finalMaxStackPointer < contextSize ? finalMaxStackPointer + 1 : finalMaxStackPointer;
    }

    private static int decodeStackPointer(final SqueakImageContext image, final byte[] bc, final int index, final DecodedExtension extension, final int sp, final byte[] joins) {
        final int indexWithExt = index + extension.offset;
        final int b = Byte.toUnsignedInt(bc[indexWithExt]);
        if (b < BC.BYTECODE_PRIM_ADD) { // 0-79: pushes
            return sp + 1;
        } else if (b < BC.SEND_LIT_SEL0_0) { // 80-111: special selector sends
            return sp - image.getSpecialSelectorNumArgs(b - BC.BYTECODE_PRIM_ADD);
        } else if (b < BC.SEND_LIT_SEL1_0) { // 112-127: sends with 0 args
            return sp;
        } else if (b < BC.SEND_LIT_SEL2_0) { // 128-143: sends with 1 arg
            return sp - 1;
        } else if (b < BC.SEND_IMPLICIT_SEL0_0) { // 144-159: sends with 2 args
            return sp - 2;
        } else if (b < BC.POP_INTO_RCVR_VAR_0) { // 160-175: implicit receiver sends with 0 args
            return sp + 1;
        } else if (b < BC.SHORT_UJUMP_0) { // 176-191: store and pops
            return sp - 1;
        } else if (b < BC.SHORT_CJUMP_TRUE_0) { // 192-199: short jumps
            return jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp, AbstractInterpreterNode.calculateShortOffset(b));
        } else if (b < BC.RETURN_RECEIVER) { // 200-215: short conditional jumps
            return jumpAndResetStackAfterBranchOrReturn(joins, index + 1, sp - 1, AbstractInterpreterNode.calculateShortOffset(b));
        }
        return switch (b) {
            case BC.RETURN_RECEIVER -> resetStackAfterBranchOrReturn(joins, index + 1, sp);
            case BC.RETURN_TOP_FROM_METHOD, BC.EXT_RETURN_TOP_FROM_BLOCK -> resetStackAfterBranchOrReturn(joins, index + 1, sp - 1);
            case BC.DUPLICATE_TOP -> sp + 1;
            case BC.POP_STACK -> Byte.toUnsignedInt(joins[index]) == SP_NIL_TAG ? sp - 1 : Math.max(SP_BIAS, sp - 1);
            case BC.EXT_NOP -> sp;
            case BC.EXT_PUSH_RECEIVER_VARIABLE, BC.EXT_PUSH_LITERAL_VARIABLE, BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_INTEGER, BC.LONG_PUSH_TEMPORARY_VARIABLE -> sp + 1;
            case BC.PUSH_NEW_ARRAY -> {
                final byte param = bc[indexWithExt + 1];
                final int arraySize = param & 127;
                yield sp + 1 - (param < 0 ? arraySize : 0);
            }
            case BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_LITERAL_VARIABLE, BC.LONG_STORE_TEMPORARY_VARIABLE -> sp;
            case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE -> sp - 1;
            case BC.EXT_SEND -> sp - decodeNumArgs(bc, indexWithExt, extension.extB);
            case BC.EXT_SEND_SUPER -> sp - decodeNumArgs(bc, indexWithExt, extension.extB >= 64 ? extension.extB & 63 : extension.extB);
            /* The receiver of absent receiver sends is not on the stack. */
            case BC.EXT_SEND_IMPLICIT, BC.EXT_SEND_DYNAMIC_SUPER, BC.EXT_SEND_SELF, BC.EXT_SEND_OUTER -> sp + 1 - decodeNumArgs(bc, indexWithExt, extension.extB);
            case BC.EXT_UNCONDITIONAL_JUMP -> {
                final int delta = InterpreterNewsqueakV4Node.calculateLongExtendedOffset(bc[indexWithExt + 1], extension.extB);
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp, delta);
            }
            case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE -> {
                final int delta = InterpreterNewsqueakV4Node.calculateLongExtendedOffset(bc[indexWithExt + 1], extension.extB);
                yield jumpAndResetStackAfterBranchOrReturn(joins, indexWithExt + 2, sp - 1, delta);
            }
            case BC.CALL_PRIMITIVE -> sp;
            case BC.PUSH_REMOTE_TEMP_LONG -> sp + 1;
            case BC.STORE_REMOTE_TEMP_LONG -> sp;
            case BC.STORE_AND_POP_REMOTE_TEMP_LONG -> sp - 1;
            case BC.EXT_PUSH_CLOSURE -> {
                final int byteA = Byte.toUnsignedInt(bc[indexWithExt + 1]);
                final int numCopied = (byteA >> 3 & 0x7) + (extension.extA >> 4) * 8;
                yield sp + 1 - numCopied;
            }
            default -> throw SqueakException.create("Not a bytecode:", b);
        };
    }

    private static int decodeNumArgs(final byte[] bc, final int index, final int extB) {
        return (Byte.toUnsignedInt(bc[index + 1]) & 7) + (extB << 3);
    }

    private static int jumpAndResetStackAfterBranchOrReturn(final byte[] joins, final int pc, final int sp, final int delta) {
        if (delta < 0) {
            assert Byte.toUnsignedInt(joins[pc + delta]) == sp : "bad join";
        } else if (pc + delta < joins.length) {
            joins[pc + delta] = (byte) sp;
        }
        return resetStackAfterBranchOrReturn(joins, pc, sp);
    }

    private static int resetStackAfterBranchOrReturn(final byte[] joins, final int pc, final int sp) {
        if (pc < joins.length) {
            final int spAtPC = Byte.toUnsignedInt(joins[pc]);
            return spAtPC == SP_NIL_TAG ? sp : spAtPC;
        } else {
            return sp;
        }
    }

    @Override
    protected String decodeBytecodeToString(final CompiledCodeObject code, final int b, final int index) {
        final byte[] bc = code.getBytes();
        if (b < 16) {
            return "pushRcvr: " + (b & 15);
        } else if (b < 32) {
            return "pushLitVar: " + code.getLiteral(b & 15);
        } else if (b < 64) {
            return "pushConstant: " + code.getLiteral(b & 31);
        } else if (b < 76) {
            return "pushTemp: " + (b - 64);
        } else if (BC.BYTECODE_PRIM_ADD <= b && b < BC.SEND_LIT_SEL0_0) {
            return "send: " + code.getSqueakClass().getImage().getSpecialSelector(b - BC.BYTECODE_PRIM_ADD).asStringUnsafe();
        } else if (BC.SEND_LIT_SEL0_0 <= b && b < BC.SEND_IMPLICIT_SEL0_0) {
            return "send: " + code.getLiteral(b & 0xF);
        } else if (BC.SEND_IMPLICIT_SEL0_0 <= b && b < BC.POP_INTO_RCVR_VAR_0) {
            return "sendImplicit: " + code.getLiteral(b & 0xF);
        } else if (BC.POP_INTO_RCVR_VAR_0 <= b && b < BC.POP_INTO_TEMP_VAR_0) {
            return "popIntoRcvr: " + (b & 7);
        } else if (BC.POP_INTO_TEMP_VAR_0 <= b && b < BC.SHORT_UJUMP_0) {
            return "popIntoTemp: " + (b & 7);
        } else if (BC.SHORT_UJUMP_0 <= b && b < BC.SHORT_CJUMP_TRUE_0) {
            return "jumpTo: +" + AbstractInterpreterNode.calculateShortOffset(b);
        } else if (BC.SHORT_CJUMP_TRUE_0 <= b && b < BC.SHORT_CJUMP_FALSE_0) {
            return "jumpTrue: +" + AbstractInterpreterNode.calculateShortOffset(b);
        } else if (BC.SHORT_CJUMP_FALSE_0 <= b && b < BC.RETURN_RECEIVER) {
            return "jumpFalse: +" + AbstractInterpreterNode.calculateShortOffset(b);
        }
        return switch (b) {
            case BC.PUSH_RECEIVER -> "self";
            case BC.EXT_PUSH_PSEUDO_VARIABLE_OR_OUTER -> "pushPseudoVariableOrOuter";
            case BC.PUSH_CONSTANT_ZERO -> "pushConstant: 0";
            case BC.PUSH_CONSTANT_ONE -> "pushConstant: 1";
            case BC.RETURN_RECEIVER -> "returnSelf";
            case BC.RETURN_TOP_FROM_METHOD -> "returnTop";
            case BC.EXT_RETURN_TOP_FROM_BLOCK -> "blockReturn";
            case BC.DUPLICATE_TOP -> "dup";
            case BC.POP_STACK -> "pop";
            case BC.EXT_NOP -> "nop";
            case BC.EXT_A -> "extA: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_B -> "extB: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_PUSH_RECEIVER_VARIABLE -> "pushRcvr: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_PUSH_LITERAL_VARIABLE -> "pushLitVar: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_PUSH_LITERAL -> "pushConstant: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_PUSH_INTEGER -> "pushInteger: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.LONG_PUSH_TEMPORARY_VARIABLE -> "pushTemp: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.PUSH_NEW_ARRAY -> "push: (Array new: " + (bc[index + 1] & 127) + ")";
            case BC.EXT_STORE_RECEIVER_VARIABLE -> "storeIntoRcvr: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_STORE_LITERAL_VARIABLE -> "storeIntoLit: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.LONG_STORE_TEMPORARY_VARIABLE -> "storeIntoTemp: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE -> "popIntoRcvr: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE -> "popIntoLit: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE -> "popIntoTemp: " + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_SEND -> "send: " + sendToString(bc, index);
            case BC.EXT_SEND_SUPER -> "sendSuper: " + sendToString(bc, index);
            case BC.EXT_SEND_IMPLICIT -> "sendImplicit: " + sendToString(bc, index);
            case BC.EXT_SEND_DYNAMIC_SUPER -> "sendDynamicSuper: " + sendToString(bc, index);
            case BC.EXT_UNCONDITIONAL_JUMP -> "jumpTo: +" + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_JUMP_IF_TRUE -> "jumpTrue: +" + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_JUMP_IF_FALSE -> "jumpFalse: +" + Byte.toUnsignedInt(bc[index + 1]);
            case BC.EXT_SEND_SELF -> "sendSelf: " + sendToString(bc, index);
            case BC.CALL_PRIMITIVE -> "callPrimitive: " + (Byte.toUnsignedInt(bc[index + 1]) + (Byte.toUnsignedInt(bc[index + 2]) << 8));
            case BC.PUSH_REMOTE_TEMP_LONG -> "pushTemp: " + Byte.toUnsignedInt(bc[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bc[index + 2]);
            case BC.STORE_REMOTE_TEMP_LONG -> "storeTemp: " + Byte.toUnsignedInt(bc[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bc[index + 2]);
            case BC.STORE_AND_POP_REMOTE_TEMP_LONG -> "popTemp: " + Byte.toUnsignedInt(bc[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bc[index + 2]);
            case BC.EXT_PUSH_CLOSURE -> "closureNumCopied: " + (Byte.toUnsignedInt(bc[index + 1]) >> 3 & 7) + " numArgs: " + (bc[index + 1] & 7) + " bytes: " + Byte.toUnsignedInt(bc[index + 2]);
            case BC.EXT_SEND_OUTER -> "sendOuter: " + sendToString(bc, index) + " depth: " + Byte.toUnsignedInt(bc[index + 2]);
            default -> "unknown: " + b;
        };
    }

    private static String sendToString(final byte[] bc, final int index) {
        final int byte1 = Byte.toUnsignedInt(bc[index + 1]);
        return "literal " + (byte1 >> 3) + " numArgs: " + (byte1 & 7);
    }
}

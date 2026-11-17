/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl;

import java.util.ArrayList;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.SqueakBytecodeTranslator.UnsupportedBytecodeException;

/**
 * Bytecode-set independent intermediate representation of Squeak bytecode. The SistaV1 and
 * V3PlusClosures bytecode sets are decoded into a list of {@link Instruction}s, which is then
 * translated into Bytecode DSL operations by {@link SqueakBytecodeTranslator}.
 */
public final class SqueakBytecodeIR {
    public static final byte PUSH_RECEIVER_VARIABLE = 0; // a = index
    public static final byte PUSH_LITERAL_VARIABLE = 1; // a = literal index
    public static final byte PUSH_LITERAL_CONSTANT = 2; // a = literal index
    public static final byte PUSH_TEMPORARY = 3; // a = temp index
    public static final byte PUSH_RECEIVER = 4;
    public static final byte PUSH_CONSTANT = 5; // constant
    public static final byte PUSH_ACTIVE_CONTEXT = 6;
    public static final byte DUP = 7;
    public static final byte POP = 8;
    public static final byte STORE_RECEIVER_VARIABLE = 9; // a = index, b = pop?
    public static final byte STORE_LITERAL_VARIABLE = 10; // a = literal index, b = pop?
    public static final byte STORE_TEMPORARY = 11; // a = temp index, b = pop?
    public static final byte PUSH_REMOTE_TEMP = 12; // a = index in vector, b = vector temp index
    public static final byte STORE_REMOTE_TEMP = 13; // a = index in vector, b = vector temp index, c = pop?
    public static final byte PUSH_NEW_ARRAY = 14; // a = size, b = pop values?
    public static final byte PUSH_FULL_CLOSURE = 15; // a = literal index, b = numCopied, c = flags
    public static final byte PUSH_CLOSURE = 16; // a = numArgs, b = numCopied, c = blockSize
    public static final byte SEND = 17; // a = literal index, b = numArgs
    public static final byte SEND_SPECIAL = 18; // a = special selector index, b = numArgs
    public static final byte SEND_SUPER = 19; // a = literal index, b = numArgs, c = directed?
    public static final byte JUMP = 20; // target
    public static final byte JUMP_TRUE = 21; // target
    public static final byte JUMP_FALSE = 22; // target
    public static final byte RETURN_RECEIVER = 23;
    public static final byte RETURN_CONSTANT = 24; // constant
    public static final byte RETURN_TOP = 25;
    public static final byte BLOCK_RETURN_NIL = 26;
    public static final byte BLOCK_RETURN_TOP = 27;
    public static final byte CALL_PRIMITIVE = 28; // a = primitive index
    public static final byte NOP = 29;
    public static final byte UNKNOWN = 30; // a = bytecode

    public static final int FULL_CLOSURE_IGNORE_OUTER_CONTEXT = 1;
    public static final int FULL_CLOSURE_RECEIVER_ON_STACK = 2;

    /** Marks instructions that cannot be reached (their stack depth is unknown). */
    public static final int SP_UNREACHABLE = -1;

    private SqueakBytecodeIR() {
    }

    public static final class Instruction {
        /** Zero-based pc of the instruction, including its extension prefixes. */
        public final int pc;
        /** Zero-based pc of the following instruction (after any embedded block). */
        public final int nextPC;
        public final byte kind;
        public final int a;
        public final int b;
        public final int c;
        public final Object constant;
        /** Zero-based target pc of jumps, -1 otherwise. */
        public final int target;
        /** Squeak stack pointer (temps included) before this instruction. */
        public int spBefore = SP_UNREACHABLE;

        Instruction(final int pc, final int nextPC, final byte kind, final int a, final int b, final int c, final Object constant, final int target) {
            this.pc = pc;
            this.nextPC = nextPC;
            this.kind = kind;
            this.a = a;
            this.b = b;
            this.c = c;
            this.constant = constant;
            this.target = target;
        }

        public boolean isJump() {
            return kind == JUMP || kind == JUMP_TRUE || kind == JUMP_FALSE;
        }

        public boolean isBackwardJump() {
            return isJump() && target <= pc;
        }

        public boolean isReturn() {
            return kind >= RETURN_RECEIVER && kind <= BLOCK_RETURN_TOP;
        }

        /** Whether control never continues with the following instruction. */
        public boolean isTerminator() {
            return kind == JUMP || isReturn() || kind == UNKNOWN;
        }

        public boolean isReachable() {
            return spBefore != SP_UNREACHABLE;
        }

        public int stackDelta() {
            return switch (kind) {
                case PUSH_RECEIVER_VARIABLE, PUSH_LITERAL_VARIABLE, PUSH_LITERAL_CONSTANT, PUSH_TEMPORARY, PUSH_RECEIVER, PUSH_CONSTANT, PUSH_ACTIVE_CONTEXT, DUP, PUSH_REMOTE_TEMP -> 1;
                case POP, JUMP_TRUE, JUMP_FALSE, RETURN_TOP, BLOCK_RETURN_TOP -> -1;
                case STORE_RECEIVER_VARIABLE, STORE_LITERAL_VARIABLE, STORE_TEMPORARY -> -b;
                case STORE_REMOTE_TEMP -> -c;
                case PUSH_NEW_ARRAY -> b != 0 ? 1 - a : 1;
                case PUSH_FULL_CLOSURE -> 1 - b - ((c & FULL_CLOSURE_RECEIVER_ON_STACK) != 0 ? 1 : 0);
                case PUSH_CLOSURE -> 1 - b;
                case SEND, SEND_SPECIAL -> -b;
                case SEND_SUPER -> -b - c;
                case JUMP, RETURN_RECEIVER, RETURN_CONSTANT, BLOCK_RETURN_NIL, CALL_PRIMITIVE, NOP, UNKNOWN -> 0;
                default -> throw SqueakException.create("Unknown instruction kind:", kind);
            };
        }

        @Override
        public String toString() {
            return pc + ": kind=" + kind + " a=" + a + " b=" + b + " c=" + c + (constant != null ? " constant=" + constant : "") + (target >= 0 ? " target=" + target : "") + " sp=" + spBefore;
        }
    }

    /** Collects instructions while decoding. */
    static final class Builder {
        private final ArrayList<Instruction> instructions = new ArrayList<>();

        void add(final int pc, final int nextPC, final byte kind) {
            add(pc, nextPC, kind, 0, 0, 0);
        }

        void add(final int pc, final int nextPC, final byte kind, final int a) {
            add(pc, nextPC, kind, a, 0, 0);
        }

        void add(final int pc, final int nextPC, final byte kind, final int a, final int b) {
            add(pc, nextPC, kind, a, b, 0);
        }

        void add(final int pc, final int nextPC, final byte kind, final int a, final int b, final int c) {
            instructions.add(new Instruction(pc, nextPC, kind, a, b, c, null, -1));
        }

        void addConstant(final int pc, final int nextPC, final byte kind, final Object constant) {
            instructions.add(new Instruction(pc, nextPC, kind, 0, 0, 0, constant, -1));
        }

        void addJump(final int pc, final int nextPC, final byte kind, final int target) {
            instructions.add(new Instruction(pc, nextPC, kind, 0, 0, 0, null, target));
        }

        Instruction[] toArray() {
            return instructions.toArray(new Instruction[0]);
        }
    }

    /**
     * Decodes the bytecodes of the given method or block and computes the stack depth for each
     * instruction.
     */
    public static Instruction[] decode(final CompiledCodeObject code) {
        final SqueakImageContext image = code.getSqueakClass().getImage();
        final int startPC = code.getStartPCZeroBased();
        final int endPC = code.getMaxPCZeroBased();
        final Builder builder = new Builder();
        if (code.getHasV3PlusClosuresBytecodes()) {
            decodeV3PlusClosures(image, code.getBytes(), startPC, endPC, builder);
        } else {
            decodeSistaV1(image, code.getBytes(), startPC, endPC, builder);
        }
        final Instruction[] instructions = builder.toArray();
        computeStackPointers(instructions, startPC, endPC, code.getInitialSP());
        return instructions;
    }

    private static void computeStackPointers(final Instruction[] instructions, final int startPC, final int endPC, final int initialSP) {
        /* Stack depth at jump targets, indexed by pc - startPC; 0 = unknown, otherwise sp + 1. */
        final int[] targetSP = new int[endPC - startPC + 1];
        int sp = initialSP;
        boolean reachable = true;
        for (final Instruction insn : instructions) {
            final int recorded = targetSP[insn.pc - startPC];
            if (recorded != 0) {
                if (reachable && sp != recorded - 1) {
                    throw new UnsupportedBytecodeException("Inconsistent stack depth at pc " + insn.pc + ": " + sp + " vs. " + (recorded - 1));
                }
                sp = recorded - 1;
                reachable = true;
            }
            if (!reachable) {
                continue; // dead code
            }
            insn.spBefore = sp;
            sp += insn.stackDelta();
            if (sp < 0) {
                throw new UnsupportedBytecodeException("Negative stack depth at " + insn);
            }
            if (insn.isJump()) {
                final int index = insn.target - startPC;
                if (index < 0 || index >= targetSP.length) {
                    throw new UnsupportedBytecodeException("Jump target out of range: " + insn);
                }
                if (targetSP[index] == 0) {
                    targetSP[index] = sp + 1;
                } else if (targetSP[index] != sp + 1) {
                    throw new UnsupportedBytecodeException("Inconsistent stack depth at jump target of " + insn);
                }
            }
            if (insn.isTerminator()) {
                reachable = false;
            }
        }
    }

    /*
     * SistaV1
     */

    private static void decodeSistaV1(final SqueakImageContext image, final byte[] bc, final int startPC, final int endPC, final Builder b) {
        int pc = startPC;
        while (pc < endPC) {
            final int insnPC = pc;
            int extA = 0;
            int extB = 0;
            boolean hasExtB = false;
            int op = Byte.toUnsignedInt(bc[pc++]);
            while (op == 0xE0 || op == 0xE1) {
                final int value = Byte.toUnsignedInt(bc[pc++]);
                if (op == 0xE0) {
                    extA = extA << 8 | value;
                } else {
                    /* The leading extB byte is signed, subsequent bytes are unsigned. */
                    extB = hasExtB ? extB << 8 | value : (byte) value;
                    hasExtB = true;
                }
                op = Byte.toUnsignedInt(bc[pc++]);
            }
            if (op <= 0x0F) {
                b.add(insnPC, pc, PUSH_RECEIVER_VARIABLE, op & 0xF);
            } else if (op <= 0x1F) {
                b.add(insnPC, pc, PUSH_LITERAL_VARIABLE, op & 0xF);
            } else if (op <= 0x3F) {
                b.add(insnPC, pc, PUSH_LITERAL_CONSTANT, op & 0x1F);
            } else if (op <= 0x47) {
                b.add(insnPC, pc, PUSH_TEMPORARY, op & 0x7);
            } else if (op <= 0x4B) {
                b.add(insnPC, pc, PUSH_TEMPORARY, (op & 0x3) + 8);
            } else if (op <= 0x5F) {
                switch (op) {
                    case 0x4C -> b.add(insnPC, pc, PUSH_RECEIVER);
                    case 0x4D -> b.addConstant(insnPC, pc, PUSH_CONSTANT, BooleanObject.TRUE);
                    case 0x4E -> b.addConstant(insnPC, pc, PUSH_CONSTANT, BooleanObject.FALSE);
                    case 0x4F -> b.addConstant(insnPC, pc, PUSH_CONSTANT, NilObject.SINGLETON);
                    case 0x50 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, 0L);
                    case 0x51 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, 1L);
                    case 0x52 -> {
                        if (extB == 0) {
                            b.add(insnPC, pc, PUSH_ACTIVE_CONTEXT);
                        } else {
                            b.add(insnPC, pc, UNKNOWN, op);
                        }
                    }
                    case 0x53 -> b.add(insnPC, pc, DUP);
                    case 0x58 -> b.add(insnPC, pc, RETURN_RECEIVER);
                    case 0x59 -> b.addConstant(insnPC, pc, RETURN_CONSTANT, BooleanObject.TRUE);
                    case 0x5A -> b.addConstant(insnPC, pc, RETURN_CONSTANT, BooleanObject.FALSE);
                    case 0x5B -> b.addConstant(insnPC, pc, RETURN_CONSTANT, NilObject.SINGLETON);
                    case 0x5C -> b.add(insnPC, pc, RETURN_TOP);
                    case 0x5D -> b.add(insnPC, pc, BLOCK_RETURN_NIL);
                    case 0x5E -> {
                        if (extA == 0) {
                            b.add(insnPC, pc, BLOCK_RETURN_TOP);
                        } else {
                            b.add(insnPC, pc, UNKNOWN, op);
                        }
                    }
                    case 0x5F -> b.add(insnPC, pc, NOP);
                    default -> b.add(insnPC, pc, UNKNOWN, op);
                }
            } else if (op <= 0x7F) {
                final int index = op - 0x60;
                b.add(insnPC, pc, SEND_SPECIAL, index, image.getSpecialSelectorNumArgs(index));
            } else if (op <= 0x8F) {
                b.add(insnPC, pc, SEND, op & 0xF, 0);
            } else if (op <= 0x9F) {
                b.add(insnPC, pc, SEND, op & 0xF, 1);
            } else if (op <= 0xAF) {
                b.add(insnPC, pc, SEND, op & 0xF, 2);
            } else if (op <= 0xB7) {
                b.addJump(insnPC, pc, JUMP, pc + (op & 0x7) + 1);
            } else if (op <= 0xBF) {
                b.addJump(insnPC, pc, JUMP_TRUE, pc + (op & 0x7) + 1);
            } else if (op <= 0xC7) {
                b.addJump(insnPC, pc, JUMP_FALSE, pc + (op & 0x7) + 1);
            } else if (op <= 0xCF) {
                b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, op & 0x7, 1);
            } else if (op <= 0xD7) {
                b.add(insnPC, pc, STORE_TEMPORARY, op & 0x7, 1);
            } else if (op == 0xD8) {
                b.add(insnPC, pc, POP);
            } else if (op <= 0xDF) {
                b.add(insnPC, pc, UNKNOWN, op);
            } else if (op <= 0xF7) {
                final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                switch (op) {
                    case 0xE2 -> b.add(insnPC, pc, PUSH_RECEIVER_VARIABLE, byte1 + (extA << 8));
                    case 0xE3 -> b.add(insnPC, pc, PUSH_LITERAL_VARIABLE, byte1 + (extA << 8));
                    case 0xE4 -> b.add(insnPC, pc, PUSH_LITERAL_CONSTANT, byte1 + (extA << 8));
                    case 0xE5 -> b.add(insnPC, pc, PUSH_TEMPORARY, byte1);
                    case 0xE7 -> b.add(insnPC, pc, PUSH_NEW_ARRAY, byte1 & 127, byte1 > 127 ? 1 : 0);
                    case 0xE8 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, (long) (byte1 + (extB << 8)));
                    case 0xE9 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, CharacterObject.valueOf(byte1 + (extA << 8)));
                    case 0xEA -> b.add(insnPC, pc, SEND, (byte1 >> 3) + (extA << 5), (byte1 & 7) + (extB << 3));
                    case 0xEB -> {
                        final boolean isDirected = extB >= 64;
                        final int extBValue = isDirected ? extB & 63 : extB;
                        b.add(insnPC, pc, SEND_SUPER, (byte1 >> 3) + (extA << 5), (byte1 & 7) + (extBValue << 3), isDirected ? 1 : 0);
                    }
                    case 0xED -> b.addJump(insnPC, pc, JUMP, pc + byte1 + (extB << 8));
                    case 0xEE -> b.addJump(insnPC, pc, JUMP_TRUE, pc + byte1 + (extB << 8));
                    case 0xEF -> b.addJump(insnPC, pc, JUMP_FALSE, pc + byte1 + (extB << 8));
                    case 0xF0 -> b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, byte1 + (extA << 8), 1);
                    case 0xF1 -> b.add(insnPC, pc, STORE_LITERAL_VARIABLE, byte1 + (extA << 8), 1);
                    case 0xF2 -> b.add(insnPC, pc, STORE_TEMPORARY, byte1, 1);
                    case 0xF3 -> b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, byte1 + (extA << 8), 0);
                    case 0xF4 -> b.add(insnPC, pc, STORE_LITERAL_VARIABLE, byte1 + (extA << 8), 0);
                    case 0xF5 -> b.add(insnPC, pc, STORE_TEMPORARY, byte1, 0);
                    default -> b.add(insnPC, pc, UNKNOWN, op);
                }
            } else {
                final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                final int byte2 = Byte.toUnsignedInt(bc[pc++]);
                switch (op) {
                    case 0xF8 -> b.add(insnPC, pc, CALL_PRIMITIVE, byte1 + (byte2 << 8));
                    case 0xF9 -> {
                        final int flags = ((byte2 & 0x40) != 0 ? FULL_CLOSURE_IGNORE_OUTER_CONTEXT : 0) | ((byte2 & 0x80) != 0 ? FULL_CLOSURE_RECEIVER_ON_STACK : 0);
                        b.add(insnPC, pc, PUSH_FULL_CLOSURE, byte1 + (extA << 8), byte2 & 63, flags);
                    }
                    case 0xFA -> {
                        final int numArgs = (byte1 & 0x07) + (extA & 0x0F) * 8;
                        final int numCopied = (byte1 >> 3 & 0x7) + (extA >> 4) * 8;
                        final int blockSize = byte2 + (extB << 8);
                        /* The embedded block starts at nextPC - blockSize. */
                        pc += blockSize;
                        b.add(insnPC, pc, PUSH_CLOSURE, numArgs, numCopied, blockSize);
                    }
                    case 0xFB -> b.add(insnPC, pc, PUSH_REMOTE_TEMP, byte1, byte2);
                    case 0xFC -> b.add(insnPC, pc, STORE_REMOTE_TEMP, byte1, byte2, 0);
                    case 0xFD -> b.add(insnPC, pc, STORE_REMOTE_TEMP, byte1, byte2, 1);
                    default -> b.add(insnPC, pc, UNKNOWN, op);
                }
            }
        }
    }

    /*
     * V3PlusClosures
     */

    private static void decodeV3PlusClosures(final SqueakImageContext image, final byte[] bc, final int startPC, final int endPC, final Builder b) {
        int pc = startPC;
        while (pc < endPC) {
            final int insnPC = pc;
            final int op = Byte.toUnsignedInt(bc[pc++]);
            if (op <= 15) {
                b.add(insnPC, pc, PUSH_RECEIVER_VARIABLE, op & 0xF);
            } else if (op <= 31) {
                b.add(insnPC, pc, PUSH_TEMPORARY, op & 0xF);
            } else if (op <= 63) {
                b.add(insnPC, pc, PUSH_LITERAL_CONSTANT, op & 0x1F);
            } else if (op <= 95) {
                b.add(insnPC, pc, PUSH_LITERAL_VARIABLE, op & 0x1F);
            } else if (op <= 103) {
                b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, op & 0x7, 1);
            } else if (op <= 111) {
                b.add(insnPC, pc, STORE_TEMPORARY, op & 0x7, 1);
            } else if (op <= 127) {
                switch (op) {
                    case 112 -> b.add(insnPC, pc, PUSH_RECEIVER);
                    case 113 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, BooleanObject.TRUE);
                    case 114 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, BooleanObject.FALSE);
                    case 115 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, NilObject.SINGLETON);
                    case 116 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, -1L);
                    case 117 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, 0L);
                    case 118 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, 1L);
                    case 119 -> b.addConstant(insnPC, pc, PUSH_CONSTANT, 2L);
                    case 120 -> b.add(insnPC, pc, RETURN_RECEIVER);
                    case 121 -> b.addConstant(insnPC, pc, RETURN_CONSTANT, BooleanObject.TRUE);
                    case 122 -> b.addConstant(insnPC, pc, RETURN_CONSTANT, BooleanObject.FALSE);
                    case 123 -> b.addConstant(insnPC, pc, RETURN_CONSTANT, NilObject.SINGLETON);
                    case 124 -> b.add(insnPC, pc, RETURN_TOP);
                    case 125 -> b.add(insnPC, pc, BLOCK_RETURN_TOP);
                    default -> b.add(insnPC, pc, UNKNOWN, op);
                }
            } else if (op <= 143) {
                switch (op) {
                    case 128 -> {
                        final int descriptor = Byte.toUnsignedInt(bc[pc++]);
                        final int index = descriptor & 63;
                        switch (descriptor >> 6) {
                            case 0 -> b.add(insnPC, pc, PUSH_RECEIVER_VARIABLE, index);
                            case 1 -> b.add(insnPC, pc, PUSH_TEMPORARY, index);
                            case 2 -> b.add(insnPC, pc, PUSH_LITERAL_CONSTANT, index);
                            default -> b.add(insnPC, pc, PUSH_LITERAL_VARIABLE, index);
                        }
                    }
                    case 129, 130 -> {
                        final int descriptor = Byte.toUnsignedInt(bc[pc++]);
                        final int index = descriptor & 63;
                        final int pop = op == 130 ? 1 : 0;
                        switch (descriptor >> 6) {
                            case 0 -> b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, index, pop);
                            case 1 -> b.add(insnPC, pc, STORE_TEMPORARY, index, pop);
                            case 2 -> b.add(insnPC, pc, UNKNOWN, op);
                            default -> b.add(insnPC, pc, STORE_LITERAL_VARIABLE, index, pop);
                        }
                    }
                    case 131 -> {
                        final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                        b.add(insnPC, pc, SEND, byte1 & 0x1F, byte1 >> 5);
                    }
                    case 132 -> {
                        final int byte2 = Byte.toUnsignedInt(bc[pc++]);
                        final int byte3 = Byte.toUnsignedInt(bc[pc++]);
                        switch (byte2 >> 5) {
                            case 0 -> b.add(insnPC, pc, SEND, byte3, byte2 & 31);
                            case 1 -> b.add(insnPC, pc, SEND_SUPER, byte3, byte2 & 31, 0);
                            case 2 -> b.add(insnPC, pc, PUSH_RECEIVER_VARIABLE, byte3);
                            case 3 -> b.add(insnPC, pc, PUSH_LITERAL_CONSTANT, byte3);
                            case 4 -> b.add(insnPC, pc, PUSH_LITERAL_VARIABLE, byte3);
                            case 5 -> b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, byte3, 0);
                            case 6 -> b.add(insnPC, pc, STORE_RECEIVER_VARIABLE, byte3, 1);
                            default -> b.add(insnPC, pc, STORE_LITERAL_VARIABLE, byte3, 0);
                        }
                    }
                    case 133 -> {
                        final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                        b.add(insnPC, pc, SEND_SUPER, byte1 & 0x1F, byte1 >> 5, 0);
                    }
                    case 134 -> {
                        final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                        b.add(insnPC, pc, SEND, byte1 & 0x3F, byte1 >> 6);
                    }
                    case 135 -> b.add(insnPC, pc, POP);
                    case 136 -> b.add(insnPC, pc, DUP);
                    case 137 -> b.add(insnPC, pc, PUSH_ACTIVE_CONTEXT);
                    case 138 -> {
                        final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                        b.add(insnPC, pc, PUSH_NEW_ARRAY, byte1 & 127, byte1 > 127 ? 1 : 0);
                    }
                    case 139 -> {
                        final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                        final int byte2 = Byte.toUnsignedInt(bc[pc++]);
                        b.add(insnPC, pc, CALL_PRIMITIVE, byte1 + (byte2 << 8));
                    }
                    case 140, 141, 142 -> {
                        final int remoteTempIndex = Byte.toUnsignedInt(bc[pc++]);
                        final int tempVectorIndex = Byte.toUnsignedInt(bc[pc++]);
                        if (op == 140) {
                            b.add(insnPC, pc, PUSH_REMOTE_TEMP, remoteTempIndex, tempVectorIndex);
                        } else {
                            b.add(insnPC, pc, STORE_REMOTE_TEMP, remoteTempIndex, tempVectorIndex, op == 142 ? 1 : 0);
                        }
                    }
                    default -> { // 143
                        final int numArgsNumCopied = Byte.toUnsignedInt(bc[pc++]);
                        final int blockSize = Byte.toUnsignedInt(bc[pc++]) << 8 | Byte.toUnsignedInt(bc[pc++]);
                        /* The embedded block starts at nextPC - blockSize. */
                        pc += blockSize;
                        b.add(insnPC, pc, PUSH_CLOSURE, numArgsNumCopied & 0xF, numArgsNumCopied >> 4 & 0xF, blockSize);
                    }
                }
            } else if (op <= 151) {
                b.addJump(insnPC, pc, JUMP, pc + (op & 0x7) + 1);
            } else if (op <= 159) {
                b.addJump(insnPC, pc, JUMP_FALSE, pc + (op & 0x7) + 1);
            } else if (op <= 167) {
                final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                b.addJump(insnPC, pc, JUMP, pc + ((op & 7) - 4 << 8) + byte1);
            } else if (op <= 175) {
                final int byte1 = Byte.toUnsignedInt(bc[pc++]);
                b.addJump(insnPC, pc, op <= 171 ? JUMP_TRUE : JUMP_FALSE, pc + ((op & 3) << 8) + byte1);
            } else if (op <= 207) {
                final int index = op - 176;
                b.add(insnPC, pc, SEND_SPECIAL, index, image.getSpecialSelectorNumArgs(index));
            } else if (op <= 223) {
                b.add(insnPC, pc, SEND, op & 0xF, 0);
            } else if (op <= 239) {
                b.add(insnPC, pc, SEND, op & 0xF, 1);
            } else {
                b.add(insnPC, pc, SEND, op & 0xF, 2);
            }
        }
    }
}

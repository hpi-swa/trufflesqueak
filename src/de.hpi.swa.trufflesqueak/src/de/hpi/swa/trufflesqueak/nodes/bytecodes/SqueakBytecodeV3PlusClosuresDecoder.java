/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public final class SqueakBytecodeV3PlusClosuresDecoder extends AbstractSqueakBytecodeDecoder {
    public static final SqueakBytecodeV3PlusClosuresDecoder SINGLETON = new SqueakBytecodeV3PlusClosuresDecoder();

    private SqueakBytecodeV3PlusClosuresDecoder() {
    }

    @Override
    public boolean hasStoreIntoTemp1AfterCallPrimitive(final CompiledCodeObject code) {
        final byte[] bytes = code.getBytes();
        return Byte.toUnsignedInt(bytes[3]) == 129 && (Byte.toUnsignedInt(bytes[4]) >> 6 & 3) == 1;
    }

    @Override
    public AbstractBytecodeNode decodeBytecode(final VirtualFrame frame, final CompiledCodeObject code, final AbstractBytecodeNode[] bytecodeNodes, final int index, final int sp) {
        CompilerAsserts.neverPartOfCompilation();
        final byte[] bytecode = code.getBytes();
        final int b = Byte.toUnsignedInt(bytecode[index]);
        return switch (b) {
            case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F //
                -> PushBytecodes.PushReceiverVariableNode.create(frame, index + 1, sp + 1, b & 15);
            case 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F //
                -> new PushBytecodes.PushTemporaryLocationNode(frame, index + 1, sp + 1, b & 15);
            case 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F //
                -> new PushBytecodes.PushLiteralConstantNode(frame, code, index + 1, sp + 1, b & 31);
            case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F //
                -> PushBytecodes.PushLiteralVariableNode.create(frame, code, index + 1, sp + 1, b & 31);
            case 0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67 -> new StoreBytecodes.PopIntoReceiverVariableNode(frame, index + 1, sp - 1, b & 7);
            case 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F -> new StoreBytecodes.PopIntoTemporaryLocationNode(frame, index + 1, sp - 1, b & 7);
            case 0x70 -> new PushBytecodes.PushReceiverNode(frame, index + 1, sp + 1);
            case 0x71 -> new PushBytecodes.PushConstantTrueNode(frame, index + 1, sp + 1);
            case 0x72 -> new PushBytecodes.PushConstantFalseNode(frame, index + 1, sp + 1);
            case 0x73 -> new PushBytecodes.PushConstantNilNode(frame, index + 1, sp + 1);
            case 0x74 -> new PushBytecodes.PushConstantMinusOneNode(frame, index + 1, sp + 1);
            case 0x75 -> new PushBytecodes.PushConstantZeroNode(frame, index + 1, sp + 1);
            case 0x76 -> new PushBytecodes.PushConstantOneNode(frame, index + 1, sp + 1);
            case 0x77 -> new PushBytecodes.PushConstantTwoNode(frame, index + 1, sp + 1);
            case 0x78 -> new ReturnBytecodes.ReturnReceiverNode(frame, sp);
            case 0x79 -> new ReturnBytecodes.ReturnConstantTrueNode(frame, sp);
            case 0x7A -> new ReturnBytecodes.ReturnConstantFalseNode(frame, sp);
            case 0x7B -> new ReturnBytecodes.ReturnConstantNilNode(frame, sp);
            case 0x7C -> new ReturnBytecodes.ReturnTopFromMethodNode(frame, sp);
            case 0x7D -> new ReturnBytecodes.ReturnTopFromBlockNode(frame, sp);
            case 0x7E, 0x7F -> new MiscellaneousBytecodes.UnknownBytecodeNode(index + 1, sp, b);
            case 0x80 -> MiscellaneousBytecodes.ExtendedBytecodes.createPush(frame, code, index + 2, sp, bytecode[index + 1]);
            case 0x81 -> MiscellaneousBytecodes.ExtendedBytecodes.createStoreInto(frame, code, index + 2, sp, bytecode[index + 1]);
            case 0x82 -> MiscellaneousBytecodes.ExtendedBytecodes.createPopInto(frame, code, index + 2, sp, bytecode[index + 1]);
            case 0x83 -> {
                final byte byte1 = bytecode[index + 1];
                final NativeObject selector = (NativeObject) code.getLiteral(byte1 & 31);
                final int numArgs = Byte.toUnsignedInt(byte1) >> 5;
                yield new SendBytecodes.SelfSendNode(frame, index + 2, sp, selector, numArgs);
            }
            case 0x84 -> MiscellaneousBytecodes.DoubleExtendedDoAnythingNode.create(frame, code, index + 3, sp, bytecode[index + 1], bytecode[index + 2]);
            case 0x85 -> {
                final byte byte1 = bytecode[index + 1];
                final int literalIndex = byte1 & 31;
                final int numArgs = Byte.toUnsignedInt(byte1) >> 5;
                yield new SendBytecodes.SuperSendNode(frame, code, index + 2, sp, literalIndex, numArgs);
            }
            case 0x86 -> {
                final byte byte1 = bytecode[index + 1];
                final NativeObject selector = (NativeObject) code.getLiteral(byte1 & 63);
                final int numArgs = Byte.toUnsignedInt(byte1) >> 6;
                yield new SendBytecodes.SelfSendNode(frame, index + 2, sp, selector, numArgs);
            }
            case 0x87 -> new MiscellaneousBytecodes.PopNode(frame, index + 1, sp - 1);
            case 0x88 -> new MiscellaneousBytecodes.DupNode(frame, index + 1, sp + 1);
            case 0x89 -> new PushBytecodes.PushActiveContextNode(frame, index + 1, sp + 1);
            case 0x8A -> PushBytecodes.PushNewArrayNode.create(frame, index + 2, sp + 1, bytecode[index + 1]);
            case 0x8B -> MiscellaneousBytecodes.CallPrimitiveNode.create(frame, code, index + 3, sp, bytecode[index + 1], bytecode[index + 2]);
            case 0x8C -> PushBytecodes.PushRemoteTempNode.create(frame, index + 3, sp + 1, bytecode[index + 1], bytecode[index + 2]);
            case 0x8D -> new StoreBytecodes.StoreIntoRemoteTempNode(frame, index + 3, sp, bytecode[index + 1], bytecode[index + 2]);
            case 0x8E -> new StoreBytecodes.PopIntoRemoteTempNode(frame, index + 3, sp - 1, bytecode[index + 1], bytecode[index + 2]);
            case 0x8F -> PushBytecodes.PushClosureNode.create(frame, code, index + 4, sp + 1, bytecode[index + 1], bytecode[index + 2], bytecode[index + 3]);
            case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97 -> JumpBytecodes.createUnconditionalShortJump(bytecodeNodes, index, sp, b);
            case 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F -> JumpBytecodes.ConditionalJumpOnFalseNode.createShort(frame, index + 1, sp - 1, b);
            case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7 -> JumpBytecodes.createUnconditionalLongJump(bytecodeNodes, index, sp, b, bytecode[index + 1]);
            case 0xA8, 0xA9, 0xAA, 0xAB -> JumpBytecodes.ConditionalJumpOnTrueNode.createLong(frame, index + 2, sp - 1, b, bytecode[index + 1]);
            case 0xAC, 0xAD, 0xAE, 0xAF -> JumpBytecodes.ConditionalJumpOnFalseNode.createLong(frame, index + 2, sp - 1, b, bytecode[index + 1]);
            case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF  //
                -> SendBytecodes.SendSpecialNode.create(frame, index + 1, sp, b - 176);
            case 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF //
                -> new SendBytecodes.SelfSendNode(frame, index + 1, sp, (NativeObject) code.getLiteral(b & 0xF), 0);
            case 0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF //
                -> new SendBytecodes.SelfSendNode(frame, index + 1, sp, (NativeObject) code.getLiteral(b & 0xF), 1);
            case 0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF //
                -> new SendBytecodes.SelfSendNode(frame, index + 1, sp, (NativeObject) code.getLiteral(b & 0xF), 2);
            default -> throw SqueakException.create("Not a bytecode:", b);
        };
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
                final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                yield switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
                    case 0 -> "pushRcvr: " + variableIndex;
                    case 1 -> "pushTemp: " + variableIndex;
                    case 2 -> "pushConstant: " + code.getLiteral(variableIndex);
                    case 3 -> "pushLit: " + variableIndex;
                    default -> throw SqueakException.create("unexpected type for ExtendedPush");
                };
            }
            case 129 -> {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                yield switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
                    case 0 -> "storeIntoRcvr: " + variableIndex;
                    case 1 -> "storeIntoTemp: " + variableIndex;
                    case 2 -> "unknown: " + b1;
                    case 3 -> "storeIntoLit: " + variableIndex;
                    default -> throw SqueakException.create("illegal ExtendedStore bytecode");
                };
            }
            case 130 -> {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                yield switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
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
        final int b = Byte.toUnsignedInt(code.getBytes()[index]);
        return switch (b) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, //
                43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, //
                81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, //
                115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127 -> 1;
            case 128, 129, 130, 131 -> 2;
            case 132 -> 3;
            case 133, 134 -> 2;
            case 135, 136, 137 -> 1;
            case 138 -> 2;
            case 139, 140, 141, 142 -> 3;
            case 143 -> 4;
            case 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159 -> 1;
            case 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175 -> 2;
            case 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, //
                208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, //
                238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255 -> 1;
            default -> throw SqueakException.create("Unknown bytecode:", b);
        };
    }

    @Override
    public int determineMaxNumStackSlots(final CompiledCodeObject code) {
        return code.getSqueakContextSize(); // FIXME: Implement for v3
    }
}

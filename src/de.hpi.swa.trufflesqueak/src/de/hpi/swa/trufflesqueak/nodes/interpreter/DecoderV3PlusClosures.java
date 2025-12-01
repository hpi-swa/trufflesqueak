/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public final class DecoderV3PlusClosures extends AbstractDecoder {
    public static final DecoderV3PlusClosures SINGLETON = new DecoderV3PlusClosures();

    private DecoderV3PlusClosures() {
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

/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
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
    public int findLineNumber(final CompiledCodeObject code, final int targetIndex) {
        final int trailerPosition = trailerPosition(code);
        assert 0 <= targetIndex && targetIndex <= trailerPosition;
        int index = 0;
        int lineNumber = 1;
        while (index != targetIndex) {
            index += decodeNumBytes(code, index);
            lineNumber++;
        }
        assert lineNumber <= trailerPosition;
        return lineNumber;
    }

    @Override
    public int trailerPosition(final CompiledCodeObject code) {
        return code.isCompiledBlock() ? code.getBytes().length : trailerPosition(code.getBytes());
    }

    private static int trailerPosition(final byte[] bytecode) {
        final int bytecodeLength = bytecode.length;
        final int flagByte = Byte.toUnsignedInt(bytecode[bytecodeLength - 1]);
        final int index = (flagByte >> 2) + 1;
        switch (index) {
            case 1: // #decodeNoTrailer
            case 5: // #decodeSourceBySelector
                return bytecodeLength - 1;
            case 2: // #decodeClearedTrailer
            case 3: // #decodeTempsNamesQCompress
            case 4: // #decodeTempsNamesZip
            case 6: // #decodeSourceByStringIdentifier
            case 7: // #decodeEmbeddedSourceQCompress
            case 8: // #decodeEmbeddedSourceZip
                return decodeLengthField(bytecode, bytecodeLength, flagByte);
            case 9: // decodeVarLengthSourcePointer
                int pos = bytecodeLength - 2;
                while (bytecode[pos] < 0) {
                    pos--;
                }
                return pos;
            case 64: // decodeSourcePointer
                return bytecodeLength - 4;
            default:
                throw SqueakException.create("Undefined method encoding (see CompiledMethodTrailer).");
        }
    }

    private static int decodeLengthField(final byte[] bytecode, final int bytecodeLength, final int flagByte) {
        final int numBytes = (flagByte & 3) + 1;
        int length = 0;
        final int firstLengthValueIndex = bytecodeLength - 2;
        for (int i = 0; i < numBytes; i++) {
            length = (length << 8) + Byte.toUnsignedInt(bytecode[firstLengthValueIndex - i]);
        }
        return bytecodeLength - (1 + numBytes + length);
    }

    @Override
    public boolean hasStoreIntoTemp1AfterCallPrimitive(final CompiledCodeObject code) {
        final byte[] bytes = code.getBytes();
        return Byte.toUnsignedInt(bytes[3]) == 129 && (Byte.toUnsignedInt(bytes[4]) >> 6 & 3) == 1;
    }

    @Override
    public AbstractBytecodeNode decodeBytecode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
        CompilerAsserts.neverPartOfCompilation();
        final byte[] bytecode = code.getBytes();
        final int b = Byte.toUnsignedInt(bytecode[index]);
        //@formatter:off
        switch (b) {
            case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E: case 0x0F:
                return PushBytecodes.PushReceiverVariableNode.create(code, index, 1, b & 15);
            case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
            case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
                return new PushBytecodes.PushTemporaryLocationNode(code, index, 1, b & 15);
            case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
            case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C: case 0x2D: case 0x2E: case 0x2F:
            case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: case 0x36: case 0x37:
            case 0x38: case 0x39: case 0x3A: case 0x3B: case 0x3C: case 0x3D: case 0x3E: case 0x3F:
                return new PushBytecodes.PushLiteralConstantNode(code, index, 1, b & 31);
            case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x46: case 0x47:
            case 0x48: case 0x49: case 0x4A: case 0x4B: case 0x4C: case 0x4D: case 0x4E: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53: case 0x54: case 0x55: case 0x56: case 0x57:
            case 0x58: case 0x59: case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                return  PushBytecodes.PushLiteralVariableNode.create(code, index, 1, b & 31);
            case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x66: case 0x67:
                return new StoreBytecodes.PopIntoReceiverVariableNode(code, index, 1, b & 7);
            case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6E: case 0x6F:
                return new StoreBytecodes.PopIntoTemporaryLocationNode(code, index, 1, b & 7);
            case 0x70:
                return PushBytecodes.PushReceiverNode.create(code, index);
            case 0x71:
                return new PushBytecodes.PushConstantTrueNode(code, index);
            case 0x72:
                return new PushBytecodes.PushConstantFalseNode(code, index);
            case 0x73:
                return new PushBytecodes.PushConstantNilNode(code, index);
            case 0x74:
                return new PushBytecodes.PushConstantMinusOneNode(code, index);
            case 0x75:
                return new PushBytecodes.PushConstantZeroNode(code, index);
            case 0x76:
                return new PushBytecodes.PushConstantOneNode(code, index);
            case 0x77:
                return new PushBytecodes.PushConstantTwoNode(code, index);
            case 0x78:
                return new ReturnBytecodes.ReturnReceiverNode(frame, code, index);
            case 0x79:
                return new ReturnBytecodes.ReturnConstantTrueNode(frame, code, index);
            case 0x7A:
                return new ReturnBytecodes.ReturnConstantFalseNode(frame, code, index);
            case 0x7B:
                return new ReturnBytecodes.ReturnConstantNilNode(frame, code, index);
            case 0x7C:
                return new ReturnBytecodes.ReturnTopFromMethodNode(frame, code, index);
            case 0x7D:
                return new ReturnBytecodes.ReturnTopFromBlockNode(code, index);
            case 0x7E:
            case 0x7F:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 0x80:
                return MiscellaneousBytecodes.ExtendedBytecodes.createPush(code, index, 2, bytecode[index + 1]);
            case 0x81:
                return MiscellaneousBytecodes.ExtendedBytecodes.createStoreInto(code, index, 2, bytecode[index + 1]);
            case 0x82:
                return MiscellaneousBytecodes.ExtendedBytecodes.createPopInto(code, index, 2, bytecode[index + 1]);
            case 0x83: {
                final byte byte1 = bytecode[index + 1];
                final NativeObject selector = (NativeObject) code.getLiteral(byte1 & 31);
                final int numArgs = Byte.toUnsignedInt(byte1) >> 5;
                return SendBytecodes.SelfSendNode.create(code, index, 2, selector, numArgs);
            }
            case 0x84:
                return MiscellaneousBytecodes.DoubleExtendedDoAnythingNode.create(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 0x85:
                return new SendBytecodes.SuperSendNode(code, index, 2, bytecode[index + 1]);
            case 0x86: {
                final byte byte1 = bytecode[index + 1];
                final NativeObject selector = (NativeObject) code.getLiteral(byte1 & 63);
                final int numArgs = Byte.toUnsignedInt(byte1) >> 6;
                return SendBytecodes.SelfSendNode.create(code, index, 2, selector, numArgs);
            }
            case 0x87:
                return new MiscellaneousBytecodes.PopNode(code, index);
            case 0x88:
                return new MiscellaneousBytecodes.DupNode(code, index);
            case 0x89:
                return new PushBytecodes.PushActiveContextNode(code, index);
            case 0x8A:
                return PushBytecodes.PushNewArrayNode.create(code, index, 2, bytecode[index + 1]);
            case 0x8B:
                return MiscellaneousBytecodes.CallPrimitiveNode.create(code, index, bytecode[index + 1], bytecode[index + 2]);
            case 0x8C:
                return new PushBytecodes.PushRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 0x8D:
                return new StoreBytecodes.StoreIntoRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 0x8E:
                return new StoreBytecodes.PopIntoRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 0x8F:
                return PushBytecodes.PushClosureNode.create(code, index, bytecode[index + 1], bytecode[index + 2], bytecode[index + 3]);
            case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
                return JumpBytecodes.UnconditionalJumpNode.createShort(code, index, b);
            case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createShort(code, index, b);
            case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
                return JumpBytecodes.UnconditionalJumpNode.createLong(code, index, b, bytecode[index + 1]);
            case 0xA8: case 0xA9: case 0xAA: case 0xAB:
                return JumpBytecodes.ConditionalJumpOnTrueNode.createLong(code, index, b, bytecode[index + 1]);
            case 0xAC: case 0xAD: case 0xAE: case 0xAF:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createLong(code, index, b, bytecode[index + 1]);
            case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6: case 0xB7:
            case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case 0xBF:
            case 0xC0: case 0xC1: case 0xC2: case 0xC3: case 0xC4: case 0xC5: case 0xC6: case 0xC7:
            case 0xC8: case 0xC9: case 0xCA: case 0xCB: case 0xCC: case 0xCD: case 0xCE: case 0xCF:
                return SendBytecodes.AbstractSendSpecialSelectorQuickNode.create(frame, code, index, b - 176);
            case 0xD0: case 0xD1: case 0xD2: case 0xD3: case 0xD4: case 0xD5: case 0xD6: case 0xD7:
            case 0xD8: case 0xD9: case 0xDA: case 0xDB: case 0xDC: case 0xDD: case 0xDE: case 0xDF:
                return SendBytecodes.SelfSendNode.create(code, index, 1, (NativeObject) code.getLiteral(b & 0xF), 0);
            case 0xE0: case 0xE1: case 0xE2: case 0xE3: case 0xE4: case 0xE5: case 0xE6: case 0xE7:
            case 0xE8: case 0xE9: case 0xEA: case 0xEB: case 0xEC: case 0xED: case 0xEE: case 0xEF:
                return SendBytecodes.SelfSendNode.create(code, index, 1, (NativeObject) code.getLiteral(b & 0xF), 1);
            case 0xF0: case 0xF1: case 0xF2: case 0xF3: case 0xF4: case 0xF5: case 0xF6: case 0xF7:
            case 0xF8: case 0xF9: case 0xFA: case 0xFB: case 0xFC: case 0xFD: case 0xFE: case 0xFF:
                return SendBytecodes.SelfSendNode.create(code, index, 1, (NativeObject) code.getLiteral(b & 0xF), 2);
            default:
                throw SqueakException.create("Not a bytecode:", b);
        }
        //@formatter:on
    }

    @Override
    public String decodeToString(final CompiledCodeObject code) {
        CompilerAsserts.neverPartOfCompilation();
        final StringBuilder sb = new StringBuilder();
        final int trailerPosition = trailerPosition(code);
        int bytecodeIndex = 0;
        int lineIndex = 1;
        int indent = 0;
        final byte[] bytes = code.getBytes();
        while (bytecodeIndex < trailerPosition) {
            final int currentByte = Byte.toUnsignedInt(bytes[bytecodeIndex]);
            sb.append(lineIndex);
            for (int j = 0; j < 1 + indent; j++) {
                sb.append(' ');
            }
            final int numBytecodes = decodeNumBytes(code, bytecodeIndex);
            sb.append('<');
            for (int j = bytecodeIndex; j < bytecodeIndex + numBytecodes; j++) {
                if (j > bytecodeIndex) {
                    sb.append(' ');
                }
                if (j < bytes.length) {
                    sb.append(String.format("%02X", bytes[j]));
                }
            }
            sb.append("> ");
            sb.append(decodeBytecodeToString(code, currentByte, bytecodeIndex));
            if (currentByte == 143) {
                indent++; // increment indent on push closure
            } else if (currentByte == 125) {
                indent--; // decrement indent on block return
            }
            lineIndex++;
            bytecodeIndex += numBytecodes;
            if (bytecodeIndex < trailerPosition) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String decodeBytecodeToString(final CompiledCodeObject code, final int b0, final int index) {
        final byte[] bytecode = code.getBytes();
        //@formatter:off
        switch (b0) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                return "pushRcvr: " + (b0 & 15);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return "pushTemp: " + (b0 & 15);
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
                return "pushConstant: " + code.getLiteral(b0 & 31);
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
            case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79:
            case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
                return "pushLitVar: " + code.getLiteral(b0 & 31);
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
                return "popIntoRcvr: " + (b0 & 7);
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
                return "popIntoTemp: " + (b0 & 7);
            case 112:
                return "self";
            case 113:
                return "pushConstant: true";
            case 114:
                return "pushConstant: false";
            case 115:
                return "pushConstant: nil";
            case 116:
                return "pushConstant: -1";
            case 117:
                return "pushConstant: 0";
            case 118:
                return "pushConstant: 1";
            case 119:
                return "pushConstant: 2";
            case 120:
                return "returnSelf";
            case 121:
                return "return: true";
            case 122:
                return "return: false";
            case 123:
                return "return: nil";
            case 124:
                return "returnTop";
            case 125:
                return "blockReturn";
            case 126:
            case 127:
                return "unknown: " + b0;
            case 128: {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
                    case 0:
                        return "pushRcvr: " + variableIndex;
                    case 1:
                        return "pushTemp: " + variableIndex;
                    case 2:
                        return "pushConstant: " + code.getLiteral(variableIndex);
                    case 3:
                        return "pushLit: " + variableIndex;
                    default:
                        throw SqueakException.create("unexpected type for ExtendedPush");
                }
            }
            case 129: {
                final byte b1 = bytecode[index + 1];
                 final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
                    case 0:
                        return "storeIntoRcvr: " + variableIndex;
                    case 1:
                        return "storeIntoTemp: " + variableIndex;
                    case 2:
                        return "unknown: " + b1;
                    case 3:
                        return "storeIntoLit: " + variableIndex;
                    default:
                        throw SqueakException.create("illegal ExtendedStore bytecode");
                }
            }
            case 130: {
                final byte b1 = bytecode[index + 1];
                final int variableIndex = MiscellaneousBytecodes.ExtendedBytecodes.variableIndex(b1);
                switch (MiscellaneousBytecodes.ExtendedBytecodes.variableType(b1)) {
                    case 0:
                        return "popIntoRcvr: " + variableIndex;
                    case 1:
                        return "popIntoTemp: " + variableIndex;
                    case 2:
                        return "unknown: " + b1;
                    case 3:
                        return "popIntoLit: " + variableIndex;
                    default:
                        throw SqueakException.create("illegal ExtendedStore bytecode");
                }
            }
            case 131:
                return "send: " +  code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 31);
            case 132: {
                final int b1 = Byte.toUnsignedInt(bytecode[index + 1]);
                final int b2 = Byte.toUnsignedInt(bytecode[index + 2]);
                switch (b1 >> 5) {
                    case 0:
                        return "send: " + code.getLiteral(b2);
                    case 1:
                        return "sendSuper: " + code.getLiteral(b2 & 31);
                    case 2:
                        return "pushRcvr: " + b2;
                    case 3:
                        return "pushConstant: " + code.getLiteral(b2);
                    case 4:
                        return "pushLit: " + b2;
                    case 5:
                        return "storeIntoRcvr: " + b2;
                    case 6:
                        return "popIntoRcvr: " + b2;
                    case 7:
                        return "storeIntoLit: " + b2;
                    default:
                        return "unknown: " + b1;
                }
            }
            case 133:
                return "sendSuper: " + code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 31);
            case 134:
                return "send: " + code.getLiteral(Byte.toUnsignedInt(bytecode[index + 1]) & 63);
            case 135:
                return "pop";
            case 136:
                return "dup";
            case 137:
                return "pushThisContext:";
            case 138:
                return "push: (Array new: " + (Byte.toUnsignedInt(bytecode[index + 1]) & 127) + ")";
            case 139:
                return "callPrimitive: " + (Byte.toUnsignedInt(bytecode[index + 1]) + (Byte.toUnsignedInt(bytecode[index + 2]) << 8));
            case 140:
                return "pushTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 141:
                return "storeIntoTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 142:
                return "popIntoTemp: " + Byte.toUnsignedInt(bytecode[index + 1]) + " inVectorAt: " + Byte.toUnsignedInt(bytecode[index + 2]);
            case 143: {
                final byte b1 = bytecode[index + 1];
                final int start = index + PushBytecodes.PushClosureNode.NUM_BYTECODES;
                final int end = start + (Byte.toUnsignedInt(bytecode[index + 2]) << 8 | Byte.toUnsignedInt(bytecode[index + 3]));
                return "closureNumCopied: " + (b1 >> 4 & 0xF) + " numArgs: " + (b1 & 0xF) + " bytes " + start + " to " + end;
            }
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
                return "jumpTo: " + ((b0 & 7) + 1);
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
                return "jumpFalse: " + ((b0 & 7) + 1);
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
                return "jumpTo: " + (((b0 & 7) - 4 << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 168: case 169: case 170: case 171:
                return "jumpTrue: " + (((b0 & 3) << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 172: case 173: case 174: case 175:
                return "jumpFalse: " + (((b0 & 3) << 8) + Byte.toUnsignedInt(bytecode[index + 1]));
            case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183:
            case 184: case 185: case 186: case 187: case 188: case 189: case 190: case 191:
            case 192: case 193: case 194: case 195: case 196: case 197: case 198: case 199:
            case 200: case 201: case 202: case 203: case 204: case 205: case 206: case 207:
                return "send: " + code.getSqueakClass().getImage().getSpecialSelector(b0 - 176).asStringUnsafe();
            case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215:
            case 216: case 217: case 218: case 219: case 220: case 221: case 222: case 223:
            case 224: case 225: case 226: case 227: case 228: case 229: case 230: case 231:
            case 232: case 233: case 234: case 235: case 236: case 237: case 238: case 239:
            case 240: case 241: case 242: case 243: case 244: case 245: case 246: case 247:
            case 248: case 249: case 250: case 251: case 252: case 253: case 254: case 255:
                return "send: " + code.getLiteral(b0 & 0xF);
            default:
                throw SqueakException.create("Unknown bytecode:", b0);
        }
        //@formatter:on
    }

    @Override
    public int pcPreviousTo(final CompiledCodeObject code, final int pc) {
        final int initialPC = code.getInitialPC();
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

    private static int decodeNumBytes(final CompiledCodeObject code, final int index) {
        final int b = Byte.toUnsignedInt(code.getBytes()[index]);
        //@formatter:off
        switch (b) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
            case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79:
            case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
            case 112: case 113: case 114: case 115: case 116: case 117: case 118: case 119:
            case 120: case 121: case 122: case 123: case 124: case 125: case 126: case 127:
                return 1;
            case 128: case 129: case 130: case 131:
                return 2;
            case 132:
                return 3;
            case 133:
            case 134:
                return 2;
            case 135: case 136: case 137:
                return 1;
            case 138:
                return 2;
            case 139: case 140: case 141: case 142:
                return 3;
            case 143:
                return PushBytecodes.PushClosureNode.NUM_BYTECODES;
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
                return 1;
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
            case 168: case 169: case 170: case 171: case 172: case 173: case 174: case 175:
                return 2;
            case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183:
            case 184: case 185: case 186: case 187: case 188: case 189: case 190: case 191:
            case 192: case 193: case 194: case 195: case 196: case 197: case 198: case 199:
            case 200: case 201: case 202: case 203: case 204: case 205: case 206: case 207:
            case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215:
            case 216: case 217: case 218: case 219: case 220: case 221: case 222: case 223:
            case 224: case 225: case 226: case 227: case 228: case 229: case 230: case 231:
            case 232: case 233: case 234: case 235: case 236: case 237: case 238: case 239:
            case 240: case 241: case 242: case 243: case 244: case 245: case 246: case 247:
            case 248: case 249: case 250: case 251: case 252: case 253: case 254: case 255:
                return 1;
            default:
                throw SqueakException.create("Unknown bytecode:", b);
        }
        //@formatter:on
    }
}

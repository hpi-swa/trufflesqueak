/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public final class SqueakBytecodeV3PlusClosuresDecoder {
    private SqueakBytecodeV3PlusClosuresDecoder() {
    }

    public static AbstractBytecodeNode[] decode(final CompiledCodeObject code) {
        final int trailerPosition = trailerPosition(code);
        final AbstractBytecodeNode[] nodes = new AbstractBytecodeNode[trailerPosition];
        int index = 0;
        while (index < trailerPosition) {
            final AbstractBytecodeNode bytecodeNode = decodeBytecode(code, index);
            nodes[index] = bytecodeNode;
            index = bytecodeNode.getSuccessorIndex();
        }
        return nodes;
    }

    public static int findLineNumber(final CompiledCodeObject code, final int targetIndex) {
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

    public static int trailerPosition(final CompiledCodeObject code) {
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

    public static AbstractBytecodeNode decodeBytecode(final CompiledCodeObject code, final int index) {
        CompilerAsserts.neverPartOfCompilation();
        final byte[] bytecode = code.getBytes();
        final int b = Byte.toUnsignedInt(bytecode[index]);
        //@formatter:off
        switch (b) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                return PushBytecodes.PushReceiverVariableNode.create(code, index, 1, b & 15);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return new PushBytecodes.PushTemporaryLocationNode(code, index, 1, b & 15);
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
                return new PushBytecodes.PushLiteralConstantNode(code, index, 1, b & 31);
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
            case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79:
            case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
                return new PushBytecodes.PushLiteralVariableNode(code, index, 1, b & 31);
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
                return new StoreBytecodes.PopIntoReceiverVariableNode(code, index, 1, b & 7);
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
                return new StoreBytecodes.PopIntoTemporaryLocationNode(code, index, 1, b & 7);
            case 112:
                return PushBytecodes.PushReceiverNode.create(code, index);
            case 113:
                return new PushBytecodes.PushConstantTrueNode(code, index);
            case 114:
                return new PushBytecodes.PushConstantFalseNode(code, index);
            case 115:
                return new PushBytecodes.PushConstantNilNode(code, index);
            case 116:
                return new PushBytecodes.PushConstantMinusOneNode(code, index);
            case 117:
                return new PushBytecodes.PushConstantZeroNode(code, index);
            case 118:
                return new PushBytecodes.PushConstantOneNode(code, index);
            case 119:
                return new PushBytecodes.PushConstantTwoNode(code, index);
            case 120:
                return ReturnBytecodes.ReturnReceiverNode.create(code, index);
            case 121:
                return ReturnBytecodes.ReturnConstantTrueNode.create(code, index);
            case 122:
                return ReturnBytecodes.ReturnConstantFalseNode.create(code, index);
            case 123:
                return ReturnBytecodes.ReturnConstantNilNode.create(code, index);
            case 124:
                return ReturnBytecodes.ReturnTopFromMethodNode.create(code, index);
            case 125:
                return ReturnBytecodes.ReturnTopFromBlockNode.create(code, index);
            case 126:
            case 127:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 128:
                return MiscellaneousBytecodes.ExtendedBytecodes.createPush(code, index, 2, bytecode[index + 1]);
            case 129:
                return MiscellaneousBytecodes.ExtendedBytecodes.createStoreInto(code, index, 2, bytecode[index + 1]);
            case 130:
                return MiscellaneousBytecodes.ExtendedBytecodes.createPopInto(code, index, 2, bytecode[index + 1]);
            case 131:
                return new SendBytecodes.SingleExtendedSendNode(code, index, 2, bytecode[index + 1]);
            case 132:
                return MiscellaneousBytecodes.DoubleExtendedDoAnythingNode.create(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 133:
                return new SendBytecodes.SingleExtendedSuperNode(code, index, 2, bytecode[index + 1]);
            case 134:
                return new SendBytecodes.SecondExtendedSendNode(code, index, 2, bytecode[index + 1]);
            case 135:
                return new MiscellaneousBytecodes.PopNode(code, index);
            case 136:
                return new MiscellaneousBytecodes.DupNode(code, index);
            case 137:
                return new PushBytecodes.PushActiveContextNode(code, index);
            case 138:
                return PushBytecodes.PushNewArrayNode.create(code, index, 2, bytecode[index + 1]);
            case 139:
                return MiscellaneousBytecodes.CallPrimitiveNode.create(code, index, bytecode[index + 1], bytecode[index + 2]);
            case 140:
                return new PushBytecodes.PushRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 141:
                return new StoreBytecodes.StoreIntoRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 142:
                return new StoreBytecodes.PopIntoRemoteTempNode(code, index, 3, bytecode[index + 1], bytecode[index + 2]);
            case 143:
                return PushBytecodes.PushClosureNode.create(code, index, 4, bytecode[index + 1], bytecode[index + 2], bytecode[index + 3]);
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
                return JumpBytecodes.UnconditionalJumpNode.createShort(code, index, b);
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createShort(code, index, b);
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
                return JumpBytecodes.UnconditionalJumpNode.createLong(code, index, b, bytecode[index + 1]);
            case 168: case 169: case 170: case 171:
                return JumpBytecodes.ConditionalJumpOnTrueNode.createLong(code, index, b, bytecode[index + 1]);
            case 172: case 173: case 174: case 175:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createLong(code, index, b, bytecode[index + 1]);
            case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183:
            case 184: case 185: case 186: case 187: case 188: case 189: case 190: case 191:
            case 192: case 193: case 194: case 195: case 196: case 197: case 198: case 199:
            case 200: case 201: case 202: case 203: case 204: case 205: case 206: case 207:
                return SendBytecodes.SendSpecialSelectorNode.create(code, index, b - 176);
            case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215:
            case 216: case 217: case 218: case 219: case 220: case 221: case 222: case 223:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 0);
            case 224: case 225: case 226: case 227: case 228: case 229: case 230: case 231:
            case 232: case 233: case 234: case 235: case 236: case 237: case 238: case 239:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 1);
            case 240: case 241: case 242: case 243: case 244: case 245: case 246: case 247:
            case 248: case 249: case 250: case 251: case 252: case 253: case 254: case 255:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 2);
            default:
                throw SqueakException.create("Unknown bytecode:", b);
        }
        //@formatter:on
    }

    public static String decodeToString(final CompiledCodeObject code) {
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

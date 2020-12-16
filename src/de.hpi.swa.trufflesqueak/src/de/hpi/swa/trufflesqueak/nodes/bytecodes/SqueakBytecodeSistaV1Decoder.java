/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

public final class SqueakBytecodeSistaV1Decoder extends AbstractSqueakBytecodeDecoder {
    public static final SqueakBytecodeSistaV1Decoder SINGLETON = new SqueakBytecodeSistaV1Decoder();

    private SqueakBytecodeSistaV1Decoder() {
    }

    @Override
    public AbstractBytecodeNode[] decode(final CompiledCodeObject code) {
        final int trailerPosition = trailerPosition(code);
        final AbstractBytecodeNode[] nodes = new AbstractBytecodeNode[trailerPosition];
        int index = 0;
        while (index < trailerPosition) {
            final AbstractBytecodeNode bytecodeNode = decodeBytecode(code, index);
            nodes[index] = bytecodeNode;
            index += decodeNumBytes(code, index);
        }
        return nodes;
    }

    @Override
    public int findLineNumber(final CompiledCodeObject code, final int targetIndex) {
        final int trailerPosition = trailerPosition(code);
        assert 0 <= targetIndex && targetIndex <= trailerPosition : targetIndex + " not between 0 and " + trailerPosition;
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
        return Byte.toUnsignedInt(bytes[3]) == 245;
    }

    @Override
    public AbstractBytecodeNode decodeBytecode(final CompiledCodeObject code, final int index) {
        return decodeBytecode(code, index, 0, 0, 0);
    }

    public static AbstractBytecodeNode decodeBytecode(final CompiledCodeObject code, final int index, final int extBytes, final int extA, final int extB) {
        CompilerAsserts.neverPartOfCompilation();
        final byte[] bytecode = code.getBytes();
        final int indexWithExt = index + extBytes;
        final int b = Byte.toUnsignedInt(bytecode[indexWithExt]);
        //@formatter:off
        switch (b) {
            case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E: case 0x0F:
                return PushBytecodes.PushReceiverVariableNode.create(code, index, 1, b & 0xF);
            case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: case 0x16: case 0x17:
            case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:
                return new PushBytecodes.PushLiteralVariableNode(code, index, 1, b & 0xF);
            case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
            case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C: case 0x2D: case 0x2E: case 0x2F:
            case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: case 0x36: case 0x37:
            case 0x38: case 0x39: case 0x3A: case 0x3B: case 0x3C: case 0x3D: case 0x3E: case 0x3F:
                return new PushBytecodes.PushLiteralConstantNode(code, index, 1, b & 31);
            case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x46: case 0x47:
                return new PushBytecodes.PushTemporaryLocationNode(code, index, 1, b & 0x7);
            case 0x48: case 0x49: case 0x4A: case 0x4B:
                return new PushBytecodes.PushTemporaryLocationNode(code, index, 1, (b & 3) + 8);
            case 0x4C:
                return PushBytecodes.PushReceiverNode.create(code, index);
            case 0x4D:
                return new PushBytecodes.PushConstantTrueNode(code, index);
            case 0x4E:
                return new PushBytecodes.PushConstantFalseNode(code, index);
            case 0x4F:
                return new PushBytecodes.PushConstantNilNode(code, index);
            case 0x50:
                return new PushBytecodes.PushConstantZeroNode(code, index);
            case 0x51:
                return new PushBytecodes.PushConstantOneNode(code, index);
            case 0x52:
                // Push thisContext, (then Extend B = 1 => push thisProcess)
                if (extB == 0) {
                    return new PushBytecodes.PushActiveContextNode(code, index);
                } else {
                    return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
                }
            case 0x53:
                return new MiscellaneousBytecodes.DupNode(code, index);
            case 0x54: case 0x55: case 0x56: case 0x57:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 0x58:
                return ReturnBytecodes.ReturnReceiverNode.create(code, index);
            case 0x59:
                return ReturnBytecodes.ReturnConstantTrueNode.create(code, index);
            case 0x5A:
                return ReturnBytecodes.ReturnConstantFalseNode.create(code, index);
            case 0x5B:
                return ReturnBytecodes.ReturnConstantNilNode.create(code, index);
            case 0x5C:
                return ReturnBytecodes.ReturnTopFromMethodNode.create(code, index);
            case 0x5D:
                return ReturnBytecodes.ReturnNilFromBlockNode.create(code, index);
            case 0x5E:
                if (extA == 0) {
                    return ReturnBytecodes.ReturnTopFromBlockNode.create(code, index);
                } else { // shouldBeImplemented, see #genExtReturnTopFromBlock
                    return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
                }
            case 0x5F:
                return new MiscellaneousBytecodes.NopBytecodeNode(code, index);
            case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x66: case 0x67:
            case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6E: case 0x6F:
            case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75: case 0x76: case 0x77:
            case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C: case 0x7D: case 0x7E: case 0x7F:
                return SendBytecodes.SendSpecialSelectorNode.create(code, index, b - 96);
            case 0x80: case 0x81: case 0x82: case 0x83: case 0x84: case 0x85: case 0x86: case 0x87:
            case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8E: case 0x8F:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 0);
            case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
            case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 1);
            case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
            case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAE: case 0xAF:
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 1, b & 0xF, 2);
            case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                return JumpBytecodes.UnconditionalJumpNode.createShort(code, index, b);
            case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                return JumpBytecodes.ConditionalJumpOnTrueNode.createShort(code, index, b);
            case 0xC0: case 0xC1: case 0xC2: case 0xC3: case 0xC4: case 0xC5: case 0xC6: case 0xC7:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createShort(code, index, b);
            case 0xC8: case 0xC9: case 0xCA: case 0xCB: case 0xCC: case 0xCD: case 0xCE: case 0xCF:
                return new StoreBytecodes.PopIntoReceiverVariableNode(code, index, 1, b & 7);
            case 0xD0: case 0xD1: case 0xD2: case 0xD3: case 0xD4: case 0xD5: case 0xD6: case 0xD7:
                return new StoreBytecodes.PopIntoTemporaryLocationNode(code, index, 1, b & 7);
            case 0xD8:
                return new MiscellaneousBytecodes.PopNode(code, index);
            case 0xD9:
                return new SendBytecodes.SendSelfSelectorNode(code, index, 1, code.getSqueakClass().getImage().getSpecialSelector(SPECIAL_OBJECT.SELECTOR_SISTA_TRAP), 1); // FIXME: Unconditional trap
            case 0xDA: case 0xDB: case 0xDC: case 0xDD: case 0xDE: case 0xDF:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 0xE0:
                return decodeBytecode(code, index, extBytes + 2, (extA << 8) + Byte.toUnsignedInt(bytecode[indexWithExt + 1]), extB);
            case 0xE1:
                return decodeBytecode(code, index, extBytes + 2, extA, (extB << 8) + bytecode[indexWithExt + 1]);
            case 0xE2:
                return PushBytecodes.PushReceiverVariableNode.create(code, index, 2 + extBytes, Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + (extA << 8));
            case 0xE3:
                return new PushBytecodes.PushLiteralVariableNode(code, index, 2 + extBytes, Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + (extA << 8));
            case 0xE4:
                return new PushBytecodes.PushLiteralConstantNode(code, index, 2 + extBytes, Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + (extA << 8));
            case 0xE5:
                return new PushBytecodes.PushTemporaryLocationNode(code, index, 2, Byte.toUnsignedInt(bytecode[indexWithExt + 1]));
            case 0xE6:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 0xE7:
                return PushBytecodes.PushNewArrayNode.create(code, index, 2, bytecode[indexWithExt + 1]);
            case 0xE8:
                return new PushBytecodes.PushSmallIntegerNode(code, index, 2 + extBytes, Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + (extB << 8));
            case 0xE9:
                return new PushBytecodes.PushCharacterNode(code, index, 2 + extBytes, Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + (extB << 8));
            case 0xEA: {
                final int byte1 = Byte.toUnsignedInt(bytecode[indexWithExt + 1]);
                final int literalIndex = (byte1 >> 3) + (extA << 5);
                final int numArgs = (byte1 & 7) + (extB << 3);
                return SendBytecodes.SendLiteralSelectorNode.create(code, index, 2 + extBytes, literalIndex, numArgs);
            }
            case 0xEB: {
                boolean isDirected = false;
                int extBValue = extB;
                if (extBValue >= 64) {
                    isDirected = true;
                    extBValue = extBValue & 63;
                }
                final int byte1 = Byte.toUnsignedInt(bytecode[indexWithExt + 1]);
                final int literalIndex = (byte1 >> 3) + (extA << 5);
                final int numArgs = (byte1 & 7) + (extBValue << 3);
                if (isDirected) {
                    return new SendBytecodes.DirectedSuperSendNode(code, index, 2 + extBytes, literalIndex, numArgs);
                } else {
                    return new SendBytecodes.SingleExtendedSuperNode(code, index, 2 + extBytes, literalIndex, numArgs);
                }
            }
            case 0xEC:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 1, b);
            case 0xED:
                return JumpBytecodes.UnconditionalJumpNode.createLongExtended(code, index, 2 + extBytes, bytecode[indexWithExt + 1], extB);
            case 0xEE:
                return JumpBytecodes.ConditionalJumpOnTrueNode.createLongExtended(code, index, 2 + extBytes, bytecode[indexWithExt + 1], extB);
            case 0xEF:
                return JumpBytecodes.ConditionalJumpOnFalseNode.createLongExtended(code, index, 2 + extBytes, bytecode[indexWithExt + 1], extB);
            case 0xF0:
                return new StoreBytecodes.PopIntoReceiverVariableNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF1:
                return new StoreBytecodes.PopIntoLiteralVariableNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF2:
                return new StoreBytecodes.PopIntoTemporaryLocationNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF3:
                return new StoreBytecodes.StoreIntoReceiverVariableNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF4:
                return new StoreBytecodes.StoreIntoLiteralVariableNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF5:
                return new StoreBytecodes.StoreIntoTemporaryLocationNode(code, index, 2 + extBytes, bytecode[indexWithExt + 1] + (extA << 8));
            case 0xF6:
            case 0xF7:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 2, b);
            case 0xF8: {
                // TODO: use `final boolean m = bytecode[indexWithExt + 2] >> 8 == 0;`
                // TODO: use `final int s = bytecode[indexWithExt + 2] >> 5 & 3;`
                final int j = bytecode[indexWithExt + 2] & 31;
                final int primitiveIndex = Byte.toUnsignedInt(bytecode[indexWithExt + 1]) + j;
                assert 1 <= primitiveIndex && primitiveIndex < 32767 : "primitiveIndex out of range";
                if (primitiveIndex < 1000) {
                    return new MiscellaneousBytecodes.CallPrimitiveNode(code, index, primitiveIndex);
                }
                switch (primitiveIndex) {
                    case 1000:
                        return new InlinePrimitiveBytecodes.PrimClassNode(code, index);
                    case 1001:
                        return new InlinePrimitiveBytecodes.PrimNumSlotsNode(code, index);
                    case 1002:
                        return new InlinePrimitiveBytecodes.PrimBasicSizeNode(code, index);
                    case 1003:
                        return new InlinePrimitiveBytecodes.PrimNumBytesNode(code, index);
                    case 1004:
                        return new InlinePrimitiveBytecodes.PrimNumShortsNode(code, index);
                    case 1005:
                        return new InlinePrimitiveBytecodes.PrimNumWordsNode(code, index);
                    case 1006:
                        return new InlinePrimitiveBytecodes.PrimNumDoubleWordsNode(code, index);
                    case 1020:
                        return new InlinePrimitiveBytecodes.PrimIdentityHashNode(code, index);
                    case 1021:
                        return new InlinePrimitiveBytecodes.PrimIdentityHashSmallIntegerNode(code, index);
                    case 1022:
                        return new InlinePrimitiveBytecodes.PrimIdentityHashCharacterNode(code, index);
                    case 1023:
                        return new InlinePrimitiveBytecodes.PrimIdentityHashSmallFloatNode(code, index);
                    case 1024:
                        return new InlinePrimitiveBytecodes.PrimIdentityHashBehaviorNode(code, index);
                    case 1030:
                        return new InlinePrimitiveBytecodes.PrimImmediateAsIntegerCharacterNode(code, index);
                    case 1031:
                        return new InlinePrimitiveBytecodes.PrimImmediateAsIntegerSmallFloatNode(code, index);
                    case 1032:
                        return new InlinePrimitiveBytecodes.PrimImmediateAsFloatNode(code, index);
                    case 2000:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerAddNode(code, index);
                    case 2001:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerSubtractNode(code, index);
                    case 2002:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerMultiplyNode(code, index);
                    case 2003:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerDivideNode(code, index);
                    case 2004:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerFloorDivideNode(code, index);
                    case 2005:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerFloorModNode(code, index);
                    case 2006:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerQuoNode(code, index);
                    case 2016:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerBitAndNode(code, index);
                    case 2017:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerBitOrNode(code, index);
                    case 2018:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerBitXorNode(code, index);
                    case 2019:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerBitShiftLeftNode(code, index);
                    case 2020:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerBitShiftRightNode(code, index);
                    case 2032:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerGreaterThanNode(code, index);
                    case 2033:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerLessThanNode(code, index);
                    case 2034:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerGreaterOrEqualNode(code, index);
                    case 2035:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerLessOrEqualNode(code, index);
                    case 2036:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerEqualNode(code, index);
                    case 2037:
                        return new InlinePrimitiveBytecodes.PrimSmallIntegerNotEqualNode(code, index);
                    case 2065:
                        return new InlinePrimitiveBytecodes.PrimByteAtNode(code, index);
                    case 2066:
                        return new InlinePrimitiveBytecodes.PrimShortAtNode(code, index);
                    case 2067:
                        return new InlinePrimitiveBytecodes.PrimWordAtNode(code, index);
                    case 2068:
                        return new InlinePrimitiveBytecodes.PrimDoubleWordAtNode(code, index);
                    case 3001:
                        return new InlinePrimitiveBytecodes.PrimByteAtPutNode(code, index);
                    case 3002:
                        return new InlinePrimitiveBytecodes.PrimShortAtPutNode(code, index);
                    case 3003:
                        return new InlinePrimitiveBytecodes.PrimWordAtPutNode(code, index);
                    case 3004:
                        return new InlinePrimitiveBytecodes.PrimDoubleWordAtPutNode(code, index);
                    case 3021:
                        return new InlinePrimitiveBytecodes.PrimByteEqualsNode(code, index);
                    case 4000:
                        return new InlinePrimitiveBytecodes.PrimFillFromToWithNode(code, index);
                    default:
                        return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 3, b);
                }
            }
            case 0xF9:
                return PushBytecodes.AbstractPushFullClosureNode.createExtended(code, index, 3, extA, bytecode[indexWithExt + 1], bytecode[indexWithExt + 2]);
            case 0xFA:
                return PushBytecodes.PushClosureNode.createExtended(code, index, 3 + extBytes, extA, extB, bytecode[indexWithExt + 1], bytecode[indexWithExt + 2]);
            case 0xFB:
                return new PushBytecodes.PushRemoteTempNode(code, index, 3, bytecode[indexWithExt + 1], bytecode[indexWithExt + 2]);
            case 0xFC:
                return new StoreBytecodes.StoreIntoRemoteTempNode(code, index, 3, bytecode[indexWithExt + 1], bytecode[indexWithExt + 2]);
            case 0xFD:
                return new StoreBytecodes.PopIntoRemoteTempNode(code, index, 3, bytecode[indexWithExt + 1], bytecode[indexWithExt + 2]);
            case 0xFE:
            case 0xFF:
                return new MiscellaneousBytecodes.UnknownBytecodeNode(code, index, 3, b);
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

    private static String decodeBytecodeToString(final CompiledCodeObject code, final int b, final int index) {
        final byte[] bytecode = code.getBytes();
        //@formatter:off
        switch (b) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                return "pushRcvr: " + (b & 15);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return "pushLitVar: " + code.getLiteral(b & 15);
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
                return "pushConstant: " + code.getLiteral(b & 31);
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
                return "pushTemp: " + (b & 7);
            case 72: case 73: case 74: case 75:
                return "pushTemp: " + (b & 3) + 8;
            case 76:
                return "self";
            case 77:
                return "pushConstant: true";
            case 78:
                return "pushConstant: false";
            case 79:
                return "pushConstant: nil";
            case 80:
                return "pushConstant: 0";
            case 81:
                return "pushConstant: 1";
            case 82:
                return "pushThisContext:";
            case 83:
                return "dup";
            case 84: case 85: case 86: case 87:
                return "unknown";
            case 88:
                return "returnSelf";
            case 89:
                return "return: true";
            case 90:
                return "return: false";
            case 91:
                return "return: nil";
            case 92:
                return "returnTop";
            case 93:
            case 94:
                return "blockReturn";
            case 95:
                return "nop";
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
            case 112: case 113: case 114: case 115: case 116: case 117: case 118: case 119:
            case 120: case 121: case 122: case 123: case 124: case 125: case 126: case 127:
                return "send: " +  code.getSqueakClass().getImage().getSpecialSelector(b - 96).asStringUnsafe();
            case 128: case 129: case 130: case 131: case 132: case 133: case 134: case 135:
            case 136: case 137: case 138: case 139: case 140: case 141: case 142: case 143:
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
            case 168: case 169: case 170: case 171: case 172: case 173: case 174: case 175:
                return "send: " + code.getLiteral(b & 0xF);
            case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183:
                return "jumpTo: ";
            case 184: case 185: case 186: case 187: case 188: case 189: case 190: case 191:
                return "jumpTrue: ";
            case 192: case 193: case 194: case 195: case 196: case 197: case 198: case 199:
                return "jumpFalse: ";
            case 200: case 201: case 202: case 203: case 204: case 205: case 206: case 207:
                return "popIntoRcvr: " + (b & 7);
            case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215:
                return "popIntoTemp: " + (b & 7);
            case 216:
                return "pop";
            case 217:
                return "send: " + code.getSqueakClass().getImage().getSpecialSelector(SPECIAL_OBJECT.SELECTOR_SISTA_TRAP).asStringUnsafe();
            case 218: case 219: case 220: case 221: case 222: case 223:
                return "unknown";
            case 224:
                return "extA";
            case 225:
                return "extB";
            case 226:
                return "pushRcvr: ";
            case 227:
                return "pushLitVar: ";
            case 228:
                return "pushConstant: ";
            case 229:
                return "pushTemp: ";
            case 230:
                return "unknown";
            case 231:
                return "push: (Array new: " + (bytecode[index + 1] & 127) + ")";
            case 232:
                return "pushConstant: ";
            case 233:
                return "pushConstant: $";
            case 234:
                return "send: ";
            case 235:
                return "sendSuper: ";
            case 236:
                return "unknown";
            case 237:
                return "jumpTo: ";
            case 238:
                return "jumpTrue: ";
            case 239:
                return "jumpFalse: ";
            case 240:
                return "popIntoRcvr: ";
            case 241:
                return "popIntoLit: ";
            case 242:
                return "popIntoTemp: ";
            case 243:
                return "storeIntoRcvr: ";
            case 244:
                return "storeIntoLit: ";
            case 245:
                return "storeIntoTemp: ";
            case 246:
            case 247:
                return "unknown";
            case 248: {
                final int j = bytecode[index + 2] & 31;
                final int primitiveIndex = Byte.toUnsignedInt(bytecode[index + 1]) + j;
                return "callPrimitive: " + primitiveIndex;
            }
            case 249:
                return "pushFullClosure: (self literalAt: ?) numCopied: ? numArgs: ?";
            case 250:
                return "closureNumCopied: ? numArgs: ? bytes ? to ?";
            case 251:
                return "pushTemp: inVectorAt: ";
            case 252:
                return "storeTemp: inVectorAt: ";
            case 253:
                return "popTemp: inVectorAt: ";
            case 254:
            case 255:
                return "unknown";
            default:
                throw SqueakException.create("Unknown bytecode:", b);
        }
        //@formatter:on
    }

    private static int decodeNumBytes(final CompiledCodeObject code, final int index) {
        final int b = Byte.toUnsignedInt(code.getBytes()[index]);
        if (b <= 223) {
            return 1;
        } else if (b <= 247) {
            return 2;
        } else {
            return 3;
        }
    }
}

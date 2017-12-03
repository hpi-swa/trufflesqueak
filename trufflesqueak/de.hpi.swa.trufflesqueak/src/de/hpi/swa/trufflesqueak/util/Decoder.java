package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DoubleExtendedDoAnythingNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedPushNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreAndPopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopTemporaryVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushActiveContextNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushClosureNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushNewArrayNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushTemporaryVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromBlockNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromMethodNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SecondExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;

public class Decoder {

    private CompiledCodeObject code;
    private int successorIndex = 0;
    private byte[] bc;

    public Decoder(byte[] bc, CompiledCodeObject code) {
        this.bc = bc;
        this.code = code;
    }

    public SqueakBytecodeNode[] decode(SqueakBytecodeNode[] nodes) {
        while (successorIndex < bc.length) {
            nodes[successorIndex] = decodeNextByte();
        }
        return nodes;
    }

    private SqueakImageContext getImage() {
        return code.image;
    }

    private int nextByte() {
        if (successorIndex >= bc.length) {
            return 0;
        }
        int b = bc[successorIndex];
        if (b < 0) {
            b = 256 + b;
        }
        successorIndex++;
        return b;
    }

    private SqueakBytecodeNode decodeNextByte() {
        int b = nextByte();
        //@formatter:off
        switch (b) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                return new PushReceiverVariableNode(code, successorIndex, b & 15);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return new PushTemporaryVariableNode(code, successorIndex, b & 15);
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
                return new PushLiteralConstantNode(code, successorIndex, b & 31);
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
            case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79:
            case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
                return new PushLiteralVariableNode(code, successorIndex, b & 31);
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
                return new StoreAndPopReceiverVariableNode(code, successorIndex, b & 7);
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
                return new StoreAndPopTemporaryVariableNode(code, successorIndex, b & 7);
            case 112:
                return new PushReceiverNode(code, successorIndex);
            case 113:
                return new PushConstantNode(code, successorIndex, true);
            case 114:
                return new PushConstantNode(code, successorIndex, false);
            case 115:
                return new PushConstantNode(code, successorIndex, null);
            case 116:
                return new PushConstantNode(code, successorIndex, -1);
            case 117:
                return new PushConstantNode(code, successorIndex, 0);
            case 118:
                return new PushConstantNode(code, successorIndex, 1);
            case 119:
                return new PushConstantNode(code, successorIndex, 2);
            case 120:
                return new ReturnReceiverNode(code, successorIndex);
            case 121:
                return new ReturnConstantNode(code, successorIndex, true);
            case 122:
                return new ReturnConstantNode(code, successorIndex, false);
            case 123:
                return new ReturnConstantNode(code, successorIndex, null);
            case 124:
                return new ReturnTopFromMethodNode(code, successorIndex);
            case 125:
                return new ReturnTopFromBlockNode(code, successorIndex);
            case 126:
                return new UnknownBytecodeNode(code, successorIndex, b);
            case 127:
                return new UnknownBytecodeNode(code, successorIndex, b);
            case 128:
                return ExtendedPushNode.create(code, successorIndex, nextByte());
            case 129:
                return ExtendedStoreNode.create(code, successorIndex, nextByte());
            case 130:
                return new ExtendedStoreAndPopNode(code, successorIndex, nextByte());
            case 131:
                return new SingleExtendedSendNode(code, successorIndex, nextByte());
            case 132:
                return DoubleExtendedDoAnythingNode.create(code, successorIndex, nextByte(), nextByte());
            case 133:
                int currentByte = nextByte();
                return new SingleExtendedSuperNode(code, successorIndex, currentByte & 31, currentByte >> 5);
            case 134:
                return new SecondExtendedSendNode(code, successorIndex, nextByte());
            case 135:
                return new PopNode(code, successorIndex);
            case 136:
                return new DupNode(code, successorIndex);
            case 137:
                return new PushActiveContextNode(code, successorIndex);
            case 138:
                return new PushNewArrayNode(code, successorIndex, nextByte());
            case 139:
                successorIndex += 2; // skip next two bytes which belong to this bytecode
                return new CallPrimitiveNode(code, successorIndex);
            case 140:
                return new PushRemoteTempNode(code, successorIndex, nextByte(), nextByte());
            case 141:
                return new StoreRemoteTempNode(code, successorIndex, nextByte(), nextByte());
            case 142:
                return new StoreAndPopRemoteTempNode(code, successorIndex, nextByte(), nextByte());
            case 143:
                return new PushClosureNode(code, successorIndex, nextByte(), nextByte(), nextByte());
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
                return new UnconditionalJumpNode(code, successorIndex, b);
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
                return new ConditionalJumpNode(code, successorIndex, b);
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
                return new UnconditionalJumpNode(code, successorIndex, b, nextByte());
            case 168: case 169: case 170: case 171:
                return new ConditionalJumpNode(code, successorIndex, b, nextByte(), true);
            case 172: case 173: case 174: case 175:
                return new ConditionalJumpNode(code, successorIndex, b, nextByte(), false);
            case 176:
                return new SendSelectorNode(code, successorIndex, getImage().plus, 1);
            case 177:
                return new SendSelectorNode(code, successorIndex, getImage().minus, 1);
            case 178:
                return new SendSelectorNode(code, successorIndex, getImage().lt, 1);
            case 179:
                return new SendSelectorNode(code, successorIndex, getImage().gt, 1);
            case 180:
                return new SendSelectorNode(code, successorIndex, getImage().le, 1);
            case 181:
                return new SendSelectorNode(code, successorIndex, getImage().ge, 1);
            case 182:
                return new SendSelectorNode(code, successorIndex, getImage().eq, 1);
            case 183:
                return new SendSelectorNode(code, successorIndex, getImage().ne, 1);
            case 184:
                return new SendSelectorNode(code, successorIndex, getImage().times, 1);
            case 185:
                return new SendSelectorNode(code, successorIndex, getImage().div, 1);
            case 186:
                return new SendSelectorNode(code, successorIndex, getImage().modulo, 1);
            case 187:
                return new SendSelectorNode(code, successorIndex, getImage().pointAt, 1);
            case 188:
                return new SendSelectorNode(code, successorIndex, getImage().bitShift, 1);
            case 189:
                return new SendSelectorNode(code, successorIndex, getImage().divide, 1);
            case 190:
                return new SendSelectorNode(code, successorIndex, getImage().bitAnd, 1);
            case 191:
                return new SendSelectorNode(code, successorIndex, getImage().bitOr, 1);
            case 192:
                return new SendSelectorNode(code, successorIndex, getImage().at, 1);
            case 193:
                return new SendSelectorNode(code, successorIndex, getImage().atput, 2);
            case 194:
                return new SendSelectorNode(code, successorIndex, getImage().size_, 0);
            case 195:
                return new SendSelectorNode(code, successorIndex, getImage().next, 0);
            case 196:
                return new SendSelectorNode(code, successorIndex, getImage().nextPut, 1);
            case 197:
                return new SendSelectorNode(code, successorIndex, getImage().atEnd, 0);
            case 198:
                return new SendSelectorNode(code, successorIndex, getImage().equivalent, 1);
            case 199:
                return new SendSelectorNode(code, successorIndex, getImage().klass, 0);
            case 200:
                return new SendSelectorNode(code, successorIndex, getImage().blockCopy, 1);
            case 201:
                return new SendSelectorNode(code, successorIndex, getImage().value, 0);
            case 202:
                return new SendSelectorNode(code, successorIndex, getImage().valueWithArg, 1);
            case 203:
                return new SendSelectorNode(code, successorIndex, getImage().do_, 1);
            case 204:
                return new SendSelectorNode(code, successorIndex, getImage().new_, 0);
            case 205:
                return new SendSelectorNode(code, successorIndex, getImage().newWithArg, 1);
            case 206:
                return new SendSelectorNode(code, successorIndex, getImage().x, 0);
            case 207:
                return new SendSelectorNode(code, successorIndex, getImage().y, 0);
            default:
                return new SendNode(code, successorIndex, b);
        }
        //@formatter:on
    }
}

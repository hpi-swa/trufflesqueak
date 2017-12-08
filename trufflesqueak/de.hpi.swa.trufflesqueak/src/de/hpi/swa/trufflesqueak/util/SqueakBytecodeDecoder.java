package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DoubleExtendedDoAnythingNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedPushNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreAndPopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopTemporaryVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SecondExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;

public class SqueakBytecodeDecoder {

    private CompiledCodeObject code;
    private int currentIndex = 0;
    private byte[] bc;

    public SqueakBytecodeDecoder(byte[] bc, CompiledCodeObject code) {
        this.bc = bc;
        this.code = code;
    }

    public SqueakBytecodeNode[] decode(SqueakBytecodeNode[] nodes) {
        while (currentIndex < bc.length) {
            nodes[currentIndex] = decodeNextByte();
        }
        return nodes;
    }

    private SqueakImageContext getImage() {
        return code.image;
    }

    private int nextByte() {
        if (currentIndex >= bc.length) {
            return 0;
        }
        int b = bc[currentIndex];
        if (b < 0) {
            b = 256 + b;
        }
        currentIndex++;
        return b;
    }

    private int nextIndex(int offset) {
        return currentIndex + offset;
    }

    private SqueakBytecodeNode decodeNextByte() {
        int b = nextByte();
        //@formatter:off
        switch (b) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                return new PushReceiverVariableNode(code, nextIndex(0), b & 15);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return new PushTemporaryVariableNode(code, nextIndex(0), b & 15);
            case 32: case 33: case 34: case 35: case 36: case 37: case 38: case 39:
            case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47:
            case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55:
            case 56: case 57: case 58: case 59: case 60: case 61: case 62: case 63:
                return new PushLiteralConstantNode(code, nextIndex(0), b & 31);
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71:
            case 72: case 73: case 74: case 75: case 76: case 77: case 78: case 79:
            case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87:
            case 88: case 89: case 90: case 91: case 92: case 93: case 94: case 95:
                return new PushLiteralVariableNode(code, nextIndex(0), b & 31);
            case 96: case 97: case 98: case 99: case 100: case 101: case 102: case 103:
                return new StoreAndPopReceiverVariableNode(code, nextIndex(0), b & 7);
            case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111:
                return new StoreAndPopTemporaryVariableNode(code, nextIndex(0), b & 7);
            case 112:
                return new PushReceiverNode(code, nextIndex(0));
            case 113:
                return new PushConstantNode(code, nextIndex(0), true);
            case 114:
                return new PushConstantNode(code, nextIndex(0), false);
            case 115:
                return new PushConstantNode(code, nextIndex(0), code.image.nil);
            case 116:
                return new PushConstantNode(code, nextIndex(0), -1);
            case 117:
                return new PushConstantNode(code, nextIndex(0), 0);
            case 118:
                return new PushConstantNode(code, nextIndex(0), 1);
            case 119:
                return new PushConstantNode(code, nextIndex(0), 2);
            case 120:
                return new ReturnReceiverNode(code, nextIndex(0));
            case 121:
                return new ReturnConstantNode(code, nextIndex(0), true);
            case 122:
                return new ReturnConstantNode(code, nextIndex(0), false);
            case 123:
                return new ReturnConstantNode(code, nextIndex(0), code.image.nil);
            case 124:
                return new ReturnTopFromMethodNode(code, nextIndex(0));
            case 125:
                return new ReturnTopFromBlockNode(code, nextIndex(0));
            case 126:
                return new UnknownBytecodeNode(code, nextIndex(0), b);
            case 127:
                return new UnknownBytecodeNode(code, nextIndex(0), b);
            case 128:
                return ExtendedPushNode.create(code, nextIndex(1), nextByte());
            case 129:
                return ExtendedStoreNode.create(code, nextIndex(1), nextByte());
            case 130:
                return ExtendedStoreAndPopNode.create(code, nextIndex(1), nextByte());
            case 131:
                return new SingleExtendedSendNode(code, nextIndex(1), nextByte());
            case 132:
                return DoubleExtendedDoAnythingNode.create(code, nextIndex(2), nextByte(), nextByte());
            case 133:
                return new SingleExtendedSuperNode(code, nextIndex(1), nextByte());
            case 134:
                return new SecondExtendedSendNode(code, nextIndex(1), nextByte());
            case 135:
                return new PopNode(code, nextIndex(0));
            case 136:
                return new DupNode(code, nextIndex(0));
            case 137:
                return new PushActiveContextNode(code, nextIndex(0));
            case 138:
                return new PushNewArrayNode(code, nextIndex(1), nextByte());
            case 139:
                return new CallPrimitiveNode(code, nextIndex(2), nextByte(), nextByte());
            case 140:
                return new PushRemoteTempNode(code, nextIndex(2), nextByte(), nextByte());
            case 141:
                return new StoreRemoteTempNode(code, nextIndex(2), nextByte(), nextByte());
            case 142:
                return new StoreAndPopRemoteTempNode(code, nextIndex(2), nextByte(), nextByte());
            case 143:
                return new PushClosureNode(code, nextIndex(3), nextByte(), nextByte(), nextByte());
            case 144: case 145: case 146: case 147: case 148: case 149: case 150: case 151:
                return new UnconditionalJumpNode(code, nextIndex(0), b);
            case 152: case 153: case 154: case 155: case 156: case 157: case 158: case 159:
                return new ConditionalJumpNode(code, nextIndex(0), b);
            case 160: case 161: case 162: case 163: case 164: case 165: case 166: case 167:
                return new UnconditionalJumpNode(code, nextIndex(1), b, nextByte());
            case 168: case 169: case 170: case 171:
                return new ConditionalJumpNode(code, nextIndex(1), b, nextByte(), true);
            case 172: case 173: case 174: case 175:
                return new ConditionalJumpNode(code, nextIndex(1), b, nextByte(), false);
            case 176:
                return new SendSelectorNode(code, nextIndex(0), getImage().plus, 1);
            case 177:
                return new SendSelectorNode(code, nextIndex(0), getImage().minus, 1);
            case 178:
                return new SendSelectorNode(code, nextIndex(0), getImage().lt, 1);
            case 179:
                return new SendSelectorNode(code, nextIndex(0), getImage().gt, 1);
            case 180:
                return new SendSelectorNode(code, nextIndex(0), getImage().le, 1);
            case 181:
                return new SendSelectorNode(code, nextIndex(0), getImage().ge, 1);
            case 182:
                return new SendSelectorNode(code, nextIndex(0), getImage().eq, 1);
            case 183:
                return new SendSelectorNode(code, nextIndex(0), getImage().ne, 1);
            case 184:
                return new SendSelectorNode(code, nextIndex(0), getImage().times, 1);
            case 185:
                return new SendSelectorNode(code, nextIndex(0), getImage().div, 1);
            case 186:
                return new SendSelectorNode(code, nextIndex(0), getImage().modulo, 1);
            case 187:
                return new SendSelectorNode(code, nextIndex(0), getImage().pointAt, 1);
            case 188:
                return new SendSelectorNode(code, nextIndex(0), getImage().bitShift, 1);
            case 189:
                return new SendSelectorNode(code, nextIndex(0), getImage().divide, 1);
            case 190:
                return new SendSelectorNode(code, nextIndex(0), getImage().bitAnd, 1);
            case 191:
                return new SendSelectorNode(code, nextIndex(0), getImage().bitOr, 1);
            case 192:
                return new SendSelectorNode(code, nextIndex(0), getImage().at, 1);
            case 193:
                return new SendSelectorNode(code, nextIndex(0), getImage().atput, 2);
            case 194:
                return new SendSelectorNode(code, nextIndex(0), getImage().size_, 0);
            case 195:
                return new SendSelectorNode(code, nextIndex(0), getImage().next, 0);
            case 196:
                return new SendSelectorNode(code, nextIndex(0), getImage().nextPut, 1);
            case 197:
                return new SendSelectorNode(code, nextIndex(0), getImage().atEnd, 0);
            case 198:
                return new SendSelectorNode(code, nextIndex(0), getImage().equivalent, 1);
            case 199:
                return new SendSelectorNode(code, nextIndex(0), getImage().klass, 0);
            case 200:
                return new SendSelectorNode(code, nextIndex(0), getImage().blockCopy, 1);
            case 201:
                return new SendSelectorNode(code, nextIndex(0), getImage().value, 0);
            case 202:
                return new SendSelectorNode(code, nextIndex(0), getImage().valueWithArg, 1);
            case 203:
                return new SendSelectorNode(code, nextIndex(0), getImage().do_, 1);
            case 204:
                return new SendSelectorNode(code, nextIndex(0), getImage().new_, 0);
            case 205:
                return new SendSelectorNode(code, nextIndex(0), getImage().newWithArg, 1);
            case 206:
                return new SendSelectorNode(code, nextIndex(0), getImage().x, 0);
            case 207:
                return new SendSelectorNode(code, nextIndex(0), getImage().y, 0);
            case 208: case 209: case 210: case 211: case 212: case 213: case 214: case 215:
            case 216: case 217: case 218: case 219: case 220: case 221: case 222: case 223:
                return new SendNode(code, nextIndex(0), b & 0xF, 0);
            case 224: case 225: case 226: case 227: case 228: case 229: case 230: case 231:
            case 232: case 233: case 234: case 235: case 236: case 237: case 238: case 239:
                return new SendNode(code, nextIndex(0), b & 0xF, 1);
            case 240: case 241: case 242: case 243: case 244: case 245: case 246: case 247:
            case 248: case 249: case 250: case 251: case 252: case 253: case 254: case 255:
                return new SendNode(code, nextIndex(0), b & 0xF, 2);
        }
        //@formatter:on
        throw new RuntimeException("Unknown bytecode: " + b);
    }
}
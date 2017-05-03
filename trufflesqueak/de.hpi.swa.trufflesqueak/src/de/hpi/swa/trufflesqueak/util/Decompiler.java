package de.hpi.swa.trufflesqueak.util;

import java.util.Stack;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DoubleExtendedDoAnythingNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DupNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedPushNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreAndPopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopIntoTemporaryVariable;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PopNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushActiveContextNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushClosureNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushNewArrayNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromBlockNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromMethodNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreRemoteTempNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.TemporaryVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ConditionalJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SecondExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.Send;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;

/**
 * This class is modeled after the Squeak 6.0 Decompiler
 *
 * @author Tim
 *
 */
public class Decompiler {
    private byte[] bytes;
    private CompiledMethodObject method;
    private SqueakImageContext image;

    public Decompiler(SqueakImageContext img, CompiledMethodObject cm, byte[] bc) {
        image = img;
        method = cm;
        bytes = bc;
    }

    public SqueakBytecodeNode[] getAST() {
        int index[] = {0};

        Vector<SqueakBytecodeNode> sequence = new Vector<>();
        while (index[0] < bytes.length) {
            SqueakBytecodeNode node = decodeByteAt(index);
            while (sequence.size() < index[0] - 1) {
                sequence.add(null); // fill parameter byte indices
            }
            sequence.add(node);
        }

        Stack<SqueakNode> stack = new Stack<>();
        Stack<SqueakNode> statements = new Stack<>();
        for (int i = 0; i < sequence.size(); i++) {
            SqueakBytecodeNode node = sequence.get(i);
            if (node != null) {
                node.interpretOn(stack, statements, sequence);
                if (node instanceof ReturnNode) {
                    // when we have a return on the main path, we don't need to continue
                    break;
                }
            }
        }
        return statements.toArray(new SqueakBytecodeNode[statements.size()]);
    }

    private int nextByte(int[] indexRef) {
        int index = indexRef[0];
        if (bytes.length <= index) {
            return 0;
        }
        int b = bytes[index];
        if (b < 0) {
            b = 256 + b;
        }
        indexRef[0] = index + 1;
        return b;
    }

    private SqueakBytecodeNode decodeByteAt(int[] indexRef) {
        int index = indexRef[0];
        int b = nextByte(indexRef);
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return new ReceiverVariableNode(method, index, b & 15);
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                return new TemporaryVariableNode(method, index, b & 15);
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                return new LiteralConstantNode(method, index, b & 31);
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                return new LiteralVariableNode(method, index, b & 31);
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
                return new PopIntoReceiverVariableNode(method, index, b & 7);
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
                return new PopIntoTemporaryVariable(method, index, b & 7);
            case 112:
                return new ReceiverNode(method, index);
            case 113:
                return new ConstantNode(method, index, true);
            case 114:
                return new ConstantNode(method, index, false);
            case 115:
                return new ConstantNode(method, index, null);
            case 116:
                return new ConstantNode(method, index, -1);
            case 117:
                return new ConstantNode(method, index, 0);
            case 118:
                return new ConstantNode(method, index, 1);
            case 119:
                return new ConstantNode(method, index, 2);
            case 120:
                return new ReturnReceiverNode(method, index);
            case 121:
                return new ReturnConstantNode(method, index, true);
            case 122:
                return new ReturnConstantNode(method, index, false);
            case 123:
                return new ReturnConstantNode(method, index, null);
            case 124:
                return new ReturnTopFromMethodNode(method, index);
            case 125:
                return new ReturnTopFromBlockNode(method, index);
            case 126:
                return new UnknownBytecodeNode(method, index, b);
            case 127:
                return new UnknownBytecodeNode(method, index, b);
            case 128:
                return new ExtendedPushNode(method, index, nextByte(indexRef));
            case 129:
                return new ExtendedStoreNode(method, index, nextByte(indexRef));
            case 130:
                return new ExtendedStoreAndPopNode(method, index, nextByte(indexRef));
            case 131:
                return new SingleExtendedSendNode(method, index, nextByte(indexRef));
            case 132:
                return DoubleExtendedDoAnythingNode.create(method, index, nextByte(indexRef), nextByte(indexRef));
            case 133:
                int nextByte = nextByte(indexRef);
                return new SingleExtendedSuperNode(method, index, nextByte & 31, nextByte >> 5);
            case 134:
                return new SecondExtendedSendNode(method, index, nextByte(indexRef));
            case 135:
                return new PopNode(method, index);
            case 136:
                return new DupNode(method, index);
            case 137:
                return new PushActiveContextNode(method, index);
            case 138:
                return new PushNewArrayNode(method, index, nextByte(indexRef));
            case 139:
                return new CallPrimitiveNode(method, index, nextByte(indexRef), nextByte(indexRef));
            case 140:
                return new PushRemoteTempNode(method, index, nextByte(indexRef), nextByte(indexRef));
            case 141:
                return new StoreRemoteTempNode(method, index, nextByte(indexRef), nextByte(indexRef));
            case 142:
                return new StoreAndPopRemoteTempNode(method, index, nextByte(indexRef), nextByte(indexRef));
            case 143:
                return new PushClosureNode(method, index, nextByte(indexRef), nextByte(indexRef), nextByte(indexRef));
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
                return new UnconditionalJump(method, index, b);
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                return new ConditionalJump(method, index, b);
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
                return new UnconditionalJump(method, index, b, nextByte(indexRef));
            case 168:
            case 169:
            case 170:
            case 171:
                return new ConditionalJump(method, index, b, nextByte(indexRef), true);
            case 172:
            case 173:
            case 174:
            case 175:
                return new ConditionalJump(method, index, b, nextByte(indexRef), false);
            case 176:
                return new SendSelector(method, index, image.plus, 1);
            case 177:
                return new SendSelector(method, index, image.minus, 1);
            case 178:
                return new SendSelector(method, index, image.lt, 1);
            case 179:
                return new SendSelector(method, index, image.gt, 1);
            case 180:
                return new SendSelector(method, index, image.le, 1);
            case 181:
                return new SendSelector(method, index, image.ge, 1);
            case 182:
                return new SendSelector(method, index, image.eq, 1);
            case 183:
                return new SendSelector(method, index, image.ne, 1);
            case 184:
                return new SendSelector(method, index, image.times, 1);
            case 185:
                return new SendSelector(method, index, image.divide, 1);
            case 186:
                return new SendSelector(method, index, image.modulo, 1);
            case 187:
                return new SendSelector(method, index, image.pointAt, 1);
            case 188:
                return new SendSelector(method, index, image.bitShift, 1);
            case 189:
                return new SendSelector(method, index, image.div, 1);
            case 190:
                return new SendSelector(method, index, image.bitAnd, 1);
            case 191:
                return new SendSelector(method, index, image.bitOr, 1);
            case 192:
                return new SendSelector(method, index, image.at, 1);
            case 193:
                return new SendSelector(method, index, image.atput, 2);
            case 194:
                return new SendSelector(method, index, image.size_, 0);
            case 195:
                return new SendSelector(method, index, image.next, 0);
            case 196:
                return new SendSelector(method, index, image.nextPut, 1);
            case 197:
                return new SendSelector(method, index, image.atEnd, 0);
            case 198:
                return new SendSelector(method, index, image.equivalent, 1);
            case 199:
                return new SendSelector(method, index, image.klass, 0);
            case 200:
                return new SendSelector(method, index, image.blockCopy, 1);
            case 201:
                return new SendSelector(method, index, image.value, 0);
            case 202:
                return new SendSelector(method, index, image.valueWithArg, 1);
            case 203:
                return new SendSelector(method, index, image.do_, 1);
            case 204:
                return new SendSelector(method, index, image.new_, 0);
            case 205:
                return new SendSelector(method, index, image.newWithArg, 1);
            case 206:
                return new SendSelector(method, index, image.x, 0);
            case 207:
                return new SendSelector(method, index, image.y, 0);
            default:
                return new Send(method, index, b);
        }
    }
}

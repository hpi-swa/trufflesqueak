package de.hpi.swa.trufflesqueak.util;

import java.util.Vector;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.nodes.BytecodeSequence;
import de.hpi.swa.trufflesqueak.nodes.Loop;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.UnconditionalJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.CallPrimitive;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DoubleExtendedDoAnything;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Dup;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedPush;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStore;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreAndPop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Pop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushActiveContext;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushClosure;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushConst;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushLiteralConst;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushNewArray;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiver;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiverVariable;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushRemoteTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushVariable;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnConst;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnReceiver;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromBlock;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromMethod;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SecondExtendedSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Send;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SingleExtendedSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SingleExtendedSuper;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRcvr;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRemoteTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreRemoteTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.Jump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LongJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LongJumpIfFalse;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LongJumpIfTrue;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ShortCondJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ShortJump;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAdd;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAt;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAtEnd;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAtPut;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitAnd;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitOr;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitShift;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBlockCopy;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimClass;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimDiv;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimDivide;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimDo;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimEquivalent;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimGreaterOrEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimGreaterThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimLessOrEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimLessThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMakePoint;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMod;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMul;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNew;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNewArg;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNext;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNextPut;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNotEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimPtX;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimPtY;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimSize;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimSub;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimValue;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimValueArg;

public class Decompiler {
    private byte[] bytes;
    private CompiledMethodObject method;
    private SqueakImageContext image;

    public Decompiler(SqueakImageContext img, CompiledMethodObject cm, byte[] bc) {
        image = img;
        method = cm;
        bytes = bc;
    }

    public SqueakBytecodeNode getAST() {
        int index[] = {0};
        Vector<SqueakBytecodeNode> sequence = new Vector<>();
        SqueakBytecodeNode currentNode = null;
        while (index[0] < bytes.length) {
            int idx = index[0];
            SqueakBytecodeNode node = decodeByteAt(index);

            if (node instanceof UnconditionalJump) {
                int targetPC = ((Jump) node).getTargetPC();
                assert targetPC < idx;
                // backjump, we're in a loop

                int conditionPC = targetPC;
                int curIdx = targetPC;
                while (curIdx < idx) {
                    SqueakBytecodeNode jumpBodyNode = sequence.get(curIdx);
                    if (jumpBodyNode instanceof Jump) {
                        conditionPC = ((Jump) jumpBodyNode).getTargetPC();
                        if (conditionPC > idx) {
                            assert conditionPC == idx + 1 || conditionPC == idx + 2;
                            // this jump would take us behind the unconditional back jump, so the
                            // code up to here is the condition
                            break;
                        }
                    }
                }
                SqueakBytecodeNode[] condition = sequence.subList(targetPC, conditionPC).toArray(new SqueakBytecodeNode[conditionPC - targetPC]);
                SqueakBytecodeNode[] body = sequence.subList(conditionPC, idx).toArray(new SqueakBytecodeNode[idx - conditionPC]);

                for (int j = targetPC; j < idx; j++) {
                    sequence.setElementAt(null, j);
                }
                sequence.add(idx, new Loop(condition, body));
            } else {
                sequence.add(idx, node);
            }

            if (currentNode != null) {
                currentNode.setNext(node);
            }
            currentNode = node;
        }
        return new BytecodeSequence(method, sequence.toArray(new SqueakBytecodeNode[sequence.size()]));
    }

    private SqueakBytecodeNode decodeByteAt(int[] indexRef) {
        int index = indexRef[0];
        int b = bytes[index];
        if (b < 0) {
            b = Math.abs(b) + 127;
        }
        indexRef[0] = index + 1;
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
                return new PushReceiverVariable(method, b & 15);
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
                return new PushTemp(method, b & 15);
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
                return new PushLiteralConst(method, b & 31);
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
                return new PushVariable(method, b & 31);
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
                return new StoreAndPopRcvr(method, b & 7);
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
                return new StoreAndPopTemp(method, b & 7);
            case 112:
                return new PushReceiver(method);
            case 113:
                return new PushConst(method, image.sqTrue);
            case 114:
                return new PushConst(method, image.sqFalse);
            case 115:
                return new PushConst(method, image.nil);
            case 116:
                return new PushConst(method, new SmallInteger(-1));
            case 117:
                return new PushConst(method, new SmallInteger(0));
            case 118:
                return new PushConst(method, new SmallInteger(1));
            case 119:
                return new PushConst(method, new SmallInteger(2));
            case 120:
                return new ReturnReceiver(method);
            case 121:
                return new ReturnConst(method, image.sqTrue);
            case 122:
                return new ReturnConst(method, image.sqFalse);
            case 123:
                return new ReturnConst(method, image.nil);
            case 124:
                return new ReturnTopFromMethod(method);
            case 125:
                return new ReturnTopFromBlock(method);
            case 126:
                return new UnknownBytecode(method);
            case 127:
                return new UnknownBytecode(method);
            case 128:
                return new ExtendedPush(method, bytes[indexRef[0] = ++index]);
            case 129:
                return new ExtendedStore(method, bytes[indexRef[0] = ++index]);
            case 130:
                return new ExtendedStoreAndPop(method, bytes[indexRef[0] = ++index]);
            case 131:
                return new SingleExtendedSend(method, bytes[indexRef[0] = ++index]);
            case 132:
                return new DoubleExtendedDoAnything(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 133:
                return new SingleExtendedSuper(method, bytes[indexRef[0] = ++index]);
            case 134:
                return new SecondExtendedSend(method, bytes[indexRef[0] = ++index]);
            case 135:
                return new Pop(method);
            case 136:
                return new Dup(method);
            case 137:
                return new PushActiveContext(method);
            case 138:
                return new PushNewArray(method, bytes[indexRef[0] = ++index]);
            case 139:
                return new CallPrimitive(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 140:
                return new PushRemoteTemp(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 141:
                return new StoreRemoteTemp(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 142:
                return new StoreAndPopRemoteTemp(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 143:
                return new PushClosure(method, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
                return new ShortJump(method, b);
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                return new ShortCondJump(method, b);
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
                return new LongJump(method, b, bytes[indexRef[0] = ++index]);
            case 168:
            case 169:
            case 170:
            case 171:
                return new LongJumpIfTrue(method, b, bytes[indexRef[0] = ++index]);
            case 172:
            case 173:
            case 174:
            case 175:
                return new LongJumpIfFalse(method, b, bytes[indexRef[0] = ++index]);
            case 176:
                return new PrimAdd();
            case 177:
                return new PrimSub();
            case 178:
                return new PrimLessThan();
            case 179:
                return new PrimGreaterThan();
            case 180:
                return new PrimLessOrEqual();
            case 181:
                return new PrimGreaterOrEqual();
            case 182:
                return new PrimEqual();
            case 183:
                return new PrimNotEqual();
            case 184:
                return new PrimMul();
            case 185:
                return new PrimDivide();
            case 186:
                return new PrimMod();
            case 187:
                return new PrimMakePoint();
            case 188:
                return new PrimBitShift();
            case 189:
                return new PrimDiv();
            case 190:
                return new PrimBitAnd();
            case 191:
                return new PrimBitOr();
            case 192:
                return new PrimAt();
            case 193:
                return new PrimAtPut();
            case 194:
                return new PrimSize();
            case 195:
                return new PrimNext();
            case 196:
                return new PrimNextPut();
            case 197:
                return new PrimAtEnd();
            case 198:
                return new PrimEquivalent();
            case 199:
                return new PrimClass();
            case 200:
                return new PrimBlockCopy();
            case 201:
                return new PrimValue();
            case 202:
                return new PrimValueArg();
            case 203:
                return new PrimDo();
            case 204:
                return new PrimNew();
            case 205:
                return new PrimNewArg();
            case 206:
                return new PrimPtX();
            case 207:
                return new PrimPtY();
            default:
                return new Send(method, b);
        }
    }
}

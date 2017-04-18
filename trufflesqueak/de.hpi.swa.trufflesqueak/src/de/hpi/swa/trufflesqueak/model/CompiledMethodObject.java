package de.hpi.swa.trufflesqueak.model;

import java.util.Vector;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.CallPrimitive;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.DoubleExtendedDoAnything;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Dup;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedPush;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStore;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreAndPop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LongJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LongJumpIfFalse;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LongJumpIfTrue;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ShortCondJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ShortJump;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SingleExtendedSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SingleExtendedSuper;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRcvr;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopRemoteTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreAndPopTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.StoreRemoteTemp;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.UnknownBytecode;
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
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class CompiledMethodObject extends SqueakObject implements TruffleObject {
    private static final String CLOSURE = "closure";
    private static final String SELF = "self";
    private static final String RECEIVER = "receiver";
    private static final String PC = "pc";
    private static final String STACK_POINTER = "stackPointer";
    protected BaseSqueakObject[] literals;
    protected byte[] bytes;
    private int numLiterals;
    private boolean isOptimized;
    private boolean hasPrimitive;
    private boolean needsLargeFrame;
    private int numTemps;
    private int numArgs;
    private int accessModifier;
    private boolean altInstructionSet;
    private SqueakBytecodeNode ast;
    private FrameDescriptor frameDescriptor;
    public FrameSlot receiverSlot;
    public FrameSlot selfSlot;
    public FrameSlot closureSlot;
    public FrameSlot stackPointerSlot;
    public FrameSlot pcSlot;
    public FrameSlot[] stackSlots;

    public CompiledMethodObject() {
    }

    public CompiledMethodObject(SqueakImageContext img, byte[] bc, BaseSqueakObject[] lits) {
        image = img;
        literals = lits;
        bytes = bc;
        setHeader(lits[0]);
        ast = getAST();
    }

    public CompiledMethodObject(SqueakImageContext img, byte[] bc) {
        this(img, bc, new BaseSqueakObject[]{new SmallInteger(0)});
    }

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        Vector<Integer> data = chunk.data();
        int header = data.get(0) >> 1; // header is a tagged small integer
        int literalsize = header & 0x7fff;
        BaseSqueakObject[] ptrs = chunk.getPointers(literalsize + 1);
        literals = ptrs;
        bytes = chunk.getBytes(literals.length);
        setHeader(literals[0]);
        ast = getAST();
    }

    private SqueakBytecodeNode getAST() {
        int index[] = {0};
        Vector<SqueakBytecodeNode> stack = new Vector<>();
        SqueakBytecodeNode firstNode = null;
        SqueakBytecodeNode currentNode = null;
        while (index[0] < bytes.length) {
            SqueakBytecodeNode node = decodeByteAt(index);
            if (firstNode == null) {
                firstNode = node;
            }
            if (currentNode != null) {
                currentNode.setNext(node);
            }
            currentNode = node;
        }
        return firstNode;
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
                return new PushReceiverVariable(this, b & 15);
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
                return new PushTemp(this, b & 15);
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
                return new PushLiteralConst(this, b & 31);
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
                return new PushVariable(this, b & 31);
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
                return new StoreAndPopRcvr(this, b & 7);
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
                return new StoreAndPopTemp(this, b & 7);
            case 112:
                return new PushReceiver(this);
            case 113:
                return new PushConst(this, image.sqTrue);
            case 114:
                return new PushConst(this, image.sqFalse);
            case 115:
                return new PushConst(this, image.nil);
            case 116:
                return new PushConst(this, new SmallInteger(-1));
            case 117:
                return new PushConst(this, new SmallInteger(0));
            case 118:
                return new PushConst(this, new SmallInteger(1));
            case 119:
                return new PushConst(this, new SmallInteger(2));
            case 120:
                return new ReturnReceiver(this);
            case 121:
                return new ReturnConst(this, image.sqTrue);
            case 122:
                return new ReturnConst(this, image.sqFalse);
            case 123:
                return new ReturnConst(this, image.nil);
            case 124:
                return new ReturnTopFromMethod(this);
            case 125:
                return new ReturnTopFromBlock(this);
            case 126:
                return new UnknownBytecode(this);
            case 127:
                return new UnknownBytecode(this);
            case 128:
                return new ExtendedPush(this, bytes[indexRef[0] = ++index]);
            case 129:
                return new ExtendedStore(this, bytes[indexRef[0] = ++index]);
            case 130:
                return new ExtendedStoreAndPop(this, bytes[indexRef[0] = ++index]);
            case 131:
                return new SingleExtendedSend(this, bytes[indexRef[0] = ++index]);
            case 132:
                return new DoubleExtendedDoAnything(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 133:
                return new SingleExtendedSuper(this, bytes[indexRef[0] = ++index]);
            case 134:
                return new SecondExtendedSend(this, bytes[indexRef[0] = ++index]);
            case 135:
                return new Pop(this);
            case 136:
                return new Dup(this);
            case 137:
                return new PushActiveContext(this);
            case 138:
                return new PushNewArray(this, bytes[indexRef[0] = ++index]);
            case 139:
                return new CallPrimitive(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 140:
                return new PushRemoteTemp(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 141:
                return new StoreRemoteTemp(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 142:
                return new StoreAndPopRemoteTemp(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 143:
                return new PushClosure(this, bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index], bytes[indexRef[0] = ++index]);
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
                return new ShortJump(this, b);
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
                return new ShortCondJump(this, b);
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
                return new LongJump(this, b, bytes[indexRef[0] = ++index]);
            case 168:
            case 169:
            case 170:
            case 171:
                return new LongJumpIfTrue(this, b, bytes[indexRef[0] = ++index]);
            case 172:
            case 173:
            case 174:
            case 175:
                return new LongJumpIfFalse(this, b, bytes[indexRef[0] = ++index]);
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
                return new Send(this, b);
        }
    }

    private void setHeader(BaseSqueakObject baseSqueakObject) {
        assert baseSqueakObject instanceof SmallInteger;
        SmallInteger smallInt = (SmallInteger) baseSqueakObject;
        int[] splitHeader = BitSplitter.splitter(smallInt.getValue(), new int[]{15, 1, 1, 1, 6, 4, 2, 1});
        numLiterals = splitHeader[0];
        isOptimized = splitHeader[1] == 1;
        hasPrimitive = splitHeader[2] == 1;
        needsLargeFrame = splitHeader[3] == 1;
        numTemps = splitHeader[4];
        numArgs = splitHeader[5];
        accessModifier = splitHeader[6];
        altInstructionSet = splitHeader[7] == 1;
        frameDescriptor = new FrameDescriptor(image.nil);

        int squeakFrameSize = 16;
        if (needsLargeFrame) {
            squeakFrameSize = 40;
        }
        stackSlots = new FrameSlot[squeakFrameSize];
        for (int i = 0; i < squeakFrameSize; i++) {
            stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Object);
        }
        pcSlot = frameDescriptor.addFrameSlot(PC, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(STACK_POINTER, FrameSlotKind.Byte);
        receiverSlot = frameDescriptor.addFrameSlot(RECEIVER, FrameSlotKind.Object);
        selfSlot = frameDescriptor.addFrameSlot(SELF, FrameSlotKind.Object);
        closureSlot = frameDescriptor.addFrameSlot(CLOSURE, FrameSlotKind.Object);
    }

    public VirtualFrame createFrame(BaseSqueakObject receiver) {
        return createFrame(receiver, new BaseSqueakObject[]{});
    }

    public VirtualFrame createFrame(BaseSqueakObject receiver, BaseSqueakObject[] arguments) {
        VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(arguments, frameDescriptor);
        frame.setObject(receiverSlot, receiver);
        frame.setInt(stackPointerSlot, numTemps);
        frame.setInt(pcSlot, 0);
        return frame;
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        String className = "UnknownClass";
        String selector = "unknownSelector";
        if (literals.length > 0) {
            BaseSqueakObject baseSqueakObject = literals[literals.length - 1];
            if (baseSqueakObject instanceof PointersObject) {
                if (((PointersObject) baseSqueakObject).size() == 2) {
                    try {
                        baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
                    } catch (InvalidIndex e) {
                        assert false;
                    }
                }
                if (((PointersObject) baseSqueakObject).isClass()) {
                    className = ((PointersObject) baseSqueakObject).nameAsClass();
                }
            }

            if (literals.length > 1) {
                baseSqueakObject = literals[literals.length - 2];
                if (baseSqueakObject instanceof NativeObject) {
                    selector = baseSqueakObject.toString();
                }
            }
        }
        return className + ">>" + selector;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof CompiledMethodObject) {
            if (super.become(other)) {

                BaseSqueakObject[] literals2 = ((CompiledMethodObject) other).literals;
                ((CompiledMethodObject) other).literals = this.literals;
                this.literals = literals2;

                byte[] bytes2 = ((CompiledMethodObject) other).bytes;
                ((CompiledMethodObject) other).bytes = this.bytes;
                this.bytes = bytes2;
                return true;
            }
        }
        return false;
    }

    public SqueakBytecodeNode getBytecodeAST() {
        return ast;
    }

    @Override
    public int size() {
        return literals.length * 4 + bytes.length;
    }
}

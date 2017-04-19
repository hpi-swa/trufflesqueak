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
import de.hpi.swa.trufflesqueak.nodes.BytecodeSequence;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.Chunk;
import de.hpi.swa.trufflesqueak.util.Decompiler;

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
    private BytecodeSequence ast;
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
        ast = new Decompiler(image, this, bytes).getAST();
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
        ast = new Decompiler(image, this, bytes).getAST();
    }

    private void setHeader(BaseSqueakObject baseSqueakObject) {
        assert baseSqueakObject instanceof SmallInteger;
        int hdr = ((SmallInteger) baseSqueakObject).getValue();
        extractHeaderBits(hdr);
        prepareFrameDescriptor();
    }

    private void extractHeaderBits(int hdr) {
        int[] splitHeader = BitSplitter.splitter(hdr, new int[]{15, 1, 1, 1, 6, 4, 2, 1});
        numLiterals = splitHeader[0];
        isOptimized = splitHeader[1] == 1;
        hasPrimitive = splitHeader[2] == 1;
        needsLargeFrame = splitHeader[3] == 1;
        numTemps = splitHeader[4];
        numArgs = splitHeader[5];
        accessModifier = splitHeader[6];
        altInstructionSet = splitHeader[7] == 1;
    }

    private void prepareFrameDescriptor() {
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

    public BytecodeSequence getBytecodeAST() {
        return ast;
    }

    @Override
    public int size() {
        return literals.length * 4 + bytes.length;
    }
}

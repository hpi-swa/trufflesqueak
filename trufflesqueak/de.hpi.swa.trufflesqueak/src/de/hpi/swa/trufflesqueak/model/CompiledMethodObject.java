package de.hpi.swa.trufflesqueak.model;

import java.util.Vector;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.nodes.BytecodeSequence;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.Chunk;
import de.hpi.swa.trufflesqueak.util.Decompiler;

public class CompiledMethodObject extends SqueakObject implements TruffleObject {
    private static final String CLOSURE = "closure";
    private static final String SELF = "self";
    private static final String RECEIVER = "receiver";
    private static final String PC = "pc";
    private static final String STACK_POINTER = "stackPointer";
    @CompilationFinal(dimensions = 1) private BaseSqueakObject[] literals;
    private byte[] bytes;
    private int numLiterals;
    private boolean isOptimized;
    private boolean hasPrimitive;
    private boolean needsLargeFrame;
    @CompilationFinal private int numTemps;
    @CompilationFinal private int numArgs;
    private int accessModifier;
    private boolean altInstructionSet;
    private BytecodeSequence ast;
    private FrameDescriptor frameDescriptor;

    @CompilationFinal public FrameSlot receiverSlot;
    @CompilationFinal public FrameSlot selfSlot;
    @CompilationFinal public FrameSlot closureSlot;
    @CompilationFinal public FrameSlot stackPointerSlot;
    @CompilationFinal public FrameSlot pcSlot;
    @CompilationFinal(dimensions = 1) public FrameSlot[] stackSlots;
    private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");

    public CompiledMethodObject() {
        super();
    }

    public CompiledMethodObject(SqueakImageContext img, byte[] bc, BaseSqueakObject[] lits) {
        setImage(img);
        setBytesAndLiterals(lits, bc);
    }

    private void setBytesAndLiterals(BaseSqueakObject[] lits, byte[] bc) {
        literals = lits;
        bytes = bc;
        decodeHeader(literals[0]);
        prepareFrameDescriptor();
        ast = new Decompiler(getImage(), this, bytes).getAST();
        callTarget = Truffle.getRuntime().createCallTarget(new SqueakMethodNode(getImage().getLanguage(), this));
        callTargetStable.invalidate();
    }

    public CompiledMethodObject(SqueakImageContext img, byte[] bc) {
        this(img, bc, new BaseSqueakObject[]{img.wrapInt(0)});
    }

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        Vector<Integer> data = chunk.data();
        int header = data.get(0) >> 1; // header is a tagged small integer
        int literalsize = header & 0x7fff;
        BaseSqueakObject[] ptrs = chunk.getPointers(literalsize + 1);
        literals = ptrs;
        bytes = chunk.getBytes(ptrs.length);
    }

    private void decodeHeader(BaseSqueakObject baseSqueakObject) {
        assert baseSqueakObject instanceof SmallInteger;
        int hdr = baseSqueakObject.unsafeUnwrapInt();
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
        frameDescriptor = new FrameDescriptor(getImage().nil);
        int squeakFrameSize = 16;
        if (needsLargeFrame) {
            squeakFrameSize = 40;
        }
        stackSlots = new FrameSlot[squeakFrameSize];
        for (int i = 0; i < squeakFrameSize; i++) {
            stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
        }
        pcSlot = frameDescriptor.addFrameSlot(PC, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(STACK_POINTER, FrameSlotKind.Int);
        receiverSlot = frameDescriptor.addFrameSlot(RECEIVER, FrameSlotKind.Illegal);
        selfSlot = frameDescriptor.addFrameSlot(SELF, FrameSlotKind.Object);
        closureSlot = frameDescriptor.addFrameSlot(CLOSURE, FrameSlotKind.Object);
    }

    public VirtualFrame createTestFrame(BaseSqueakObject receiver) {
        return createTestFrame(receiver, new BaseSqueakObject[]{});
    }

    public VirtualFrame createTestFrame(BaseSqueakObject receiver, BaseSqueakObject[] arguments) {
        Object[] args = new Object[arguments.length + 1];
        int i = 0;
        args[i++] = receiver;
        for (BaseSqueakObject o : arguments) {
            args[i++] = o;
        }
        return Truffle.getRuntime().createVirtualFrame(args, frameDescriptor);
    }

    public RootCallTarget getCallTarget() {
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreter();
            setBytesAndLiterals(literals, bytes);
        }
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        String className = "UnknownClass";
        String selector = "unknownSelector";
        ClassObject classObject = getCompiledInClass();
        if (classObject != null) {
            className = classObject.nameAsClass();
        }
        NativeObject selectorObj = getCompiledInSelector();
        if (selectorObj != null) {
            selector = selectorObj.toString();
        }
        return className + ">>" + selector;
    }

    public NativeObject getCompiledInSelector() {
        if (literals.length > 1) {
            BaseSqueakObject lit = literals[literals.length - 2];
            if (lit instanceof NativeObject) {
                return (NativeObject) lit;
            }
        }
        return null;
    }

    public ClassObject getCompiledInClass() {
        if (literals.length == 0) {
            return null;
        }
        BaseSqueakObject baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject) {
            if (((PointersObject) baseSqueakObject).size() == 2) {
                baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
            }
        }
        if (baseSqueakObject instanceof ClassObject) {
            return (ClassObject) baseSqueakObject;
        }
        return null;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof CompiledMethodObject) {
            if (super.become(other)) {
                BaseSqueakObject[] literals2 = ((CompiledMethodObject) other).literals;
                byte[] bytes2 = ((CompiledMethodObject) other).bytes;
                ((CompiledMethodObject) other).setBytesAndLiterals(literals, bytes);
                this.setBytesAndLiterals(literals2, bytes2);
                return true;
            }
        }
        return false;
    }

    public BytecodeSequence getBytecodeAST() {
        return ast;
    }

    public int getBytecodeOffset() {
        return literals.length * 4;
    }

    @Override
    public int size() {
        return literals.length * 4 + bytes.length;
    }

    @Override
    public BaseSqueakObject at0(int idx) {
        if (idx < literals.length) {
            return literals[idx / 4];
        } else {
            return getImage().wrapInt(bytes[idx]);
        }
    }

    @Override
    public void atput0(int idx, BaseSqueakObject obj) throws UnwrappingError {
        if (idx < literals.length) {
            literals[idx / 4] = obj;
        } else {
            bytes[idx] = (byte) obj.unwrapInt();
        }
        setBytesAndLiterals(literals, bytes);
    }

    public BaseSqueakObject getLiteral(int idx) {
        if (literals.length > idx + 1) {
            return literals[idx + 1];
        } else {
            return literals[0];
        }
    }

    @Override
    public int instsize() {
        return 0;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public final int getNumTemps() {
        return numTemps;
    }

    public final int getNumArgs() {
        return numArgs;
    }
}

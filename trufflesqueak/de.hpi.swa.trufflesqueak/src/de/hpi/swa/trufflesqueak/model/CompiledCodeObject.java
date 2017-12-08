package de.hpi.swa.trufflesqueak.model;

import java.util.Vector;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.BytecodeSequenceNode;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.Chunk;

public abstract class CompiledCodeObject extends SqueakObject {
    // constants
    public static final String CLOSURE = "closure";
    public static final String SELF = "self";
    public static final String RECEIVER = "receiver";
    public static final String PC = "pc";
    public static final String STACK_POINTER = "stackPointer";
    public static final String MARKER = "marker";
    public static final String METHOD = "method";
    // code
    protected BytecodeSequenceNode bytesNode;
    Source source;
    // frame info
    private FrameDescriptor frameDescriptor;
    @CompilationFinal public FrameSlot receiverSlot;
    @CompilationFinal public FrameSlot thisContextSlot;
    @CompilationFinal public FrameSlot closureSlot;
    @CompilationFinal(dimensions = 1) public FrameSlot[] stackSlots;
    @CompilationFinal public FrameSlot markerSlot;
    @CompilationFinal public FrameSlot methodSlot;
    @CompilationFinal public FrameSlot stackPointerSlot;
    private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");
    // header info and data
    @CompilationFinal(dimensions = 1) protected Object[] literals;
    @CompilationFinal protected int numArgs;
    @SuppressWarnings("unused") private int numLiterals;
    @SuppressWarnings("unused") private boolean isOptimized;
    protected boolean hasPrimitive;
    boolean needsLargeFrame;
    @CompilationFinal int numTemps;
    @SuppressWarnings("unused") private int accessModifier;
    @SuppressWarnings("unused") private boolean altInstructionSet;

    abstract public NativeObject getCompiledInSelector();

    abstract public ClassObject getCompiledInClass();

    public CompiledCodeObject(SqueakImageContext img) {
        this(img, img.compiledMethodClass);
    }

    public CompiledCodeObject(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
        prepareFrameDescriptor();
    }

    protected CompiledCodeObject(CompiledCodeObject original) {
        this(original.image, original.getSqClass());
        setBytecodeSequenceAndLiterals(original.literals, original.bytesNode);
    }

    protected void setBytecodeSequenceAndLiterals(Object[] lits, BytecodeSequenceNode bNode) {
        literals = lits;
        bytesNode = bNode;
        updateAndInvalidateCallTargets();
    }

    // This creates a new BytecodeSequenceNode
    protected void setBytesAndLiterals(Object[] lits, byte[] bc) {
        literals = lits;
        createBytesNode(bc);
        updateAndInvalidateCallTargets();
    }

    private void createBytesNode(byte[] bc) {
        bytesNode = new BytecodeSequenceNode(bc, this);
        bytesNode.initialize();
    }

    @TruffleBoundary
    protected void updateAndInvalidateCallTargets() {
        decodeHeader();
// prepareFrameDescriptor();
        callTarget = Truffle.getRuntime().createCallTarget(new SqueakMethodNode(image.getLanguage(), this));
        callTargetStable.invalidate();
    }

    public Source getSource() {
        return source;
    }

    private int frameSize() {
        if (needsLargeFrame) {
            return 56;
        }
        return 16;
    }

    private void prepareFrameDescriptor() {
        frameDescriptor = new FrameDescriptor(null);
        stackSlots = new FrameSlot[frameSize() + 100]; // TODO: + class inst size
        for (int i = 0; i < stackSlots.length; i++) {
            stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
        }
        receiverSlot = frameDescriptor.addFrameSlot(RECEIVER, FrameSlotKind.Object);
        thisContextSlot = frameDescriptor.addFrameSlot(SELF, FrameSlotKind.Object);
        closureSlot = frameDescriptor.addFrameSlot(CLOSURE, FrameSlotKind.Object);
        markerSlot = frameDescriptor.addFrameSlot(MARKER, FrameSlotKind.Object);
        methodSlot = frameDescriptor.addFrameSlot(METHOD, FrameSlotKind.Object);
        stackPointerSlot = frameDescriptor.addFrameSlot(STACK_POINTER, FrameSlotKind.Int);
    }

    public VirtualFrame createTestFrame(Object receiver) {
        return createTestFrame(receiver, new Object[]{});
    }

    public VirtualFrame createTestFrame(Object receiver, Object[] arguments) {
        VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(arguments, frameDescriptor);
        frame.setObject(receiverSlot, receiver);
        return frame;
    }

    public RootCallTarget getCallTarget() {
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreter();
            setBytecodeSequenceAndLiterals(literals, bytesNode);
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

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public int getNumTemps() {
        return numTemps;
    }

    public final int getNumArgs() {
        return numArgs;
    }

    public FrameSlot getStackSlot(int i) {
        if (i >= stackSlots.length) { // This is fine, ignore for decoder
            return stackSlots[0];
        }
        return stackSlots[i];
    }

    public int getNumStackSlots() {
        return stackSlots.length;
    }

    public void setHeader(int hdr) {
        setLiteral(0, hdr);
    }

    public void setBytes(byte[] bc) {
        setBytesAndLiterals(literals, bc);
    }

    public void setLiterals(Object[] lits) {
        setBytecodeSequenceAndLiterals(lits, bytesNode);
    }

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
        Vector<Integer> data = chunk.data();
        int header = data.get(0) >> 1; // header is a tagged small integer
        int literalsize = header & 0x7fff;
        Object[] ptrs = chunk.getPointers(literalsize + 1);
        literals = ptrs;
        createBytesNode(chunk.getBytes(ptrs.length));
    }

    void decodeHeader() {
        int hdr = getHeader();
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

    public int getHeader() {
        Object object = literals[0];
        assert object instanceof Integer;
        int hdr = (int) object;
        return hdr;
    }

    public void setNumArgs(int num) {
        int hdrWithoutArgs = getHeader() & ~(0xF << 24);
        setHeader((num & 0xF << 24) | hdrWithoutArgs);
    }

    public void setNumTemps(int num) {
        int hdrWithoutTemps = getHeader() & ~(0x3F << 18);
        setHeader((num & 0x3F << 18) | hdrWithoutTemps);
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof CompiledMethodObject) {
            if (super.become(other)) {
                Object[] literals2 = ((CompiledCodeObject) other).literals;
                BytecodeSequenceNode bytes2 = ((CompiledCodeObject) other).bytesNode;
                ((CompiledCodeObject) other).setBytecodeSequenceAndLiterals(literals, bytesNode);
                this.setBytecodeSequenceAndLiterals(literals2, bytes2);
                return true;
            }
        }
        return false;
    }

    public int getBytecodeOffset() {
        return literals.length * 4;
    }

    @Override
    public int size() {
        return literals.length * 4 + bytesNode.getBytes().length;
    }

    @Override
    public Object at0(int idx) {
        if (idx < literals.length) {
            return literals[idx / 4];
        } else {
            return Byte.toUnsignedInt(bytesNode.getBytes()[idx]);
        }
    }

    @Override
    public void atput0(int idx, Object obj) {
        if (idx < literals.length) {
            setLiteral(idx / 4, obj);
        } else {
            byte[] bc = bytesNode.getBytes();
            bc[idx] = (byte) obj;
            setBytesAndLiterals(literals, bc);
        }
    }

    public Object getLiteral(int idx) {
        if (literals.length > idx + 1) {
            return literals[idx + 1];
        } else {
            return literals[0];
        }
    }

    public void setLiteral(int i, Object obj) {
        literals[i] = obj;
        setBytecodeSequenceAndLiterals(literals, bytesNode);
    }

    @Override
    public int instsize() {
        return 0;
    }

    public boolean hasPrimitive() {
        return hasPrimitive;
    }

    public int primitiveIndex() {
        byte[] bytes = bytesNode.getBytes();
        if (hasPrimitive() && bytes.length >= 3) {
            return Byte.toUnsignedInt(bytes[1]) + (Byte.toUnsignedInt(bytes[2]) << 8);
        } else {
            return 0;
        }
    }

    public BytecodeSequenceNode getBytecodeNode() {
        return bytesNode;
    }

    public Object[] getLiterals() {
        return literals;
    }

    abstract public CompiledMethodObject getMethod();
}
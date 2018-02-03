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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.EnterCodeNode;
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public abstract class CompiledCodeObject extends SqueakObject {
    public static enum SLOT_IDENTIFIER {
        THIS_CONTEXT_OR_MARKER,
        INSTRUCTION_POINTER,
        STACK_POINTER,
    }

    private static final int BYTES_PER_WORD = 4;

    // frame info
    @CompilationFinal private FrameDescriptor frameDescriptor;
    @CompilationFinal public FrameSlot thisContextOrMarkerSlot;
    @CompilationFinal(dimensions = 1) public FrameSlot[] stackSlots;
    @CompilationFinal public FrameSlot instructionPointerSlot;
    @CompilationFinal public FrameSlot stackPointerSlot;
    // header info and data
    @CompilationFinal(dimensions = 1) protected Object[] literals;
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal private int numArgs;
    @CompilationFinal protected int numLiterals;
    @CompilationFinal private boolean isOptimized;
    @CompilationFinal private boolean hasPrimitive;
    @CompilationFinal private boolean needsLargeFrame = true; // defaults to true
    @CompilationFinal private int numTemps;
    @CompilationFinal private int accessModifier;
    @CompilationFinal private boolean altInstructionSet;

    @CompilationFinal private final Assumption noContextNeeded = Truffle.getRuntime().createAssumption("Does not need a materialized context");

    @CompilationFinal private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    @CompilationFinal private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");

    abstract public NativeObject getCompiledInSelector();

    abstract public ClassObject getCompiledInClass();

    protected CompiledCodeObject(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
    }

    protected CompiledCodeObject(SqueakImageContext img) {
        this(img, img.compiledMethodClass);
    }

    protected CompiledCodeObject(CompiledCodeObject original) {
        this(original.image, original.getSqClass());
        setLiteralsAndBytes(original.literals, original.bytes);
    }

    private void setLiteralsAndBytes(Object[] literals, byte[] bytes) {
        this.literals = literals;
        decodeHeader();
        this.bytes = bytes;
        invalidateAndCreateNewCallTargets();
    }

    public Source getSource() {
        if (source == null) {
            source = Source.newBuilder(CompiledCodeObjectPrinter.getString(this)).mimeType(SqueakLanguage.MIME_TYPE).name(toString()).build();
        }
        return source;
    }

    public int frameSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    @TruffleBoundary
    protected void prepareFrameDescriptor() {
        frameDescriptor = new FrameDescriptor();
        int numStackSlots = frameSize() + getSqClass().getBasicInstanceSize();
        stackSlots = new FrameSlot[numStackSlots];
        for (int i = 0; i < stackSlots.length; i++) {
            stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
        }
        thisContextOrMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER, FrameSlotKind.Object);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);
    }

    public RootCallTarget getCallTarget() {
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callTarget = invalidateAndCreateNewCallTargets();
        }
        return callTarget;
    }

    @TruffleBoundary
    private RootCallTarget invalidateAndCreateNewCallTargets() {
        callTargetStable.invalidate();
        return Truffle.getRuntime().createCallTarget(EnterCodeNode.create(image.getLanguage(), this));
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
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

    public int getNumCopiedValues() {
        return 0;
    }

    public int getNumArgsAndCopiedValues() {
        return numArgs + getNumCopiedValues();
    }

    public int getNumLiterals() {
        return numLiterals;
    }

    public FrameSlot getStackSlot(int i) {
        if (i >= stackSlots.length) { // This is fine, ignore for decoder
            return null;
        }
        return stackSlots[i];
    }

    public int convertTempIndexToStackIndex(int tempIndex) {
        return tempIndex - getNumArgsAndCopiedValues();
    }

    public int getNumStackSlots() {
        return stackSlots.length;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        Vector<Integer> data = chunk.data();
        int header = data.get(0) >> 1; // header is a tagged small integer
        int literalsize = header & 0x7fff;
        Object[] ptrs = chunk.getPointers(literalsize + 1);
        assert literals == null;
        literals = ptrs;
        decodeHeader();
        assert bytes == null;
        bytes = chunk.getBytes(ptrs.length);
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
        prepareFrameDescriptor();
    }

    public int getHeader() {
        Object object = literals[0];
        assert object instanceof Integer;
        int hdr = (int) object;
        return hdr;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof CompiledMethodObject) {
            if (super.become(other)) {
                Object[] literals2 = ((CompiledCodeObject) other).literals;
                byte[] bytes2 = ((CompiledCodeObject) other).bytes;
                ((CompiledCodeObject) other).setLiteralsAndBytes(literals, bytes);
                this.setLiteralsAndBytes(literals2, bytes2);
                return true;
            }
        }
        return false;
    }

    /*
     * Answer the program counter for the receiver's first bytecode.
     *
     */
    public int getInitialPC() {
        // pc is offset by header + numLiterals, +1 for one-based addressing
        return getBytecodeOffset() + 1;
    }

    public int getBytecodeOffset() {
        return (1 + numLiterals) * BYTES_PER_WORD; // header plus numLiterals
    }

    @Override
    public int size() {
        return getBytecodeOffset() + bytes.length;
    }

    @Override
    public Object at0(int index) {
        if (index < getBytecodeOffset()) {
            assert index % BYTES_PER_WORD == 0;
            return literals[index / BYTES_PER_WORD];
        } else {
            int realIndex = index - getBytecodeOffset();
            assert realIndex < bytes.length;
            return Byte.toUnsignedInt(bytes[realIndex]);
        }
    }

    @Override
    public void atput0(int index, Object obj) {
        assert index >= 0;
        if (index < getBytecodeOffset()) {
            assert index % BYTES_PER_WORD == 0;
            setLiteral(index / BYTES_PER_WORD, obj);
        } else {
            int realIndex = index - getBytecodeOffset();
            assert realIndex < bytes.length;
            if (obj instanceof Integer) {
                Integer value = (Integer) obj;
                bytes[realIndex] = value.byteValue();
            } else {
                bytes[realIndex] = (byte) obj;
            }
        }
    }

    public Object getLiteral(int index) {
        int literalIndex = 1 + index; // skip header
        if (literalIndex < literals.length) {
            return literals[literalIndex];
        } else {
            return literals[0]; // for decoder
        }
    }

    public void setLiteral(int i, Object obj) {
        if (i == 0) {
            assert obj instanceof Integer;
            int oldNumLiterals = numLiterals;
            literals[0] = obj;
            decodeHeader();
            assert numLiterals == oldNumLiterals;
        } else {
            literals[i] = obj;
        }
    }

    @Override
    public int instsize() {
        return 0;
    }

    public boolean hasPrimitive() {
        return hasPrimitive;
    }

    public int primitiveIndex() {
        if (hasPrimitive && bytes.length >= 3) {
            return Byte.toUnsignedInt(bytes[1]) + (Byte.toUnsignedInt(bytes[2]) << 8);
        } else {
            return 0;
        }
    }

    public Object[] getLiterals() {
        return literals;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public abstract CompiledMethodObject getMethod();

    public Assumption getNoContextNeededAssumption() {
        return noContextNeeded;
    }

    public void invalidateNoContextNeededAssumption() {
        noContextNeeded.invalidate();
    }

    public boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == 198;
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }
}
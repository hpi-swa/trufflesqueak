package de.hpi.swa.graal.squeak.model;

import java.util.List;

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

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.util.BitSplitter;
import de.hpi.swa.graal.squeak.util.AbstractImageChunk;

public abstract class CompiledCodeObject extends SqueakObject {
    public enum SLOT_IDENTIFIER {
        THIS_CONTEXT_OR_MARKER,
        INSTRUCTION_POINTER,
        STACK_POINTER,
    }

    protected static final int BYTES_PER_WORD = 4;

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
    @CompilationFinal protected boolean needsLargeFrame = true; // defaults to true
    @CompilationFinal private int numTemps;
    @CompilationFinal private long accessModifier;
    @CompilationFinal private boolean altInstructionSet;
    @CompilationFinal public static boolean alwaysNonVirtualized = false;

    @CompilationFinal private final Assumption canBeVirtualized = Truffle.getRuntime().createAssumption("Does not need a materialized context");

    @CompilationFinal private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    @CompilationFinal private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");

    public abstract NativeObject getCompiledInSelector();

    public abstract ClassObject getCompiledInClass();

    protected CompiledCodeObject(final SqueakImageContext img, final ClassObject klass) {
        super(img, klass);
        if (alwaysNonVirtualized) {
            invalidateCanBeVirtualizedAssumption();
        }
    }

    protected CompiledCodeObject(final SqueakImageContext img) {
        this(img, img.compiledMethodClass);
    }

    protected CompiledCodeObject(final CompiledCodeObject original) {
        this(original.image, original.getSqClass());
        setLiteralsAndBytes(original.literals.clone(), original.bytes.clone());
    }

    private void setLiteralsAndBytes(final Object[] literals, final byte[] bytes) {
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

    public final int frameSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    @TruffleBoundary
    protected void prepareFrameDescriptor() {
        frameDescriptor = new FrameDescriptor();
        thisContextOrMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER, FrameSlotKind.Object);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Long);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Long);
        if (canBeVirtualized()) {
            final long numStackSlots = frameSize() + getSqClass().getBasicInstanceSize();
            stackSlots = new FrameSlot[(int) numStackSlots];
            for (int i = 0; i < stackSlots.length; i++) {
                stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
            }
        }
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
        final ClassObject classObject = getCompiledInClass();
        if (classObject != null) {
            className = classObject.nameAsClass();
        }
        final NativeObject selectorObj = getCompiledInSelector();
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

    public FrameSlot getStackSlot(final int i) {
        if (i >= stackSlots.length) { // This is fine, ignore for decoder
            return null;
        }
        return stackSlots[i];
    }

    public long getNumStackSlots() {
        return stackSlots.length;
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        final List<Integer> data = chunk.data();
        final int header = data.get(0) >> 1; // header is a tagged small integer
        final int literalsize = header & 0x7fff;
        final Object[] ptrs = chunk.getPointers(literalsize + 1);
        assert literals == null;
        literals = ptrs;
        decodeHeader();
        assert bytes == null;
        bytes = chunk.getBytes(ptrs.length);
    }

    protected void decodeHeader() {
        final int hdr = getHeader();
        final int[] splitHeader = BitSplitter.splitter(hdr, new int[]{15, 1, 1, 1, 6, 4, 2, 1});
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
        final Object object = literals[0];
        assert object instanceof Long;
        return ((Long) object).intValue();
    }

    @Override
    public boolean become(final BaseSqueakObject other) {
        if (!(other instanceof CompiledMethodObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] literals2 = ((CompiledCodeObject) other).literals;
        final byte[] bytes2 = ((CompiledCodeObject) other).bytes;
        ((CompiledCodeObject) other).setLiteralsAndBytes(literals, bytes);
        this.setLiteralsAndBytes(literals2, bytes2);
        return true;
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

    public abstract int getOffset(); // offset in the method's bytecode

    @Override
    public final int size() {
        return getBytecodeOffset() + bytes.length;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    @Override
    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset() - getOffset()) {
            assert index % BYTES_PER_WORD == 0;
            return literals[index / BYTES_PER_WORD];
        } else {
            final int realIndex = index - getBytecodeOffset() - getOffset();
            assert realIndex >= 0;
            return Byte.toUnsignedLong(bytes[realIndex]);
        }
    }

    @Override
    public void atput0(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        assert index >= 0;
        if (index < getBytecodeOffset()) {
            assert index % BYTES_PER_WORD == 0;
            setLiteral(index / BYTES_PER_WORD, obj);
        } else {
            final int realIndex = index - getBytecodeOffset();
            assert realIndex < bytes.length;
            if (obj instanceof Integer) {
                final Integer value = (Integer) obj;
                bytes[realIndex] = value.byteValue();
            } else if (obj instanceof Long) {
                bytes[realIndex] = ((Long) obj).byteValue();
            } else {
                bytes[realIndex] = (byte) obj;
            }
        }
    }

    public Object getLiteral(final long longIndex) {
        final int index = (int) longIndex;
        final int literalIndex = 1 + index; // skip header
        if (literalIndex < literals.length) {
            return literals[literalIndex];
        } else {
            return literals[0]; // for decoder
        }
    }

    public void setLiteral(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        if (index == 0) {
            assert obj instanceof Long;
            final int oldNumLiterals = numLiterals;
            literals[0] = obj;
            decodeHeader();
            assert numLiterals == oldNumLiterals;
        } else {
            literals[index] = obj;
        }
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

    public boolean canBeVirtualized() {
        return canBeVirtualized.isValid();
    }

    public Assumption getCanBeVirtualizedAssumption() {
        return canBeVirtualized;
    }

    public void invalidateCanBeVirtualizedAssumption() {
        canBeVirtualized.invalidate();
    }

    public boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == 198;
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }

    public boolean isDoesNotUnderstand() {
        return getCompiledInSelector() == image.doesNotUnderstand;
    }

    public static long makeHeader(final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) {
        long header = 0;
        header += (numArgs & 0x0F) << 24;
        header += (numTemps & 0x3F) << 18;
        header += numLiterals & 0x7FFF;
        header += hasPrimitive ? 65536 : 0;
        header += needsLargeFrame ? 0x20000 : 0;
        return header;
    }
}

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

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.util.BitSplitter;
import de.hpi.swa.graal.squeak.util.StopWatch;

public abstract class CompiledCodeObject extends AbstractSqueakObject {
    public enum SLOT_IDENTIFIER {
        THIS_CONTEXT_OR_MARKER,
        INSTRUCTION_POINTER,
        STACK_POINTER,
    }

    protected static final int BYTES_PER_WORD = 4;

    // frame info
    @CompilationFinal private static FrameDescriptor frameDescriptorTemplate;
    @CompilationFinal public static FrameSlot thisContextOrMarkerSlot;
    @CompilationFinal public static FrameSlot instructionPointerSlot;
    @CompilationFinal public static FrameSlot stackPointerSlot;
    @CompilationFinal private FrameDescriptor frameDescriptor;
    @CompilationFinal(dimensions = 1) public FrameSlot[] stackSlots;
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

    @CompilationFinal private final Assumption canBeVirtualized = Truffle.getRuntime().createAssumption("CompiledCodeObject: does not need a materialized context");

    @CompilationFinal private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    @CompilationFinal private final CyclicAssumption callTargetStable = new CyclicAssumption("CompiledCodeObject assumption");

    static {
        frameDescriptorTemplate = new FrameDescriptor();
        thisContextOrMarkerSlot = frameDescriptorTemplate.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER, FrameSlotKind.Object);
        instructionPointerSlot = frameDescriptorTemplate.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptorTemplate.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);
    }

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
        createNewCallTarget();
    }

    public final Source getSource() {
        if (source == null) {
            source = Source.newBuilder(CompiledCodeObjectPrinter.getString(this)).mimeType(SqueakLanguage.MIME_TYPE).name(toString()).build();
        }
        return source;
    }

    public final int frameSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    @TruffleBoundary
    protected final void prepareFrameDescriptor() {
        frameDescriptor = frameDescriptorTemplate.shallowCopy();
        if (canBeVirtualized()) {
            final int frameSize = frameSize();
            stackSlots = new FrameSlot[frameSize];
            for (int i = 0; i < frameSize; i++) {
                stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
            }
        }
    }

    public RootCallTarget getSplitCallTarget() {
        return getCallTarget();
    }

    public final RootCallTarget getCallTarget() {
        if (callTarget == null) {
            createNewCallTarget();
        }
        return callTarget;
    }

    private void createNewCallTarget() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(image.getLanguage(), this));
    }

    public final Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public final FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public final int getNumArgs() {
        return numArgs;
    }

    public final int getNumTemps() {
        return numTemps;
    }

    public final int getNumLiterals() {
        return numLiterals;
    }

    public final FrameSlot getStackSlot(final int i) {
        if (i >= stackSlots.length) { // This is fine, ignore for decoder
            return null;
        }
        return stackSlots[i];
    }

    public final long getNumStackSlots() {
        return stackSlots.length;
    }

    public final void fillin(final AbstractImageChunk chunk) {
        // final StopWatch fillinWatch = StopWatch.start("compiledCodeFillin");
        super.fillinHashAndClass(chunk);
        final List<Integer> data = chunk.data();
        final int header = data.get(0) >> 1; // header is a tagged small integer
        final int literalsize = header & 0x7fff;
        final Object[] ptrs = chunk.getPointers(literalsize + 1);
        assert literals == null;
        literals = ptrs;
        decodeHeader();
        assert bytes == null;
        bytes = chunk.getBytes(ptrs.length);
        // if (fillinWatch.stop() > 1 * 500_000) {
        // fillinWatch.printTimeMS();
        // }
    }

    protected final void decodeHeader() {
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

    public final int getHeader() {
        final Object object = literals[0];
        assert object instanceof Long;
        return ((Long) object).intValue();
    }

    @Override
    public final boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof CompiledMethodObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final CompiledCodeObject otherCodeObject = (CompiledCodeObject) other;
        final Object[] literals2 = otherCodeObject.literals;
        final byte[] bytes2 = otherCodeObject.bytes;
        otherCodeObject.setLiteralsAndBytes(literals, bytes);
        this.setLiteralsAndBytes(literals2, bytes2);
        otherCodeObject.callTargetStable.invalidate();
        callTargetStable.invalidate();
        return true;
    }

    public final int getBytecodeOffset() {
        return (1 + numLiterals) * BYTES_PER_WORD; // header plus numLiterals
    }

    public final int size() {
        return getBytecodeOffset() + bytes.length;
    }

    public final void atput0(final long longIndex, final Object obj) {
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

    public final Object getLiteral(final long longIndex) {
        final int index = (int) longIndex;
        final int literalIndex = 1 + index; // skip header
        if (literalIndex < literals.length) {
            return literals[literalIndex];
        } else {
            return literals[0]; // for decoder
        }
    }

    public final void setLiteral(final long longIndex, final Object obj) {
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

    public final boolean hasPrimitive() {
        return hasPrimitive;
    }

    public final int primitiveIndex() {
        if (hasPrimitive && bytes.length >= 3) {
            return Byte.toUnsignedInt(bytes[1]) + (Byte.toUnsignedInt(bytes[2]) << 8);
        } else {
            return 0;
        }
    }

    public final Object[] getLiterals() {
        return literals;
    }

    public final byte[] getBytes() {
        return bytes;
    }

    public final boolean canBeVirtualized() {
        return canBeVirtualized.isValid();
    }

    public final Assumption getCanBeVirtualizedAssumption() {
        return canBeVirtualized;
    }

    public final void invalidateCanBeVirtualizedAssumption() {
        canBeVirtualized.invalidate();
    }

    public final boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == 198;
    }

    public final boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }

    public static final long makeHeader(final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) {
        long header = 0;
        header += (numArgs & 0x0F) << 24;
        header += (numTemps & 0x3F) << 18;
        header += numLiterals & 0x7FFF;
        header += hasPrimitive ? 65536 : 0;
        header += needsLargeFrame ? 0x20000 : 0;
        return header;
    }
}

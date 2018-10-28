package de.hpi.swa.graal.squeak.model;

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

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public abstract class CompiledCodeObject extends AbstractSqueakObject {
    public enum SLOT_IDENTIFIER {
        THIS_CONTEXT_OR_MARKER,
        INSTRUCTION_POINTER,
        STACK_POINTER,
    }

    // frame info
    @CompilationFinal private FrameDescriptor frameDescriptor;
    @CompilationFinal public FrameSlot thisContextOrMarkerSlot;
    @CompilationFinal public FrameSlot instructionPointerSlot;
    @CompilationFinal public FrameSlot stackPointerSlot;
    @CompilationFinal(dimensions = 1) private FrameSlot[] stackSlots;
    // header info and data
    @CompilationFinal(dimensions = 1) protected Object[] literals;
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal private int numArgs;
    @CompilationFinal protected int numLiterals;
    @CompilationFinal private boolean hasPrimitive;
    @CompilationFinal protected boolean needsLargeFrame = false;
    @CompilationFinal private int numTemps;

    private final int numCopiedValues; // for block closures

    private static final boolean ALWAYS_NON_VIRTUALIZED = false;
    private final Assumption canBeVirtualized = Truffle.getRuntime().createAssumption("CompiledCodeObject: does not need a materialized context");

    private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("CompiledCodeObject assumption");

    protected CompiledCodeObject(final SqueakImageContext image, final int numCopiedValues) {
        // Always use CompiledMethod, CompiledBlock not needed.
        super(image, image.compiledMethodClass);
        if (ALWAYS_NON_VIRTUALIZED) {
            invalidateCanBeVirtualizedAssumption();
        }
        this.numCopiedValues = numCopiedValues;
    }

    protected CompiledCodeObject(final SqueakImageContext image, final int hash, final int numCopiedValues) {
        // Always use CompiledMethod, CompiledBlock not needed.
        super(image, hash, image.compiledMethodClass);
        if (ALWAYS_NON_VIRTUALIZED) {
            invalidateCanBeVirtualizedAssumption();
        }
        this.numCopiedValues = numCopiedValues;
    }

    protected CompiledCodeObject(final CompiledCodeObject original) {
        this(original.image, original.numCopiedValues);
        setLiteralsAndBytes(original.literals.clone(), original.bytes.clone());
    }

    private void setLiteralsAndBytes(final Object[] literals, final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.literals = literals;
        decodeHeader();
        this.bytes = bytes;
        createNewCallTarget();
    }

    public final Source getSource() {
        if (source == null) {
            /*
             * sourceSection requested when logging transferToInterpreters. Therefore, do not
             * trigger another TTI here which otherwise would cause endless recursion in Truffle
             * debug code.
             */
            source = Source.newBuilder(SqueakLanguageConfig.ID, CompiledCodeObjectPrinter.getString(this), toString()).build();
        }
        return source;
    }

    public final int sqContextSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    @TruffleBoundary
    private void prepareFrameDescriptor() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        frameDescriptor = new FrameDescriptor();
        thisContextOrMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT_OR_MARKER, FrameSlotKind.Object);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);
        /**
         * Arguments and copied values are also pushed onto the stack in {@link EnterCodeNode},
         * therefore there must be enough slots for all these values as well as the Squeak stack.
         */
        final int numFrameSlots = getNumArgsAndCopied() + sqContextSize();
        stackSlots = new FrameSlot[numFrameSlots];
        for (int i = 0; i < numFrameSlots; i++) {
            stackSlots[i] = frameDescriptor.addFrameSlot(i, FrameSlotKind.Illegal);
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

    public final int getNumArgsAndCopied() {
        return numArgs + numCopiedValues;
    }

    public final int getNumTemps() {
        return numTemps;
    }

    public final int getNumLiterals() {
        return numLiterals;
    }

    public final FrameSlot getStackSlot(final int i) {
        if (i < 0) {
            image.printToStdErr("Bad stack access, falling back to bottom-most stack slot...");
            return stackSlots[0];
        }
        return stackSlots[i];
    }

    public final long getNumStackSlots() {
        return stackSlots.length;
    }

    public final void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final long[] words = chunk.getWords();
        // header is a tagged small integer
        final long header = (words[0] >> (image.flags.is64bit() ? 3 : 1));
        final int numberOfLiterals = (int) (header & 0x7fff);
        final Object[] ptrs = chunk.getPointers(numberOfLiterals + 1);
        assert literals == null;
        literals = ptrs;
        decodeHeader();
        assert bytes == null;
        bytes = chunk.getBytes(ptrs.length * image.flags.wordSize());
    }

    protected final void decodeHeader() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final int hdr = getHeader();
        final int[] splitHeader = MiscUtils.bitSplitter(hdr, new int[]{15, 1, 1, 1, 6, 4, 2, 1});
        numLiterals = splitHeader[0];
        // TODO: isOptimized = splitHeader[1] == 1;
        hasPrimitive = splitHeader[2] == 1;
        needsLargeFrame = splitHeader[3] == 1;
        numTemps = splitHeader[4];
        numArgs = splitHeader[5];
        // TODO: accessModifier = splitHeader[6];
        // TODO: altInstructionSet = splitHeader[7] == 1;
        prepareFrameDescriptor();
    }

    public final int getHeader() {
        return (int) (long) literals[0];
    }

    public final void become(final CompiledCodeObject other) {
        becomeOtherClass(other);
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        other.setLiteralsAndBytes(literals, bytes);
        this.setLiteralsAndBytes(literals2, bytes2);
        other.callTargetStable.invalidate();
        callTargetStable.invalidate();
    }

    public final int getBytecodeOffset() {
        return (1 + numLiterals) * image.flags.wordSize(); // header plus numLiterals
    }

    public final void atput0(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        assert index >= 0;
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (index < getBytecodeOffset()) {
            assert index % image.flags.wordSize() == 0;
            setLiteral(index / image.flags.wordSize(), obj);
        } else {
            final int realIndex = index - getBytecodeOffset();
            assert realIndex < bytes.length;
            if (obj instanceof Integer) {
                bytes[realIndex] = (byte) (int) obj;
            } else if (obj instanceof Long) {
                bytes[realIndex] = (byte) (long) obj;
            } else {
                bytes[realIndex] = (byte) obj;
            }
        }
    }

    public final Object getLiteral(final long longIndex) {
        return literals[(int) (1 + longIndex)]; // +1 for skipping header.
    }

    public final void setLiteral(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
        if (hasPrimitive() && bytes.length >= 3) {
            return (Byte.toUnsignedInt(bytes[2]) << 8) + Byte.toUnsignedInt(bytes[1]);
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
        return (numArgs & 0x0F) << 24 | (numTemps & 0x3F) << 18 | numLiterals & 0x7FFF | (needsLargeFrame ? 0x20000 : 0) | (hasPrimitive ? 0x10000 : 0);
    }
}

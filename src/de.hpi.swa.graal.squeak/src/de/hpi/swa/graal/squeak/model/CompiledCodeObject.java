/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageConstants;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.nodes.ResumeContextNode.ResumeContextRootNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

@ExportLibrary(InteropLibrary.class)
public abstract class CompiledCodeObject extends AbstractSqueakObjectWithHash {
    private static final String SOURCE_UNAVAILABLE_NAME = "<unavailable>";
    public static final String SOURCE_UNAVAILABLE_CONTENTS = "Source unavailable";

    public enum SLOT_IDENTIFIER {
        THIS_MARKER,
        THIS_CONTEXT,
        INSTRUCTION_POINTER,
        STACK_POINTER,
    }

    // frame info
    private final FrameDescriptor frameDescriptor;
    private final FrameSlot thisMarkerSlot;
    private final FrameSlot thisContextSlot;
    private final FrameSlot instructionPointerSlot;
    private final FrameSlot stackPointerSlot;
    @CompilationFinal(dimensions = 1) protected FrameSlot[] stackSlots;
    // header info and data
    @CompilationFinal(dimensions = 1) protected Object[] literals;
    @CompilationFinal(dimensions = 1) protected byte[] bytes;
    @CompilationFinal protected int numArgs;
    @CompilationFinal protected int numLiterals;
    @CompilationFinal protected boolean hasPrimitive;
    @CompilationFinal protected boolean needsLargeFrame = false;
    @CompilationFinal protected int numTemps;

    @CompilationFinal(dimensions = 1) private CompiledBlockObject[] innerBlocks;

    private final int numCopiedValues; // for block closures

    private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("CompiledCodeObject assumption");
    private final Assumption doesNotNeedSender = Truffle.getRuntime().createAssumption("CompiledCodeObject doesNotNeedSender assumption");
    @CompilationFinal private RootCallTarget resumptionCallTarget;

    protected CompiledCodeObject(final SqueakImageContext image, final int hash, final int numCopiedValues) {
        super(image, hash);
        this.numCopiedValues = numCopiedValues;

        frameDescriptor = new FrameDescriptor();
        thisMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_MARKER, FrameSlotKind.Object);
        thisContextSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT, FrameSlotKind.Illegal);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);
    }

    protected CompiledCodeObject(final CompiledCodeObject original) {
        super(original);
        numCopiedValues = original.numCopiedValues;
        frameDescriptor = original.frameDescriptor;
        thisMarkerSlot = original.thisMarkerSlot;
        thisContextSlot = original.thisContextSlot;
        instructionPointerSlot = original.instructionPointerSlot;
        stackPointerSlot = original.stackPointerSlot;
        stackSlots = original.stackSlots;
        setLiteralsAndBytes(original.literals.clone(), original.bytes.clone());
    }

    private void setLiteralsAndBytes(final Object[] literals, final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.literals = literals;
        decodeHeader();
        this.bytes = bytes;
        innerBlocks = null; // Remove any inner blocks.
        renewCallTarget();
    }

    public final Source getSource() {
        CompilerAsserts.neverPartOfCompilation();
        if (source == null) {
            String name;
            String contents;
            try {
                name = toString();
                contents = SqueakBytecodeDecoder.decodeToString(this);
            } catch (final RuntimeException e) {
                name = SOURCE_UNAVAILABLE_NAME;
                contents = SOURCE_UNAVAILABLE_CONTENTS;
            }
            source = Source.newBuilder(SqueakLanguageConfig.ID, contents, name).mimeType("text/plain").build();
        }
        return source;
    }

    public final int getSqueakContextSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    public final RootCallTarget getCallTarget() {
        if (callTarget == null) {
            renewCallTarget();
        }
        return callTarget;
    }

    private void renewCallTarget() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        callTargetStable.invalidate();
        initializeCallTargetUnsafe();
    }

    protected final void initializeCallTargetUnsafe() {
        callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(image.getLanguage(), this));
    }

    public final Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    @TruffleBoundary
    public final RootCallTarget getResumptionCallTarget(final ContextObject context) {
        if (resumptionCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            resumptionCallTarget = Truffle.getRuntime().createCallTarget(ResumeContextRootNode.create(image.getLanguage(), context));
        } else {
            final ResumeContextRootNode resumeNode = (ResumeContextRootNode) resumptionCallTarget.getRootNode();
            if (resumeNode.getActiveContext() != context) {
                /**
                 * This is a trick: we set the activeContext of the {@link ResumeContextRootNode} to
                 * the given context to be able to reuse the call target.
                 */
                resumeNode.setActiveContext(context);
            }
        }
        return resumptionCallTarget;
    }

    public final Assumption getDoesNotNeedSenderAssumption() {
        return doesNotNeedSender;
    }

    public final FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public final FrameSlot getThisMarkerSlot() {
        return thisMarkerSlot;
    }

    public final FrameSlot getThisContextSlot() {
        return thisContextSlot;
    }

    public final FrameSlot getInstructionPointerSlot() {
        return instructionPointerSlot;
    }

    public final FrameSlot getStackPointerSlot() {
        return stackPointerSlot;
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
        assert 0 <= i && i < stackSlots.length : "Bad stack access";
        if (stackSlots[i] == null) {
            // Lazily add frame slots.
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackSlots[i] = frameDescriptor.addFrameSlot(i + 1, FrameSlotKind.Illegal);
        }
        return stackSlots[i];
    }

    public final FrameSlot[] getStackSlotsUnsafe() {
        return stackSlots;
    }

    public final int getNumStackSlots() {
        /**
         * Arguments and copied values are also pushed onto the stack in {@link EnterCodeNode},
         * therefore there must be enough slots for all these values as well as the Squeak stack.
         */
        return getSqueakContextSize();
    }

    @Override
    public ClassObject getSqueakClass() {
        return image.compiledMethodClass;
    }

    @Override
    public final void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // header is a tagged small integer
        final long header = chunk.getWord(0) >> 3;
        final int numberOfLiterals = (int) (header & 0x7fff);
        final Object[] ptrs = chunk.getPointers(numberOfLiterals + 1);
        assert literals == null;
        literals = ptrs;
        decodeHeader();
        assert bytes == null;
        bytes = Arrays.copyOfRange(chunk.getBytes(), ptrs.length * SqueakImageConstants.WORD_SIZE, chunk.getBytes().length);
        assert innerBlocks == null : "Should not have any inner blocks yet";
    }

    private int getHeader() {
        return (int) (long) literals[0];
    }

    protected final void decodeHeader() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final int header = getHeader();
        numLiterals = CompiledCodeHeaderDecoder.getNumLiterals(header);
        hasPrimitive = CompiledCodeHeaderDecoder.getHasPrimitive(header);
        needsLargeFrame = CompiledCodeHeaderDecoder.getNeedsLargeFrame(header);
        numTemps = CompiledCodeHeaderDecoder.getNumTemps(header);
        numArgs = CompiledCodeHeaderDecoder.getNumArguments(header);
        ensureCorrectNumberOfStackSlots();
    }

    protected void ensureCorrectNumberOfStackSlots() {
        final int requiredNumberOfStackSlots = getNumStackSlots();
        if (stackSlots == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackSlots = new FrameSlot[requiredNumberOfStackSlots];
            return;
        }
        final int currentNumberOfStackSlots = stackSlots.length;
        if (currentNumberOfStackSlots < requiredNumberOfStackSlots) {
            // Grow number of stack slots.
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackSlots = Arrays.copyOf(stackSlots, requiredNumberOfStackSlots);
        } else if (currentNumberOfStackSlots > requiredNumberOfStackSlots) {
            // Shrink number of stack slots.
            CompilerDirectives.transferToInterpreterAndInvalidate();
            for (int i = requiredNumberOfStackSlots; i < currentNumberOfStackSlots; i++) {
                frameDescriptor.removeFrameSlot(i);
            }
            stackSlots = Arrays.copyOf(stackSlots, requiredNumberOfStackSlots);
        }
    }

    public final void become(final CompiledCodeObject other) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        other.setLiteralsAndBytes(literals, bytes);
        setLiteralsAndBytes(literals2, bytes2);
        other.callTargetStable.invalidate();
        callTargetStable.invalidate();
    }

    public final int getBytecodeOffset() {
        return (1 + numLiterals) * SqueakImageConstants.WORD_SIZE; // header plus numLiterals
    }

    public final void atput0(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        assert index >= 0;
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (index < getBytecodeOffset()) {
            assert index % SqueakImageConstants.WORD_SIZE == 0;
            setLiteral(index / SqueakImageConstants.WORD_SIZE, obj);
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
        assert hasPrimitive() && bytes.length >= 3;
        return (Byte.toUnsignedInt(bytes[2]) << 8) + Byte.toUnsignedInt(bytes[1]);
    }

    public final boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == 198;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    public final Object[] getLiterals() {
        return literals;
    }

    public final byte[] getBytes() {
        return bytes;
    }

    public abstract CompiledMethodObject getMethod();

    public static final long makeHeader(final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) {
        return (numArgs & 0x0F) << 24 | (numTemps & 0x3F) << 18 | numLiterals & 0x7FFF | (needsLargeFrame ? 0x20000 : 0) | (hasPrimitive ? 0x10000 : 0);
    }

    public CompiledBlockObject findBlock(final CompiledMethodObject method, final int numClosureArgs, final int numCopied, final int successorIndex, final int blockSize) {
        if (innerBlocks != null) {
            // TODO: Avoid instanceof checks (same code in CompiledBlockObject).
            final int additionalOffset = this instanceof CompiledBlockObject ? ((CompiledBlockObject) this).getOffset() : 0;
            final int offset = additionalOffset + successorIndex;
            for (final CompiledBlockObject innerBlock : innerBlocks) {
                if (innerBlock.getOffset() == offset) {
                    return innerBlock;
                }
            }
        }
        return addInnerBlock(CompiledBlockObject.create(this, method, numClosureArgs, numCopied, successorIndex, blockSize));
    }

    private CompiledBlockObject addInnerBlock(final CompiledBlockObject innerBlock) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (innerBlocks == null) {
            innerBlocks = new CompiledBlockObject[]{innerBlock};
        } else {
            innerBlocks = Arrays.copyOf(innerBlocks, innerBlocks.length + 1);
            innerBlocks[innerBlocks.length - 1] = innerBlock;
        }
        return innerBlock;
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    protected final boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    protected final long getArraySize() {
        return literals.length;
    }

    @SuppressWarnings("static-method")
    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementInsertable")
    protected final boolean isArrayElementReadable(final long index) {
        return 0 <= index && index < literals.length;
    }

    @ExportMessage
    protected final Object readArrayElement(final long index) throws InvalidArrayIndexException {
        if (isArrayElementReadable(index)) {
            return literals[(int) index];
        } else {
            throw InvalidArrayIndexException.create(index);
        }
    }

    @ExportMessage
    protected final void writeArrayElement(final long index, final Object value,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode) throws InvalidArrayIndexException {
        if (isArrayElementReadable(index)) {
            literals[(int) index] = wrapNode.executeWrap(value);
        } else {
            throw InvalidArrayIndexException.create(index);
        }
    }

    /**
     * CompiledCode Header Specification.
     *
     * <pre>
     *   (index 0)      15 bits:   number of literals (#numLiterals)
     *   (index 15)      1 bit:    jit without counters - reserved for methods that have been optimized by Sista
     *   (index 16)      1 bit:    has primitive
     *   (index 17)      1 bit:    whether a large frame size is needed (#frameSize => either SmallFrame or LargeFrame)
     *   (index 18)      6 bits:   number of temporary variables (#numTemps)
     *   (index 24)      4 bits:   number of arguments to the method (#numArgs)
     *   (index 28)      2 bits:   reserved for an access modifier (00-unused, 01-private, 10-protected, 11-public), although accessors for bit 29 exist (see #flag).
     *   sign bit:       1 bit:    selects the instruction set, >= 0 Primary, < 0 Secondary (#signFlag)
     * </pre>
     */
    private static final class CompiledCodeHeaderDecoder {
        private static final int NUM_LITERALS_SIZE = 1 << 15;
        private static final int NUM_TEMPS_TEMPS_SIZE = 1 << 6;
        private static final int NUM_ARGUMENTS_SIZE = 1 << 4;

        private static int getNumLiterals(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 0, NUM_LITERALS_SIZE);
        }

        private static boolean getHasPrimitive(final long headerWord) {
            return (headerWord & 1 << 16) != 0;
        }

        private static boolean getNeedsLargeFrame(final long headerWord) {
            return (headerWord & 1 << 17) != 0;
        }

        private static int getNumTemps(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 18, NUM_TEMPS_TEMPS_SIZE);
        }

        private static int getNumArguments(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 24, NUM_ARGUMENTS_SIZE);
        }
    }
}

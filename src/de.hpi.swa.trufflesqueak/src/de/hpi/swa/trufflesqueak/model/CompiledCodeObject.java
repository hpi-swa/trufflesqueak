/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import org.graalvm.collections.EconomicMap;

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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ADDITIONAL_METHOD_STATE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_BINDING;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.EnterCodeNode;
import de.hpi.swa.trufflesqueak.nodes.ResumeContextNode.ResumeContextRootNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractSqueakBytecodeDecoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeSistaV1Decoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeV3PlusClosuresDecoder;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

@ExportLibrary(InteropLibrary.class)
public final class CompiledCodeObject extends AbstractSqueakObjectWithClassAndHash {
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
    @CompilationFinal protected boolean needsLargeFrame;
    @CompilationFinal protected int numTemps;

    private AbstractSqueakBytecodeDecoder decoder;

    /*
     * With FullBlockClosure support, CompiledMethods store CompiledBlocks in their literals and
     * CompiledBlocks their outer method in their last literal. For traditional BlockClosures, we
     * need to do something similar, but with CompiledMethods only (CompiledBlocks are not used
     * then). The next two fields are used to store "shadowBlocks", which are light copies of the
     * outer method with a new call target, and the outer method to be used for closure activations.
     */
    private EconomicMap<Integer, CompiledCodeObject> shadowBlocks;
    private CompiledCodeObject outerMethod;

    private Source source;

    @CompilationFinal private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("CompiledCodeObject callTargetStable assumption");
    private final Assumption doesNotNeedSender = Truffle.getRuntime().createAssumption("CompiledCodeObject doesNotNeedSender assumption");
    @CompilationFinal private RootCallTarget resumptionCallTarget;

    @TruffleBoundary
    public CompiledCodeObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
        frameDescriptor = new FrameDescriptor();
        thisMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_MARKER, FrameSlotKind.Object);
        thisContextSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT, FrameSlotKind.Illegal);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);
    }

    public CompiledCodeObject(final SqueakImageContext image, final byte[] bc, final Object[] lits, final ClassObject classObject) {
        this(image, AbstractSqueakObjectWithClassAndHash.HASH_UNINITIALIZED, classObject);
        literals = lits;
        decodeHeader();
        bytes = bc;
    }

    protected CompiledCodeObject(final CompiledCodeObject original) {
        super(original);
        frameDescriptor = original.frameDescriptor;
        thisMarkerSlot = original.thisMarkerSlot;
        thisContextSlot = original.thisContextSlot;
        instructionPointerSlot = original.instructionPointerSlot;
        stackPointerSlot = original.stackPointerSlot;
        stackSlots = original.stackSlots;
        setLiteralsAndBytes(original.literals.clone(), original.bytes.clone());
        decoder = original.decoder;
    }

    protected CompiledCodeObject(final CompiledCodeObject outerCode, final int startPC) {
        super(outerCode);
        outerCode.shadowBlocks.put(startPC, this);

        // Find outer method
        CompiledCodeObject currentOuterCode = outerCode;
        while (currentOuterCode.outerMethod != null) {
            currentOuterCode = currentOuterCode.outerMethod;
        }
        assert currentOuterCode.isCompiledMethod();
        outerMethod = currentOuterCode;

        frameDescriptor = new FrameDescriptor();
        thisMarkerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_MARKER, FrameSlotKind.Object);
        thisContextSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT, FrameSlotKind.Illegal);
        instructionPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER, FrameSlotKind.Int);
        stackPointerSlot = frameDescriptor.addFrameSlot(SLOT_IDENTIFIER.STACK_POINTER, FrameSlotKind.Int);

        // header info and data
        literals = outerCode.literals;
        bytes = outerCode.bytes;
        numArgs = outerCode.numArgs;
        numLiterals = outerCode.numLiterals;
        hasPrimitive = outerCode.hasPrimitive;
        needsLargeFrame = outerCode.needsLargeFrame;
        numTemps = outerCode.numTemps;

        decoder = outerCode.decoder;

        ensureCorrectNumberOfStackSlots();
    }

    private CompiledCodeObject(final int size, final SqueakImageContext image, final ClassObject classObject) {
        this(image, AbstractSqueakObjectWithClassAndHash.HASH_UNINITIALIZED, classObject);
        bytes = new byte[size];
    }

    public static CompiledCodeObject newOfSize(final SqueakImageContext image, final int size, final ClassObject classObject) {
        return new CompiledCodeObject(size, image, classObject);
    }

    public CompiledCodeObject getOrCreateShadowBlock(final int startPC) {
        CompilerAsserts.neverPartOfCompilation();
        if (shadowBlocks == null) {
            shadowBlocks = EconomicMap.create();
        }
        final CompiledCodeObject copy = shadowBlocks.get(startPC);
        if (copy == null) {
            return new CompiledCodeObject(this, startPC);
        } else {
            return copy;
        }
    }

    public CompiledCodeObject getOuterMethod() {
        assert outerMethod != null;
        return outerMethod;
    }

    private void setLiteralsAndBytes(final Object[] literals, final byte[] bytes) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.literals = literals;
        decodeHeader();
        this.bytes = bytes;
        renewCallTarget();
    }

    public Source getSource() {
        CompilerAsserts.neverPartOfCompilation();
        if (source == null) {
            String name = null;
            String contents;
            try {
                name = toString();
                contents = decoder.decodeToString(this);
            } catch (final RuntimeException e) {
                if (name == null) {
                    name = SOURCE_UNAVAILABLE_NAME;
                }
                contents = SOURCE_UNAVAILABLE_CONTENTS;
            }
            source = Source.newBuilder(SqueakLanguageConfig.ID, contents, name).mimeType("text/plain").build();
        }
        return source;
    }

    public int getSqueakContextSize() {
        return needsLargeFrame ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    public RootCallTarget getCallTarget() {
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeCallTargetUnsafe();
        }
        return callTarget;
    }

    private void renewCallTarget() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        callTargetStable.invalidate();
        initializeCallTargetUnsafe();
    }

    protected void initializeCallTargetUnsafe() {
        CompilerAsserts.neverPartOfCompilation();
        callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(SqueakLanguage.getContext().getLanguage(), this));
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    @TruffleBoundary
    public RootCallTarget getResumptionCallTarget(final ContextObject context) {
        if (resumptionCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            resumptionCallTarget = Truffle.getRuntime().createCallTarget(ResumeContextRootNode.create(SqueakLanguage.getContext().getLanguage(), context));
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

    public Assumption getDoesNotNeedSenderAssumption() {
        return doesNotNeedSender;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public FrameSlot getThisMarkerSlot() {
        return thisMarkerSlot;
    }

    public FrameSlot getThisContextSlot() {
        return thisContextSlot;
    }

    public FrameSlot getInstructionPointerSlot() {
        return instructionPointerSlot;
    }

    public FrameSlot getStackPointerSlot() {
        return stackPointerSlot;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public int getNumTemps() {
        return numTemps;
    }

    public int getNumLiterals() {
        return numLiterals;
    }

    public boolean getSignFlag() {
        return CompiledCodeHeaderDecoder.getSignFlag((long) literals[0]);
    }

    public FrameSlot getStackSlot(final int i) {
        assert 0 <= i && i < stackSlots.length : "Bad stack access";
        if (stackSlots[i] == null) {
            // Lazily add frame slots.
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackSlots[i] = frameDescriptor.addFrameSlot(i + 1, FrameSlotKind.Illegal);
        }
        return stackSlots[i];
    }

    public FrameSlot[] getStackSlotsUnsafe() {
        return stackSlots;
    }

    public int getNumStackSlots() {
        /**
         * Arguments and copied values are also pushed onto the stack in {@link EnterCodeNode},
         * therefore there must be enough slots for all these values as well as the Squeak stack.
         */
        return getSqueakContextSize();
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
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
    }

    public AbstractBytecodeNode[] asBytecodeNodesEmpty() {
        return new AbstractBytecodeNode[decoder.trailerPosition(this)];
    }

    public AbstractBytecodeNode bytecodeNodeAt(final VirtualFrame frame, final int pc) {
        return decoder.decodeBytecode(frame, this, pc);
    }

    public int findLineNumber(final int index) {
        return decoder.findLineNumber(this, index);
    }

    protected void decodeHeader() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final long header = (long) literals[0];
        numLiterals = CompiledCodeHeaderDecoder.getNumLiterals(header);
        hasPrimitive = CompiledCodeHeaderDecoder.getHasPrimitive(header);
        needsLargeFrame = CompiledCodeHeaderDecoder.getNeedsLargeFrame(header);
        numTemps = CompiledCodeHeaderDecoder.getNumTemps(header);
        numArgs = CompiledCodeHeaderDecoder.getNumArguments(header);
        decoder = getSignFlag() ? SqueakBytecodeV3PlusClosuresDecoder.SINGLETON : SqueakBytecodeSistaV1Decoder.SINGLETON;
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

    public void become(final CompiledCodeObject other) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        other.setLiteralsAndBytes(literals, bytes);
        setLiteralsAndBytes(literals2, bytes2);
        other.callTargetStable.invalidate();
        callTargetStable.invalidate();
    }

    public int getBytecodeOffset() {
        return (1 + numLiterals) * SqueakImageConstants.WORD_SIZE; // header plus numLiterals
    }

    public long at0(final long index) {
        final int offset = getBytecodeOffset();
        if (index < offset) {
            CompilerDirectives.transferToInterpreter();
            // FIXME: check bounds of compiled code objects
            throw new ArrayIndexOutOfBoundsException();
        } else {
            return Byte.toUnsignedLong(UnsafeUtils.getByte(bytes, index - offset));
        }
    }

    public void atput0(final long longIndex, final Object obj) {
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

    public Object getLiteral(final long longIndex) {
        return literals[(int) (1 + longIndex)]; // +1 for skipping header.
    }

    public void setLiteral(final long longIndex, final Object obj) {
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

    public boolean hasPrimitive() {
        return hasPrimitive;
    }

    public int primitiveIndex() {
        assert hasPrimitive() && bytes.length >= 3;
        return (Byte.toUnsignedInt(bytes[2]) << 8) + Byte.toUnsignedInt(bytes[1]);
    }

    public boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == 198;
    }

    public CompiledCodeObject shallowCopy() {
        return new CompiledCodeObject(this);
    }

    @Override
    public int getNumSlots() {
        return 1 /* header */ + getNumLiterals() + (int) Math.ceil((double) bytes.length / 8);
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return getBytecodeOffset() + bytes.length;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        if (isCompiledBlock()) {
            return "[] in " + getMethod().toString();
        } else {
            String className = "UnknownClass";
            String selector = "unknownSelector";
            final ClassObject methodClass = getMethodClassSlow();
            if (methodClass != null) {
                className = methodClass.getClassName();
            }
            final NativeObject selectorObj = getCompiledInSelector();
            if (selectorObj != null) {
                selector = selectorObj.asStringUnsafe();
            }
            return className + ">>" + selector;
        }
    }

    public Object[] getLiterals() {
        return literals;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public static long makeHeader(final boolean signFlag, final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) {
        return (signFlag ? 0 : 1) << 31 | (numArgs & 0x0F) << 24 | (numTemps & 0x3F) << 18 | numLiterals & 0x7FFF | (needsLargeFrame ? 0x20000 : 0) | (hasPrimitive ? 0x10000 : 0);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < getLiterals().length; j++) {
                if (fromPointer == getLiterals()[j]) {
                    final Object toPointer = to[i];
                    // FIXME: literals are @CompilationFinal, assumption needed (maybe
                    // pointersBecome should not modify literals at all?).
                    getLiterals()[j] = toPointer;
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        for (final Object literal : getLiterals()) {
            tracer.addIfUnmarked(literal);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceAllIfNecessary(getLiterals());
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        final int formatOffset = getNumSlots() * SqueakImageConstants.WORD_SIZE - size();
        assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
        if (writeHeader(writer, formatOffset)) {
            // Write header manually to ensure it is always written as SmallInteger.
            writer.writeLong((long) literals[0] << SqueakImageConstants.NUM_TAG_BITS | SqueakImageConstants.SMALL_INTEGER_TAG);
            for (int i = 1; i < literals.length; i++) {
                writer.writeObject(literals[i]);
            }
            writer.writeBytes(getBytes());
            final int byteOffset = getBytes().length % SqueakImageConstants.WORD_SIZE;
            if (byteOffset > 0) {
                writer.writePadding(SqueakImageConstants.WORD_SIZE - byteOffset);
            }
        }
    }

    /*
     * CompiledMethod
     */

    public boolean isCompiledMethod() {
        return getSqueakClass().isCompiledMethodClass();
    }

    /* Answer the program counter for the receiver's first bytecode. */
    public int getInitialPC() {
        // pc is offset by header + numLiterals, +1 for one-based addressing
        return getBytecodeOffset() + 1;
    }

    public NativeObject getCompiledInSelector() {
        /**
         *
         * By convention the penultimate literal of a method is either its selector or an instance
         * of AdditionalMethodState. AdditionalMethodState holds the method's selector and any
         * pragmas and properties of the method. AdditionalMethodState may also be used to add
         * instance variables to a method, albeit ones held in the method's AdditionalMethodState.
         * Subclasses of CompiledMethod that want to add state should subclass AdditionalMethodState
         * to add the state they want, and implement methodPropertiesClass on the class side of the
         * CompiledMethod subclass to answer the specialized subclass of AdditionalMethodState.
         * Enterprising programmers are encouraged to try and implement this support automatically
         * through suitable modifications to the compiler and class builder.
         */
        CompilerAsserts.neverPartOfCompilation("Do not use getCompiledInSelector() in compiled code");
        final Object penultimateLiteral = literals[literals.length - 2];
        if (penultimateLiteral instanceof NativeObject) {
            return (NativeObject) penultimateLiteral;
        } else if (penultimateLiteral instanceof VariablePointersObject) {
            final VariablePointersObject penultimateLiteralAsPointer = (VariablePointersObject) penultimateLiteral;
            assert penultimateLiteralAsPointer.size() >= ADDITIONAL_METHOD_STATE.SELECTOR;
            return (NativeObject) penultimateLiteralAsPointer.instVarAt0Slow(ADDITIONAL_METHOD_STATE.SELECTOR);
        } else {
            return null;
        }
    }

    /** CompiledMethod>>#methodClassAssociation. */
    private AbstractSqueakObject getMethodClassAssociation() {
        /**
         * From the CompiledMethod class description:
         *
         * The last literal in a CompiledMethod must be its methodClassAssociation, a binding whose
         * value is the class the method is installed in. The methodClassAssociation is used to
         * implement super sends. If a method contains no super send then its methodClassAssociation
         * may be nil (as would be the case for example of methods providing a pool of inst var
         * accessors).
         */
        return (AbstractSqueakObject) literals[literals.length - 1];
    }

    public boolean hasMethodClass(final AbstractPointersObjectReadNode readNode) {
        final AbstractSqueakObject mca = getMethodClassAssociation();
        return mca != NilObject.SINGLETON && readNode.execute((AbstractPointersObject) mca, CLASS_BINDING.VALUE) != NilObject.SINGLETON;
    }

    public ClassObject getMethodClassSlow() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (hasMethodClass(readNode)) {
            return getMethodClass(readNode);
        }
        return null;
    }

    /** CompiledMethod>>#methodClass. */
    public ClassObject getMethodClass(final AbstractPointersObjectReadNode readNode) {
        return (ClassObject) readNode.execute((AbstractPointersObject) getMethodClassAssociation(), CLASS_BINDING.VALUE);
    }

    public void setHeader(final long header) {
        literals = new Object[]{header};
        decodeHeader();
        literals = new Object[1 + numLiterals];
        literals[0] = header;
        for (int i = 1; i < literals.length; i++) {
            literals[i] = NilObject.SINGLETON;
        }
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }

    public boolean hasStoreIntoTemp1AfterCallPrimitive() {
        assert hasPrimitive;
        return decoder.hasStoreIntoTemp1AfterCallPrimitive(this);
    }

    /*
     * CompiledBlock
     */

    public boolean isCompiledBlock() {
        return !getSqueakClass().isCompiledMethodClass();
    }

    public CompiledCodeObject getMethod() {
        if (isCompiledMethod()) {
            return this;
        } else {
            return getMethodUnsafe();
        }
    }

    public CompiledCodeObject getMethodUnsafe() {
        assert !isCompiledMethod();
        return (CompiledCodeObject) literals[literals.length - 1];
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    protected boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    protected long getArraySize() {
        return literals.length;
    }

    @SuppressWarnings("static-method")
    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementInsertable")
    protected boolean isArrayElementReadable(final long index) {
        return 0 <= index && index < literals.length;
    }

    @ExportMessage
    protected Object readArrayElement(final long index) throws InvalidArrayIndexException {
        if (isArrayElementReadable(index)) {
            return literals[(int) index];
        } else {
            throw InvalidArrayIndexException.create(index);
        }
    }

    @ExportMessage
    protected void writeArrayElement(final long index, final Object value,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode) throws InvalidArrayIndexException {
        if (isArrayElementReadable(index)) {
            literals[(int) index] = wrapNode.executeWrap(value);
        } else {
            throw InvalidArrayIndexException.create(index);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasExecutableName() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    @TruffleBoundary
    public Object getExecutableName() {
        return toString();
    }

    @ExportMessage
    public Object execute(final Object[] receiverAndArguments,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode,
                    @Exclusive @Cached final DispatchUneagerlyNode dispatchNode) throws ArityException {
        final int actualArity = receiverAndArguments.length;
        final int expectedArity = 1 + getNumArgs(); // receiver + arguments
        if (actualArity == expectedArity) {
            return dispatchNode.executeDispatch(this, wrapNode.executeObjects(receiverAndArguments), InteropSenderMarker.SINGLETON);
        } else {
            throw ArityException.create(expectedArity, actualArity);
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

        private static boolean getSignFlag(final long headerWord) {
            return headerWord >= 0;
        }
    }
}

/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NonIdempotent;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ADDITIONAL_METHOD_STATE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_BINDING;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.ResumeContextRootNode;
import de.hpi.swa.trufflesqueak.nodes.StartContextRootNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractDecoder;
import de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractDecoder.ShadowBlockParams;
import de.hpi.swa.trufflesqueak.nodes.interpreter.DecoderSistaV1;
import de.hpi.swa.trufflesqueak.nodes.interpreter.DecoderV3PlusClosures;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives.PrimNoopNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

import static de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractDecoder.trailerPosition;

@SuppressWarnings("static-method")
public final class CompiledCodeObject extends AbstractSqueakObjectWithClassAndHash {
    private static final String SOURCE_UNAVAILABLE_NAME = "<unavailable>";
    public static final String SOURCE_UNAVAILABLE_CONTENTS = "Source unavailable";

    private static final DispatchPrimitiveNode UNINITIALIZED_PRIMITIVE_NODE = DispatchPrimitiveNode.create(new PrimNoopNode(), 0);

    // header info and data
    @CompilationFinal private int header;
    /*
     * TODO: literals and bytes can change (and probably more?) and should not be @CompilationFinal.
     * Literals are cached in the AST and bytes are represented by nodes, so this should not affect
     * performance. Find out why it does affect performance.
     */
    @CompilationFinal(dimensions = 1) private Object[] literals;
    @CompilationFinal(dimensions = 1) private byte[] bytes;

    private DispatchPrimitiveNode primitiveNodeOrNull = UNINITIALIZED_PRIMITIVE_NODE;

    private ExecutionData executionData;

    /**
     * Additional metadata that is only needed when the CompiledCodeObject is actually executed.
     * Moving these fields into CompiledCodeObject would unnecessarily bloat the size of every
     * instance that is not actually executed.
     */
    static final class ExecutionData {
        private FrameDescriptor frameDescriptor;

        /*
         * With FullBlockClosure support, CompiledMethods store CompiledBlocks in their literals and
         * CompiledBlocks their outer method in their last literal. For traditional BlockClosures,
         * we need to do something similar, but with CompiledMethods only (CompiledBlocks are not
         * used then). The next two fields are used to store "shadow blocks", which are copies of
         * the outer method with a new call target, and the outer method to be used for closure
         * activations.
         */
        private CompiledCodeObject outerMethod;
        private int outerMethodStartPC;

        private Source source;

        private RootCallTarget callTarget;
        private CyclicAssumption callTargetStable;
        private Assumption doesNotNeedThisContext;
        private RootCallTarget resumptionCallTarget;
    }

    public CompiledCodeObject(final SqueakImageChunk chunk) {
        super(chunk);
        // header is a tagged small integer
        final long headerWord = (chunk.getWord(0) >> SqueakImageConstants.NUM_TAG_BITS);
        header = CompiledCodeHeaderUtils.toInt(headerWord);
        literals = chunk.getPointers(1, getNumHeaderAndLiterals());
        bytes = Arrays.copyOfRange(chunk.getBytes(), getBytecodeOffset(), chunk.getBytes().length);
    }

    public CompiledCodeObject(final byte[] bytes, final long header, final Object[] literals, final ClassObject classObject) {
        super(classObject);
        this.header = CompiledCodeHeaderUtils.toInt(header);
        this.literals = literals;
        this.bytes = bytes;
    }

    public CompiledCodeObject(final CompiledCodeObject original) {
        super(original);
        if (original.hasExecutionData()) {
            getExecutionData().frameDescriptor = original.executionData.frameDescriptor;
        }
        setLiteralsAndBytes(original.header, original.literals.clone(), original.bytes.clone());
        primitiveNodeOrNull = original.primitiveNodeOrNull;
    }

    private CompiledCodeObject(final CompiledCodeObject outerCode, final int startPC) {
        super(outerCode);

        // store outer method and startPC
        final ExecutionData data = getExecutionData();
        data.outerMethod = outerCode.isShadowBlock() ? outerCode.getExecutionData().outerMethod : outerCode;
        assert data.outerMethod.isCompiledMethod();
        data.outerMethodStartPC = startPC;

        // header info and data
        header = outerCode.header;
        literals = outerCode.literals;
        bytes = outerCode.bytes;
    }

    private CompiledCodeObject(final int size, final ClassObject classObject) {
        super(classObject);
        bytes = new byte[size];
    }

    public static CompiledCodeObject newOfSize(final int size, final ClassObject classObject) {
        return new CompiledCodeObject(size, classObject);
    }

    private boolean hasExecutionData() {
        return executionData != null;
    }

    private ExecutionData getExecutionData() {
        if (!hasExecutionData()) {
            executionData = new ExecutionData();
        }
        return executionData;
    }

    /**
     * A shadow block works similar to a CompiledBlock (e.g., has an outerCode reference) and is
     * used when activating a traditional BlockClosure to ensure these activations use their own
     * call target. This is not needed for FullBlockClosures because they have their own
     * CompiledBlock instance.
     */
    @TruffleBoundary
    public CompiledCodeObject createShadowBlock(final int startPC) {
        return new CompiledCodeObject(this, startPC);
    }

    public boolean isShadowBlock() {
        return hasExecutionData() && executionData.outerMethod != null;
    }

    public int getOuterMethodStartPC() {
        assert isShadowBlock();
        return executionData.outerMethodStartPC;
    }

    public void setOuterMethodStartPC(final int pc) {
        assert isShadowBlock();
        executionData.outerMethodStartPC = pc;
        assert executionData.outerMethodStartPC > executionData.outerMethod.getInitialPC();
    }

    public int getOuterMethodStartPCZeroBased() {
        return getOuterMethodStartPC() - executionData.outerMethod.getInitialPC();
    }

    private void setLiteralsAndBytes(final int header, final Object[] literals, final byte[] bytes) {
        this.header = header;
        this.literals = literals;
        this.bytes = bytes;
        invalidateCallTargetStable("new literals and bytes");
    }

    public Source getSource() {
        CompilerAsserts.neverPartOfCompilation();
        if (getExecutionData().source == null) {
            String name = null;
            String contents;
            try {
                name = toString();
                contents = getDecoder().decodeToString(this);
            } catch (final RuntimeException e) {
                if (name == null) {
                    name = SOURCE_UNAVAILABLE_NAME;
                }
                contents = SOURCE_UNAVAILABLE_CONTENTS;
            }
            executionData.source = Source.newBuilder(SqueakLanguageConfig.ID, contents, name).mimeType("text/plain").build();
        }
        return executionData.source;
    }

    // used by CompiledMethod>>callTarget
    public RootCallTarget getCallTargetOrNull() {
        if (hasExecutionData()) {
            return executionData.callTarget;
        } else {
            return null;
        }
    }

    @TruffleBoundary
    public RootCallTarget getCallTarget() {
        if (getExecutionData().callTarget == null) {
            initializeCallTarget();
        }
        return executionData.callTarget;
    }

    @TruffleBoundary
    private void initializeCallTarget() {
        assert !(hasPrimitive() && PrimitiveNodeFactory.isNonFailing(this)) : "Should not create rood node for non failing primitives";
        executionData.callTarget = new StartContextRootNode(SqueakImageContext.getSlow(), this).getCallTarget();
    }

    private void invalidateCallTarget() {
        invalidateCallTarget("CompiledCodeObject modification");
    }

    private void invalidateCallTarget(final String reason) {
        if (hasExecutionData()) {
            if (executionData.callTargetStable != null) {
                executionData.callTargetStable.invalidate(reason);
            }
            executionData.callTarget = null;
        }
    }

    public void flushCache() {
        /* Invalidate callTargetStable assumption to ensure this method is released from caches. */
        invalidateCallTarget("primitive 116");
    }

    public void flushCacheBySelector() {
        /* Invalidate callTargetStable assumption to ensure this method is released from caches. */
        invalidateCallTarget("primitive 119");
    }

    @TruffleBoundary
    private CyclicAssumption callTargetStable() {
        if (getExecutionData().callTargetStable == null) {
            initializeCallTargetStable();
        }
        return executionData.callTargetStable;
    }

    @TruffleBoundary
    private void initializeCallTargetStable() {
        executionData.callTargetStable = new CyclicAssumption("CompiledCodeObject callTargetStable assumption");
    }

    private void invalidateCallTargetStable(final String reason) {
        if (hasExecutionData()) {
            if (executionData.callTargetStable != null) {
                executionData.callTargetStable.invalidate(reason);
            }
        }
    }

    public Assumption getCallTargetStable() {
        return callTargetStable().getAssumption();
    }

    @TruffleBoundary
    public Assumption getDoesNotNeedThisContextAssumption() {
        if (getExecutionData().doesNotNeedThisContext == null) {
            initializeDoesNotNeedThisContextAssumption();
        }
        return executionData.doesNotNeedThisContext;
    }

    @TruffleBoundary
    private void initializeDoesNotNeedThisContextAssumption() {
        if (isUnwindMarked() || isExceptionHandlerMarked()) {
            executionData.doesNotNeedThisContext = Assumption.NEVER_VALID;
        } else {
            executionData.doesNotNeedThisContext = Truffle.getRuntime().createAssumption("CompiledCodeObject doesNotNeedThisContext assumption");
        }
    }

    @TruffleBoundary
    public RootCallTarget getResumptionCallTarget(final ContextObject context) {
        if (getExecutionData().resumptionCallTarget == null) {
            executionData.resumptionCallTarget = new ResumeContextRootNode(SqueakImageContext.getSlow(), context).getCallTarget();
        } else {
            final ResumeContextRootNode resumeNode = (ResumeContextRootNode) executionData.resumptionCallTarget.getRootNode();
            /*
             * Set the activeContext of the {@link ResumeContextRootNode} to the given context to
             * reuse the call target. There may be methods with multiple suspension points. Since
             * the interrupt handler can trigger at all kinds of locations, it does not make sense
             * to create a resumption call target per instruction pointer.
             */
            resumeNode.setActiveContext(context);
        }
        return executionData.resumptionCallTarget;
    }

    public FrameDescriptor getFrameDescriptor() {
        CompilerAsserts.neverPartOfCompilation();
        if (getExecutionData().frameDescriptor == null) {
            final CompiledCodeObject exposedMethod;
            if (isShadowBlock()) {
                /* Never let shadow blocks escape, use their outer method instead. */
                exposedMethod = executionData.outerMethod;
            } else {
                exposedMethod = this;
            }
            executionData.frameDescriptor = FrameAccess.newFrameDescriptor(exposedMethod, getMaxNumStackSlots());
        }
        return executionData.frameDescriptor;
    }

    @Idempotent
    public int getNumArgs() {
        return CompiledCodeHeaderUtils.getNumArguments(header);
    }

    public int getNumTemps() {
        return CompiledCodeHeaderUtils.getNumTemps(header);
    }

    public int getNumLiterals() {
        return CompiledCodeHeaderUtils.getNumLiterals(header);
    }

    public int getNumHeaderAndLiterals() {
        return 1 + getNumLiterals();
    }

    public int getBytecodeOffset() {
        return getNumHeaderAndLiterals() * SqueakImageConstants.WORD_SIZE;
    }

    public int getSqueakContextSize() {
        return CompiledCodeHeaderUtils.getNeedsLargeFrame(header) ? CONTEXT.LARGE_FRAMESIZE : CONTEXT.SMALL_FRAMESIZE;
    }

    public int getMaxNumStackSlots() {
        if (bytes == null) {
            // When creating a new Context for a new Process, Smalltalk will initialize the Context
            // out of order (sender will be set before method) causing a frame to be created before
            // bytes is set in this object.
            return getSqueakContextSize();
        } else if (isShadowBlock()) {
            final int initialPC = getOuterMethodStartPCZeroBased();
            final ShadowBlockParams params = getDecoder().decodeShadowBlock(this, initialPC);
            return params.numArgs() + params.numCopied() + getDecoder().determineMaxNumStackSlots(this, initialPC, initialPC + params.blockSize());
        } else {
            return getNumTemps() + getDecoder().determineMaxNumStackSlots(this, 0, trailerPosition(this));
        }
    }

    public boolean getSignFlag() {
        return CompiledCodeHeaderUtils.getSignFlag(header);
    }

    private AbstractDecoder getDecoder() {
        CompilerAsserts.neverPartOfCompilation();
        return getSignFlag() ? DecoderV3PlusClosures.SINGLETON : DecoderSistaV1.SINGLETON;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        assert literals != null && bytes != null;
    }

    public void become(final CompiledCodeObject other) {
        final int header2 = other.header;
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        final CompiledCodeObject outerMethod2 = other.hasExecutionData() ? other.executionData.outerMethod : null;
        other.setLiteralsAndBytes(header, literals, bytes);
        if (hasExecutionData() && executionData.outerMethod != null) {
            other.getExecutionData().outerMethod = executionData.outerMethod;
        }
        other.invalidateCallTargetStable("become");
        setLiteralsAndBytes(header2, literals2, bytes2);
        if (outerMethod2 != null) {
            getExecutionData().outerMethod = outerMethod2;
        }
        other.invalidateCallTarget();
        invalidateCallTarget();
    }

    public long at0(final long index) {
        final int offset = getBytecodeOffset();
        if (index < offset) {
            throw PrimitiveFailed.BAD_INDEX;
        } else {
            return Byte.toUnsignedLong(bytes[(int) index - offset]);
        }
    }

    public void atput0(final long index, final long value) {
        final int offset = getBytecodeOffset();
        if (index < offset) {
            throw PrimitiveFailed.BAD_INDEX;
        } else {
            bytes[(int) index - offset] = (byte) value;
            invalidateCallTarget();
        }
    }

    public Object getLiteral(final long longIndex) {
        return UnsafeUtils.getObject(literals, longIndex);
    }

    public void setLiteral(final long longIndex, final Object obj) {
        UnsafeUtils.putObject(literals, longIndex, obj);
    }

    /** See storeLiteralVariable:withValue:. */
    public Object getAndResolveLiteral(final long longIndex) {
        final Object litVar = getLiteral(longIndex);
        if (litVar instanceof final AbstractSqueakObjectWithClassAndHash obj && !obj.isNotForwarded()) {
            CompilerDirectives.transferToInterpreter();
            final AbstractSqueakObjectWithClassAndHash forwarded = obj.getForwardingPointer();
            UnsafeUtils.putObject(literals, longIndex, forwarded);
            return forwarded;
        } else {
            return litVar;
        }
    }

    @Idempotent
    public boolean hasPrimitive() {
        return CompiledCodeHeaderUtils.getHasPrimitive(header);
    }

    public int primitiveIndex() {
        assert hasPrimitive() && bytes.length >= 3;
        return (Byte.toUnsignedInt(bytes[2]) << 8) + Byte.toUnsignedInt(bytes[1]);
    }

    @Idempotent
    @TruffleBoundary
    public DispatchPrimitiveNode getPrimitiveNodeOrNull() {
        if (primitiveNodeOrNull == UNINITIALIZED_PRIMITIVE_NODE) {
            initializePrimitiveNodeOrNull();
        }
        return primitiveNodeOrNull;
    }

    @TruffleBoundary
    private void initializePrimitiveNodeOrNull() {
        if (hasPrimitive()) {
            final AbstractPrimitiveNode nodeOrNull = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(this);
            if (nodeOrNull != null) {
                primitiveNodeOrNull = DispatchPrimitiveNode.create(nodeOrNull, getNumArgs());
            } else {
                primitiveNodeOrNull = null;
            }
        } else {
            primitiveNodeOrNull = null;
        }
    }

    public boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == PrimitiveNodeFactory.PRIMITIVE_ENSURE_MARKER_INDEX;
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == PrimitiveNodeFactory.PRIMITIVE_ON_DO_MARKER_INDEX;
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
        try {
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
                return (isShadowBlock() ? "[] embedded in " : "") + className + "#" + selector;
            }
        } catch (NullPointerException e) {
            return (isShadowBlock() ? "[] embedded in " : "") + "UnknownClass#unknownSelector";
        }
    }

    public Object[] getLiterals() {
        return literals;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean pointsTo(final Object thang) {
        if (thang instanceof Long value && value == header) {
            return true;
        }
        return ArrayUtils.contains(getLiterals(), thang);
    }

    @Override
    public void forwardTo(final AbstractSqueakObjectWithClassAndHash pointer) {
        super.forwardTo(pointer);
        invalidateCallTargetStable("forwarded");
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        for (int i = 0; i < literals.length; i++) {
            final Object replacement = fromToMap.get(literals[i]);
            if (replacement != null) {
                literals[i] = replacement;
                invalidateCallTarget("Literal changed via becomeForward:");
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        tracer.addAllIfUnmarked(literals);
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceAllIfNecessary(literals);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        final int numSlots = getNumSlots();
        final int formatOffset = numSlots * SqueakImageConstants.WORD_SIZE - size();
        assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
        if (writeHeader(writer, numSlots, formatOffset)) {
            final long headerWord = getHeader();
            writer.writeSmallInteger(headerWord);
            writer.writeObjects(literals);
            writer.writeBytes(bytes);
            final int byteOffset = bytes.length % SqueakImageConstants.WORD_SIZE;
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
        /*
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
        final Object penultimateLiteral = literals[getNumLiterals() - 2];
        if (penultimateLiteral instanceof final NativeObject o) {
            return o;
        } else if (penultimateLiteral instanceof final VariablePointersObject penultimateLiteralAsPointer) {
            assert penultimateLiteralAsPointer.size() >= ADDITIONAL_METHOD_STATE.SELECTOR;
            return (NativeObject) penultimateLiteralAsPointer.instVarAt0Slow(ADDITIONAL_METHOD_STATE.SELECTOR);
        } else {
            return null;
        }
    }

    /** CompiledMethod>>#methodClassAssociation. */
    private Object getMethodClassAssociation() {
        /*
         * From the CompiledMethod class description:
         *
         * The last literal in a CompiledMethod must be its methodClassAssociation, a binding whose
         * value is the class the method is installed in. The methodClassAssociation is used to
         * implement super sends. If a method contains no super send then its methodClassAssociation
         * may be nil (as would be the case for example of methods providing a pool of inst var
         * accessors).
         */
        return literals[getNumLiterals() - 1];
    }

    public boolean hasMethodClass(final AbstractPointersObjectReadNode readNode, final Node inlineTarget) {
        final Object mca = getMethodClassAssociation();
        if (mca instanceof final AbstractPointersObject apo) {
            return readNode.execute(inlineTarget, apo, CLASS_BINDING.VALUE) != NilObject.SINGLETON;
        } else {
            return false;
        }
    }

    public ClassObject getMethodClassSlow() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (hasMethodClass(readNode, null)) {
            final ClassObject methodClass = getMethodClass(readNode, null);
            if (methodClass.isNotForwarded()) {
                return methodClass;
            } else {
                final ClassObject forwardedMethodClass = (ClassObject) methodClass.getForwardingPointer();
                AbstractPointersObjectWriteNode.executeUncached((AbstractPointersObject) getMethodClassAssociation(), CLASS_BINDING.VALUE, forwardedMethodClass);
                return forwardedMethodClass;
            }
        }
        return null;
    }

    /** CompiledMethod>>#methodClass. */
    @NonIdempotent
    public ClassObject getMethodClass(final AbstractPointersObjectReadNode readNode, final Node inlineTarget) {
        return (ClassObject) readNode.execute(inlineTarget, (AbstractPointersObject) getMethodClassAssociation(), CLASS_BINDING.VALUE);
    }

    public long getHeader() {
        return header;
    }

    public void initializeHeader(final long value) {
        this.header = CompiledCodeHeaderUtils.toInt(value);
        literals = ArrayUtils.withAll(getNumLiterals(), NilObject.SINGLETON);
    }

    public void setHeader(final long value) {
        final int oldNumLiterals = getNumLiterals();
        header = CompiledCodeHeaderUtils.toInt(value);
        assert getNumLiterals() == oldNumLiterals;
        invalidateCallTarget();
    }

    public boolean hasStoreIntoTemp1AfterCallPrimitive() {
        assert hasPrimitive();
        return getDecoder().hasStoreIntoTemp1AfterCallPrimitive(this);
    }

    @TruffleBoundary
    public int pcPreviousTo(final int pc) {
        return getDecoder().pcPreviousTo(this, pc);
    }

    /*
     * CompiledBlock
     */

    public boolean isCompiledBlock() {
        return !isCompiledMethod();
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
        return (CompiledCodeObject) literals[getNumLiterals() - 1];
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
    public static final class CompiledCodeHeaderUtils {
        public static int makeHeader(final boolean signFlag, final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) {
            return numLiterals & 0x7FFF | (hasPrimitive ? 0x10000 : 0) | (needsLargeFrame ? 0x20000 : 0) | (numTemps & 0x3F) << 18 | (numArgs & 0x0F) << 24 | (signFlag ? 0 : Integer.MIN_VALUE);
        }

        private static int getNumLiterals(final int header) {
            return header & 0x7FFF;
        }

        private static boolean getHasPrimitive(final int header) {
            return (header & 0x10000) != 0;
        }

        private static boolean getNeedsLargeFrame(final int header) {
            return (header & 0x20000) != 0;
        }

        private static int getNumTemps(final int header) {
            return (header >> 18) & 0x3F;
        }

        private static int getNumArguments(final int header) {
            return (header >> 24) & 0x0F;
        }

        private static boolean getSignFlag(final int header) {
            return header >= 0;
        }

        // Convert 61bit to more compact 32bit representation
        private static int toInt(final long headerWord) {
            return (int) (headerWord | (headerWord < 0 ? Integer.MIN_VALUE : 0));
        }
    }
}

/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import static de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractDecoder.trailerPosition;

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

@SuppressWarnings("static-method")
public final class CompiledCodeObject extends AbstractSqueakObjectWithClassAndHash {
    private static final String SOURCE_UNAVAILABLE_NAME = "<unavailable>";
    public static final String SOURCE_UNAVAILABLE_CONTENTS = "Source unavailable";

    private static final DispatchPrimitiveNode UNINITIALIZED_PRIMITIVE_NODE = DispatchPrimitiveNode.create(new PrimNoopNode(), 0);

    // header info and data
    @CompilationFinal private long internalHeader;
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

        private ShadowBlockMetadata shadowBlockMetadata;

        private Source source;

        private RootCallTarget callTarget;
        private CyclicAssumption callTargetStable;
        private Assumption doesNotNeedThisContext;
        private RootCallTarget resumptionCallTarget;
    }

    /**
     * With FullBlockClosure support, CompiledMethods store CompiledBlocks in their literals and
     * CompiledBlocks their outer method in their last literal. For traditional BlockClosures, we
     * need to do something similar, but with CompiledMethods only (CompiledBlocks are not used
     * then). This class stores data for "shadow blocks", which are copies of the outer method with
     * a new call target, and the outer method to be used for closure activations.
     */
    static final class ShadowBlockMetadata {
        private final CompiledCodeObject outerMethod;
        private int outerMethodStartPC;

        private final ShadowBlockParams params;

        private ShadowBlockMetadata(final CompiledCodeObject outerCode, final int startPC) {
            outerMethod = outerCode.getOuterMethod();
            assert outerMethod.isCompiledMethod();
            outerMethodStartPC = startPC;
            params = outerMethod.getDecoder().decodeShadowBlock(outerMethod, startPC - outerMethod.getInitialPC());
        }

        private ShadowBlockMetadata(final CompiledCodeObject outerCode, final int startPC, final int numArgs, final int numCopied, final int blockSize) {
            outerMethod = outerCode.getOuterMethod();
            assert outerMethod.isCompiledMethod();
            outerMethodStartPC = startPC;
            params = new ShadowBlockParams(numArgs, numCopied, blockSize);
        }

        public int getOuterMethodStartPC() {
            return outerMethodStartPC;
        }

        public void setOuterMethodStartPC(final int pc) {
            outerMethodStartPC = pc;
            assert outerMethodStartPC > outerMethod.getInitialPC();
        }

        public int getOuterMethodStartPCZeroBased() {
            return outerMethodStartPC - outerMethod.getInitialPC();
        }

        public int getOuterMethodMaxPCZeroBased() {
            return getOuterMethodStartPCZeroBased() + params.blockSize();
        }

        public int getInitialSP() {
            return params.numArgs() + params.numCopied();
        }
    }

    public CompiledCodeObject(final SqueakImageChunk chunk) {
        super(chunk);
        // header is a tagged small integer
        final long headerWord = (chunk.getWord(0) >> SqueakImageConstants.NUM_TAG_BITS);
        internalHeader = CompiledCodeHeaderUtils.fromSmallIntegerValue(headerWord);
        literals = chunk.getPointers(1, getNumHeaderAndLiterals());
        bytes = Arrays.copyOfRange(chunk.getBytes(), getBytecodeOffset(), chunk.getBytes().length);
    }

    public CompiledCodeObject(final byte[] bytes, final long headerWord, final Object[] literals, final ClassObject classObject) {
        super(classObject);
        this.internalHeader = CompiledCodeHeaderUtils.fromSmallIntegerValue(headerWord);
        this.literals = literals;
        this.bytes = bytes;
    }

    public CompiledCodeObject(final CompiledCodeObject original) {
        super(original);
        if (original.hasExecutionData()) {
            getExecutionData().frameDescriptor = original.executionData.frameDescriptor;
        }
        setLiteralsAndBytes(original.internalHeader, original.literals.clone(), original.bytes.clone());
        primitiveNodeOrNull = original.primitiveNodeOrNull;
    }

    private CompiledCodeObject(final CompiledCodeObject outerCode, final ShadowBlockMetadata shadowBlockMetadata) {
        super(outerCode);
        CompilerAsserts.neverPartOfCompilation();

        // header info and data
        internalHeader = outerCode.internalHeader;
        literals = outerCode.literals;
        bytes = outerCode.bytes;

        // store outer method and startPC
        getExecutionData().shadowBlockMetadata = shadowBlockMetadata;
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

    @TruffleBoundary
    public CompiledCodeObject createShadowBlock(final int startPC) {
        return new CompiledCodeObject(this, new ShadowBlockMetadata(this, startPC));
    }

    public CompiledCodeObject createShadowBlock(final int startPC, final int numArgs, final int numCopied, final int blockSize) {
        return new CompiledCodeObject(this, new ShadowBlockMetadata(this, startPC, numArgs, numCopied, blockSize));
    }

    public boolean isShadowBlock() {
        return hasExecutionData() && executionData.shadowBlockMetadata != null;
    }

    private ShadowBlockMetadata getShadowBlockMetadata() {
        assert isShadowBlock();
        return executionData.shadowBlockMetadata;
    }

    public CompiledCodeObject getOuterMethod() {
        if (isShadowBlock()) {
            return getShadowBlockMetadata().outerMethod;
        } else {
            return this;
        }
    }

    public int getOuterMethodStartPC() {
        return getShadowBlockMetadata().getOuterMethodStartPC();
    }

    public void setOuterMethodStartPC(final int pc) {
        assert isShadowBlock();
        getShadowBlockMetadata().setOuterMethodStartPC(pc);
    }

    public int getShadowBlockNumArgs() {
        return getShadowBlockMetadata().params.numArgs();
    }

    private int getShadowBlockNumCopied() {
        return getShadowBlockMetadata().params.numCopied();
    }

    private int findCompiledBlockNumCopied() {
        assert isCompiledBlock() && !getHasV3PlusClosuresBytecodes();
        CompilerAsserts.neverPartOfCompilation();
        return DecoderSistaV1.decodeFullBlockClosureNumCopied(this);
    }

    private void setLiteralsAndBytes(final long internalHeader, final Object[] literals, final byte[] bytes) {
        this.internalHeader = internalHeader;
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
            /* Never let shadow blocks escape, use their outer method instead. */
            final CompiledCodeObject exposedMethod = isShadowBlock() ? executionData.shadowBlockMetadata.outerMethod : this;
            executionData.frameDescriptor = FrameAccess.newFrameDescriptor(exposedMethod, getMaxNumStackSlots());
        }
        return executionData.frameDescriptor;
    }

    @Idempotent
    public int getNumArgs() {
        return CompiledCodeHeaderUtils.getNumArguments(internalHeader);
    }

    /** Includes copied arguments from closures. */
    public int getNumArgsAndCopied() {
        CompilerAsserts.neverPartOfCompilation();
        if (isCompiledBlock()) {
            return getNumArgs() + findCompiledBlockNumCopied();
        } else if (isShadowBlock()) {
            return getShadowBlockNumArgs() + getShadowBlockNumCopied();
        } else {
            return getNumArgs(); // normal method
        }
    }

    public int getNumTemps() {
        return CompiledCodeHeaderUtils.getNumTemps(internalHeader);
    }

    public int getNumLiterals() {
        return CompiledCodeHeaderUtils.getNumLiterals(internalHeader);
    }

    public int getNumHeaderAndLiterals() {
        return 1 + getNumLiterals();
    }

    public int getBytecodeOffset() {
        return getNumHeaderAndLiterals() * SqueakImageConstants.WORD_SIZE;
    }

    public int getSqueakContextSize() {
        return CompiledCodeHeaderUtils.getSqueakContextSize(internalHeader);
    }

    public int getMaxStackSize() {
        return getSqueakContextSize();
    }

    public int getStartPCZeroBased() {
        return isShadowBlock() ? getShadowBlockMetadata().getOuterMethodStartPCZeroBased() : 0;
    }

    public int getMaxPCZeroBased() {
        return isShadowBlock() ? getShadowBlockMetadata().getOuterMethodMaxPCZeroBased() : trailerPosition(this);
    }

    public int getInitialSP() {
        return isShadowBlock() ? getShadowBlockMetadata().getInitialSP() : getNumTemps();
    }

    public int getMaxNumStackSlots() {
        return getDecoder().determineMaxNumStackSlots(this, getStartPCZeroBased(), getMaxPCZeroBased(), getInitialSP());
    }

    public boolean getHasV3PlusClosuresBytecodes() {
        return CompiledCodeHeaderUtils.getHasV3PlusClosuresBytecodes(internalHeader);
    }

    private AbstractDecoder getDecoder() {
        CompilerAsserts.neverPartOfCompilation();
        return getHasV3PlusClosuresBytecodes() ? DecoderV3PlusClosures.SINGLETON : DecoderSistaV1.SINGLETON;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        assert literals != null && bytes != null;
    }

    public void become(final CompiledCodeObject other) {
        final long internalHeader2 = other.internalHeader;
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        final ShadowBlockMetadata meta2 = other.hasExecutionData() ? other.executionData.shadowBlockMetadata : null;
        other.setLiteralsAndBytes(internalHeader, literals, bytes);
        if (hasExecutionData() && executionData.shadowBlockMetadata != null) {
            other.getExecutionData().shadowBlockMetadata = executionData.shadowBlockMetadata;
        }
        other.invalidateCallTargetStable("become");
        setLiteralsAndBytes(internalHeader2, literals2, bytes2);
        if (meta2 != null) {
            getExecutionData().shadowBlockMetadata = meta2;
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
        return CompiledCodeHeaderUtils.getHasPrimitive(internalHeader);
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
    }

    public Object[] getLiterals() {
        return literals;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean pointsTo(final Object thang) {
        if (thang instanceof Long value && value == getHeader()) {
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

    public boolean hasMethodClass(final AbstractPointersObjectReadNode readNode) {
        final Object mca = getMethodClassAssociation();
        if (mca instanceof final AbstractPointersObject apo) {
            return readNode.execute(apo, CLASS_BINDING.VALUE) != NilObject.SINGLETON;
        } else {
            return false;
        }
    }

    public ClassObject getMethodClassSlow() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (hasMethodClass(readNode)) {
            final ClassObject methodClass = getMethodClass(readNode);
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
    public ClassObject getMethodClass(final AbstractPointersObjectReadNode readNode) {
        return (ClassObject) readNode.execute((AbstractPointersObject) getMethodClassAssociation(), CLASS_BINDING.VALUE);
    }

    public long getHeader() {
        return CompiledCodeHeaderUtils.toSmallIntegerValue(internalHeader);
    }

    public void initializeHeader(final long value) {
        this.internalHeader = CompiledCodeHeaderUtils.fromSmallIntegerValue(value);
        literals = ArrayUtils.withAll(getNumLiterals(), NilObject.SINGLETON);
    }

    public void setHeader(final long value) {
        final int oldNumLiterals = getNumLiterals();
        internalHeader = CompiledCodeHeaderUtils.fromSmallIntegerValue(value);
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
     * IMAGE:
     *
     *   (index 0)      15 bits:   number of literals (#numLiterals)
     *   (index 15)      1 bit:    jit without counters - reserved for methods that have been optimized by Sista
     *   (index 16)      1 bit:    has primitive
     *   (index 17)      1 bit:    whether a large frame size is needed (#frameSize => either SmallFrame or LargeFrame)
     *   (index 18)      6 bits:   number of temporary variables (#numTemps)
     *   (index 24)      4 bits:   number of arguments to the method (#numArgs)
     *   (index 28)      2 bits:   reserved for an access modifier (00-unused, 01-private, 10-protected, 11-public), although accessors for bit 29 exist (see #flag).
     *   (index 30)      2 bits:   reserved, 0
     *
     *   (index 32)      5 bits:   reserved, 0
     *   (index 37)^     1 bit:    whether a huge frame is needed (frameSize = 128)
     *   (index 38)^     2 bits:   number of temporary variables extension upper bits
     *   (index 40)      4 bits:   reserved, 0
     *   (index 44)^     4 bits:   number of arguments extension upper bits
     *   (index 48)      12 bits:  reserved, 0
     *
     *   sign bit:       1 bit:    selects the instruction set, >= 0 Primary, < 0 Secondary (#signFlag)
     *
     * ^ These 3 fields are zero in vanilla Squeak and Cuis images. They are used to extend numArgs / numTemps.
     *
     * Since CompiledCode's hash includes the header value in the hash computation, we need to ensure that
     * the header value as it appears to Smalltalk is the same on TS and OSVM. We can do this by restricting the
     * value of the header as visible to Smalltalk to fall within OSVM's SmallInteger 61 bit range.
     *
     * Internally, we store a rearranged 64-bit header and arrange it back to the OSVM format when needed.
     *
     * CompiledCodeObject header:
     *
     *   (index 0)      16 bits:   number of literals (#numLiterals)
     *   (index 16)      8 bits:   number of temporary variables (#numTemps)
     *   (index 24)      8 bits:   number of arguments to the method (#numArgs)
     *   (index 32)      2 bits:   reserved for an access modifier, although accessors for upper bit exist.
     *   (index 34)      1 bit:    jit without counters - reserved for methods that have been optimized by Sista
     *   (index 35)      1 bit:    has primitive
     *   (index 36)      2 bits:   frame size: 00 = SmallFrame, 01 = LargeFrame, 1X = HugeFrame
     *   (index 38)      25 bits:  reserved, 0
     *   sign bit:       1 bit:    selects the instruction set, >= 0 Primary, < 0 Secondary (#signFlag)
     * </pre>
     */
    public static final class CompiledCodeHeaderUtils {

        // Frame size table indexed by the concatenation of hugeFrame and largeFrame bits.
        // HUGE_FRAMESIZE limits numArgs / numTemps to 127.
        private static final int[] FRAME_SIZES = {
                        CONTEXT.SMALL_FRAMESIZE,
                        CONTEXT.LARGE_FRAMESIZE,
                        CONTEXT.HUGE_FRAMESIZE,
                        CONTEXT.HUGE_FRAMESIZE
        };

        // Create an OSVM SmallInteger value header for an internally generated method.
        // Restricted to 15 arguments and 63 temporaries.
        public static long makeHeaderWord(final boolean hasV3PlusClosuresBytecodes, final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive,
                        final boolean needsLargeFrame) {
            return numLiterals & 0x7FFF | (hasPrimitive ? 0x10000 : 0) | (needsLargeFrame ? 0x20000 : 0) | (numTemps & 0x3F) << 18 | (numArgs & 0x0F) << 24 |
                            (hasV3PlusClosuresBytecodes ? 0 : SqueakImageConstants.SMALL_INTEGER_MIN_VAL);
        }

        private static int getNumLiterals(final long internalHeader) {
            // Mask to field size; size limited in image to 15 bits.
            return (int) internalHeader & 0xFFFF;
        }

        private static int getNumTemps(final long internalHeader) {
            // Mask to field size; size limited in image to 6 bits (7 bits with extension).
            return (int) (internalHeader >> 16) & 0xFF;
        }

        private static int getNumArguments(final long internalHeader) {
            // Mask to field size; size limited in image to 6 bits (7 bits with extension).
            return (int) (internalHeader >> 24) & 0xFF;
        }

        private static boolean getHasPrimitive(final long internalHeader) {
            return (internalHeader & 0x800000000L) != 0;
        }

        public static int getSqueakContextSize(final long internalHeader) {
            return FRAME_SIZES[(int) (internalHeader >> 36) & 0x3];
        }

        private static boolean getHasV3PlusClosuresBytecodes(final long internalHeader) {
            return internalHeader >= 0;
        }

        // Convert 61-or-more-bit SmallInteger into internal representation.
        public static long fromSmallIntegerValue(final long headerWord) {
            // @formatter:off
            return
                // 1. MASTER PASS-THROUGH (Shift = 0)
                // Includes: Sign (63), hugeFrame (37), numArgs low (24-27), numLiterals (0-14)
                (headerWord & 0x800000200F007FFFL) |

                // 2. SHARED EXTENSION SHIFT (Shift = -16)
                // Args Ext (44-47 -> 28-31) & Temps Ext (38-39 -> 22-23)
                ((headerWord & 0x0000F0C000000000L) >> 16) |

                // 3. FLAGS (Shift = +19)
                // [largeFrame | hasPrim | jit] (15-17 -> 34-36)
                ((headerWord & 0x38000L) << 19) |

                // 4. ACCESS (Shift = +4)
                // (28-29 -> 32-33)
                ((headerWord & 0x30000000L) << 4) |

                // 5. NUMTEMPS LOW (Shift = -2)
                // (18-23 -> 16-21)
                ((headerWord & 0xFC0000L) >> 2);
            // @formatter:on
        }

        // Convert internal representation back to 64-bit representation as a SmallInteger.
        public static long toSmallIntegerValue(final long internalHeader) {
            // @formatter:off
            return
                // 0. Sign bit with sign extension to 61-bits
                (internalHeader < 0 ? SqueakImageConstants.SMALL_INTEGER_MIN_VAL : 0) |

                // 1. MASTER PASS-THROUGH (Shift = 0)
                // HugeFrame (37), numArgs low (24-27), numLiterals (0-14)
                (internalHeader & 0x000000200F007FFFL) |

                // 2. SHARED EXTENSION SHIFT (Shift = +16)
                // Args Ext (28-31 -> 44-47) & Temps Ext (22-23 -> 38-39)
                ((internalHeader & 0x00000000F0C00000L) << 16) |

                // 3. FLAGS (Shift = -19)
                // [largeFrame | hasPrim | jit] (34-36 -> 15-17)
                ((internalHeader & 0x1C00000000L) >> 19) |

                // 4. ACCESS (Shift = -4)
                // (32-33 -> 28-29)
                ((internalHeader & 0x300000000L) >> 4) |

                // 5. NUMTEMPS LOW (Shift = +2)
                // (16-21 -> 18-23)
                ((internalHeader & 0x3F0000L) << 2);
            // @formatter:on
        }
    }
}

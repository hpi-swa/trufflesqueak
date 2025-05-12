/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;
import java.util.Deque;

import org.graalvm.collections.EconomicMap;

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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractSqueakBytecodeDecoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeSistaV1Decoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeV3PlusClosuresDecoder;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchPrimitiveNode;
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
    @CompilationFinal private int header;
    /*
     * TODO: literals and bytes can change (and probably more?) and should not be @CompilationFinal.
     * Literals are cached in the AST and bytes are represented by nodes, so this should not affect
     * performance. Find out why it does affect performance.
     */
    @CompilationFinal(dimensions = 1) private Object[] literals;
    @CompilationFinal(dimensions = 1) private byte[] bytes;

    @CompilationFinal private DispatchPrimitiveNode primitiveNodeOrNull = UNINITIALIZED_PRIMITIVE_NODE;

    @CompilationFinal private ExecutionData executionData;

    /**
     * Additional metadata that is only needed when the CompiledCodeObject is actually executed.
     * Moving these fields into CompiledCodeObject would unnecessarily bloat the size of every
     * instance that is not actually executed.
     */
    static final class ExecutionData {
        // frame info
        @CompilationFinal private FrameDescriptor frameDescriptor;

        /*
         * With FullBlockClosure support, CompiledMethods store CompiledBlocks in their literals and
         * CompiledBlocks their outer method in their last literal. For traditional BlockClosures,
         * we need to do something similar, but with CompiledMethods only (CompiledBlocks are not
         * used then). The next two fields are used to store "shadowBlocks", which are light copies
         * of the outer method with a new call target, and the outer method to be used for closure
         * activations.
         */
        private EconomicMap<Integer, CompiledCodeObject> shadowBlocks;
        @CompilationFinal private CompiledCodeObject outerMethod;

        private Source source;

        @CompilationFinal private RootCallTarget callTarget;
        @CompilationFinal private CyclicAssumption callTargetStable;
        @CompilationFinal private Assumption doesNotNeedSender;
        @CompilationFinal private RootCallTarget resumptionCallTarget;
    }

    @TruffleBoundary
    public CompiledCodeObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    public CompiledCodeObject(final SqueakImageContext image, final byte[] bytes, final long header, final Object[] literals, final ClassObject classObject) {
        super(image, classObject);
        this.header = CompiledCodeHeaderUtils.toInt(header);
        this.literals = literals;
        this.bytes = bytes;
    }

    private CompiledCodeObject(final CompiledCodeObject original) {
        super(original);
        if (original.hasExecutionData()) {
            getExecutionData().frameDescriptor = original.executionData.frameDescriptor;
        }
        setLiteralsAndBytes(original.header, original.literals.clone(), original.bytes.clone());
    }

    private CompiledCodeObject(final CompiledCodeObject outerCode, final int startPC) {
        super(outerCode);
        outerCode.getExecutionData().shadowBlocks.put(startPC, this);

        // Find outer method
        CompiledCodeObject currentOuterCode = outerCode;
        while (currentOuterCode.getExecutionData().outerMethod != null) {
            currentOuterCode = currentOuterCode.executionData.outerMethod;
        }
        assert currentOuterCode.isCompiledMethod();
        getExecutionData().outerMethod = currentOuterCode;

        // header info and data
        header = outerCode.header;
        literals = outerCode.literals;
        bytes = outerCode.bytes;
    }

    private CompiledCodeObject(final int size, final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
        bytes = new byte[size];
    }

    public static CompiledCodeObject newOfSize(final SqueakImageContext image, final int size, final ClassObject classObject) {
        return new CompiledCodeObject(size, image, classObject);
    }

    private boolean hasExecutionData() {
        return executionData != null;
    }

    private ExecutionData getExecutionData() {
        if (!hasExecutionData()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            executionData = new ExecutionData();
        }
        return executionData;
    }

    public CompiledCodeObject getOrCreateShadowBlock(final int startPC) {
        CompilerAsserts.neverPartOfCompilation();
        if (getExecutionData().shadowBlocks == null) {
            executionData.shadowBlocks = EconomicMap.create();
        }
        final CompiledCodeObject copy = executionData.shadowBlocks.get(startPC);
        if (copy == null) {
            return new CompiledCodeObject(this, startPC);
        } else {
            return copy;
        }
    }

    public boolean hasOuterMethod() {
        return hasExecutionData() && executionData.outerMethod != null;
    }

    private void setLiteralsAndBytes(final int header, final Object[] literals, final byte[] bytes) {
        this.header = header;
        this.literals = literals;
        this.bytes = bytes;
        callTargetStable().invalidate();
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

    public RootCallTarget getCallTarget() {
        if (getExecutionData().callTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final SqueakLanguage language = SqueakImageContext.getSlow().getLanguage();
            assert !(hasPrimitive() && PrimitiveNodeFactory.isNonFailing(this)) : "Should not create rood node for non failing primitives";
            executionData.callTarget = new StartContextRootNode(language, this).getCallTarget();
        }
        return executionData.callTarget;
    }

    private void invalidateCallTarget() {
        invalidateCallTarget("CompiledCodeObject modification");
    }

    private void invalidateCallTarget(final String reason) {
        if (hasExecutionData()) {
            callTargetStable().invalidate(reason);
            executionData.callTarget = null;
        }
    }

    public void flushCache() {
        /* Invalidate callTargetStable assumption to ensure this method is released from caches. */
        invalidateCallTarget("primitive 116");
    }

    private CyclicAssumption callTargetStable() {
        if (getExecutionData().callTargetStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            executionData.callTargetStable = new CyclicAssumption("CompiledCodeObject callTargetStable assumption");
        }
        return executionData.callTargetStable;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable().getAssumption();
    }

    public Assumption getDoesNotNeedSenderAssumption() {
        if (getExecutionData().doesNotNeedSender == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            executionData.doesNotNeedSender = Truffle.getRuntime().createAssumption("CompiledCodeObject doesNotNeedSender assumption");
        }
        return executionData.doesNotNeedSender;
    }

    @TruffleBoundary
    public RootCallTarget getResumptionCallTarget(final ContextObject context) {
        if (getExecutionData().resumptionCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            executionData.resumptionCallTarget = ResumeContextRootNode.create(SqueakImageContext.getSlow().getLanguage(), context).getCallTarget();
        } else {
            final ResumeContextRootNode resumeNode = (ResumeContextRootNode) executionData.resumptionCallTarget.getRootNode();
            if (resumeNode.getActiveContext() != context) {
                /**
                 * This is a trick: we set the activeContext of the {@link ResumeContextRootNode} to
                 * the given context to be able to reuse the call target.
                 */
                resumeNode.setActiveContext(context);
            }
        }
        return executionData.resumptionCallTarget;
    }

    public FrameDescriptor getFrameDescriptor() {
        if (getExecutionData().frameDescriptor == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* Never let synthetic compiled block escape, use outer method instead. */
            final CompiledCodeObject exposedMethod = executionData.outerMethod != null ? executionData.outerMethod : this;
            executionData.frameDescriptor = FrameAccess.newFrameDescriptor(exposedMethod);
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
        return getDecoder().determineMaxNumStackSlots(this);
    }

    public boolean getSignFlag() {
        return CompiledCodeHeaderUtils.getSignFlag(header);
    }

    private AbstractSqueakBytecodeDecoder getDecoder() {
        CompilerAsserts.neverPartOfCompilation();
        return getSignFlag() ? SqueakBytecodeV3PlusClosuresDecoder.SINGLETON : SqueakBytecodeSistaV1Decoder.SINGLETON;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // header is a tagged small integer
        final long headerWord = (chunk.getWord(0) >> SqueakImageConstants.NUM_TAG_BITS);
        header = CompiledCodeHeaderUtils.toInt(headerWord);
        assert literals == null;
        literals = chunk.getPointers(1, getNumHeaderAndLiterals());
        assert bytes == null;
        bytes = Arrays.copyOfRange(chunk.getBytes(), getBytecodeOffset(), chunk.getBytes().length);
    }

    public AbstractBytecodeNode[] asBytecodeNodesEmpty() {
        return new AbstractBytecodeNode[AbstractSqueakBytecodeDecoder.trailerPosition(this)];
    }

    public AbstractBytecodeNode bytecodeNodeAt(final VirtualFrame frame, final AbstractBytecodeNode[] bytecodeNodes, final int pc) {
        return getDecoder().decodeBytecode(frame, this, bytecodeNodes, pc);
    }

    public int findLineNumber(final int successorIndex) {
        return getDecoder().findLineNumber(this, successorIndex);
    }

    public void become(final CompiledCodeObject other) {
        final int header2 = other.header;
        final Object[] literals2 = other.literals;
        final byte[] bytes2 = other.bytes;
        final EconomicMap<Integer, CompiledCodeObject> shadowBlocks2 = other.hasExecutionData() ? other.executionData.shadowBlocks : null;
        final CompiledCodeObject outerMethod2 = other.hasExecutionData() ? other.executionData.outerMethod : null;
        other.setLiteralsAndBytes(header, literals, bytes);
        if (hasExecutionData() && (executionData.shadowBlocks != null || executionData.outerMethod != null)) {
            other.getExecutionData().shadowBlocks = executionData.shadowBlocks;
            other.executionData.outerMethod = executionData.outerMethod;
        }
        other.callTargetStable().invalidate();
        setLiteralsAndBytes(header2, literals2, bytes2);
        if (shadowBlocks2 != null || outerMethod2 != null) {
            getExecutionData().shadowBlocks = shadowBlocks2;
            executionData.outerMethod = outerMethod2;
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
        final int index = (int) longIndex;
        if (index == 0) {
            assert obj instanceof Long;
            final int oldNumLiterals = getNumLiterals();
            header = CompiledCodeHeaderUtils.toInt((long) obj);
            assert getNumLiterals() == oldNumLiterals;
        } else {
            literals[index - 1] = obj;
        }
        invalidateCallTarget();
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
    public DispatchPrimitiveNode getPrimitiveNodeOrNull() {
        if (primitiveNodeOrNull == UNINITIALIZED_PRIMITIVE_NODE) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
        return primitiveNodeOrNull;
    }

    public boolean isUnwindMarked() {
        return hasPrimitive() && primitiveIndex() == PrimitiveNodeFactory.PRIMITIVE_ENSURE_MARKER_INDEX;
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == PrimitiveNodeFactory.PRIMITIVE_ON_DO_MARKER_INDEX;
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
            return className + ">>#" + selector;
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
    public void allInstances(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result) {
        allInstancesAll(literals, currentMarkingFlag, result);
        if (hasExecutionData()) {
            allInstances(executionData.outerMethod, currentMarkingFlag, result);
            // Hide shadow blocks from image
        }
    }

    @Override
    public void allInstancesOf(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result, final ClassObject targetClass) {
        allInstancesOfAll(literals, currentMarkingFlag, result, targetClass);
        if (hasExecutionData()) {
            allInstancesOf(executionData.outerMethod, currentMarkingFlag, result, targetClass);
            // Hide shadow blocks from image
        }
    }

    @Override
    public void pointersBecomeOneWay(final boolean currentMarkingFlag, final Object[] from, final Object[] to) {
        final int literalsLength = literals.length;
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            final Object toPointer = to[i];
            for (int j = 0; j < literalsLength; j++) {
                if (literals[j] == fromPointer) {
                    // FIXME: literals are @CompilationFinal, assumption needed (maybe
                    // pointersBecome should not modify literals at all?).
                    literals[j] = toPointer;
                }
            }
            if (hasExecutionData() && fromPointer == executionData.outerMethod && toPointer instanceof final CompiledCodeObject o) {
                executionData.outerMethod = o;
            }
        }
        pointersBecomeOneWayAll(literals, currentMarkingFlag, from, to);
        if (hasExecutionData()) {
            pointersBecomeOneWay(executionData.outerMethod, currentMarkingFlag, from, to);
            // Migrate all shadow blocks
            if (executionData.shadowBlocks != null) {
                for (final CompiledCodeObject shadowBlock : executionData.shadowBlocks.getValues()) {
                    pointersBecomeOneWay(shadowBlock, currentMarkingFlag, from, to);
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        tracer.addAllIfUnmarked(literals);
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceAllIfNecessary(literals);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        final int formatOffset = getNumSlots() * SqueakImageConstants.WORD_SIZE - size();
        assert 0 <= formatOffset && formatOffset <= 7 : "too many odd bits (see instSpec)";
        if (writeHeader(writer, formatOffset)) {
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
        /**
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
            return getMethodClass(readNode, null);
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

    public void setHeader(final long header) {
        this.header = CompiledCodeHeaderUtils.toInt(header);
        literals = ArrayUtils.withAll(getNumLiterals(), NilObject.SINGLETON);
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

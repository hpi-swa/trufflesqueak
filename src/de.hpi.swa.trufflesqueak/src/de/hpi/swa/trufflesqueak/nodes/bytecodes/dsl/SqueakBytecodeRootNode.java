/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.AbstractStandardSendReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLDirectedSuperSendNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSend0Node;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSend1Node;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSend2Node;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSendNaryNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSendValueNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSendValueWithArgNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.DSLSendNodes.DSLSuperSendNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsInLoopNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsQuickNode.CheckForInterruptsQuickImplNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameAccess.DSLFrameInfo;

/**
 * Bytecode DSL interpreter for Squeak/Smalltalk methods and blocks. The same root node class (and
 * therefore the same generated interpreter) is used for the SistaV1 and the V3PlusClosures bytecode
 * sets: both are translated into the semantic operations defined here.
 *
 * <p>
 * Frame layout (see {@link DSLFrameInfo}): the first two DSL locals hold the {@link ContextObject}
 * (or {@code null}) and the externalized Squeak pc and sp. They are followed by one DSL local per
 * Squeak stack slot (temps and operand stack).
 */
@GenerateBytecode(//
                languageClass = SqueakLanguage.class, //
                enableUncachedInterpreter = true, //
                defaultUncachedThreshold = "UNCACHED_THRESHOLD", //
                boxingEliminationTypes = {long.class, double.class, boolean.class}, //
                enableBlockScoping = false, //
                defaultLocalValue = "NIL")
public abstract class SqueakBytecodeRootNode extends RootNode implements BytecodeRootNode {
    protected static final Object NIL = NilObject.SINGLETON;
    /* TODO: turn into a proper language option. */
    protected static final int UNCACHED_THRESHOLD = Integer.getInteger("trufflesqueak.bytecode-dsl.uncached-threshold", 16);

    private static final int CONTEXT_SLOT = DSLFrameInfo.THIS_CONTEXT;
    private static final int PC_AND_SP_SLOT = DSLFrameInfo.PC_AND_SP;
    private static final int STACK_START = DSLFrameInfo.STACK_START;

    private final DSLFrameInfo frameInfo;
    @CompilationFinal private CompiledCodeObject code;
    @CompilationFinal private Assumption doesNotNeedThisContext;
    @CompilationFinal private int numArgsAndCopied = -1;
    @CompilationFinal private boolean modifiesContext;
    /* Only used for resumption roots. */
    private ContextObject activeContext;

    protected SqueakBytecodeRootNode(final SqueakLanguage language, final FrameDescriptor.Builder builder) {
        this(language, builder, new DSLFrameInfo());
    }

    private SqueakBytecodeRootNode(final SqueakLanguage language, final FrameDescriptor.Builder builder, final DSLFrameInfo frameInfo) {
        super(language, builder.info(frameInfo).build());
        this.frameInfo = frameInfo;
    }

    public final void initialize(final CompiledCodeObject theCode, final boolean couldModifyContext) {
        CompilerAsserts.neverPartOfCompilation();
        code = theCode;
        doesNotNeedThisContext = theCode.getDoesNotNeedThisContextAssumption();
        modifiesContext = couldModifyContext;
        /* Never let shadow blocks escape, use their outer method instead. */
        frameInfo.initialize(theCode.getOuterMethod(), theCode.getMaxNumStackSlots());
    }

    public final CompiledCodeObject getCode() {
        return code;
    }

    public final ContextObject getActiveContext() {
        return activeContext;
    }

    public final void setActiveContext(final ContextObject context) {
        activeContext = context;
    }

    @Override
    public final String getName() {
        return toString();
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return String.valueOf(code);
    }

    /*
     * Exception interception
     */

    @Override
    public final Object interceptControlFlowException(final ControlFlowException ex, final VirtualFrame frame, final BytecodeNode bytecodeNode, final int bytecodeIndex) throws Throwable {
        if (ex instanceof NonVirtualReturn || ex instanceof ProcessSwitch || ex instanceof CannotReturnToTarget) {
            /* Make sure the context of this frame is available on the heap. */
            GetOrCreateContextWithFrameNode.executeUncached(frame);
        }
        exitContext(this);
        throw ex;
    }

    @Override
    public final Throwable interceptInternalException(final Throwable t, final VirtualFrame frame, final BytecodeNode bytecodeNode, final int bytecodeIndex) {
        exitContext(this);
        if (t instanceof final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            return SqueakImageContext.get(this).tryToSignalLowSpace(frame, e);
        }
        return t;
    }

    private static boolean isCountableStackFrame() {
        return CompilerDirectives.inInterpreter() || CompilerDirectives.inCompilationRoot();
    }

    private static void exitContext(final Node node) {
        if (isCountableStackFrame()) {
            SqueakImageContext.get(node).exitingContext();
        }
    }

    /*
     * Helpers shared by operations
     */

    private static void externalizePCAndSP(final VirtualFrame frame, final int pcAndSp) {
        frame.setLong(PC_AND_SP_SLOT, DSLFrameInfo.encodePCAndSP(pcAndSp & 0xFFFF, pcAndSp >>> 16));
    }

    public static int encodePCAndSP(final int pc, final int sp) {
        assert 0 <= pc && pc <= 0xFFFF && 0 <= sp && sp <= 0x7FFF;
        return sp << 16 | pc;
    }

    private static CannotReturnToTarget cannotReturn(final VirtualFrame frame, final Object returnValue) {
        CompilerDirectives.transferToInterpreter();
        throw new CannotReturnToTarget(returnValue, GetOrCreateContextWithFrameNode.executeUncached(frame));
    }

    @TruffleBoundary
    private static ContextObject firstUnwindMarkedOrThrowNLR(final AbstractSqueakObject senderOrNil, final ContextObject homeContext, final Object returnValue) {
        AbstractSqueakObject currentLink = senderOrNil;
        ContextObject firstMarkedContext = null;
        while (currentLink instanceof final ContextObject context) {
            if (context == homeContext) {
                if (firstMarkedContext == null) {
                    throw new NonLocalReturn(returnValue, homeContext);
                }
                return firstMarkedContext;
            }
            if (firstMarkedContext == null && context.isUnwindMarked()) {
                firstMarkedContext = context;
            }
            currentLink = context.getFrameSender();
        }
        return null;
    }

    @ExplodeLoop
    private static void copyArguments(final VirtualFrame frame, final int count) {
        CompilerAsserts.partialEvaluationConstant(count);
        final Object[] arguments = frame.getArguments();
        for (int i = 0; i < count; i++) {
            frame.setObject(STACK_START + i, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
    }

    private static void copyArgumentsSlow(final VirtualFrame frame, final int count) {
        final Object[] arguments = frame.getArguments();
        for (int i = 0; i < count; i++) {
            frame.setObject(STACK_START + i, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
    }

    private static void suspendAndSwitch(final VirtualFrame frame, final Node node) {
        CompilerDirectives.transferToInterpreter();
        final SqueakImageContext image = SqueakImageContext.get(node);
        final ContextObject activeContext = GetOrCreateContextWithFrameNode.executeUncached(frame);
        AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT, activeContext);
        throw ProcessSwitch.SINGLETON;
    }

    private static void ensureContextReceiverIsNotActive(final VirtualFrame frame, final SqueakBytecodeRootNode root) {
        if (root.modifiesContext && FrameAccess.getReceiver(frame) instanceof final ContextObject context && context.isPotentiallyActiveOnTruffleStack()) {
            context.markAsInactiveOnTruffleStack();
            if (context.isActuallyActiveOnTruffleStack()) {
                suspendAndSwitch(frame, root);
            }
        }
    }

    /*
     * ========================================================================================
     * Method entry
     * ========================================================================================
     */

    /** Sets up the frame of a fresh method or block activation. */
    @Operation
    @ConstantOperand(type = int.class, name = "initialPCAndSP")
    @ConstantOperand(type = boolean.class, name = "checkForInterrupts")
    public static final class EnterMethod {
        @Specialization
        static void doEnter(final VirtualFrame frame, final int initialPCAndSP, final boolean checkForInterrupts,
                        @Bind final SqueakBytecodeRootNode root) {
            frame.setObject(CONTEXT_SLOT, null);
            externalizePCAndSP(frame, initialPCAndSP);
            if (CompilerDirectives.inInterpreter() && !CompilerDirectives.isPartialEvaluationConstant(root)) {
                /* Uncached interpreter. */
                copyArgumentsSlow(frame, FrameAccess.getNumArguments(frame));
            } else {
                int count = root.numArgsAndCopied;
                if (count < 0) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    root.numArgsAndCopied = count = FrameAccess.getNumArguments(frame);
                }
                assert count == FrameAccess.getNumArguments(frame);
                copyArguments(frame, count);
            }
            if (!root.doesNotNeedThisContext.isValid()) {
                GetOrCreateContextWithFrameNode.executeUncached(frame);
            }
            if (isCountableStackFrame() && SqueakImageContext.get(root).enteringContextExceedsDepth()) {
                suspendAndSwitch(frame, root);
            }
            if (checkForInterrupts) {
                CheckForInterruptsQuickImplNode.create().execute(frame);
            }
            ensureContextReceiverIsNotActive(frame, root);
        }
    }

    /**
     * Sets up the frame of a resumption root from its {@link ContextObject}: the frame of the
     * context is copied into the new frame, which then becomes the frame of the context.
     */
    @Operation
    public static final class ResumeContext {
        @Specialization
        static void doResume(final VirtualFrame frame,
                        @Bind final SqueakBytecodeRootNode root) {
            final ContextObject context = root.activeContext;
            assert !context.isDead() : "Terminated contexts cannot be resumed";
            transferFrame(context, frame.materialize(), root.frameInfo.getNumStackSlots());
            context.markAsPotentiallyActiveOnTruffleStack();
            context.clearModifiedSender();
            if (isCountableStackFrame()) {
                SqueakImageContext.get(root).enteringContextExceedsDepth();
            }
            ensureContextReceiverIsNotActive(frame, root);
        }

        @TruffleBoundary
        private static void transferFrame(final ContextObject context, final MaterializedFrame target, final int numStackSlots) {
            final MaterializedFrame source = context.getTruffleFrame();
            assert source.getArguments() == target.getArguments() : "Resumption roots must be called with the arguments of the context's frame";
            final int sp = FrameAccess.getStackPointer(source);
            target.setLong(PC_AND_SP_SLOT, DSLFrameInfo.encodePCAndSP(FrameAccess.getInstructionPointer(source), sp));
            final int limit = Math.min(Math.min(sp, numStackSlots), FrameAccess.getNumStackSlots(source));
            for (int i = 0; i < limit; i++) {
                final Object value = FrameAccess.getStackValue(source, i);
                target.setObject(STACK_START + i, value == null ? NilObject.SINGLETON : value);
            }
            target.setObject(CONTEXT_SLOT, context);
            context.replaceTruffleFrame(target);
        }
    }

    /** Used by the resume dispatch at the beginning of resumption roots and their loops. */
    @Operation
    @ConstantOperand(type = int.class, name = "pc")
    public static final class ResumePCLessThan {
        @Specialization
        static boolean doCheck(final VirtualFrame frame, final int pc) {
            return (int) frame.getLong(PC_AND_SP_SLOT) < pc;
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class CheckForInterruptsInLoop {
        @Specialization
        static void doCheck(final VirtualFrame frame, final int pcAndSp) {
            CheckForInterruptsInLoopNode.createForLoopUnchecked().execute(frame, pcAndSp & 0xFFFF, pcAndSp >>> 16);
        }
    }

    /*
     * ========================================================================================
     * Pushes
     * ========================================================================================
     */

    /** Follows forwarding pointers of values read from temporaries. */
    @Operation
    public static final class Follow {
        @Specialization
        static long doLong(final long value) {
            return value;
        }

        @Specialization
        static double doDouble(final double value) {
            return value;
        }

        @Specialization
        static boolean doBoolean(final boolean value) {
            return value;
        }

        @Specialization(replaces = {"doLong", "doDouble", "doBoolean"})
        static Object doObject(final Object value,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile followProfile) {
            return DSLSendNodes.followForwarded(value, node, followProfile);
        }
    }

    @Operation
    public static final class PushReceiver {
        @Specialization
        static Object doPush(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile followProfile) {
            return DSLSendNodes.followForwarded(FrameAccess.getReceiver(frame), node, followProfile);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "index")
    public static final class PushReceiverVariable {
        @Specialization
        static Object doPush(final VirtualFrame frame, final int index,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node,
                        @Cached final InlinedConditionProfile followProfile) {
            return DSLSendNodes.followForwarded(at0Node.execute(node, FrameAccess.getReceiver(frame), index), node, followProfile);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "index")
    public static final class PushLiteralConstant {
        @Specialization
        static Object doPush(final int index,
                        @Bind final SqueakBytecodeRootNode root) {
            return root.code.getAndResolveLiteral(index);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "index")
    public static final class PushLiteralVariable {
        @Specialization
        static Object doPush(final int index,
                        @Bind final SqueakBytecodeRootNode root,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node) {
            final Object literal = root.code.getAndResolveLiteral(index);
            final Object value = at0Node.execute(node, literal, ASSOCIATION.VALUE);
            if (value instanceof final AbstractSqueakObjectWithClassAndHash obj && !obj.isNotForwarded()) {
                CompilerDirectives.transferToInterpreter();
                return resolveAndPatch(literal, obj);
            }
            return value;
        }

        @TruffleBoundary
        private static Object resolveAndPatch(final Object literal, final AbstractSqueakObjectWithClassAndHash obj) {
            final Object resolved = obj.resolveForwardingPointer();
            AbstractPointersObjectWriteNode.executeUncached((AbstractPointersObject) literal, ASSOCIATION.VALUE, resolved);
            return resolved;
        }
    }

    @Operation
    public static final class PushActiveContext {
        @Specialization
        static Object doPush(final VirtualFrame frame) {
            return GetOrCreateContextWithFrameNode.executeUncached(frame);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "size")
    public static final class PushNewArray {
        @Specialization
        static Object doPush(final int size,
                        @Bind final SqueakBytecodeRootNode root) {
            final Object[] values = new Object[size];
            java.util.Arrays.fill(values, NilObject.SINGLETON);
            return ArrayObject.createWithStorage(SqueakImageContext.get(root).arrayClass, values);
        }
    }

    @Operation
    public static final class PushNewArrayWithValues {
        @Specialization
        static Object doPush(@Variadic final Object[] values,
                        @Bind final SqueakBytecodeRootNode root) {
            return ArrayObject.createWithStorage(SqueakImageContext.get(root).arrayClass, values);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "indexInVector")
    public static final class PushRemoteTemp {
        @Specialization
        static Object doPush(final int indexInVector, final Object tempVector,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node,
                        @Cached final InlinedConditionProfile followProfile) {
            return DSLSendNodes.followForwarded(at0Node.execute(node, tempVector, indexInVector), node, followProfile);
        }
    }

    /** Full block closure (SistaV1); the receiver is the receiver of the current frame. */
    @Operation
    @ConstantOperand(type = int.class, name = "literalIndex")
    @ConstantOperand(type = boolean.class, name = "ignoreOuterContext")
    public static final class PushFullClosure {
        @Specialization
        static Object doPush(final VirtualFrame frame, final int literalIndex, final boolean ignoreOuterContext, @Variadic final Object[] copiedValues,
                        @Bind final SqueakBytecodeRootNode root) {
            final CompiledCodeObject block = (CompiledCodeObject) root.code.getLiteral(literalIndex);
            final ContextObject outerContext = ignoreOuterContext ? null : GetOrCreateContextWithFrameNode.executeUncached(frame);
            return new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    /** Full block closure (SistaV1) with an explicit receiver. */
    @Operation
    @ConstantOperand(type = int.class, name = "literalIndex")
    @ConstantOperand(type = boolean.class, name = "ignoreOuterContext")
    public static final class PushFullClosureWithReceiver {
        @Specialization
        static Object doPush(final VirtualFrame frame, final int literalIndex, final boolean ignoreOuterContext, final Object receiver, @Variadic final Object[] copiedValues,
                        @Bind final SqueakBytecodeRootNode root) {
            final CompiledCodeObject block = (CompiledCodeObject) root.code.getLiteral(literalIndex);
            final ContextObject outerContext = ignoreOuterContext ? null : GetOrCreateContextWithFrameNode.executeUncached(frame);
            return new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext);
        }
    }

    /** Embedded ("shadow") block closure. */
    @Operation
    @ConstantOperand(type = CompiledCodeObject.class, name = "shadowBlock")
    public static final class PushClosure {
        @Specialization
        static Object doPush(final VirtualFrame frame, final CompiledCodeObject shadowBlock, @Variadic final Object[] copiedValues) {
            final ContextObject outerContext = GetOrCreateContextWithFrameNode.executeUncached(frame);
            return new BlockClosureObject(true, shadowBlock, shadowBlock.getShadowBlockNumArgs(), copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    @Operation
    public static final class PushErrorObject {
        @Specialization
        @TruffleBoundary
        static Object doPush(@Bind final SqueakBytecodeRootNode root) {
            final SqueakImageContext image = SqueakImageContext.get(root);
            final int primFailCode = image.getPrimFailCode();
            final ArrayObject errorTable = image.primitiveErrorTable;
            if (primFailCode < errorTable.getObjectLength()) {
                return errorTable.getObject(primFailCode);
            } else {
                return (long) primFailCode;
            }
        }
    }

    /*
     * ========================================================================================
     * Stores
     * ========================================================================================
     */

    @Operation
    @ConstantOperand(type = int.class, name = "index")
    public static final class StoreReceiverVariable {
        @Specialization
        static void doStore(final VirtualFrame frame, final int index, final Object value,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, FrameAccess.getReceiver(frame), index, value);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "index")
    public static final class StoreLiteralVariable {
        @Specialization
        static void doStore(final int index, final Object value,
                        @Bind final SqueakBytecodeRootNode root,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, root.code.getAndResolveLiteral(index), ASSOCIATION.VALUE, value);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "indexInVector")
    public static final class StoreRemoteTemp {
        @Specialization
        static void doStore(final int indexInVector, final Object tempVector, final Object value,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, tempVector, indexInVector, value);
        }
    }

    /*
     * ========================================================================================
     * Sends
     * ========================================================================================
     */

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class Send0 {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver,
                        @Cached final DSLSend0Node sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class Send1 {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    private static Object send1(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1, final DSLSend1Node sendNode) {
        externalizePCAndSP(frame, pcAndSp);
        return sendNode.execute(frame, selector, receiver, arg1);
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class Send2 {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1, final Object arg2,
                        @Cached final DSLSend2Node sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver, arg1, arg2);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class SendN {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, @Variadic final Object[] arguments,
                        @Cached final DSLSendNaryNode sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver, arguments);
        }
    }

    @Operation(forceCached = true)
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class SuperSend {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, @Variadic final Object[] arguments,
                        @Bind final SqueakBytecodeRootNode root,
                        @Cached final DSLSuperSendNode sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, root.code, selector, receiver, arguments);
        }
    }

    @Operation(forceCached = true)
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class DirectedSuperSend {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object lookupClass, final Object receiver, @Variadic final Object[] arguments,
                        @Cached final DSLDirectedSuperSendNode sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, ((ClassObject) lookupClass).getResolvedSuperclass(), receiver, arguments);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class SendValue {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver,
                        @Cached final DSLSendValueNode sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class SendValueWithArg {
        @Specialization
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSendValueWithArgNode sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver, arg1);
        }
    }

    /*
     * ========================================================================================
     * Special selectors with fast paths
     * ========================================================================================
     */

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimAdd {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        static Object doLongWithOverflow(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs,
                        @Bind final SqueakBytecodeRootNode root) {
            return LargeIntegers.add(SqueakImageContext.get(root), lhs, rhs);
        }

        @Specialization
        static double doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs + rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimSubtract {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        static Object doLongWithOverflow(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs,
                        @Bind final SqueakBytecodeRootNode root) {
            return LargeIntegers.subtract(SqueakImageContext.get(root), lhs, rhs);
        }

        @Specialization
        static double doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs - rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimMultiply {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        static Object doLongWithOverflow(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs,
                        @Bind final SqueakBytecodeRootNode root) {
            return LargeIntegers.multiply(SqueakImageContext.get(root), lhs, rhs);
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    /** Smalltalk {@code //} (floored division). */
    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimDiv {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return Math.floorDiv(lhs, rhs);
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }

        static boolean isOverflowDivision(final long lhs, final long rhs) {
            return lhs == Long.MIN_VALUE && rhs == -1;
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimBitAnd {
        @Specialization
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs & rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimBitOr {
        @Specialization
        static long doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs | rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimLessThan {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs < rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs < rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimGreaterThan {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs > rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs > rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimLessOrEqual {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs <= rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs <= rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimGreaterOrEqual {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs >= rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs >= rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimEqual {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs == rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs == rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimNotEqual {
        @Specialization
        static boolean doLong(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final long lhs, final long rhs) {
            return lhs != rhs;
        }

        @Specialization
        static boolean doDouble(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final double lhs, final double rhs) {
            return lhs != rhs;
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver, final Object arg1,
                        @Cached final DSLSend1Node sendNode) {
            return send1(frame, selector, pcAndSp, receiver, arg1, sendNode);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class, name = "selector")
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class PrimSize {
        @Specialization(guards = "isByteString(root, receiver)")
        static long doByteString(@SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final int pcAndSp, final NativeObject receiver,
                        @SuppressWarnings("unused") @Bind final SqueakBytecodeRootNode root) {
            return receiver.getByteLength();
        }

        @Fallback
        static Object doSend(final VirtualFrame frame, final NativeObject selector, final int pcAndSp, final Object receiver,
                        @Cached final DSLSend0Node sendNode) {
            externalizePCAndSP(frame, pcAndSp);
            return sendNode.execute(frame, selector, receiver);
        }

        static boolean isByteString(final SqueakBytecodeRootNode root, final NativeObject receiver) {
            return SqueakImageContext.get(root).isByteString(receiver);
        }
    }

    @Operation
    public static final class PrimIdentical {
        @Specialization
        static boolean doIdentical(final Object receiver, final Object arg1,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(node, receiver, arg1);
        }
    }

    @Operation
    public static final class PrimNotIdentical {
        @Specialization
        static boolean doNotIdentical(final Object receiver, final Object arg1,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectIdentityNode identityNode) {
            return !identityNode.execute(node, receiver, arg1);
        }
    }

    @Operation
    public static final class PrimClass {
        @Specialization
        static Object doClass(final Object receiver,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }
    }

    /*
     * ========================================================================================
     * Control flow
     * ========================================================================================
     */

    @Operation
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class IsTrue {
        @Specialization
        static boolean doBoolean(@SuppressWarnings("unused") final int pcAndSp, final boolean value) {
            return value;
        }

        @Fallback
        static boolean doMustBeBoolean(final VirtualFrame frame, final int pcAndSp, final Object value,
                        @Bind final SqueakBytecodeRootNode root) {
            throw sendMustBeBoolean(frame, pcAndSp, value, root);
        }
    }

    @Operation
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class IsFalse {
        @Specialization
        static boolean doBoolean(@SuppressWarnings("unused") final int pcAndSp, final boolean value) {
            return !value;
        }

        @Fallback
        static boolean doMustBeBoolean(final VirtualFrame frame, final int pcAndSp, final Object value,
                        @Bind final SqueakBytecodeRootNode root) {
            throw sendMustBeBoolean(frame, pcAndSp, value, root);
        }
    }

    private static SqueakException sendMustBeBoolean(final VirtualFrame frame, final int pcAndSp, final Object value, final SqueakBytecodeRootNode root) {
        CompilerDirectives.transferToInterpreter();
        externalizePCAndSP(frame, pcAndSp);
        final SqueakImageContext image = SqueakImageContext.get(root);
        image.mustBeBooleanSelector.executeAsSymbolSlow(image, frame, value);
        throw SqueakException.create("Should not be reached");
    }

    /**
     * Return from a method, or local return from a block (i.e., return to the sender).
     */
    @Operation
    public static final class NormalReturn {
        @Specialization
        static Object doReturn(final VirtualFrame frame, final Object value,
                        @Bind final SqueakBytecodeRootNode root) {
            if (FrameAccess.hasModifiedSender(frame)) {
                final AbstractSqueakObject sender = FrameAccess.getSender(frame);
                if (sender instanceof final ContextObject ctx && !ctx.isDead()) {
                    throw new NonVirtualReturn(value, sender);
                } else {
                    throw cannotReturn(frame, value);
                }
            } else {
                FrameAccess.terminateFrame(frame);
                exitContext(root);
                return value;
            }
        }
    }

    /**
     * Method return ("^") executed in a block: returns to the sender of the block's home context.
     */
    @Operation
    @ConstantOperand(type = int.class, name = "pcAndSp")
    public static final class NonLocalReturnFromBlock {
        @Specialization
        static Object doReturn(final VirtualFrame frame, final int pcAndSp, final Object value,
                        @Bind final SqueakBytecodeRootNode root,
                        @Cached final DSLSend2Node aboutToReturnNode) {
            externalizePCAndSP(frame, pcAndSp);
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canReturnToSender()) {
                final ContextObject firstMarkedContext = firstUnwindMarkedOrThrowNLR(FrameAccess.getSender(frame), homeContext, value);
                if (firstMarkedContext != null) {
                    aboutToReturnNode.execute(frame, SqueakImageContext.get(root).aboutToReturnSelector, GetOrCreateContextWithFrameNode.executeUncached(frame), value, firstMarkedContext);
                }
            }
            throw cannotReturn(frame, value);
        }
    }
}

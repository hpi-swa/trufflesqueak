/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import static de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode.ensureFinite;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.BytecodeConfig;
import com.oracle.truffle.api.bytecode.BytecodeLabel;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeParser;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.EpilogReturn;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.bytecode.StoreBytecodeIndex;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.exceptions.Returns.AbstractStandardSendReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.Dispatch0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectedSuperNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchSuperNaryNode;
import de.hpi.swa.trufflesqueak.nodes.interpreter.InterpreterSistaV1Node;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsInLoopNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/** SistaV1 translation using DSL-managed locals, structured loops, and bytecode locations. */
@ImportStatic({SqueakGuards.class, SmalltalkSistaV1Interpreter.class})
@GenerateBytecode(//
                languageClass = SqueakLanguage.class, //
                defaultLocalValue = "defaultLocalValue()", //
                enableUncachedInterpreter = true, //
                enableTailCallHandlers = true, //
                enableBlockScoping = false, //
                storeBytecodeIndexInFrame = true, //
                boxingEliminationTypes = {long.class, double.class, boolean.class})
public abstract class SmalltalkSistaV1Interpreter extends RootNode implements BytecodeRootNode {
    private final SistaV1Frame frameInfo;
    @CompilerDirectives.CompilationFinal private boolean isBlock;
    @CompilerDirectives.CompilationFinal private int countableLocal;
    @CompilerDirectives.CompilationFinal private Assumption doesNotNeedThisContext;
    @CompilerDirectives.CompilationFinal(dimensions = 1) private Object[] restoredValues;
    @CompilerDirectives.CompilationFinal private ContextObject resumedContext;
    @Child private CheckForInterruptsQuickNode interruptNode;
    @Child private CheckForInterruptsInLoopNode loopInterruptNode;

    protected SmalltalkSistaV1Interpreter(final SqueakLanguage language, final FrameDescriptor.Builder descriptor) {
        super(language, descriptor.info(new SistaV1Frame()).build());
        frameInfo = (SistaV1Frame) getFrameDescriptor().getInfo();
    }

    @Override
    public String getName() {
        return frameInfo.code() == null ? "SistaV1" : frameInfo.code().toString();
    }

    @Override
    public Object interceptControlFlowException(final ControlFlowException exception, final VirtualFrame frame, final BytecodeNode bytecode, final int bci) throws Throwable {
        GetOrCreateContextWithFrameNode.executeUncached(frame);
        if (exception instanceof Returns.NonLocalReturn) {
            frameInfo.state(frame).dead = true;
        }
        leave(frame);
        throw exception;
    }

    @Override
    public Throwable interceptInternalException(final Throwable exception, final VirtualFrame frame, final BytecodeNode bytecode, final int bci) {
        leave(frame);
        return exception;
    }

    private void leave(final VirtualFrame frame) {
        if (Boolean.TRUE.equals(getBytecodeNode().getLocalValue(0, frame, countableLocal))) {
            getBytecodeNode().setLocalValue(0, frame, countableLocal, false);
            SqueakImageContext.get(this).exitingContext();
        }
    }

    private Object returnValue(final VirtualFrame frame, final Object value, final boolean local) {
        if (!local && isBlock) {
            final ContextObject home = FrameAccess.getClosure(frame).getHomeContext();
            if (home.canReturnToSender()) {
                AbstractSqueakObject link = FrameAccess.getSender(frame);
                ContextObject unwind = null;
                while (link instanceof final ContextObject context) {
                    if (context == home) {
                        if (unwind == null) {
                            throw new Returns.NonLocalReturn(value, home);
                        }
                        CompilerDirectives.transferToInterpreter();
                        SqueakImageContext.get(this).aboutToReturnSelector.executeAsSymbolSlow(SqueakImageContext.get(this), frame,
                                        GetOrCreateContextWithFrameNode.executeUncached(frame), value, unwind);
                        break;
                    }
                    if (unwind == null && context.isUnwindMarked()) {
                        unwind = context;
                    }
                    link = context.getFrameSender();
                }
            }
            throw new CannotReturnToTarget(value, GetOrCreateContextWithFrameNode.executeUncached(frame));
        }
        if (FrameAccess.hasModifiedSender(frame)) {
            final AbstractSqueakObject sender = FrameAccess.getSender(frame);
            if (sender instanceof final ContextObject context && !context.isDead()) {
                throw new NonVirtualReturn(value, sender);
            }
            throw new CannotReturnToTarget(value, GetOrCreateContextWithFrameNode.executeUncached(frame));
        }
        final SistaV1Frame.ContextState state = frameInfo.existingState(frame);
        if (state != null) {
            state.dead = true;
        }
        FrameAccess.setSender(frame, NilObject.SINGLETON);
        return value;
    }

    private static Object followForwarded(final Object value) {
        if (value instanceof final AbstractSqueakObjectWithClassAndHash object && !object.isNotForwarded()) {
            CompilerDirectives.transferToInterpreter();
            return object.resolveForwardingPointer();
        }
        return value;
    }

    private static Object handleSendReturn(final VirtualFrame frame, final AbstractStandardSendReturn result) {
        if (result.targetIsFrame(frame)) {
            return followForwarded(result.getReturnValue());
        }
        throw result;
    }

    @EpilogReturn(storeBytecodeIndex = false)
    public static final class Leave {
        @Specialization
        static Object perform(final VirtualFrame frame, final Object result, @Bind final SmalltalkSistaV1Interpreter root) {
            root.leave(frame);
            return result;
        }
    }

    @Operation(storeBytecodeIndex = true)
    public static final class CheckLoop {
        @Specialization
        static void perform(final VirtualFrame frame, @Bind final SmalltalkSistaV1Interpreter root) {
            if (root.loopInterruptNode != null) {
                root.loopInterruptNode.execute(frame);
            }
        }
    }

    @Operation(storeBytecodeIndex = true)
    public static final class CheckContextMutation {
        @Specialization
        static void perform(final VirtualFrame frame, @Bind final SmalltalkSistaV1Interpreter root) {
            if (FrameAccess.getReceiver(frame) instanceof final ContextObject context && context.isPotentiallyActiveOnTruffleStack()) {
                context.markAsInactiveOnTruffleStack();
                if (context.isActuallyActiveOnTruffleStack()) {
                    final SqueakImageContext image = SqueakImageContext.get(root);
                    AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT,
                                    GetOrCreateContextWithFrameNode.executeUncached(frame));
                    throw ProcessSwitch.SINGLETON;
                }
            }
        }
    }

    @Operation(storeBytecodeIndex = false)
    public static final class IsCountable {
        @Specialization
        static boolean perform() {
            return CompilerDirectives.inInterpreter() || CompilerDirectives.inCompilationRoot();
        }
    }

    @Operation(storeBytecodeIndex = true)
    public static final class Enter {
        @Specialization
        static void perform(final VirtualFrame frame, final boolean countable, @Bind final SmalltalkSistaV1Interpreter root) {
            if (root.resumedContext != null) {
                root.frameInfo.state(frame).context = root.resumedContext;
                root.resumedContext.resumeWithTruffleFrame(frame.materialize());
                root.resumedContext.markAsPotentiallyActiveOnTruffleStack();
                root.resumedContext.clearModifiedSender();
            }
            if (!root.doesNotNeedThisContext.isValid()) {
                GetOrCreateContextWithFrameNode.executeUncached(frame);
            }
            final SqueakImageContext image = SqueakImageContext.get(root);
            if (countable) {
                if (image.enteringContextExceedsDepth()) {
                    final ContextObject context = GetOrCreateContextWithFrameNode.executeUncached(frame);
                    AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT, context);
                    throw ProcessSwitch.SINGLETON;
                }
            }
            root.interruptNode.execute(frame);
        }
    }

    static Object defaultLocalValue() {
        CompilerAsserts.neverPartOfCompilation("Must be cached and not triggered during compilation.");
        return NilObject.SINGLETON;
    }

    @Operation(storeBytecodeIndex = false)
    @ConstantOperand(type = int.class)
    public static final class InitialTemporary {
        @Specialization
        static Object perform(final VirtualFrame frame, final int index, @Bind final SmalltalkSistaV1Interpreter root) {
            if (root.restoredValues != null) {
                return root.restoredValues[index];
            }
            final int argumentIndex = FrameAccess.getArgumentStartIndex() + index;
            return argumentIndex < frame.getArguments().length ? frame.getArguments()[argumentIndex] : NilObject.SINGLETON;
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = int.class)
    public static final class StoreIntoReceiverVariable {
        @Specialization
        static void perform(final VirtualFrame frame,
                        final int variableIndex,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, FrameAccess.getReceiver(frame), variableIndex, value);
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = Object.class)
    @ConstantOperand(type = int.class)
    public static final class StoreIntoLiteralVariable {
        @Specialization
        static void perform(final Object literal,
                        final int variableIndex,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, literal, variableIndex, value);
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = int.class)
    public static final class LoadRemoteTemp {
        @Specialization
        static Object perform(final int indexInArray,
                        final Object temp,
                        @Bind final Node inlineTarget,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node) {
            return followForwarded(at0Node.execute(inlineTarget, temp, indexInArray));
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = int.class)
    public static final class StoreIntoRemoteTemp {
        @Specialization
        static void perform(final int indexInArray,
                        final Object temp,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached(inline = true) final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, temp, indexInArray, value);
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = int.class)
    public static final class ReceiverVariable {
        @Specialization
        static Object perform(final VirtualFrame frame,
                        final int variableIndex,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node) {
            return followForwarded(at0Node.execute(node, FrameAccess.getReceiver(frame), variableIndex));
        }
    }

    @Operation(storeBytecodeIndex = true)
    @ConstantOperand(type = Object.class)
    public static final class LiteralVariable {
        @Specialization
        static Object perform(final Object literal,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectAt0Node at0Node) {
            return followForwarded(at0Node.execute(node, literal, ObjectLayouts.ASSOCIATION.VALUE));
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    public static final class ActiveContext {
        @Specialization
        static ContextObject perform(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached(inline = true) final GetOrCreateContextWithFrameNode contextNode) {
            return contextNode.executeGet(frame, node);
        }
    }

    @Operation(storeBytecodeIndex = false)
    @ConstantOperand(type = CompiledCodeObject.class)
    public static final class FullClosure {
        @Specialization
        static BlockClosureObject perform(final CompiledCodeObject block, final Object receiver, final ContextObject outerContext,
                        @Variadic final Object[] copiedValues) {
            return new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext);
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = CompiledCodeObject.class)
    public static final class Closure {
        @Specialization
        static BlockClosureObject perform(final VirtualFrame frame, final CompiledCodeObject block, @Variadic final Object[] copiedValues,
                        @Bind final Node node,
                        @Cached(inline = true) final GetOrCreateContextWithFrameNode getOrCreateContextNode) {
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame, node);
            return new BlockClosureObject(true, block, block.getShadowBlockNumArgs(), copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    @Operation(storeBytecodeIndex = true)
    public static final class ReturnTopFromMethod {
        @Specialization
        static Object perform(final VirtualFrame frame, final Object value, @Bind final SmalltalkSistaV1Interpreter root) {
            return root.returnValue(frame, value, false);
        }
    }

    @Operation(storeBytecodeIndex = true)
    public static final class ReturnTopFromClosure {
        @Specialization
        static Object perform(final VirtualFrame frame, final Object value, @Bind final SmalltalkSistaV1Interpreter root) {
            return root.returnValue(frame, value, true);
        }
    }

    @Operation(storeBytecodeIndex = false)
    @ConstantOperand(type = int.class)
    public static final class NewArray {
        @Specialization
        static ArrayObject perform(final int arraySize,
                        @Bind final Node location) {
            final SqueakImageContext image = SqueakImageContext.get(location);
            return image.asArrayOfObjects(ArrayUtils.withAll(arraySize, NilObject.SINGLETON));
        }
    }

    @Operation(storeBytecodeIndex = false)
    public static final class NewArrayFromStack {
        @Specialization
        static ArrayObject perform(@Variadic final Object[] values,
                        @Bind final Node location) {
            return SqueakImageContext.get(location).asArrayOfObjects(values);
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendNilary {
        @Specialization
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver,
                        @Cached("create(selector)") final Dispatch0Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendUnary {
        @Specialization
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1,
                        @Cached("create(selector)") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, arg1));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendBinary {
        @Specialization
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Cached("create(selector)") final Dispatch2Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, arg1, arg2));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    static NativeObject getSpecialSelector(final int index) {
        return SqueakImageContext.getSlow().getSpecialSelector(index);
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendNary {
        @Specialization
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver, @Variadic final Object[] args,
                        @Cached("create(selector)") final DispatchNaryNode dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, args));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = CompiledCodeObject.class)
    @ConstantOperand(type = NativeObject.class)
    public static final class SuperSend {
        @Specialization
        static Object perform(final VirtualFrame frame, final CompiledCodeObject code, final NativeObject selector, final Object receiver, @Variadic final Object[] args,
                        @Cached("create(code, selector)") final DispatchSuperNaryNode dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, args));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = true)
    @ConstantOperand(type = NativeObject.class)
    public static final class DirectedSuperSend {
        @Specialization
        static Object perform(final VirtualFrame frame, final NativeObject selector, final ClassObject lookupClass, final Object receiver, @Variadic final Object[] args,
                        @Cached("create(selector)") final DispatchDirectedSuperNaryNode dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, lookupClass.getResolvedSuperclass(), receiver, args));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }
    }

    @Operation(storeBytecodeIndex = false)
    public static final class PrimitiveError {
        @Specialization
        static Object perform(@Bind final SqueakImageContext image) {
            final int error = image.getPrimFailCode();
            return error < image.primitiveErrorTable.getObjectLength() ? image.primitiveErrorTable.getObject(error) : (long) error;
        }
    }

    @Operation(storeBytecodeIndex = false)
    public static final class Not {
        @Specialization
        static boolean perform(final boolean value) {
            return !value;
        }

        @Fallback
        @StoreBytecodeIndex
        static boolean mustBeBoolean(final VirtualFrame frame, final Object value, @Bind final SqueakImageContext image) {
            CompilerDirectives.transferToInterpreter();
            image.mustBeBooleanSelector.executeAsSymbolSlow(image, frame, value);
            throw SqueakException.create("mustBeBoolean returned");
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static SmalltalkSistaV1Interpreter build(final CompiledCodeObject code) {
        final var rootNodes = SmalltalkSistaV1InterpreterGen.create(SqueakLanguage.get(null), BytecodeConfig.WITH_SOURCE, new SistaV1BytecodeParser(code, -1, -1));
        return rootNodes.getNode(0);
    }

    @CompilerDirectives.TruffleBoundary
    public static SmalltalkSistaV1Interpreter resume(final ContextObject context) {
        final CompiledCodeObject code = context.getMethodOrBlock();
        final int pc = context.getInstructionPointerForBytecodeLoop();
        final int sp = context.getStackPointer();
        final Object[] values = new Object[code.getMaxNumStackSlots()];
        java.util.Arrays.fill(values, NilObject.SINGLETON);
        for (int i = 0; i < sp; i++) {
            values[i] = FrameAccess.getStackValue(context.getTruffleFrame(), i);
        }
        final SmalltalkSistaV1Interpreter root = SmalltalkSistaV1InterpreterGen.create(SqueakLanguage.get(null), BytecodeConfig.WITH_SOURCE,
                        new SistaV1BytecodeParser(code, pc, sp)).getNode(0);
        root.restoredValues = values;
        root.resumedContext = context;
        return root;
    }

    static class SistaV1BytecodeParser implements BytecodeParser<SmalltalkSistaV1InterpreterGen.Builder> {
        // Bound recursive expression emission for large, generated methods.
        private static final int MAX_EXPRESSION_DEPTH = 32;

        private final CompiledCodeObject code;
        private final BytecodeLocal[] temporarySlots;
        private final BytecodeLocal[] stackSlots;
        // A null entry means that the current value is already in its canonical stack local.
        private final StackEntry[] stackValues;
        private int currentOpcode;
        private final Map<Integer, Integer> jumpStackPointers = new HashMap<>();
        private Map<Integer, Integer> loopLocations;
        private Map<Integer, BytecodeLabel> jumpLocations;
        private BytecodeLocal resuming;

        private EmissionState emissionState;
        private int index;
        private int sp;
        private int observedSP;
        private final int resumePC;
        private final int resumeSP;
        private final Map<Integer, SistaV1Frame.Location> locations = new HashMap<>();

        SistaV1BytecodeParser(final CompiledCodeObject code, final int resumePC, final int resumeSP) {
            this.code = code;
            this.resumePC = resumePC;
            this.resumeSP = resumeSP;
            temporarySlots = new BytecodeLocal[code.getInitialSP()];
            stackSlots = new BytecodeLocal[code.getMaxNumStackSlots()];
            stackValues = new StackEntry[stackSlots.length];
        }

        @Override
        public void parse(final SmalltalkSistaV1InterpreterGen.Builder b) {
            b.beginRoot();
            b.beginSource(Source.newBuilder("smalltalk", " ".repeat(code.getBytes().length + 1), code.toString()).build());
            b.beginBlock();
            detectJumps(b);
            if (resumePC >= 0) {
                jumpLocations.putIfAbsent(resumePC, null);
                for (final int loopStart : loopLocations.keySet()) {
                    if (loopStart <= resumePC && resumePC < loopLocations.get(loopStart)) {
                        jumpLocations.putIfAbsent(loopStart, null);
                    }
                }
            }
            createLabels(b, -1);
            final BytecodeLocal contextState = b.createLocal();
            for (int i = 0; i < temporarySlots.length; i++) {
                temporarySlots[i] = b.createLocal();
                stackSlots[i] = temporarySlots[i];
            }

            for (int i = code.getInitialSP(); i < stackSlots.length; i++) {
                stackSlots[i] = b.createLocal();
            }
            for (int i = 0; i < temporarySlots.length; i++) {
                b.beginStoreLocal(temporarySlots[i]);
                b.emitInitialTemporary(i);
                b.endStoreLocal();
            }
            if (resumePC >= 0) {
                for (int i = code.getInitialSP(); i < stackSlots.length; i++) {
                    b.beginStoreLocal(stackSlots[i]);
                    b.emitInitialTemporary(i);
                    b.endStoreLocal();
                }
            }
            final BytecodeLocal countable = b.createLocal();
            b.beginStoreLocal(countable);
            b.emitIsCountable();
            b.endStoreLocal();
            b.beginEnter();
            b.emitLoadLocal(countable);
            b.endEnter();
            if (resumePC >= 0) {
                resuming = b.createLocal();
                b.beginStoreLocal(resuming);
                b.emitLoadConstant(true);
                b.endStoreLocal();
                jumpStackPointers.put(resumePC, resumeSP);
                b.emitBranch(jumpLocations.get(entryTarget(-1)));
            }
            index = code.getStartPCZeroBased();
            sp = code.getInitialSP();
            final int trailerPosition = code.getMaxPCZeroBased();

            while (index < trailerPosition) {
                final boolean isLoopStart = loopLocations.containsKey(index);
                if (isLoopStart || jumpLocations.containsKey(index)) {
                    spillStack(b);
                }
                if (isLoopStart && jumpLocations.containsKey(index)) {
                    b.emitLabel(jumpLocations.get(index));
                    finishResumeEntry(b);
                }
                if (isLoopStart) {
                    /*
                     * Keep the original Sista control flow inside an infinite structured loop.
                     * Forward conditional jumps leave the loop through their regular labels; the
                     * backward jump itself is represented by endWhile(). A Sista loop condition
                     * may span several bytecodes, so it must not be inferred from the first
                     * bytecode at the back-jump target.
                     */
                    b.beginWhile();
                    b.emitLoadConstant(true);
                    b.beginBlock();
                    createLabels(b, index);
                    if (resumePC > index && resumePC < loopLocations.get(index)) {
                        b.beginIfThen();
                        b.emitLoadLocal(resuming);
                        b.emitBranch(jumpLocations.get(entryTarget(index)));
                        b.endIfThen();
                    }
                }
                final BytecodeLabel jumpLabel = jumpLocations.get(index);
                if (jumpLabel != null && !isLoopStart) {
                    final Integer jumpStackPointer = jumpStackPointers.get(index);
                    if (jumpStackPointer != null) {
                        sp = jumpStackPointer;
                    }
                    b.emitLabel(jumpLabel);
                    finishResumeEntry(b);
                }
                final int size = instructionSize(index);
                final int start = index;
                observedSP = -1;
                b.beginSourceSection(index, size);
                index += translateBytecode(b, 0, 0, 0, 0);
                b.endSourceSection();
                locations.putIfAbsent(start, new SistaV1Frame.Location(index, observedSP < 0 ? sp : observedSP));
                final boolean isLoopEnd = loopLocations.containsValue(index);
                if (isLoopEnd) {
                    spillStack(b);
                    for (final var loop : loopLocations.entrySet()) {
                        if (loop.getValue() == index) {
                            locations.put(start, new SistaV1Frame.Location(loop.getKey(), sp));
                        }
                    }
                    b.beginSourceSection(start, instructionSize(start));
                    b.emitCheckLoop();
                    b.endSourceSection();
                    b.endBlock();
                    b.endWhile();
                }
            }
            b.endBlock();
            b.endSource();
            final SmalltalkSistaV1Interpreter root = b.endRoot();
            root.countableLocal = countable.getLocalOffset();
            root.isBlock = code.isCompiledBlock() || code.isShadowBlock();
            final int[] localOffsets = new int[stackSlots.length];
            for (int i = 0; i < localOffsets.length; i++) {
                localOffsets[i] = (i < temporarySlots.length ? temporarySlots[i] : stackSlots[i]).getLocalOffset();
            }
            root.frameInfo.initialize(root, code, localOffsets, locations, new SistaV1Frame.Location(resumePC >= 0 ? resumePC : code.getStartPCZeroBased(), resumeSP >= 0 ? resumeSP
                            : code.getInitialSP()), contextState.getLocalOffset());
            root.doesNotNeedThisContext = code.getDoesNotNeedThisContextAssumption();
            root.interruptNode = CheckForInterruptsQuickNode.createForSend(code);
            root.loopInterruptNode = CheckForInterruptsInLoopNode.createForLoop();
        }

        private void detectJumps(final SmalltalkSistaV1InterpreterGen.Builder b) {
            if (jumpLocations != null) {
                return;
            }
            loopLocations = new HashMap<>();
            jumpLocations = new HashMap<>();
            index = code.getStartPCZeroBased();
            final int trailerPosition = code.getMaxPCZeroBased();
            while (index < trailerPosition) {
                index += detectJumps(b, 0, 0, 0, 0);
            }
        }

        private int enclosingLoop(final int target) {
            int enclosing = -1;
            for (final var loop : loopLocations.entrySet()) {
                if (loop.getKey() < target && target < loop.getValue() && loop.getKey() > enclosing) {
                    enclosing = loop.getKey();
                }
            }
            return enclosing;
        }

        private void createLabels(final SmalltalkSistaV1InterpreterGen.Builder b, final int scope) {
            for (final int target : jumpLocations.keySet()) {
                if (enclosingLoop(target) == scope) {
                    jumpLocations.put(target, b.createLabel());
                }
            }
        }

        private int entryTarget(final int scope) {
            int target = resumePC;
            for (final var loop : loopLocations.entrySet()) {
                if (loop.getKey() > scope && loop.getKey() < target && resumePC < loop.getValue() && enclosingLoop(loop.getKey()) == scope) {
                    target = loop.getKey();
                }
            }
            return target;
        }

        private void finishResumeEntry(final SmalltalkSistaV1InterpreterGen.Builder b) {
            if (index == resumePC) {
                b.beginStoreLocal(resuming);
                b.emitLoadConstant(false);
                b.endStoreLocal();
            }
        }

        private int instructionSize(final int start) {
            int pc = start;
            int extB = 0;
            int numExtB = 0;
            while (getUnsignedInt(pc) == 0xE0 || getUnsignedInt(pc) == 0xE1) {
                if (getUnsignedInt(pc) == 0xE1) {
                    final int value = getUnsignedInt(pc + 1);
                    extB = numExtB++ == 0 && value > 127 ? value - 256 : (extB << 8) + value;
                }
                pc += 2;
            }
            final int op = getUnsignedInt(pc);
            final int size = op <= 223 ? 1 : op <= 247 ? 2 : 3;
            return pc - start + size + (op == 0xFA ? getUnsignedInt(pc + 2) + (extB << 8) : 0);
        }

        private byte getByte(final int index) {
            return code.getBytes()[index];
        }

        private int getUnsignedInt(final int index) {
            return Byte.toUnsignedInt(getByte(index));
        }

        private int detectJumps(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int numExtB) {
            final int indexWithExt = index + extBytes;
            final int op = getUnsignedInt(indexWithExt);
            switch (op) {
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> recordUnconditionalJump(b, 1 + InterpreterSistaV1Node.calculateShortOffset(op));
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, /* JumpOnTrue */
                    0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 /* JumpOnFalse */
                    -> recordConditionalJump(b, 1 + InterpreterSistaV1Node.calculateShortOffset(op));

                case 0xE0 -> {
                    return detectJumps(b, extBytes + 2, (extA << 8) + getUnsignedInt(indexWithExt + 1), extB, numExtB);
                }
                case 0xE1 -> {
                    final int byteValue = getUnsignedInt(indexWithExt + 1);
                    return detectJumps(b, extBytes + 2, extA, numExtB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue, numExtB + 1);
                }

                case 0xED -> recordUnconditionalJump(b, 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB));
                case 0xEE /* JumpOnTrue */, 0xEF /* JumpOnFalse */ -> recordConditionalJump(b, 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB));
                case 0xFA -> {
                    final int blockSize = getUnsignedInt(indexWithExt + 2) + (extB << 8);
                    return 3 + extBytes + blockSize;
                }
                default -> {
                    /* not a jump */
                }
            }
            if (op <= 223) {
                return 1 + extBytes;
            } else if (op <= 247) {
                return 2 + extBytes;
            } else {
                return 3 + extBytes;
            }
        }

        private void recordUnconditionalJump(final SmalltalkSistaV1InterpreterGen.Builder b, final int offset) {
            assert offset != 0;
            final int jumpTarget = index + offset;
            if (jumpTarget > index) {
                if (!jumpLocations.containsKey(jumpTarget)) {

                    jumpLocations.put(jumpTarget, null);
                }
            } else {

                loopLocations.put(jumpTarget, index + instructionSize(index));
            }
        }

        private void recordConditionalJump(final SmalltalkSistaV1InterpreterGen.Builder b, final int offset) {
            assert offset > 0;
            final int jumpTarget = index + offset;
            if (!jumpLocations.containsKey(jumpTarget)) {

                jumpLocations.put(jumpTarget, null);
            }
        }

        private void recordJumpStackPointer(final int jumpTarget) {
            final Integer previousStackPointer = jumpStackPointers.putIfAbsent(jumpTarget, sp);
            assert previousStackPointer == null || previousStackPointer == sp : "Inconsistent stack depth at jump target";
        }

        private int translateBytecode(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int numExtB) {
            final int indexWithExt = index + extBytes;
            final int op = getUnsignedInt(indexWithExt);
            currentOpcode = op;
            // These bytecodes can consume direct expressions. Other instructions are barriers.
            if (!(op <= 0x51 || op == 0x5C || op == 0x5E || op >= 0x60 && op <= 0xAF ||
                            op >= 0xB8 && op <= 0xD8 || op >= 0xE0 && op <= 0xE5 ||
                            op == 0xE8 || op == 0xE9 || op == 0xEA || op >= 0xEE && op <= 0xF5)) {
                spillStack(b);
            }
            switch (op) {
                case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F //
                    -> emitPush(b, () -> b.emitReceiverVariable(op & 0xF));
                case 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F //
                    -> emitPush(b, () -> b.emitLiteralVariable(code.getAndResolveLiteral(op & 0xF)));
                case 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F //
                    -> emitPush(b, () -> b.emitLoadConstant(code.getAndResolveLiteral(op & 0x1F)));
                case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[op & 0x7]));
                case 0x48, 0x49, 0x4A, 0x4B -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[(op & 3) + 8]));
                case 0x4C -> emitPush(b, () -> emitLoadReceiver(b));
                case 0x4D -> emitPush(b, () -> b.emitLoadConstant(BooleanObject.TRUE));
                case 0x4E -> emitPush(b, () -> b.emitLoadConstant(BooleanObject.FALSE));
                case 0x4F -> emitPush(b, () -> b.emitLoadConstant(NilObject.SINGLETON));
                case 0x50 -> emitPush(b, () -> b.emitLoadConstant(0L));
                case 0x51 -> emitPush(b, () -> b.emitLoadConstant(1L));
                case 0x52 -> {
                    if (extB == 0) {
                        emitPush(b, b::emitActiveContext);
                    } else {
                        fail(op); // unused
                    }
                }
                case 0x53 -> {
                    final StackEntry value = localEntry(b, stackSlots[sp - 1]);
                    emitPush(b, value.operation);
                }
                // unused
                case 0x58 -> emitReturn(b, () -> emitLoadReceiver(b));
                case 0x59 -> emitReturn(b, () -> b.emitLoadConstant(BooleanObject.TRUE));
                case 0x5A -> emitReturn(b, () -> b.emitLoadConstant(BooleanObject.FALSE));
                case 0x5B -> emitReturn(b, () -> b.emitLoadConstant(NilObject.SINGLETON));
                case 0x5C -> {
                    final StackEntry returnValue = popStackEntry(b);
                    spillStack(b);
                    b.beginReturn();
                    b.beginReturnTopFromMethod();
                    returnValue.operation.perform();
                    b.endReturnTopFromMethod();
                    b.endReturn();
                }
                case 0x5D -> {
                    b.beginReturn();
                    b.beginReturnTopFromClosure();
                    b.emitLoadConstant(NilObject.SINGLETON);
                    b.endReturnTopFromClosure();
                    b.endReturn();
                }
                case 0x5E -> {
                    if (extA == 0) {
                        final StackEntry returnValue = popStackEntry(b);
                        spillStack(b);
                        b.beginReturn();
                        b.beginReturnTopFromClosure();
                        returnValue.operation.perform();
                        b.endReturnTopFromClosure();
                        b.endReturn();
                    } else {
                        fail(op); // shouldBeImplemented, see #genExtReturnTopFromBlock
                    }
                }
                case 0x5F -> {
                    // nop
                }

                case 0x60 /* #+ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialAdd();
                    emitPopN(b, 2);
                    b.endSendSpecialAdd();
                });
                case 0x61 /* #- */ -> emitPush(b, 2, () -> { // #+
                    b.beginSendSpecialSubtract();
                    emitPopN(b, 2);
                    b.endSendSpecialSubtract();
                });
                case 0x62 /* #< */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialLessThan();
                    emitPopN(b, 2);
                    b.endSendSpecialLessThan();
                });
                case 0x63 /* #> */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialGreaterThan();
                    emitPopN(b, 2);
                    b.endSendSpecialGreaterThan();
                });
                case 0x64 /* #<= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialLessOrEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialLessOrEqual();
                });
                case 0x65 /* #>= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialGreaterOrEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialGreaterOrEqual();
                });
                case 0x66 /* #= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialEqual();
                });
                case 0x67 /* #~= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialNotEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialNotEqual();
                });
                case 0x68 /* #* */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialMultiply();
                    emitPopN(b, 2);
                    b.endSendSpecialMultiply();
                });
                case 0x69 /* #/ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialDivide();
                    emitPopN(b, 2);
                    b.endSendSpecialDivide();
                });
                case 0x6A /* #\\ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialFloorMod();
                    emitPopN(b, 2);
                    b.endSendSpecialFloorMod();
                });
                case 0x6B -> emitSend(b, getSpecialSelector(11), 1);
                case 0x6C /* #bitShift: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitShift();
                    emitPopN(b, 2);
                    b.endSendSpecialBitShift();
                });
                case 0x6D /* #// */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialFloorDivide();
                    emitPopN(b, 2);
                    b.endSendSpecialFloorDivide();
                });
                case 0x6E /* #bitAnd: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitAnd();
                    emitPopN(b, 2);
                    b.endSendSpecialBitAnd();
                });
                case 0x6F /* #bitOr: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitOr();
                    emitPopN(b, 2);
                    b.endSendSpecialBitOr();
                });
                case 0x70 -> emitSend(b, getSpecialSelector(16), 1);
                case 0x71 -> emitSend(b, getSpecialSelector(17), 2);
                case 0x72 -> emitSend(b, getSpecialSelector(18), 0);
                case 0x73 -> emitSend(b, getSpecialSelector(19), 0);
                case 0x74 -> emitSend(b, getSpecialSelector(20), 1);
                case 0x75 -> emitSend(b, getSpecialSelector(21), 0);
                case 0x76 /* #== */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialIdentical();
                    emitPopN(b, 2);
                    b.endSendSpecialIdentical();
                });
                case 0x77 /* #class */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialClass();
                    emitPop(b);
                    b.endSendSpecialClass();
                });
                case 0x78 -> emitSend(b, getSpecialSelector(24), 1);
                case 0x79 -> emitSend(b, getSpecialSelector(25), 0);
                case 0x7A -> emitSend(b, getSpecialSelector(26), 1);
                case 0x7B -> emitSend(b, getSpecialSelector(27), 1);
                case 0x7C -> emitSend(b, getSpecialSelector(28), 0);
                case 0x7D -> emitSend(b, getSpecialSelector(29), 1);
                case 0x7E -> emitSend(b, getSpecialSelector(30), 0);
                case 0x7F -> emitSend(b, getSpecialSelector(31), 0);
                case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F //
                    -> emitPush(b, 1, () -> {
                        b.beginSelfSendNilary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPop(b);
                        b.endSelfSendNilary();
                    });
                case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F //
                    -> emitPush(b, 2, () -> {
                        b.beginSelfSendUnary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPopN(b, 2);
                        b.endSelfSendUnary();
                    });
                case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF //
                    -> emitPush(b, 3, () -> {
                        b.beginSelfSendBinary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPopN(b, 3);
                        b.endSelfSendBinary();
                    });
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    if (jumpTarget > index) {
                        recordJumpStackPointer(jumpTarget);
                        b.emitBranch(jumpLocations.get(jumpTarget));
                    }
                }
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    final StackEntry condition = popStackEntry(b);
                    spillStack(b);
                    b.beginIfThen();
                    b.beginNot();
                    b.beginNot();
                    condition.operation.perform();
                    b.endNot();
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    final StackEntry condition = popStackEntry(b);
                    spillStack(b);
                    b.beginIfThen();
                    b.beginNot();
                    condition.operation.perform();
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF -> emitStoreIntoReceiverVariable(b, op & 7, true);
                case 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> emitStoreIntoTemporaryLocation(b, op & 7, true);
                case 0xD8 -> {
                    final StackEntry discarded = popStackEntry(b);
                    spillStack(b);
                    discarded.operation.perform();
                }
                // unused
                case 0xE0 -> {
                    return translateBytecode(b, extBytes + 2, (extA << 8) + getUnsignedInt(indexWithExt + 1), extB, numExtB);
                }
                case 0xE1 -> {
                    final int byteValue = getUnsignedInt(indexWithExt + 1);
                    return translateBytecode(b, extBytes + 2, extA, numExtB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue, numExtB + 1);
                }
                case 0xE2 -> emitPush(b, () -> b.emitReceiverVariable(getUnsignedInt(indexWithExt + 1) + (extA << 8)));
                case 0xE3 -> emitPush(b, () -> b.emitLiteralVariable(code.getAndResolveLiteral(getUnsignedInt(indexWithExt + 1) + (extA << 8))));
                case 0xE4 -> emitPush(b, () -> b.emitLoadConstant(code.getAndResolveLiteral(getUnsignedInt(indexWithExt + 1) + (extA << 8))));
                case 0xE5 -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[getUnsignedInt(indexWithExt + 1)]));
                // unused
                case 0xE7 -> {
                    final byte param = getByte(indexWithExt + 1);
                    final int arraySize = param & 127;
                    if (param < 0) {
                        final StackEntry[] values = popStackEntries(b, arraySize);
                        emitPush(b, () -> {
                            b.beginNewArrayFromStack();
                            emitStackEntries(b, values);
                            b.endNewArrayFromStack();
                        });
                    } else {
                        emitPush(b, () -> b.emitNewArray(arraySize));
                    }
                }
                case 0xE8 -> {
                    final long smallIntegerValue = getUnsignedInt(indexWithExt + 1) + (extB << 8);
                    emitPush(b, () -> b.emitLoadConstant(smallIntegerValue));
                }
                case 0xE9 -> {
                    final Object characterValue = CharacterObject.valueOf(getUnsignedInt(indexWithExt + 1) + (extA << 8));
                    emitPush(b, () -> b.emitLoadConstant(characterValue));
                }
                case 0xEA -> emitSend(b, (NativeObject) code.getAndResolveLiteral((getUnsignedInt(indexWithExt + 1) >> 3) + (extA << 5)),
                                (getUnsignedInt(indexWithExt + 1) & 7) + (extB << 3));
                case 0xEB -> emitSuperSend(b, (NativeObject) code.getAndResolveLiteral((getUnsignedInt(indexWithExt + 1) >> 3) + (extA << 5)),
                                (getUnsignedInt(indexWithExt + 1) & 7) + ((extB & 63) << 3), extB >= 64);
                // unused
                case 0xED -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    if (jumpTarget > index) {
                        recordJumpStackPointer(jumpTarget);
                        b.emitBranch(jumpLocations.get(jumpTarget));
                    }
                }
                case 0xEE -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    final StackEntry condition = popStackEntry(b);
                    spillStack(b);
                    b.beginIfThen();
                    b.beginNot();
                    b.beginNot();
                    condition.operation.perform();
                    b.endNot();
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xEF -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    final StackEntry condition = popStackEntry(b);
                    spillStack(b);
                    b.beginIfThen();
                    b.beginNot();
                    condition.operation.perform();
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xF0 -> emitStoreIntoReceiverVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF1 -> emitStoreIntoLiteralVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF2 -> emitStoreIntoTemporaryLocation(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF3 -> emitStoreIntoReceiverVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                case 0xF4 -> emitStoreIntoLiteralVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                case 0xF5 -> emitStoreIntoTemporaryLocation(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                // unused
                case 0xF8 -> {
                    if (indexWithExt + 3 < code.getMaxPCZeroBased() && getUnsignedInt(indexWithExt + 3) == 0xF5) {
                        b.beginStoreLocal(temporarySlots[sp - 1]);
                        b.emitPrimitiveError();
                        b.endStoreLocal();
                    }
                }
                case 0xF9 -> emitFullClosure(b, extA, indexWithExt);
                case 0xFA -> {
                    return emitClosure(b, extBytes, extA, extB, indexWithExt);
                }
                case 0xFB -> emitPushRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2));
                case 0xFC -> emitStoreIntoRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2), false);
                case 0xFD -> emitStoreIntoRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2), true);
                // unused
                default -> fail(op);
            }
            if (op <= 223) {
                return 1 + extBytes;
            } else if (op <= 247) {
                return 2 + extBytes;
            } else {
                return 3 + extBytes;
            }
        }

        private void emitPush(final SmalltalkSistaV1InterpreterGen.Builder b, final PushOperation pushOperation) {
            emitPush(b, 0, pushOperation);
        }

        private void emitPush(final SmalltalkSistaV1InterpreterGen.Builder b, final int numPopped, final PushOperation pushOperation) {
            for (int i = sp - numPopped; i < sp; i++) {
                if (stackValues[i] != null && stackValues[i].depth >= MAX_EXPRESSION_DEPTH) {
                    spillStack(b);
                    break;
                }
            }
            final StackEntry[] operands = popStackEntries(b, numPopped);
            observedSP = sp;
            final boolean mayCall = numPopped > 0 || currentOpcode == 0x52 || currentOpcode == 0xE7 || currentOpcode == 0xF9 || currentOpcode == 0xFA;
            if (mayCall) {
                // A callee can inspect or mutate its sender's retained operand stack.
                spillStack(b);
            }
            final int sourceIndex = index;
            final int sourceSize = instructionSize(index);
            final PushOperation expression = () -> {
                final EmissionState previousState = emissionState;
                emissionState = new EmissionState(operands);
                b.beginSourceSection(sourceIndex, sourceSize);
                pushOperation.perform();
                b.endSourceSection();
                assert emissionState.index == operands.length;
                emissionState = previousState;
            };
            int depth = 1;
            for (final StackEntry operand : operands) {
                depth = Math.max(depth, operand.depth + 1);
            }
            stackValues[sp++] = new StackEntry(expression, mayCall, depth);
        }

        @FunctionalInterface
        public interface PushOperation {
            void perform();
        }

        /** A build-time expression, or a read of a value already spilled to a canonical local. */
        private record StackEntry(PushOperation operation, boolean mayCall, int depth) {
        }

        private static StackEntry localEntry(final SmalltalkSistaV1InterpreterGen.Builder b, final BytecodeLocal local) {
            return new StackEntry(() -> b.emitLoadLocal(local), false, 0);
        }

        private void spillStack(final SmalltalkSistaV1InterpreterGen.Builder b) {
            for (int i = code.getInitialSP(); i < sp; i++) {
                final StackEntry value = stackValues[i];
                if (value != null) {
                    b.beginStoreLocal(stackSlots[i]);
                    value.operation.perform();
                    b.endStoreLocal();
                    stackValues[i] = null;
                }
            }
        }

        /** Supplies operands when emitting a deferred expression, without changing parser state. */
        private static final class EmissionState {
            private final StackEntry[] operands;
            private int index;

            private EmissionState(final StackEntry[] operands) {
                this.operands = operands;
            }
        }

        private static void emitLoadReceiver(final SmalltalkSistaV1InterpreterGen.Builder b) {
            b.emitLoadArgument(FrameAccess.getReceiverStartIndex());
        }

        private void emitPop(final SmalltalkSistaV1InterpreterGen.Builder b) {
            emitPopN(b, 1);
        }

        private void emitPopN(final SmalltalkSistaV1InterpreterGen.Builder b, final int numPopped) {
            if (emissionState == null) {
                final StackEntry[] entries = popStackEntries(b, numPopped);

                emitStackEntries(b, entries);
            } else {
                final int nextIndex = emissionState.index + numPopped;
                assert nextIndex <= emissionState.operands.length;
                while (emissionState.index < nextIndex) {
                    emissionState.operands[emissionState.index++].operation.perform();
                }
            }
        }

        private StackEntry popStackEntry(final SmalltalkSistaV1InterpreterGen.Builder b) {
            return popStackEntries(b, 1)[0];
        }

        private static void emitReturn(final SmalltalkSistaV1InterpreterGen.Builder b, final ReturnOperation returnOperation) {
            b.beginReturn();
            b.beginReturnTopFromMethod();
            returnOperation.perform();
            b.endReturnTopFromMethod();
            b.endReturn();
        }

        @FunctionalInterface
        public interface ReturnOperation {
            void perform();
        }

        private StackEntry[] popStackEntries(final SmalltalkSistaV1InterpreterGen.Builder b, final int size) {
            // Evaluate later effectful operands before reloading earlier canonical values: the
            // later call may change those values through its sender context.
            for (int i = sp - size + 1; i < sp; i++) {
                if (stackValues[i] != null && stackValues[i].mayCall) {
                    spillStack(b);
                    break;
                }
            }
            final StackEntry[] entries = new StackEntry[size];
            sp -= size;
            for (int i = 0; i < size; i++) {
                entries[i] = stackValues[sp + i] == null ? localEntry(b, stackSlots[sp + i]) : stackValues[sp + i];
                stackValues[sp + i] = null;
            }
            return entries;
        }

        private static void emitStackEntries(final SmalltalkSistaV1InterpreterGen.Builder b, final StackEntry[] entries) {
            for (final StackEntry entry : entries) {
                entry.operation.perform();
            }
        }

        private void emitStoreIntoReceiverVariable(final SmalltalkSistaV1InterpreterGen.Builder b, final int variableIndex, final boolean shouldPop) {
            final StackEntry value = prepareStoreValue(b, shouldPop);
            b.beginStoreIntoReceiverVariable(variableIndex);
            value.operation.perform();
            b.endStoreIntoReceiverVariable();
            b.emitCheckContextMutation();
        }

        private void emitStoreIntoLiteralVariable(final SmalltalkSistaV1InterpreterGen.Builder b, final int literalIndex, final boolean shouldPop) {
            final StackEntry value = prepareStoreValue(b, shouldPop);
            b.beginStoreIntoLiteralVariable(code.getAndResolveLiteral(literalIndex), ObjectLayouts.ASSOCIATION.VALUE);
            value.operation.perform();
            b.endStoreIntoLiteralVariable();
        }

        private void emitStoreIntoTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int tempIndex, final boolean shouldPop) {
            final StackEntry value = prepareStoreValue(b, shouldPop);
            b.beginStoreLocal(temporarySlots[tempIndex]);
            value.operation.perform();
            b.endStoreLocal();
        }

        private StackEntry prepareStoreValue(final SmalltalkSistaV1InterpreterGen.Builder b, final boolean shouldPop) {
            if (shouldPop) {
                final StackEntry value = popStackEntry(b);
                spillStack(b);
                return value;
            } else {
                spillStack(b);
                return localEntry(b, stackSlots[sp - 1]);
            }
        }

        private void emitPushRemoteTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int indexInArray, final int indexOfArray) {
            emitPush(b, () -> {
                b.beginLoadRemoteTemp(indexInArray);
                b.emitLoadLocal(temporarySlots[indexOfArray]);
                b.endLoadRemoteTemp();
            });
        }

        private void emitStoreIntoRemoteTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int indexInArray, final int indexOfArray, final boolean shouldPop) {
            final StackEntry value = prepareStoreValue(b, shouldPop);
            b.beginStoreIntoRemoteTemp(indexInArray);
            b.emitLoadLocal(temporarySlots[indexOfArray]);
            value.operation.perform();
            b.endStoreIntoRemoteTemp();
        }

        private void emitSend(final SmalltalkSistaV1InterpreterGen.Builder b, final NativeObject selector, final int numArgs) {
            emitPush(b, numArgs + 1, () -> {
                switch (numArgs) {
                    case 0 -> {
                        b.beginSelfSendNilary(selector);
                        emitPopN(b, 1);
                        b.endSelfSendNilary();
                    }
                    case 1 -> {
                        b.beginSelfSendUnary(selector);
                        emitPopN(b, 2);
                        b.endSelfSendUnary();
                    }
                    case 2 -> {
                        b.beginSelfSendBinary(selector);
                        emitPopN(b, 3);
                        b.endSelfSendBinary();
                    }
                    default -> {
                        b.beginSelfSendNary(selector);
                        emitPopN(b, numArgs + 1);
                        b.endSelfSendNary();
                    }
                }
            });
        }

        private void emitSuperSend(final SmalltalkSistaV1InterpreterGen.Builder b, final NativeObject selector, final int numArgs, final boolean directed) {
            emitPush(b, numArgs + 1 + (directed ? 1 : 0), () -> {
                if (directed) {
                    b.beginDirectedSuperSend(selector);
                    emissionState.operands[numArgs + 1].operation.perform();
                    emitPopN(b, numArgs + 1);
                    emissionState.index++;
                    b.endDirectedSuperSend();
                } else {
                    b.beginSuperSend(code, selector);
                    emitPopN(b, numArgs + 1);
                    b.endSuperSend();
                }
            });
        }

        private void emitFullClosure(final SmalltalkSistaV1InterpreterGen.Builder b, final int extA, final int indexWithExt) {
            final byte byteA = getByte(indexWithExt + 1);
            final byte byteB = getByte(indexWithExt + 2);
            final int literalIndex = Byte.toUnsignedInt(byteA) + (extA << 8);
            final CompiledCodeObject block = (CompiledCodeObject) code.getLiteral(literalIndex);
            final int numCopied = Byte.toUnsignedInt(byteB) & 63;
            final boolean ignoreOuterContext = (byteB >> 6 & 1) == 1;
            final boolean receiverOnStack = (byteB >> 7 & 1) == 1;

            final StackEntry[] copiedValues = popStackEntries(b, numCopied);
            final StackEntry receiver = receiverOnStack ? popStackEntries(b, 1)[0] : null;
            emitPush(b, () -> {
                b.beginFullClosure(block);
                if (receiverOnStack) {
                    receiver.operation.perform();
                } else {
                    emitLoadReceiver(b);
                }
                if (ignoreOuterContext) {
                    b.emitLoadNull();
                } else {
                    b.emitActiveContext();
                }
                emitStackEntries(b, copiedValues);
                b.endFullClosure();
            });
        }

        private int emitClosure(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int indexWithExt) {
            final byte byteA = getByte(indexWithExt + 1);
            final byte byteB = getByte(indexWithExt + 2);
            final int numArgs = (byteA & 7) + Math.floorMod(extA, 16) * 8;
            final int numCopied = (Byte.toUnsignedInt(byteA) >> 3 & 0x7) + Math.floorDiv(extA, 16) * 8;
            final int successorPC = code.getInitialPC() + index + 3 + extBytes;
            final int blockSize = Byte.toUnsignedInt(byteB) + (extB << 8);
            final CompiledCodeObject block = code.createShadowBlock(successorPC, numArgs, numCopied, blockSize);

            final StackEntry[] copiedValues = popStackEntries(b, numCopied);
            emitPush(b, () -> {
                b.beginClosure(block);
                emitStackEntries(b, copiedValues);
                b.endClosure();
            });
            return 3 + extBytes + blockSize;
        }

        private static void fail(final int opcode) {
            throw SqueakException.create("Unknown bytecode:", opcode);
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialAdd {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()")
        static double doLongDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) {
            return lhs + rhs;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(0))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialSubtract {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()")
        static double doLongDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) {
            return lhs - rhs;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(1))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialLessThan {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs < rhs);
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(2))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialGreaterThan {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs > rhs);
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(3))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialLessOrEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs <= rhs);
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(4))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialGreaterOrEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs >= rhs);
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(5))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(image, rhs)", "!isPointersObject(rhs)"})
        static boolean doQuickFalse(final long lhs, final AbstractSqueakObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.FALSE;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(6))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialNotEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(image, rhs)", "!isPointersObject(rhs)"})
        static boolean doQuickTrue(final long lhs, final AbstractSqueakObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.TRUE;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(7))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialMultiply {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()", rewriteOn = RespecializeException.class)
        static double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(8))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialDivide {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        static long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"}, replaces = "doLong")
        static Object doLongFraction(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile fractionProfile,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectWriteNode writeNode) {
            if (fractionProfile.profile(node, SqueakGuards.isIntegralWhenDividedBy(lhs, rhs))) {
                return lhs / rhs;
            } else {
                return image.asFraction(lhs, rhs, writeNode);
            }
        }

        @Specialization(guards = {"image.flags.numericPrimsMixArithmetic()", "!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        static double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(9))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialFloorMod {
        /** Profiled version of {@link Math#floorMod(long, long)}. */
        @Specialization(guards = "rhs != 0")
        static long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long r = lhs % rhs;
            // if the signs are different and modulo not zero, adjust result
            if (profile.profile(node, (lhs ^ rhs) < 0 && r != 0)) {
                return r + rhs;
            } else {
                return r;
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(10))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialBitShift {
        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        static long doLongPositive(final long receiver, final long arg) {
            return receiver << arg;
        }

        @Specialization(guards = {"arg < 0", "inLongSizeRange(arg)"})
        static long doLongNegativeInLongSizeRange(final long receiver, final long arg) {
            /*
             * The result of a right shift can only become smaller than the receiver and 0 or -1 at
             * minimum, so no BigInteger needed here.
             */
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg < 0", "!inLongSizeRange(arg)"})
        static long doLongNegative(final long receiver, @SuppressWarnings("unused") final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        static boolean isLShiftLongOverflow(final long receiver, final long arg) {
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        static boolean inLongSizeRange(final long arg) {
            return -Long.SIZE < arg;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(12))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialFloorDivide {
        /** Profiled version of {@link Math#floorDiv(long, long)}. */
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        static long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long q = lhs / rhs;
            // if the signs are different and modulo not zero, round down
            if (profile.profile(node, (lhs ^ rhs) < 0 && (q * rhs != lhs))) {
                return q - 1;
            } else {
                return q;
            }
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(13))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialBitAnd {
        @Specialization
        static long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(14))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialBitOr {
        @Specialization
        static long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

        @Fallback
        @StoreBytecodeIndex
        static Object send(final VirtualFrame frame, final Object receiver, final Object argument,
                        @Cached("create(getSpecialSelector(15))") final Dispatch1Node dispatch) {
            try {
                return followForwarded(dispatch.execute(frame, receiver, argument));
            } catch (final AbstractStandardSendReturn result) {
                return handleSendReturn(frame, result);
            }
        }

    }

    @Operation(storeBytecodeIndex = false)
    public static final class SendSpecialMakePoint {
        @Specialization
        static PointersObject doPoint(final Object xPos, final Object yPos,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, xPos, yPos);
        }
    }

    @Operation(forceCached = true, storeBytecodeIndex = false)
    public static final class SendSpecialIdentical {
        @Specialization
        static boolean perform(final Object left, final Object right,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(node, left, right);
        }
    }

    @Operation(storeBytecodeIndex = false)
    public static final class SendSpecialClass {
        @Specialization
        static Object performASO(final AbstractSqueakObject receiver,
                        @Bind final Node node,
                        @Shared("classNode") @Cached(inline = true) final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }

        @Specialization(replaces = "performASO")
        static Object perform(final Object receiver,
                        @Bind final Node node,
                        @Shared("classNode") @Cached(inline = true) final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }
    }

}

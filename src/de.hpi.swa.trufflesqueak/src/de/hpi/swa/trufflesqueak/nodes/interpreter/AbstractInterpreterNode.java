/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

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
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public abstract class AbstractInterpreterNode extends AbstractInterpreterInstrumentableNode implements BytecodeOSRNode, InstrumentableNode {
    private static final String[] READONLY_CLASSES = {"ClassBinding", "ReadOnlyVariableBinding"};
    protected static final int LOCAL_RETURN_PC = -2;

    protected static final byte BRANCH1 = 0b1;
    protected static final byte BRANCH2 = 0b10;
    protected static final byte BRANCH3 = 0b100;
    protected static final byte BRANCH4 = 0b1000;
    protected static final byte BRANCH5 = 0b10000;
    protected static final byte BRANCH6 = 0b100000;
    protected static final byte BRANCH7 = 0b1000000;

    protected final CompiledCodeObject code;
    protected final boolean isBlock;
    protected final int numArguments;

    @CompilationFinal(dimensions = 1) protected final Object[] data;
    @CompilationFinal(dimensions = 1) protected final byte[] profiles;
    @CompilationFinal private Object osrMetadata;

    @SuppressWarnings("this-escape")
    public AbstractInterpreterNode(final CompiledCodeObject code) {
        this.code = code;
        isBlock = code.isCompiledBlock() || code.isShadowBlock();
        numArguments = code.getNumArgsAndCopied();
        final int startPC = code.getStartPCZeroBased();
        final int endPC = code.getMaxPCZeroBased();
        data = new Object[endPC];
        profiles = new byte[endPC];
        processBytecode(startPC, endPC);
    }

    @SuppressWarnings("this-escape")
    public AbstractInterpreterNode(final AbstractInterpreterNode original) {
        // reusable fields
        code = original.code;
        isBlock = original.isBlock;
        numArguments = original.numArguments;
        // fresh fields
        final int startPC = code.getStartPCZeroBased();
        final int endPC = code.getMaxPCZeroBased();
        data = new Object[endPC];
        profiles = new byte[endPC];
        processBytecode(startPC, endPC);
        osrMetadata = null;
    }

    protected abstract void processBytecode(int startPC, int endPC);

    @Override
    public abstract Object execute(VirtualFrame frame, int startPC, int startSP);

    static final class ReadLiteralVariableNode extends AbstractNode {
        private final SqueakObjectAt0NodeGen at0Node = insert((SqueakObjectAt0NodeGen) SqueakObjectAt0NodeGen.create());

        @InliningCutoff
        Object execute(final Node node, final Object literal) {
            return at0Node.execute(node, literal, ASSOCIATION.VALUE);
        }
    }

    protected static final CompiledCodeObject createBlock(final CompiledCodeObject code, final int pc, final int numArgs, final int numCopied, final int blockSize) {
        return code.createShadowBlock(code.getInitialPC() + pc, numArgs, numCopied, blockSize);
    }

    protected static final BlockClosureObject createBlockClosure(final VirtualFrame frame, final CompiledCodeObject block, final Object[] copiedValues, final ContextObject outerContext) {
        return new BlockClosureObject(true, block, block.getShadowBlockNumArgs(), copiedValues, FrameAccess.getReceiver(frame), outerContext);
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver) {
        return followForwarded(currentPC, dispatch(frame, currentPC, receiver));
    }

    @InliningCutoff
    private Object dispatch(final VirtualFrame frame, final int currentPC, final Object receiver) {
        try {
            return uncheckedCast(data[currentPC], Dispatch0NodeGen.class).execute(frame, receiver);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg) {
        return followForwarded(currentPC, dispatch(frame, currentPC, receiver, arg));
    }

    @InliningCutoff
    private Object dispatch(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg) {
        try {
            return uncheckedCast(data[currentPC], Dispatch1NodeGen.class).execute(frame, receiver, arg);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg1, final Object arg2) {
        return followForwarded(currentPC, dispatch(frame, currentPC, receiver, arg1, arg2));
    }

    @InliningCutoff
    private Object dispatch(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg1, final Object arg2) {
        try {
            return uncheckedCast(data[currentPC], Dispatch2NodeGen.class).execute(frame, receiver, arg1, arg2);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object sendNary(final VirtualFrame frame, final int currentPC, final Object receiver, final Object[] arguments) {
        return followForwarded(currentPC, dispatchNary(frame, currentPC, receiver, arguments));
    }

    @InliningCutoff
    private Object dispatchNary(final VirtualFrame frame, final int currentPC, final Object receiver, final Object[] arguments) {
        try {
            return uncheckedCast(data[currentPC], DispatchNaryNodeGen.class).execute(frame, receiver, arguments);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object handleReturnException(final VirtualFrame frame, final int currentPC, final AbstractStandardSendReturn returnException) {
        final byte state = profiles[currentPC];
        enter(currentPC, state, BRANCH1);
        if (returnException.targetIsFrame(frame)) {
            enter(currentPC, state, BRANCH2);
            return returnException.getReturnValue();
        } else {
            enter(currentPC, state, BRANCH3);
            if (returnException instanceof NonLocalReturn) {
                enter(currentPC, state, BRANCH4);
                FrameAccess.terminateFrame(frame);
            }
            throw returnException;
        }
    }

    /*
     * Contexts
     */

    /** Inlined version of {@link GetOrCreateContextWithFrameNode}. */
    protected final ContextObject getOrCreateContext(final VirtualFrame frame, final int currentPC) {
        final byte state = profiles[currentPC];
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            enter(currentPC, state, BRANCH1);
            if (!context.hasTruffleFrame()) {
                enter(currentPC, state, BRANCH2);
                context.setTruffleFrame(frame.materialize());
            }
            return context;
        } else {
            enter(currentPC, state, BRANCH3);
            return new ContextObject(frame.materialize());
        }
    }

    /*
     * Literals
     */

    protected static final Object getLiteralVariableOrCreateLiteralNode(final Object literal) {
        if (literal instanceof final AbstractSqueakObjectWithClassAndHash l) {
            final String squeakClassName = l.getSqueakClassName();
            if (ArrayUtils.containsEqual(READONLY_CLASSES, squeakClassName)) {
                return SqueakObjectAt0NodeGen.executeUncached(literal, ASSOCIATION.VALUE);
            } else {
                return new ReadLiteralVariableNode();
            }
        } else {
            throw SqueakException.create("Unexpected literal", literal);
        }
    }

    protected final Object readLiteralVariable(final int currentPC, final int index) {
        final Object literalVariableOrNode = data[currentPC];
        if (literalVariableOrNode instanceof final ReadLiteralVariableNode node) {
            return node.execute(this, code.getAndResolveLiteral(index));
        } else {
            return literalVariableOrNode;
        }
    }

    /** Profiled version of {@link CompiledCodeObject#getAndResolveLiteral(long)}. */
    public final Object getAndResolveLiteral(final int currentPC, final long longIndex) {
        final Object litVar = code.getLiteral(longIndex);
        if (litVar instanceof final AbstractSqueakObjectWithClassAndHash obj) {
            enter(currentPC, profiles[currentPC], BRANCH1);
            if (!obj.isNotForwarded()) {
                CompilerDirectives.transferToInterpreter();
                final AbstractSqueakObjectWithClassAndHash forwarded = obj.getForwardingPointer();
                code.setLiteral(longIndex, forwarded);
                return forwarded;
            }
        }
        return litVar;
    }

    /*
     * Bytecode
     */

    protected static final byte getByte(final byte[] bc, final int pc) {
        return UnsafeUtils.getByte(bc, pc);
    }

    protected static final int getUnsignedInt(final byte[] bc, final int pc) {
        return Byte.toUnsignedInt(getByte(bc, pc));
    }

    protected final Object handleReturn(final VirtualFrame frame, final int currentPC, final int pc, final int sp, final Object result, final int loopCounter) {
        if (loopCounter > 0) {
            LoopNode.reportLoopCount(this, loopCounter);
        }
        if (isBlock) {
            return handleBlockReturn(frame, currentPC, pc, sp, result);
        } else {
            return handleNormalReturn(frame, currentPC, result);
        }
    }

    protected final Object handleReturnFromBlock(final VirtualFrame frame, final int currentPC, final Object result, final int loopCounter) {
        if (loopCounter > 0) {
            LoopNode.reportLoopCount(this, loopCounter);
        }
        return handleNormalReturn(frame, currentPC, result);
    }

    private Object handleNormalReturn(final VirtualFrame frame, final int currentPC, final Object result) {
        final byte state = profiles[currentPC];
        if (FrameAccess.hasModifiedSender(frame)) {
            enter(currentPC, state, BRANCH1);
            throw new NonVirtualReturn(result, FrameAccess.getSender(frame));
        } else {
            enter(currentPC, state, BRANCH2);
            FrameAccess.terminateFrame(frame);
            return result;
        }
    }

    @InliningCutoff
    private Object handleBlockReturn(final VirtualFrame frame, final int currentPC, final int pc, final int sp, final Object result) {
        // Target is sender of closure's home context.
        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
        if (homeContext.canBeReturnedTo()) {
            final ContextObject firstMarkedContext = firstUnwindMarkedOrThrowNLR(FrameAccess.getSender(frame), homeContext, result);
            if (firstMarkedContext != null) {
                externalizePCAndSP(frame, pc, sp);
                if (data[currentPC] == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    data[currentPC] = insert(Dispatch2NodeGen.create(getContext().aboutToReturnSelector));
                }
                ((Dispatch2NodeGen) data[currentPC]).execute(frame, getOrCreateContext(frame, currentPC), result, firstMarkedContext);
            }
        }
        throw cannotReturn(frame, result);
    }

    /**
     * Walk the sender chain starting at the given frame sender and terminating at homeContext.
     *
     * @return null if homeContext is not on sender chain; return first marked Context if found;
     *         raise NLR otherwise
     */
    private static ContextObject firstUnwindMarkedOrThrowNLR(final AbstractSqueakObject senderOrNil, final ContextObject homeContext, final Object returnValue) {
        AbstractSqueakObject currentLink = senderOrNil;
        ContextObject firstMarkedContext = null;

        while (currentLink instanceof final ContextObject context) {
            // Exit if we've found homeContext.
            if (context == homeContext) {
                if (firstMarkedContext == null) {
                    throw new NonLocalReturn(returnValue, homeContext);
                }
                return firstMarkedContext;
            }
            // Watch for unwind-marked ContextObjects.
            if (firstMarkedContext == null && context.isUnwindMarked()) {
                firstMarkedContext = context;
            }
            // Move to the next link.
            currentLink = context.getFrameSender();
        }

        // Reached the end of the chain without finding homeContext.
        return null;
    }

    private static CannotReturnToTarget cannotReturn(final VirtualFrame frame, final Object returnValue) {
        CompilerDirectives.transferToInterpreter();
        LogUtils.SCHEDULING.info("sendCannotReturn");
        throw new CannotReturnToTarget(returnValue, GetOrCreateContextWithFrameNode.executeUncached(frame));
    }

    /*
     * Handling of forwarding pointers
     */
    private Object followForwarded(final int currentPC, final Object value) {
        final byte state = profiles[currentPC];
        if (value instanceof final AbstractSqueakObjectWithClassAndHash object) {
            enter(currentPC, state, BRANCH6);
            return object.resolveForwardingPointer();
        } else {
            enter(currentPC, state, BRANCH7);
            return value;
        }
    }

    /*
     * Stack operations
     */

    protected final void pushFollowed(final VirtualFrame frame, final int currentPC, final int sp, final Object value) {
        setStackValue(frame, sp, followForwarded(currentPC, value));
    }

    protected final void push(final VirtualFrame frame, final int sp, final Object value) {
        setStackValue(frame, sp, value);
    }

    protected final Object pop(final VirtualFrame frame, final int sp) {
        assert sp >= numArguments;
        final int slotIndex = FrameAccess.toStackSlotIndex(sp);
        final Object result = frame.getObjectStatic(slotIndex);
        frame.setObjectStatic(slotIndex, NilObject.SINGLETON);
        return result;
    }

    protected final Object popReceiver(final VirtualFrame frame, final int sp) {
        return getStackValue(frame, sp);
    }

    protected final Object[] popN(final VirtualFrame frame, final int sp, final int numPop) {
        return numPop == 0 ? ArrayUtils.EMPTY_ARRAY : popNExploded(frame, sp, numPop);
    }

    @ExplodeLoop
    private Object[] popNExploded(final VirtualFrame frame, final int sp, final int numPop) {
        assert sp - numPop >= numArguments;
        final int topSlotIndex = FrameAccess.toStackSlotIndex(sp - 1);
        final Object[] stackValues = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            final int slotIndex = topSlotIndex - i;
            stackValues[numPop - 1 - i] = frame.getObjectStatic(slotIndex);
            frame.setObjectStatic(slotIndex, NilObject.SINGLETON);
        }
        return stackValues;
    }

    protected final Object top(final VirtualFrame frame, final int sp) {
        return getStackValue(frame, sp - 1);
    }

    protected final Object getTemp(final VirtualFrame frame, final int sp) {
        if (sp < numArguments) {
            return UnsafeUtils.getObject(frame.getArguments(), FrameAccess.getArgumentStartIndex() + sp);
        } else {
            return FrameAccess.getSlotValue(frame, FrameAccess.toStackSlotIndex(sp));
        }
    }

    protected final Object getStackValue(final VirtualFrame frame, final int sp) {
        assert sp >= numArguments;
        return FrameAccess.getSlotValue(frame, FrameAccess.toStackSlotIndex(sp));
    }

    protected final void setStackValue(final VirtualFrame frame, final int sp, final Object value) {
        assert sp >= numArguments;
        FrameAccess.setSlotValue(frame, FrameAccess.toStackSlotIndex(sp), value);
    }

    protected static final void externalizePCAndSP(final VirtualFrame frame, final int pc, final int sp) {
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
    }

    protected static final int internalizePC(final VirtualFrame frame, final int pc) {
        final int framePC = FrameAccess.getInstructionPointer(frame);
        if (pc != framePC) {
            CompilerDirectives.transferToInterpreter();
            return framePC;
        } else {
            return pc;
        }
    }

    protected final Object getErrorObject() {
        final SqueakImageContext image = getContext();
        final int primFailCode = image.getPrimFailCode();
        final ArrayObject errorTable = image.primitiveErrorTable;
        if (primFailCode < errorTable.getObjectLength()) {
            return errorTable.getObject(primFailCode);
        } else {
            return (long) primFailCode;
        }
    }

    protected final void sendMustBeBooleanInInterpreter(final VirtualFrame frame, final int pc, final Object stackValue) {
        CompilerDirectives.transferToInterpreter();
        FrameAccess.setInstructionPointer(frame, pc);
        final SqueakImageContext image = getContext();
        image.mustBeBooleanSelector.executeAsSymbolSlow(image, frame, stackValue);
        throw SqueakException.create("Should not be reached");
    }

    protected static final RuntimeException unknownBytecode() {
        CompilerDirectives.transferToInterpreter();
        throw CompilerDirectives.shouldNotReachHere("Unknown bytecode");
    }

    public static final int calculateShortOffset(final int bytecode) {
        return (bytecode & 7) + 1;
    }

    /**
     * Smaller than int[1], does not kill int[] on write and doesn't need bounds checks.
     */
    @ValueType
    protected static final class LoopCounter {
        protected static final int CHECK_LOOP_STRIDE = 1 << 14;
        protected static final double CHECK_LOOP_PROBABILITY = 1.0D / CHECK_LOOP_STRIDE;
        protected int value;
    }

    @Override
    public final CompiledCodeObject getCodeObject() {
        return code;
    }

    /*
     * Profiling
     */

    protected final void enter(final int currentPC, final byte state, final byte stateBit) {
        if ((state & stateBit) == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            profiles[currentPC] |= stateBit;
        }
    }

    /*
     * Bytecode OSR support
     */

    @Override
    public final Object executeOSR(final VirtualFrame osrFrame, final int targetPCAndSP, final Object interpreterState) {
        return execute(osrFrame, targetPCAndSP & 0xFFFF, targetPCAndSP >>> 16);
    }

    @Override
    public final Object getOSRMetadata() {
        return osrMetadata;
    }

    @Override
    public final void setOSRMetadata(final Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }

    @Override
    public final Object[] storeParentFrameInArguments(final VirtualFrame parentFrame) {
        return FrameAccess.storeParentFrameInArguments(parentFrame);
    }

    @Override
    public final Frame restoreParentFrameFromArguments(final Object[] arguments) {
        return FrameAccess.restoreParentFrameFromArguments(arguments);
    }

    /*
     * Node metadata
     */

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return StandardTags.RootTag.class == tag;
    }

    @Override
    public final String getDescription() {
        return code.toString();
    }

    @Override
    public final SourceSection getSourceSection() {
        final Source source = code.getSource();
        return source.createSection(1, 1, source.getLength());
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }
}

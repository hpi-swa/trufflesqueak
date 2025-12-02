/*
 * Copyright (c) 2025-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
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
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrims.AbstractBytecodePrim0Node;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrims.AbstractBytecodePrim1Node;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public abstract class AbstractInterpreterNode extends AbstractInterpreterInstrumentableNode implements BytecodeOSRNode, InstrumentableNode {
    private static final String[] READONLY_CLASSES = {"ClassBinding", "ReadOnlyVariableBinding"};
    protected static final int LOCAL_RETURN_PC = -2;

    protected final CompiledCodeObject code;
    protected final boolean isBlock;

    @CompilationFinal protected int numArguments;

    @CompilationFinal(dimensions = 1) protected final Object[] data;
    @CompilationFinal(dimensions = 1) protected final byte[] profiles;
    @CompilationFinal private Object osrMetadata;

    @SuppressWarnings("this-escape")
    public AbstractInterpreterNode(final CompiledCodeObject code) {
        this.code = code;
        isBlock = code.isCompiledBlock() || code.hasOuterMethod();
        numArguments = -1;
        final int maxPC = BytecodeUtils.trailerPosition(code);
        data = new Object[maxPC];
        profiles = new byte[maxPC];
        processBytecode(maxPC);
    }

    @SuppressWarnings("this-escape")
    public AbstractInterpreterNode(final AbstractInterpreterNode original) {
        // reusable fields
        code = original.code;
        isBlock = original.isBlock;
        numArguments = original.numArguments;
        // fresh fields
        final int maxPC = BytecodeUtils.trailerPosition(code);
        data = new Object[maxPC];
        profiles = new byte[maxPC];
        processBytecode(maxPC);
        osrMetadata = null;
    }

    protected abstract void processBytecode(int maxPC);

    @Override
    public abstract Object execute(VirtualFrame frame, int startPC, int startSP);

    protected static final class NormalReturnNode extends AbstractNode {
        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.create();

        Object execute(final VirtualFrame frame, final Object returnValue) {
            if (hasModifiedSenderProfile.profile(FrameAccess.hasModifiedSender(frame))) {
                throw new NonVirtualReturn(returnValue, FrameAccess.getSender(frame));
            } else {
                FrameAccess.terminateFrame(frame);
                return returnValue;
            }
        }
    }

    static final class BlockReturnNode extends AbstractNode {
        @CompilationFinal private GetOrCreateContextWithFrameNode getOrCreateContextNode;
        @CompilationFinal private Dispatch2Node sendAboutToReturnNode;

        private Object execute(final VirtualFrame frame, final int pc, final int sp, final Object returnValue) {
            externalizePCAndSP(frame, pc, sp);
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canBeReturnedTo()) {
                final ContextObject firstMarkedContext = firstUnwindMarkedOrThrowNLR(FrameAccess.getSender(frame), homeContext, returnValue);
                if (firstMarkedContext != null) {
                    getSendAboutToReturnNode().execute(frame, getGetOrCreateContextNode().executeGet(frame), returnValue, firstMarkedContext);
                }
            }
            CompilerDirectives.transferToInterpreter();
            LogUtils.SCHEDULING.info("BlockReturnNode: sendCannotReturn");
            throw new CannotReturnToTarget(returnValue, GetOrCreateContextWithFrameNode.executeUncached(frame));
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

        private GetOrCreateContextWithFrameNode getGetOrCreateContextNode() {
            if (getOrCreateContextNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOrCreateContextNode = insert(GetOrCreateContextWithFrameNode.create());
            }
            return getOrCreateContextNode;
        }

        private Dispatch2Node getSendAboutToReturnNode() {
            if (sendAboutToReturnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sendAboutToReturnNode = insert(Dispatch2NodeGen.create(getContext().aboutToReturnSelector));
            }
            return sendAboutToReturnNode;
        }
    }

    static final class ReadLiteralVariableNode extends AbstractNode {
        private final SqueakObjectAt0Node at0Node = insert(SqueakObjectAt0NodeGen.create());

        Object execute(final Node node, final CompiledCodeObject code, final int index) {
            return at0Node.execute(node, code.getAndResolveLiteral(index), ASSOCIATION.VALUE);
        }
    }

    static final class PushClosureNode extends AbstractNode {
        private final CompiledCodeObject shadowBlock;
        private final int numArgs;
        private final int closureStartPC;

        private final GetOrCreateContextWithFrameNode getOrCreateContextNode = insert(GetOrCreateContextWithFrameNode.create());

        PushClosureNode(final CompiledCodeObject code, final int pc, final int numArgs) {
            this.closureStartPC = code.getInitialPC() + pc;
            shadowBlock = code.getOrCreateShadowBlock(closureStartPC);
            this.numArgs = numArgs;
        }

        BlockClosureObject execute(final VirtualFrame frame, final Object[] copiedValues) {
            final SqueakImageContext image = getContext();
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(image, image.blockClosureClass, shadowBlock, closureStartPC, numArgs, copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver) {
        try {
            return uncheckedCast(data[currentPC], Dispatch0NodeGen.class).execute(frame, receiver);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg) {
        try {
            return uncheckedCast(data[currentPC], Dispatch1NodeGen.class).execute(frame, receiver, arg);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object send(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg1, final Object arg2) {
        try {
            return uncheckedCast(data[currentPC], Dispatch2NodeGen.class).execute(frame, receiver, arg1, arg2);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object sendNary(final VirtualFrame frame, final int currentPC, final Object receiver, final Object[] arguments) {
        try {
            return uncheckedCast(data[currentPC], DispatchNaryNodeGen.class).execute(frame, receiver, arguments);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object sendSuper(final VirtualFrame frame, final int currentPC, final Object receiver, final Object[] arguments) {
        try {
            return uncheckedCast(data[currentPC], DispatchSuperNaryNodeGen.class).execute(frame, receiver, arguments);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    protected final Object sendBytecodePrim(final VirtualFrame frame, final int currentPC, final Object receiver) {
        if (data[currentPC] instanceof AbstractBytecodePrim0Node) {
            try {
                return uncheckedCast(data[currentPC], AbstractBytecodePrim0Node.class).execute(frame, receiver);
            } catch (final UnsupportedSpecializationException | PrimitiveFailed use) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final NativeObject specialSelector = ((AbstractBytecodePrim0Node) data[currentPC]).getSpecialSelector();
                data[currentPC] = insert(Dispatch0NodeGen.create(specialSelector));
                return send(frame, currentPC, receiver);
            }
        } else {
            return send(frame, currentPC, receiver);
        }
    }

    protected final Object sendBytecodePrim(final VirtualFrame frame, final int currentPC, final Object receiver, final Object arg) {
        if (data[currentPC] instanceof AbstractBytecodePrim1Node) {
            try {
                return uncheckedCast(data[currentPC], AbstractBytecodePrim1Node.class).execute(frame, receiver, arg);
            } catch (final UnsupportedSpecializationException | PrimitiveFailed use) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final NativeObject specialSelector = ((AbstractBytecodePrim1Node) data[currentPC]).getSpecialSelector();
                data[currentPC] = insert(Dispatch1NodeGen.create(specialSelector));
                return send(frame, currentPC, receiver, arg);
            }
        } else {
            return send(frame, currentPC, receiver, arg);
        }
    }

    protected final Object handleReturnException(final VirtualFrame frame, final int currentPC, final AbstractStandardSendReturn returnException) {
        final byte state = profiles[currentPC];
        if ((state & 0b100) == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            profiles[currentPC] |= 0b100;
        }
        if (returnException.targetIsFrame(frame)) {
            if ((state & 0b1000) == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                profiles[currentPC] |= 0b1000;
            }
            return returnException.getReturnValue();
        } else {
            if ((state & 0b10000) == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                profiles[currentPC] |= 0b10000;
            }
            if (returnException instanceof NonLocalReturn) {
                if ((state & 0b100000) == 0) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    profiles[currentPC] |= 0b100000;
                }
                FrameAccess.terminateFrame(frame);
            }
            throw returnException;
        }
    }

    /*
     * Literals
     */

    protected static final Object getLiteralVariableOrCreateLiteralNode(final Object literal) {
        if (literal instanceof final AbstractSqueakObjectWithClassAndHash l) {
            final String squeakClassName = l.getSqueakClassName();
            if (ArrayUtils.containsEqual(READONLY_CLASSES, squeakClassName)) {
                return SqueakObjectAt0Node.executeUncached(literal, ASSOCIATION.VALUE);
            } else {
                return new ReadLiteralVariableNode();
            }
        } else {
            throw SqueakException.create("Unexpected literal", literal);
        }
    }

    protected final Object readLiteralVariable(final int currentPC, final int index) {
        final Object literalVariableOrNode = data[currentPC];
        if (literalVariableOrNode instanceof ReadLiteralVariableNode) {
            return uncheckedCast(data[currentPC], ReadLiteralVariableNode.class).execute(this, code, index);
        } else {
            return literalVariableOrNode;
        }
    }

    /** Profiled version of {@link CompiledCodeObject#getAndResolveLiteral(long)}. */
    public final Object getAndResolveLiteral(final int currentPC, final long longIndex) {
        final Object litVar = code.getLiteral(longIndex);
        if (litVar instanceof final AbstractSqueakObjectWithClassAndHash obj) {
            if ((profiles[currentPC] & 0b100) == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                profiles[currentPC] |= 0b100;
            }
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

    protected final Object handleReturn(final VirtualFrame frame, final int currentPC, final int pc, final int sp, final Object result) {
        if (isBlock) {
            return uncheckedCast(data[currentPC], BlockReturnNode.class).execute(frame, pc, sp, result);
        } else {
            return uncheckedCast(data[currentPC], NormalReturnNode.class).execute(frame, result);
        }
    }

    /*
     * Stack operations
     */

    protected final void push(final VirtualFrame frame, final int currentPC, final int sp, final Object value) {
        setStackValue(frame, sp, resolve(currentPC, value));
    }

    protected final Object resolve(final int currentPC, final Object value) {
        final byte state = profiles[currentPC];
        if (value instanceof AbstractSqueakObjectWithClassAndHash) {
            if ((state & 0b01) == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                profiles[currentPC] |= 0b01;
            }
            return uncheckedCast(value, AbstractSqueakObjectWithClassAndHash.class).resolveForwardingPointer();
        } else {
            if ((state & 0b10) == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                profiles[currentPC] |= 0b10;
            }
            return value;
        }
    }

    protected final void pushResolved(final VirtualFrame frame, final int sp, final Object value) {
        setStackValue(frame, sp, value);
    }

    protected final Object pop(final VirtualFrame frame, final int sp) {
        assert sp >= numArguments;
        final int slotIndex = FrameAccess.toStackSlotIndex(sp);
        try {
            final Object result = frame.getObjectStatic(slotIndex);
            frame.setObjectStatic(slotIndex, NilObject.SINGLETON);
            return result;
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            CompilerDirectives.transferToInterpreter();
            final int auxSlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            final Object result = frame.getAuxiliarySlot(auxSlotIndex);
            frame.setAuxiliarySlot(auxSlotIndex, NilObject.SINGLETON);
            return result;
        }
    }

    protected final Object popReceiver(final VirtualFrame frame, final int sp) {
        return getStackValue(frame, sp);
    }

    protected final Object[] popN(final VirtualFrame frame, final int sp, final int numPop) {
        return numPop == 0 ? ArrayUtils.EMPTY_ARRAY : popNExploded(frame, sp, numPop);
    }

    @ExplodeLoop
    protected final Object[] popNExploded(final VirtualFrame frame, final int sp, final int numPop) {
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

    protected static final int checkPCAfterSend(final VirtualFrame frame, final int pc) {
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
        throw CompilerDirectives.shouldNotReachHere("Unknown bytecode");
    }

    public static final int calculateShortOffset(final int bytecode) {
        return (bytecode & 7) + 1;
    }

    /**
     * Smaller than int[1], does not kill int[] on write and doesn't need bounds checks.
     */
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

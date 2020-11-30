/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchLookupResultNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSuperSendNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSuperSendStackedNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimExitToDebuggerNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class SendBytecodes {
    public abstract static class AbstractSendNode extends AbstractInstrumentableBytecodeNode {
        private final int argumentCount;
        @CompilationFinal private FrameSlot stackPointerSlot;
        @CompilationFinal private int stackPointer;

        @Child private FrameStackPushNode pushNode;

        private final ConditionProfile nlrProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nvrProfile = ConditionProfile.createBinaryProfile();

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int numArgs) {
            super(code, index, numBytecodes);
            argumentCount = numArgs;
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.getNumBytecodes(), original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            try {
                decrementStackPointerByNumReceiverAndArguments(frame);
                final Object result = dispatchSend(frame);
                assert result != null : "Result of a message send should not be null";
                getPushNode().execute(frame, result);
            } catch (final NonLocalReturn nlr) {
                if (nlrProfile.profile(nlr.getTargetContextOrMarker() == getMarker(frame) || nlr.getTargetContextOrMarker() == getContext(frame))) {
                    getPushNode().execute(frame, nlr.getReturnValue());
                } else {
                    throw nlr;
                }
            } catch (final NonVirtualReturn nvr) {
                if (nvrProfile.profile(nvr.getTargetContext() == getContext(frame))) {
                    getPushNode().execute(frame, nvr.getReturnValue());
                } else {
                    throw nvr;
                }
            }
        }

        private void decrementStackPointerByNumReceiverAndArguments(final VirtualFrame frame) {
            if (stackPointerSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
                stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - (1 + argumentCount);
                assert stackPointer >= 0 : "Bad stack pointer";
            }
            FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
        }

        protected abstract Object dispatchSend(VirtualFrame frame);

        private FrameStackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(FrameStackPushNode.create());
            }
            return pushNode;
        }

        public abstract NativeObject getSelector();

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            if (tag == StandardTags.CallTag.class) {
                return true;
            }
            if (tag == DebuggerTags.AlwaysHalt.class) {
                return PrimExitToDebuggerNode.SELECTOR_NAME.equals(getSelector().asStringUnsafe());
            }
            return super.hasTag(tag);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + getSelector().asStringUnsafe();
        }
    }

    public abstract static class AbstractSelfSendNode extends AbstractSendNode {
        public static final int INLINE_CACHE_SIZE = 6;

        @Child private FrameSlotReadNode peekAtReceiverNode;
        @Child private LookupClassNode lookupClassNode = LookupClassNode.create();
        @Child private LookupSelectorNode lookupSelectorNode;
        @Child private DispatchLookupResultNode dispatchNode;

        private AbstractSelfSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            final NativeObject selector = (NativeObject) sel;
            lookupSelectorNode = LookupSelectorNode.create(selector);
            dispatchNode = DispatchLookupResultNode.create(selector, numArgs);
        }

        @Override
        protected final Object dispatchSend(final VirtualFrame frame) {
            final Object receiver = peekAtReceiver(frame);
            final ClassObject receiverClass = lookupClassNode.execute(receiver);
            final Object lookupResult = lookupSelectorNode.execute(receiverClass);
            return dispatchNode.execute(frame, receiver, receiverClass, lookupResult);
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        protected final Object peekAtReceiver(final VirtualFrame frame) {
            if (peekAtReceiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int stackPointer = FrameAccess.getStackPointer(frame, code);
                peekAtReceiverNode = insert(FrameSlotReadNode.create(code.getStackSlot(stackPointer)));
            }
            return peekAtReceiverNode.executeRead(frame);
        }
    }

    public static final class SecondExtendedSendNode extends AbstractSelfSendNode {
        public SecondExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            super(code, index, numBytecodes, code.getLiteral(param & 63), Byte.toUnsignedInt(param) >> 6);
        }
    }

    public static final class SendLiteralSelectorNode extends AbstractSelfSendNode {
        public SendLiteralSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }

        public static AbstractInstrumentableBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, numArgs);
        }
    }

    public static final class SendSpecialSelectorNode extends AbstractSelfSendNode {
        private SendSpecialSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }

        public static SendSpecialSelectorNode create(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final SqueakImageContext image = code.getSqueakClass().getImage(); // TODO: Refactor
            final NativeObject specialSelector = image.getSpecialSelector(selectorIndex);
            final int numArguments = image.getSpecialSelectorNumArgs(selectorIndex);
            return new SendSpecialSelectorNode(code, index, 1, specialSelector, numArguments);
        }
    }

    public static final class SendSelfSelectorNode extends AbstractSelfSendNode {
        public SendSelfSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }
    }

    public static final class SingleExtendedSendNode extends AbstractSelfSendNode {
        public SingleExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            super(code, index, numBytecodes, code.getLiteral(param & 31), Byte.toUnsignedInt(param) >> 5);
        }
    }

    public static final class SingleExtendedSuperNode extends AbstractSendNode {
        @Child private DispatchSuperSendNode dispatchNode;

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            this(code, index, numBytecodes, param & 31, Byte.toUnsignedInt(param) >> 5);
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            final NativeObject selector = (NativeObject) code.getLiteral(literalIndex);
            dispatchNode = DispatchSuperSendNode.create(code, selector, numArgs);
        }

        @Override
        protected Object dispatchSend(final VirtualFrame frame) {
            return dispatchNode.execute(frame);
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + getSelector().asStringUnsafe();
        }
    }

    public static final class SendToSuperclassStackedNode extends AbstractSendNode {
        @Child private DispatchSuperSendStackedNode dispatchNode;

        public SendToSuperclassStackedNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            final NativeObject selector = (NativeObject) code.getLiteral(literalIndex);
            dispatchNode = DispatchSuperSendStackedNode.create(selector, numArgs);
        }

        @Override
        protected Object dispatchSend(final VirtualFrame frame) {
            return dispatchNode.execute(frame);
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + getSelector().asStringUnsafe();
        }
    }
}

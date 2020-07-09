/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelfSendNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSuperSendNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimExitToDebuggerNode;

public final class SendBytecodes {
    public abstract static class AbstractSendNode extends AbstractInstrumentableBytecodeNode {
        protected final NativeObject selector;
        private final int argumentCount;

        @Child private FrameStackPushNode pushNode;

        private final ConditionProfile nlrProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nvrProfile = ConditionProfile.createBinaryProfile();

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            super(code, index, numBytecodes);
            selector = (NativeObject) sel;
            argumentCount = argcount;
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.numBytecodes, original.selector, original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            try {
                final Object result = dispatch(frame);
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

        protected abstract Object dispatch(VirtualFrame frame);

        private FrameStackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(FrameStackPushNode.create());
            }
            return pushNode;
        }

        public final Object getSelector() {
            return selector;
        }

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            if (tag == StandardTags.CallTag.class) {
                return true;
            }
            if (tag == DebuggerTags.AlwaysHalt.class) {
                return PrimExitToDebuggerNode.SELECTOR_NAME.equals(selector.asStringUnsafe());
            }
            return super.hasTag(tag);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + selector.asStringUnsafe();
        }
    }

    public abstract static class AbstractSelfSendNode extends AbstractSendNode {
        @Child private DispatchSelfSendNode dispatchSelfSendNode;

        private AbstractSelfSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            super(code, index, numBytecodes, sel, argcount);
            dispatchSelfSendNode = DispatchSelfSendNode.create(code, selector, argcount);
        }

        @Override
        protected final Object dispatch(final VirtualFrame frame) {
            return dispatchSelfSendNode.execute(frame);
        }
    }

    public static final class SecondExtendedSendNode extends AbstractSelfSendNode {
        public SecondExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            super(code, index, numBytecodes, code.getLiteral(param & 63), Byte.toUnsignedInt(param) >> 6);
        }
    }

    public static final class SendLiteralSelectorNode extends AbstractSelfSendNode {
        public SendLiteralSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }

        public static AbstractInstrumentableBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int argCount) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }
    }

    public static final class SendSpecialSelectorNode extends AbstractSelfSendNode {
        private SendSpecialSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argcount) {
            super(code, index, numBytecodes, selector, argcount);
        }

        public static SendSpecialSelectorNode create(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final NativeObject specialSelector = code.image.getSpecialSelector(selectorIndex);
            final int numArguments = code.image.getSpecialSelectorNumArgs(selectorIndex);
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
        @Child private DispatchSuperSendNode dispatchSuperSendNode;

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            this(code, index, numBytecodes, param & 31, Byte.toUnsignedInt(param) >> 5);
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs);
            dispatchSuperSendNode = DispatchSuperSendNode.create(code, selector, numArgs);
        }

        @Override
        protected Object dispatch(final VirtualFrame frame) {
            return dispatchSuperSendNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + selector.asStringUnsafe();
        }
    }
}

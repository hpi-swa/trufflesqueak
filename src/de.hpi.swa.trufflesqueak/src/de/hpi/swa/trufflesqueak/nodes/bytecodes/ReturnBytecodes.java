/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ReturnBytecodes {

    public abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("executeReturn() should be called instead");
        }

        public final Object executeReturn(final VirtualFrame frame) {
            return executeReturnSpecialized(frame);
        }

        protected abstract Object executeReturnSpecialized(VirtualFrame frame);

        protected abstract Object getReturnValue(VirtualFrame frame);
    }

    public abstract static class AbstractNormalReturnNode extends AbstractReturnNode {
        @Child protected AbstractReturnKindNode returnNode;

        protected AbstractNormalReturnNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(code, index);
            returnNode = FrameAccess.hasClosure(frame) ? new ReturnFromClosureNode() : new ReturnFromMethodNode(frame);
        }

        @Override
        public final Object executeReturnSpecialized(final VirtualFrame frame) {
            return returnNode.execute(frame, getReturnValue(frame));
        }
    }

    private abstract static class AbstractReturnKindNode extends AbstractNode {
        protected abstract Object execute(VirtualFrame frame, Object returnValue);
    }

    private static final class ReturnFromMethodNode extends AbstractReturnKindNode {
        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.createBinaryProfile();
        private final FrameSlot contextSlot;

        private ReturnFromMethodNode(final VirtualFrame frame) {
            contextSlot = FrameAccess.findContextSlot(frame);
        }

        @Override
        protected Object execute(final VirtualFrame frame, final Object returnValue) {
            assert !FrameAccess.hasClosure(frame);
            if (hasModifiedSenderProfile.profile(hasModifiedSender(frame))) {
                throw new NonLocalReturn(returnValue, FrameAccess.getSenderContext(frame));
            } else {
                return returnValue;
            }
        }

        private boolean hasModifiedSender(final VirtualFrame frame) {
            final ContextObject context = FrameAccess.getContext(frame, contextSlot);
            return context != null && context.hasModifiedSender();
        }
    }

    private static final class ReturnFromClosureNode extends AbstractReturnKindNode {
        @Child private GetActiveProcessNode getActiveProcessNode = GetActiveProcessNode.create();

        @Override
        protected Object execute(final VirtualFrame frame, final Object returnValue) {
            assert FrameAccess.hasClosure(frame);
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canReturnTo()) {
                throw new NonLocalReturn(returnValue, (ContextObject) homeContext.getFrameSender());
            } else {
                CompilerDirectives.transferToInterpreter();
                final ContextObject contextObject = GetOrCreateContextNode.getOrCreateUncached(frame);
                lookupContext().cannotReturn.executeAsSymbolSlow(frame, contextObject, returnValue);
                throw SqueakException.create("Should not reach");
            }
        }
    }

    protected abstract static class AbstractReturnConstantNode extends AbstractNormalReturnNode {
        protected AbstractReturnConstantNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "return: " + getReturnValue(null).toString();
        }
    }

    public static final class ReturnConstantTrueNode extends AbstractReturnConstantNode {
        protected ReturnConstantTrueNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return BooleanObject.TRUE;
        }
    }

    public static final class ReturnConstantFalseNode extends AbstractReturnConstantNode {
        protected ReturnConstantFalseNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return BooleanObject.FALSE;
        }
    }

    public static final class ReturnConstantNilNode extends AbstractReturnConstantNode {
        protected ReturnConstantNilNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return NilObject.SINGLETON;
        }
    }

    public static final class ReturnReceiverNode extends AbstractNormalReturnNode {
        protected ReturnReceiverNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return FrameAccess.getReceiver(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnSelf";
        }
    }

    public abstract static class AbstractBlockReturnNode extends AbstractReturnNode {
        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.createBinaryProfile();

        protected AbstractBlockReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        public final Object executeReturnSpecialized(final VirtualFrame frame) {
            if (hasModifiedSenderProfile.profile(hasModifiedSender(frame))) {
                // Target is sender of closure's home context.
                final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                if (homeContext.canReturnTo()) {
                    throw new NonLocalReturn(getReturnValue(frame), (ContextObject) homeContext.getFrameSender());
                } else {
                    CompilerDirectives.transferToInterpreter();
                    final ContextObject contextObject = GetOrCreateContextNode.getOrCreateUncached(frame);
                    lookupContext().cannotReturn.executeAsSymbolSlow(frame, contextObject, getReturnValue(frame));
                    throw SqueakException.create("Should not reach");
                }
            } else {
                return getReturnValue(frame);
            }
        }

        private boolean hasModifiedSender(final VirtualFrame frame) {
            final ContextObject context = getContext(frame);
            return context != null && context.hasModifiedSender();
        }
    }

    public static final class ReturnTopFromBlockNode extends AbstractBlockReturnNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return popNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn";
        }
    }

    public static final class ReturnNilFromBlockNode extends AbstractBlockReturnNode {
        protected ReturnNilFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return NilObject.SINGLETON;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn: nil";
        }
    }

    public static final class ReturnTopFromMethodNode extends AbstractNormalReturnNode {
        @Child private FrameStackPopNode popNode = FrameStackPopNode.create();

        protected ReturnTopFromMethodNode(final VirtualFrame frame, final CompiledCodeObject code, final int index) {
            super(frame, code, index);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return popNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnTop";
        }
    }
}

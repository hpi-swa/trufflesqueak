/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackTopNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class ReturnBytecodes {

    public abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final int successorIndex) {
            super(successorIndex);
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
        @Child private AbstractReturnKindNode returnNode;

        protected AbstractNormalReturnNode(final VirtualFrame frame, final int successorIndex) {
            super(successorIndex);
            returnNode = FrameAccess.hasClosure(frame) ? new ReturnFromClosureNode() : new ReturnFromMethodNode();
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

        /* Return to sender (never needs to unwind) */

        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.create();

        @Override
        protected Object execute(final VirtualFrame frame, final Object returnValue) {
            assert !FrameAccess.hasClosure(frame);
            if (hasModifiedSenderProfile.profile(FrameAccess.hasModifiedSender(frame))) {
                throw new NonVirtualReturn(returnValue, FrameAccess.getSender(frame));
            } else {
                return returnValue;
            }
        }
    }

    private static final class ReturnFromClosureNode extends AbstractReturnKindNode {
        @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode;
        @Child private Dispatch2Node sendAboutToReturnNode;

        /* Return to closure's home context's sender, executing unwind blocks */

        @Override
        protected Object execute(final VirtualFrame frame, final Object returnValue) {
            assert FrameAccess.hasClosure(frame);
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canBeReturnedTo()) {
                final ContextObject firstMarkedContext = firstUnwindMarkedOrThrowNLR(FrameAccess.getSender(frame), homeContext, returnValue);
                if (firstMarkedContext != null) {
                    getSendAboutToReturnNode().execute(frame, getGetOrCreateContextNode().executeGet(frame), returnValue, firstMarkedContext);
                    throw CompilerDirectives.shouldNotReachHere();
                }
            }
            CompilerDirectives.transferToInterpreter();
            LogUtils.SCHEDULING.info("ReturnFromClosureNode: sendCannotReturn");
            throw new CannotReturnToTarget(returnValue, GetOrCreateContextWithFrameNode.executeUncached(frame));
        }

        /**
         * Walk the sender chain starting at the given frame sender and terminating at homeContext.
         *
         * @return null if homeContext is not on sender chain; return first marked Context if found;
         *         raise NLR otherwise
         */
        @TruffleBoundary
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

    protected abstract static class AbstractReturnConstantNode extends AbstractNormalReturnNode {
        protected AbstractReturnConstantNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "return: " + getReturnValue(null).toString();
        }
    }

    public static final class ReturnConstantTrueNode extends AbstractReturnConstantNode {
        public ReturnConstantTrueNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return BooleanObject.TRUE;
        }
    }

    public static final class ReturnConstantFalseNode extends AbstractReturnConstantNode {
        public ReturnConstantFalseNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return BooleanObject.FALSE;
        }
    }

    public static final class ReturnConstantNilNode extends AbstractReturnConstantNode {
        public ReturnConstantNilNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return NilObject.SINGLETON;
        }
    }

    public static final class ReturnReceiverNode extends AbstractNormalReturnNode {
        public ReturnReceiverNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
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

        /* Return to caller (never needs to unwind) */

        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.create();

        protected AbstractBlockReturnNode(final int successorIndex) {
            super(successorIndex);
        }

        @Override
        public final Object executeReturnSpecialized(final VirtualFrame frame) {
            if (hasModifiedSenderProfile.profile(FrameAccess.hasModifiedSender(frame))) {
                throw new NonVirtualReturn(getReturnValue(frame), FrameAccess.getSender(frame));
            } else {
                return getReturnValue(frame);
            }
        }
    }

    public static final class ReturnTopFromBlockNode extends AbstractBlockReturnNode {
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public ReturnTopFromBlockNode(final int successorIndex) {
            super(successorIndex);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return topNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn";
        }
    }

    public static final class ReturnNilFromBlockNode extends AbstractBlockReturnNode {
        public ReturnNilFromBlockNode(final int successorIndex) {
            super(successorIndex);
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
        @Child private FrameStackTopNode topNode = FrameStackTopNode.create();

        public ReturnTopFromMethodNode(final VirtualFrame frame, final int successorIndex) {
            super(frame, successorIndex);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return topNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnTop";
        }
    }
}

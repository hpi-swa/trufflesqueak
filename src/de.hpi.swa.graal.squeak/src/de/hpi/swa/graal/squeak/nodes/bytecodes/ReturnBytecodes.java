/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    public abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected static final AbstractSqueakObject findOnSenderChain(final ContextObject from, final ContextObject target) {
            AbstractSqueakObject sender = from.getSender();
            while (sender instanceof ContextObject) {
                if (sender == target) {
                    return sender;
                }
                sender = ((ContextObject) sender).getSender();
            }
            return NilObject.SINGLETON;
        }

        protected final boolean hasMaterializedContext(final VirtualFrame frame) {
            return getContext(frame) != null;
        }

        protected final boolean hasModifiedSender(final VirtualFrame frame) {
            final ContextObject context = getContext(frame);
            return context != null && context.hasModifiedSender();
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("executeReturn() should be called instead");
        }

        public final Object executeReturn(final VirtualFrame frame) {
            return executeReturnSpecialized(frame);
        }

        protected abstract Object executeReturnSpecialized(VirtualFrame frame);

        @SuppressWarnings("unused")
        protected Object getReturnValue(final VirtualFrame frame) {
            throw SqueakException.create("Needs to be overriden");
        }
    }

    protected abstract static class AbstractReturnWithSpecializationsNode extends AbstractReturnNode {
        @Child private GetOrCreateContextNode getOrCreateContextNode;
        @Child private SendSelectorNode cannotReturnNode;

        protected AbstractReturnWithSpecializationsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Specialization(guards = {"isCompiledMethodObject(code)", "!hasModifiedSender(frame)"})
        protected final Object doLocalReturn(final VirtualFrame frame) {
            return getReturnValue(frame);
        }

        @Specialization(guards = {"isCompiledMethodObject(code)", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturn(final VirtualFrame frame) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"isCompiledBlockObject(code)", "hasMaterializedContext(frame)"})
        protected final Object doClosureReturnFromMaterialized(final VirtualFrame frame) {
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            final ContextObject currentContext = getContext(frame);
            final boolean homeContextNotOnTheStack = null == findOnSenderChain(currentContext, homeContext);
            final Object caller = homeContext.getFrameSender();
            if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                getCannotReturnNode().executeSend(frame, currentContext, getReturnValue(frame));
                assert false : "Should not reach";
            }
            throw new NonLocalReturn(getReturnValue(frame), caller);
        }

        @Specialization(guards = {"isCompiledBlockObject(code)", "!hasMaterializedContext(frame)"})
        protected final Object doClosureReturnFromNonMaterialized(final VirtualFrame frame) {
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            boolean homeContextNotOnTheStack = false;
            final Object caller = homeContext.getFrameSender();
            if (caller != NilObject.SINGLETON) {
                final Object[] lastSender = new Object[1];
                homeContextNotOnTheStack = null == Truffle.getRuntime().iterateFrames(frameInstance -> {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                    if (!FrameAccess.isGraalSqueakFrame(current)) {
                        return null; // Foreign frame cannot be homeContext.
                    }
                    if (FrameAccess.getContext(current) == homeContext) {
                        return homeContext;
                    } else {
                        lastSender[0] = FrameAccess.getSender(current);
                        return null;
                    }
                });
                if (homeContextNotOnTheStack && lastSender[0] instanceof ContextObject) {
                    homeContextNotOnTheStack = null == findOnSenderChain((ContextObject) lastSender[0], homeContext);
                }
            }
            if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), getReturnValue(frame));
                assert false : "Should not reach";
            }
            throw new NonLocalReturn(getReturnValue(frame), caller);
        }

        private GetOrCreateContextNode getGetOrCreateContextNode() {
            if (getOrCreateContextNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOrCreateContextNode = insert(GetOrCreateContextNode.create(code));
            }
            return getOrCreateContextNode;
        }

        private SendSelectorNode getCannotReturnNode() {
            if (cannotReturnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cannotReturnNode = insert(SendSelectorNode.create(code, code.image.cannotReturn));
            }
            return cannotReturnNode;
        }
    }

    public abstract static class ReturnConstantNode extends AbstractReturnWithSpecializationsNode {
        public final Object constant;

        protected ReturnConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        public static ReturnConstantNode create(final CompiledCodeObject code, final int index, final Object value) {
            return ReturnConstantNodeGen.create(code, index, value);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return constant;
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "return: " + constant.toString();
        }
    }

    public abstract static class ReturnReceiverNode extends AbstractReturnWithSpecializationsNode {

        protected ReturnReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static ReturnReceiverNode create(final CompiledCodeObject code, final int index) {
            return ReturnReceiverNodeGen.create(code, index);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return FrameAccess.getReceiver(frame);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnSelf";
        }
    }

    public abstract static class ReturnTopFromBlockNode extends AbstractReturnNode {
        @Child private FrameStackPopNode popNode;
        @Child private SendSelectorNode cannotReturnNode;

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = FrameStackPopNode.create(code);
        }

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        @Specialization(guards = {"!hasModifiedSender(frame)"})
        protected final Object doLocalReturn(final VirtualFrame frame) {
            return getReturnValue(frame);
        }

        @Specialization(guards = {"isCompiledMethodObject(code)", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturn(final VirtualFrame frame) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"isCompiledBlockObject(code)", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturnClosure(final VirtualFrame frame) {
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            final ContextObject currentContext = FrameAccess.getContext(frame);
            final boolean homeContextNotOnTheStack = null == findOnSenderChain(currentContext, homeContext);
            final Object caller = homeContext.getFrameSender();
            if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                getCannotReturnNode().executeSend(frame, currentContext, getReturnValue(frame));
                assert false : "Should not reach";
            }
            throw new NonLocalReturn(getReturnValue(frame), caller);
        }

        private SendSelectorNode getCannotReturnNode() {
            if (cannotReturnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cannotReturnNode = insert(SendSelectorNode.create(code, code.image.cannotReturn));
            }
            return cannotReturnNode;
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.execute(frame);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn";
        }
    }

    public abstract static class ReturnTopFromMethodNode extends AbstractReturnWithSpecializationsNode {
        @Child private FrameStackPopNode popNode;

        protected ReturnTopFromMethodNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = FrameStackPopNode.create(code);
        }

        public static ReturnTopFromMethodNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.execute(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnTop";
        }
    }
}

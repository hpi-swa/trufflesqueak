package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    protected abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        @Child protected FrameArgumentNode readClosureNode = FrameArgumentNode.create(FrameAccess.CLOSURE_OR_NULL);

        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected boolean hasClosure(final VirtualFrame frame) {
            return readClosureNode.executeRead(frame) instanceof BlockClosureObject;
        }

        protected static final boolean hasModifiedSender(final VirtualFrame frame) {
            return getContext(frame).hasModifiedSender();
        }

        protected Object getReturnValue(@SuppressWarnings("unused") final VirtualFrame frame) {
            throw new SqueakException("Needs to be overriden");
        }
    }

    protected abstract static class AbstractReturnNodeWithSpecializations extends AbstractReturnNode {

        protected AbstractReturnNodeWithSpecializations(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Specialization(guards = {"!hasClosure(frame)", "isVirtualized(frame) || !hasModifiedSender(frame)"})
        protected final Object executeLocalReturn(final VirtualFrame frame) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"hasClosure(frame) || !isVirtualized(frame)", "hasClosure(frame) || hasModifiedSender(frame)"})
        protected final Object executeNonLocalReturn(final VirtualFrame frame,
                        @Cached("create()") final GetOrCreateContextNode getContextNode) {
            final ContextObject outerContext;
            final BlockClosureObject block = (BlockClosureObject) readClosureNode.executeRead(frame);
            if (block != null) {
                outerContext = block.getHomeContext();
            } else {
                outerContext = getContextNode.executeGet(frame);
            }
            throw new NonLocalReturn(getReturnValue(frame), outerContext);
        }
    }

    public abstract static class ReturnConstantNode extends AbstractReturnNodeWithSpecializations {
        private final Object constant;

        public static ReturnConstantNode create(final CompiledCodeObject code, final int index, final Object value) {
            return ReturnConstantNodeGen.create(code, index, value);
        }

        protected ReturnConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return constant;
        }

        @Override
        public final String toString() {
            return "return: " + constant.toString();
        }
    }

    public abstract static class ReturnReceiverNode extends AbstractReturnNodeWithSpecializations {
        @Child private ReceiverNode receiverNode;

        public static ReturnReceiverNode create(final CompiledCodeObject code, final int index) {
            return ReturnReceiverNodeGen.create(code, index);
        }

        protected ReturnReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            receiverNode = ReceiverNode.create(code);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return receiverNode.executeRead(frame);
        }

        @Override
        public final String toString() {
            return "returnSelf";
        }
    }

    public abstract static class ReturnTopFromBlockNode extends AbstractReturnNode {
        @Child protected StackPopNode popNode;

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        @Specialization(guards = {"isVirtualized(frame) || !hasModifiedSender(frame)"})
        protected final Object executeLocalReturn(final VirtualFrame frame) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"!isVirtualized(frame)", "hasModifiedSender(frame)"})
        protected final Object executeNonLocalReturn(final VirtualFrame frame,
                        @Cached("create()") final GetOrCreateContextNode getContextNode) {
            final ContextObject outerContext;
            final BlockClosureObject block = (BlockClosureObject) readClosureNode.executeRead(frame);
            if (block != null) {
                outerContext = block.getHomeContext();
            } else {
                outerContext = getContextNode.executeGet(frame);
            }
            throw new NonLocalReturn(getReturnValue(frame), outerContext);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.executeRead(frame);
        }

        @Override
        public final String toString() {
            return "blockReturn";
        }
    }

    public abstract static class ReturnTopFromMethodNode extends AbstractReturnNodeWithSpecializations {
        @Child protected StackPopNode popNode;

        public static ReturnTopFromMethodNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        protected ReturnTopFromMethodNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.executeRead(frame);
        }

        @Override
        public String toString() {
            return "returnTop";
        }
    }
}

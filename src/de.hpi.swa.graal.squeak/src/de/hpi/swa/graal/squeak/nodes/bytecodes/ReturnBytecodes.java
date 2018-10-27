package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    protected abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected boolean hasClosure(final VirtualFrame frame) {
            return FrameAccess.getClosure(frame) != null;
        }

        protected final boolean hasModifiedSender(final VirtualFrame frame) {
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

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            executeReturn(frame, FrameAccess.getClosure(frame));
        }

        protected abstract void executeReturn(VirtualFrame frame, Object closure);

        @Specialization(guards = {"closure == null", "isVirtualized(frame) || !hasModifiedSender(frame)"})
        protected final void doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"closure == null", "!isVirtualized(frame)", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure,
                        @Cached("create(code)") final GetOrCreateContextNode getContextNode) {
            throw new NonLocalReturn(getReturnValue(frame), getContextNode.executeGet(frame));
        }

        @Specialization(guards = {"closure != null"})
        protected final void doNonLocalReturn(final VirtualFrame frame, final BlockClosureObject closure) {
            throw new NonLocalReturn(getReturnValue(frame), closure.getHomeContext());
        }

        @Fallback
        protected static final void doFail(final Object closure) {
            throw new SqueakException("Unexpected closure argument:" + closure);
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
        @Child private StackPopNode popNode;

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            executeReturn(frame, FrameAccess.getClosure(frame));
        }

        protected abstract void executeReturn(VirtualFrame frame, Object closure);

        @Specialization(guards = {"isVirtualized(frame) || !hasModifiedSender(frame)"})
        protected final void doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"closureOrNull == null", "!isVirtualized(frame)", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull,
                        @Cached("create(code)") final GetOrCreateContextNode getContextNode) {
            throw new NonLocalReturn(getReturnValue(frame), getContextNode.executeGet(frame));
        }

        @Specialization(guards = {"closure != null", "!isVirtualized(frame)", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, final BlockClosureObject closure) {
            throw new NonLocalReturn(getReturnValue(frame), closure.getHomeContext());
        }

        @Fallback
        protected static final void doFail(final Object closure) {
            throw new SqueakException("Unexpected closure argument:" + closure);
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
        @Child private StackPopNode popNode;

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

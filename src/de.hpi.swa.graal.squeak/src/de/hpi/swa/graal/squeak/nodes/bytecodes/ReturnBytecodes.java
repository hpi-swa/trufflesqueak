package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    protected abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected final boolean hasModifiedSender(final VirtualFrame frame) {
            final ContextObject context = getContext(frame);
            return context != null && context.hasModifiedSender();
        }

        protected Object getReturnValue(@SuppressWarnings("unused") final VirtualFrame frame) {
            throw SqueakException.create("Needs to be overriden");
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

        @Specialization(guards = {"closure == null", "!hasModifiedSender(frame)"})
        protected final void doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"closure == null", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"closure != null"})
        protected final void doClosureReturn(final VirtualFrame frame, final BlockClosureObject closure) {
            // Target is sender of closure's home context.
            throw new NonLocalReturn(getReturnValue(frame), closure.getHomeContext().getFrameSender());
        }
    }

    public abstract static class ReturnConstantNode extends AbstractReturnNodeWithSpecializations {
        private final Object constant;

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

    public abstract static class ReturnReceiverNode extends AbstractReturnNodeWithSpecializations {

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
        @Child private StackPopNode popNode;

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            executeReturn(frame, FrameAccess.getClosure(frame));
        }

        protected abstract void executeReturn(VirtualFrame frame, Object closure);

        @Specialization(guards = {"!hasModifiedSender(frame)"})
        protected final void doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = {"closureOrNull == null", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"closureOrNull != null", "hasModifiedSender(frame)"})
        protected final void doNonLocalReturn(final VirtualFrame frame, final BlockClosureObject closureOrNull) {
            throw new NonLocalReturn(getReturnValue(frame), closureOrNull.getHomeContext().getFrameSender());
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.executeRead(frame);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn";
        }
    }

    public abstract static class ReturnTopFromMethodNode extends AbstractReturnNodeWithSpecializations {
        @Child private StackPopNode popNode;

        protected ReturnTopFromMethodNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        public static ReturnTopFromMethodNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame) {
            return popNode.executeRead(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnTop";
        }
    }
}

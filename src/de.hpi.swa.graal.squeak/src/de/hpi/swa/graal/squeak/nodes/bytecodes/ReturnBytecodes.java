package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    public abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        protected final boolean hasModifiedSender(final VirtualFrame frame) {
            final ContextObject context = getContext(frame);
            return context != null && context.hasModifiedSender();
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("executeReturn() should be called instead");
        }

        public final Object executeReturn(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
            return executeReturnSpecialized(frame, FrameAccess.getClosure(frame), readAndClearNode);
        }

        protected abstract Object executeReturnSpecialized(VirtualFrame frame, Object closure, FrameStackReadAndClearNode readAndClearNode);

        @SuppressWarnings("unused")
        protected Object getReturnValue(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
            throw SqueakException.create("Needs to be overriden");
        }
    }

    protected abstract static class AbstractReturnWithSpecializationsNode extends AbstractReturnNode {

        protected AbstractReturnWithSpecializationsNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Specialization(guards = {"closure == null", "!hasModifiedSender(frame)"})
        protected final Object doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure, final FrameStackReadAndClearNode readAndClearNode) {
            return getReturnValue(frame, readAndClearNode);
        }

        @Specialization(guards = {"closure == null", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closure, final FrameStackReadAndClearNode readAndClearNode) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame, readAndClearNode), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"closure != null"})
        protected final Object doClosureReturn(final VirtualFrame frame, final BlockClosureObject closure, final FrameStackReadAndClearNode readAndClearNode) {
            // Target is sender of closure's home context.
            throw new NonLocalReturn(getReturnValue(frame, readAndClearNode), closure.getHomeContext().getFrameSender());
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
        protected final Object getReturnValue(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
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
        protected final Object getReturnValue(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
            return FrameAccess.getReceiver(frame);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnSelf";
        }
    }

    public abstract static class ReturnTopFromBlockNode extends AbstractReturnNode {
        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        @Specialization(guards = {"!hasModifiedSender(frame)"})
        protected final Object doLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull, final FrameStackReadAndClearNode readAndClearNode) {
            return getReturnValue(frame, readAndClearNode);
        }

        @Specialization(guards = {"closureOrNull == null", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturn(final VirtualFrame frame, @SuppressWarnings("unused") final Object closureOrNull, final FrameStackReadAndClearNode readAndClearNode) {
            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
            throw new NonLocalReturn(getReturnValue(frame, readAndClearNode), FrameAccess.getSender(frame));
        }

        @Specialization(guards = {"closureOrNull != null", "hasModifiedSender(frame)"})
        protected final Object doNonLocalReturn(final VirtualFrame frame, final BlockClosureObject closureOrNull, final FrameStackReadAndClearNode readAndClearNode) {
            throw new NonLocalReturn(getReturnValue(frame, readAndClearNode), closureOrNull.getHomeContext().getFrameSender());
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
            return readAndClearNode.executePop(frame);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "blockReturn";
        }
    }

    public abstract static class ReturnTopFromMethodNode extends AbstractReturnWithSpecializationsNode {
        protected ReturnTopFromMethodNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static ReturnTopFromMethodNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        @Override
        protected final Object getReturnValue(final VirtualFrame frame, final FrameStackReadAndClearNode readAndClearNode) {
            return readAndClearNode.executePop(frame);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "returnTop";
        }
    }
}

package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.FreshReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ReturnBytecodes {

    protected abstract static class AbstractReturnNode extends AbstractBytecodeNode {
        @Child protected FrameSlotReadNode readContextNode;
        @Child protected FrameArgumentNode readClosureNode;

        protected AbstractReturnNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            readContextNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
            readClosureNode = FrameArgumentNode.create(FrameAccess.CLOSURE_OR_NULL);
        }

        protected boolean hasClosure(final VirtualFrame frame) {
            return readClosureNode.executeRead(frame) instanceof BlockClosureObject;
        }

        protected boolean isDirty(final VirtualFrame frame) {
            return getContext(frame).isDirty();
        }

        @Specialization(guards = {"!hasClosure(frame)", "isVirtualized(frame) || !isDirty(frame)"})
        protected Object executeLocalReturn(final VirtualFrame frame) {
            throw new FreshReturn(new LocalReturn(getReturnValue(frame)));
        }

        @Specialization(guards = {"hasClosure(frame) || !isVirtualized(frame)", "hasClosure(frame) || isDirty(frame)"})
        protected Object executeNonLocalReturn(final VirtualFrame frame) {
            final ContextObject outerContext;
            final BlockClosureObject block = (BlockClosureObject) readClosureNode.executeRead(frame);
            if (block != null) {
                outerContext = block.getHomeContext();
            } else {
                outerContext = (ContextObject) readContextNode.executeRead(frame);
            }
            throw new FreshReturn(new NonLocalReturn(getReturnValue(frame), outerContext));
        }

        protected Object getReturnValue(@SuppressWarnings("unused") final VirtualFrame frame) {
            throw new SqueakException("Needs to be overriden");
        }
    }

    public abstract static class ReturnConstantNode extends AbstractReturnNode {
        @CompilationFinal private final Object constant;

        public static ReturnConstantNode create(final CompiledCodeObject code, final int index, final Object value) {
            return ReturnConstantNodeGen.create(code, index, value);
        }

        protected ReturnConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return constant;
        }

        @Override
        public String toString() {
            return "return: " + constant.toString();
        }
    }

    public abstract static class ReturnReceiverNode extends AbstractReturnNode {
        @Child private ReceiverNode receiverNode;

        public static ReturnReceiverNode create(final CompiledCodeObject code, final int index) {
            return ReturnReceiverNodeGen.create(code, index);
        }

        protected ReturnReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            receiverNode = ReceiverNode.create(code);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return receiverNode.executeRead(frame);
        }

        @Override
        public String toString() {
            return "returnSelf";
        }
    }

    public abstract static class ReturnTopFromBlockNode extends ReturnTopFromMethodNode {

        public static ReturnTopFromBlockNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        protected ReturnTopFromBlockNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        @Override
        protected boolean hasClosure(final VirtualFrame frame) {
            return false;
        }

        @Override
        public String toString() {
            return "blockReturn";
        }
    }

    public abstract static class ReturnTopFromMethodNode extends AbstractReturnNode {
        @Child protected StackPopNode popNode;

        public static ReturnTopFromMethodNode create(final CompiledCodeObject code, final int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        protected ReturnTopFromMethodNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            popNode = StackPopNode.create(code);
        }

        @Override
        protected Object getReturnValue(final VirtualFrame frame) {
            return popNode.executeRead(frame);
        }

        @Override
        public String toString() {
            return "returnTop";
        }
    }
}

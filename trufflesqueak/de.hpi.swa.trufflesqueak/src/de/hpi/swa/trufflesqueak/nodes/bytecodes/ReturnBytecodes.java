package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ReturnBytecodes {

    protected static abstract class AbstractReturnNode extends AbstractBytecodeNode {

        protected AbstractReturnNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        protected boolean isLocalReturn(VirtualFrame frame) {
            boolean hasNoClosure = FrameAccess.getClosure(frame) == null;
            Object context = FrameAccess.getContextOrMarker(frame);
            if (context instanceof ContextObject) {
                return hasNoClosure && !((ContextObject) context).isDirty();
            } else {
                return hasNoClosure;
            }
        }

        @Specialization(guards = "isLocalReturn(frame)")
        protected Object executeLocalReturn(VirtualFrame frame) {
            throw new LocalReturn(getReturnValue(frame));
        }

        @Specialization(guards = "!isLocalReturn(frame)")
        protected Object executeNonLocalReturn(VirtualFrame frame) {
            BlockClosureObject block = FrameAccess.getClosure(frame);
            ContextObject outerContext = block.getHomeContext();
            throw new NonLocalReturn(getReturnValue(frame), null, outerContext); // FIXME: null
        }

        protected Object getReturnValue(@SuppressWarnings("unused") VirtualFrame frame) {
            throw new RuntimeException("Needs to be overriden");
        }
    }

    public static abstract class ReturnConstantNode extends AbstractReturnNode {
        @CompilationFinal private final Object constant;

        public static ReturnConstantNode create(CompiledCodeObject code, int index, Object value) {
            return ReturnConstantNodeGen.create(code, index, value);
        }

        protected ReturnConstantNode(CompiledCodeObject code, int index, Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        protected Object getReturnValue(VirtualFrame frame) {
            return constant;
        }

        @Override
        public String toString() {
            return "return: " + constant.toString();
        }
    }

    public static abstract class ReturnReceiverNode extends AbstractReturnNode {
        @Child private ReceiverNode receiverNode;

        public static ReturnReceiverNode create(CompiledCodeObject code, int index) {
            return ReturnReceiverNodeGen.create(code, index);
        }

        protected ReturnReceiverNode(CompiledCodeObject code, int index) {
            super(code, index);
            receiverNode = ReceiverNode.create(code);
        }

        @Override
        protected Object getReturnValue(VirtualFrame frame) {
            return receiverNode.executeGeneric(frame);
        }

        @Override
        public String toString() {
            return "returnSelf";
        }
    }

    public static abstract class ReturnTopFromBlockNode extends ReturnTopFromMethodNode {

        public static ReturnTopFromBlockNode create(CompiledCodeObject code, int index) {
            return ReturnTopFromBlockNodeGen.create(code, index);
        }

        protected ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        @Override
        protected boolean isLocalReturn(VirtualFrame frame) {
            Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
            return contextOrMarker instanceof ContextObject ? !((ContextObject) contextOrMarker).isDirty() : true;
        }

        @Override
        public String toString() {
            return "blockReturn";
        }
    }

    public static abstract class ReturnTopFromMethodNode extends AbstractReturnNode {
        @Child protected PopStackNode popNode;

        public static ReturnTopFromMethodNode create(CompiledCodeObject code, int index) {
            return ReturnTopFromMethodNodeGen.create(code, index);
        }

        protected ReturnTopFromMethodNode(CompiledCodeObject code, int index) {
            super(code, index);
            popNode = PopStackNode.create(code);
        }

        @Override
        protected Object getReturnValue(VirtualFrame frame) {
            return popNode.executeGeneric(frame);
        }

        @Override
        public String toString() {
            return "returnTop";
        }
    }
}

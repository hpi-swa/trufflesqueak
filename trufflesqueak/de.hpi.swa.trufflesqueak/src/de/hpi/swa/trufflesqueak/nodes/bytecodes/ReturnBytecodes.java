package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ReturnBytecodes {

    private static abstract class AbstractReturnNode extends AbstractBytecodeNode {

        protected AbstractReturnNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            BlockClosureObject block = FrameAccess.getClosure(frame);
            Object returnValue = getReturnValue(frame);
            if (block == null || localReturn()) { // TODO: should be false if context is dirty
                throw new LocalReturn(returnValue);
            } else {
                MethodContextObject targetContext = block.getOuterContextOrNull();
                throw new NonLocalReturn(returnValue, block.getFrameMarker(), targetContext);
            }
        }

        protected boolean localReturn() {
            return false;
        }

        protected abstract Object getReturnValue(VirtualFrame frame);
    }

    public static class ReturnConstantNode extends AbstractReturnNode {
        @CompilationFinal private final Object constant;

        public ReturnConstantNode(CompiledCodeObject code, int index, Object obj) {
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

    public static class ReturnReceiverNode extends AbstractReturnNode {
        @Child private ReceiverNode receiverNode;

        public ReturnReceiverNode(CompiledCodeObject code, int index) {
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

    public static class ReturnTopFromBlockNode extends ReturnTopFromMethodNode {

        public ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        @Override
        protected boolean localReturn() {
            return true;
        }

        @Override
        public String toString() {
            return "blockReturn";
        }
    }

    public static class ReturnTopFromMethodNode extends AbstractReturnNode {
        @Child protected PopStackNode popNode;

        public ReturnTopFromMethodNode(CompiledCodeObject code, int index) {
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

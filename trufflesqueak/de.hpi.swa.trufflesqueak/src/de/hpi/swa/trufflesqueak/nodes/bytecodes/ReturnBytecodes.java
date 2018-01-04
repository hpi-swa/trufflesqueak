package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public final class ReturnBytecodes {

    public static class ReturnConstantNode extends AbstractBytecodeNode {
        @CompilationFinal private final Object constant;

        public ReturnConstantNode(CompiledCodeObject code, int index, Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw new LocalReturn(constant);
        }

        @Override
        public String toString() {
            return "return: " + constant.toString();
        }
    }

    public static class ReturnReceiverNode extends AbstractBytecodeNode {
        @Child private FrameReceiverNode receiverNode = new FrameReceiverNode();

        public ReturnReceiverNode(CompiledCodeObject code, int index) {
            super(code, index);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw new LocalReturn(receiverNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "returnSelf";
        }
    }

    public static class ReturnTopFromBlockNode extends AbstractBytecodeNode {
        @Child protected PopStackNode popNode;

        public ReturnTopFromBlockNode(CompiledCodeObject code, int index) {
            super(code, index);
            popNode = new PopStackNode(code);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            throw new LocalReturn(popNode.executeGeneric(frame));
        }

        @Override
        public String toString() {
            return "blockReturn";
        }
    }

    public static class ReturnTopFromMethodNode extends ReturnTopFromBlockNode {

        public ReturnTopFromMethodNode(CompiledCodeObject code, int idx) {
            super(code, idx);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            if (getClosure(frame) == code.image.nil) {
                super.executeVoid(frame);
            } else {
                throw new RuntimeException("Not yet implemented");
                // throw new NonLocalReturn(popNode.executeGeneric(frame), ((BlockClosure)
                // getClosure(frame)).getFrameMarker());
            }
        }

        @Override
        public String toString() {
            return "returnTop";
        }
    }
}

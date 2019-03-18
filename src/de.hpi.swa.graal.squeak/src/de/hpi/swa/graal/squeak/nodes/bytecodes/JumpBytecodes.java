package de.hpi.swa.graal.squeak.nodes.bytecodes;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.ExecuteContextNode.GetSuccessorNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode.HandleConditionResultNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodesFactory.ConditionalJumpNodeFactory.HandleConditionResultNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public final class JumpBytecodes {

    public static final class ConditionalJumpNode extends AbstractBytecodeNode {
        public static final int FALSE_SUCCESSOR = 0;
        public static final int TRUE_SUCCESSOR = 1;

        private final int offset;
        private final boolean isIfTrue;
        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

        @Child private StackPopNode popNode;
        @Child private HandleConditionResultNode handleConditionResultNode;

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = shortJumpOffset(bytecode);
            isIfTrue = false;
            popNode = StackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code);
        }

        public ConditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter, final boolean condition) {
            super(code, index, numBytecodes);
            offset = longConditionalJumpOffset(bytecode, parameter);
            isIfTrue = condition;
            popNode = StackPopNode.create(code);
            handleConditionResultNode = HandleConditionResultNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        public boolean executeCondition(final VirtualFrame frame) {
            final Object result = popNode.executeRead(frame);
            return conditionProfile.profile(handleConditionResultNode.execute(frame, isIfTrue, result));
        }

        public int getJumpSuccessor() {
            return getSuccessorIndex() + offset;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            if (isIfTrue) {
                return "jumpTrue: " + offset;
            } else {
                return "jumpFalse: " + offset;
            }
        }

        protected abstract static class HandleConditionResultNode extends AbstractNodeWithCode {
            @Child private StackPushNode pushNode;
            @Child private AbstractSendNode sendMustBeBooleanNode;

            protected HandleConditionResultNode(final CompiledCodeObject code) {
                super(code);
            }

            protected static HandleConditionResultNode create(final CompiledCodeObject code) {
                return HandleConditionResultNodeGen.create(code);
            }

            protected abstract boolean execute(VirtualFrame frame, boolean expected, Object result);

            @Specialization
            protected static final boolean doBoolean(final boolean expected, final boolean result) {
                return expected == result;
            }

            @Fallback
            protected final boolean doMustBeBooleanSend(final VirtualFrame frame, @SuppressWarnings("unused") final boolean expected, final Object result) {
                getPushNode().executeWrite(frame, result);
                getSendMustBeBooleanNode().executeSend(frame);
                CompilerDirectives.transferToInterpreter();
                throw SqueakException.create("Should not be reached");
            }

            private StackPushNode getPushNode() {
                if (pushNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    pushNode = insert(StackPushNode.create(code));
                }
                return pushNode;
            }

            private AbstractSendNode getSendMustBeBooleanNode() {
                if (sendMustBeBooleanNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sendMustBeBooleanNode = insert(new SendSelectorNode(code, -1, 1, code.image.mustBeBooleanSelector, 0));
                }
                return sendMustBeBooleanNode;
            }
        }
    }

    public static final class UnconditionalJumpNode extends AbstractBytecodeNode {
        private final int offset;

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode) {
            super(code, index, numBytecodes);
            offset = shortJumpOffset(bytecode);
        }

        public UnconditionalJumpNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int bytecode, final int parameter) {
            super(code, index, numBytecodes);
            offset = longUnconditionalJumpOffset(bytecode, parameter);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // nothing to do
        }

        public int getJumpSuccessor() {
            return getSuccessorIndex() + offset;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "jumpTo: " + offset;
        }
    }

    public static final class WhileNode extends AbstractBytecodeNode {
        @Child private LoopNode loop;

        private WhileNode(final CompiledCodeObject code, final int startIndex, final int endIndex) {
            super(code, startIndex, endIndex - startIndex + 3);
            final ArrayList<AbstractBytecodeNode> bytecodeNodes = new ArrayList<>();
            int lastConditionJumpIndex = -1;
            boolean lastConditionIsIfTrue = true;
            for (int i = startIndex; i < endIndex - 1; i++) {
                final AbstractBytecodeNode bytecodeNode = SqueakBytecodeDecoder.decodeBytecode(code, i);
                if (bytecodeNode instanceof ConditionalJumpNode) {
                    lastConditionJumpIndex = i;
                    lastConditionIsIfTrue = ((ConditionalJumpNode) bytecodeNode).isIfTrue;
                }
                bytecodeNodes.add(bytecodeNode);
            }
            assert lastConditionJumpIndex > 0;
            final List<AbstractBytecodeNode> conditionNodes = bytecodeNodes.subList(0, lastConditionJumpIndex);
            final List<AbstractBytecodeNode> bodyNodes = bytecodeNodes.subList(lastConditionJumpIndex, bytecodeNodes.size());
            loop = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(code, lastConditionIsIfTrue,
                            conditionNodes.toArray(new AbstractBytecodeNode[conditionNodes.size()]),
                            bodyNodes.toArray(new AbstractBytecodeNode[bodyNodes.size()])));
        }

        public static WhileNode create(final CompiledCodeObject code, final int startIndex, final int endIndex) {
            return new WhileNode(code, startIndex, endIndex - startIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            loop.executeLoop(frame);
        }

        private static class WhileRepeatingNode extends AbstractNodeWithCode implements RepeatingNode {
            private final boolean isIfTrue;
            @Child private StackPopNode stackPopNode;
            @Children private AbstractBytecodeNode[] conditionNodes;
            @Children private AbstractBytecodeNode[] bodyNodes;
            @Child private GetSuccessorNode getSuccessorNode = GetSuccessorNode.create();
            @Child private HandleConditionResultNode handleConditionResultNode;

            WhileRepeatingNode(final CompiledCodeObject code, final boolean isIfTrue, final AbstractBytecodeNode[] conditionNodes, final AbstractBytecodeNode[] bodyNodes) {
                super(code);
                stackPopNode = StackPopNode.create(code);
                this.isIfTrue = isIfTrue;
                this.conditionNodes = conditionNodes;
                this.bodyNodes = bodyNodes;
                handleConditionResultNode = HandleConditionResultNode.create(code);
            }

            @ExplodeLoop
            public boolean executeCondition(final VirtualFrame frame) {
                for (final AbstractBytecodeNode conditionNode : conditionNodes) {
                    conditionNode.executeVoid(frame);
                }
                final Object result = stackPopNode.executeRead(frame);
                return handleConditionResultNode.execute(frame, isIfTrue, result);
            }

            @Override
            @ExplodeLoop
            public boolean executeRepeating(final VirtualFrame frame) {
                if (!executeCondition(frame)) {
                    for (final AbstractBytecodeNode bodyNode : bodyNodes) {
                        final int successor = getSuccessorNode.executeGeneric(frame, bodyNode);
                        FrameAccess.setInstructionPointer(frame, code, successor);
                        bodyNode.executeVoid(frame);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static int shortJumpOffset(final int bytecode) {
        return (bytecode & 7) + 1;
    }

    public static int longConditionalJumpOffset(final int bytecode, final int parameter) {
        return ((bytecode & 3) << 8) + parameter;
    }

    public static int longUnconditionalJumpOffset(final int bytecode, final int parameter) {
        return ((bytecode & 7) - 4 << 8) + parameter;
    }
}

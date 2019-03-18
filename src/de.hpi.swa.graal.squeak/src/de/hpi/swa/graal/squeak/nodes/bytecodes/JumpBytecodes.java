package de.hpi.swa.graal.squeak.nodes.bytecodes;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
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
            super(code, startIndex, endIndex - startIndex + 2);
            final int numBytecodeNodes = endIndex - startIndex;
            final AbstractBytecodeNode[] bytecodeNodes = new AbstractBytecodeNode[numBytecodeNodes];
            ConditionalJumpNode lastConditionalJump = null;
            assert getSuccessorIndex() == SqueakBytecodeDecoder.decodeBytecode(code, endIndex).getSuccessorIndex();
            for (int i = startIndex; i < endIndex;) {
                final AbstractBytecodeNode bytecodeNode = SqueakBytecodeDecoder.decodeBytecode(code, i);
                if (bytecodeNode instanceof ConditionalJumpNode) {
                    final ConditionalJumpNode conditionalJumpNode = (ConditionalJumpNode) bytecodeNode;
                    if (conditionalJumpNode.getJumpSuccessor() > endIndex) {
                        lastConditionalJump = conditionalJumpNode;
                    }
                }
                bytecodeNodes[i - startIndex] = bytecodeNode;
                i = bytecodeNode.getSuccessorIndex();
            }
            final boolean condition;
            final AbstractBytecodeNode[] conditionNodes;
            final AbstractBytecodeNode[] bodyNodes;
            if (lastConditionalJump == null) {
                condition = true;
                conditionNodes = null;
                bodyNodes = bytecodeNodes;
            } else {
                condition = lastConditionalJump.isIfTrue;
                final int conditionEndIndex = lastConditionalJump.getIndex() - startIndex;
                final int bodyStartIndex = lastConditionalJump.getSuccessorIndex() - startIndex;
                conditionNodes = Arrays.copyOfRange(bytecodeNodes, 0, conditionEndIndex);
                bodyNodes = bodyStartIndex < numBytecodeNodes ? Arrays.copyOfRange(bytecodeNodes, bodyStartIndex, numBytecodeNodes) : null;
            }
            loop = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(code, startIndex, condition, conditionNodes, bodyNodes));
        }

        public static WhileNode create(final CompiledCodeObject code, final int startIndex, final int endIndex) {
            return new WhileNode(code, startIndex, endIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            loop.executeLoop(frame);
        }

        private static class WhileRepeatingNode extends AbstractNodeWithCode implements RepeatingNode {
            private final int startIndex;
            private final boolean isIfTrue;
            @Child private StackPopNode stackPopNode;
            @Children private AbstractBytecodeNode[] conditionNodes;
            @Children private AbstractBytecodeNode[] bodyNodes;
            @Child private GetSuccessorNode getSuccessorNode = GetSuccessorNode.create();
            @Child private HandleConditionResultNode handleConditionResultNode;

            WhileRepeatingNode(final CompiledCodeObject code, final int startIndex, final boolean isIfTrue, final AbstractBytecodeNode[] conditionNodes, final AbstractBytecodeNode[] bodyNodes) {
                super(code);
                this.startIndex = startIndex;
                stackPopNode = StackPopNode.create(code);
                this.isIfTrue = isIfTrue;
                this.conditionNodes = conditionNodes;
                this.bodyNodes = bodyNodes;
                handleConditionResultNode = HandleConditionResultNode.create(code);
            }

            @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
            public boolean executeCondition(final VirtualFrame frame) {
                AbstractBytecodeNode node = conditionNodes[0];
                assert startIndex == node.getIndex();
                int pc = startIndex;
                CompilerAsserts.compilationConstant(conditionNodes.length);
                while (pc >= 0 && node != null) {
                    CompilerAsserts.partialEvaluationConstant(pc);
                    if (node instanceof ConditionalJumpNode) {
                        final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                        if (jumpNode.executeCondition(frame)) {
                            final int successor = jumpNode.getJumpSuccessor();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                            pc = successor;
                            node = fetchNextBytecodeNode(conditionNodes, pc, startIndex);
                            continue;
                        } else {
                            final int successor = jumpNode.getSuccessorIndex();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                            pc = successor;
                            node = fetchNextBytecodeNode(conditionNodes, pc, startIndex);
                            continue;
                        }
                    } else if (node instanceof UnconditionalJumpNode) {
                        final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                        pc = successor;
                        node = fetchNextBytecodeNode(conditionNodes, pc, startIndex);
                        continue;
                    } else {
                        final int successor = getSuccessorNode.executeGeneric(frame, node);
                        FrameAccess.setInstructionPointer(frame, code, successor);
                        node.executeVoid(frame);
                        pc = successor;
                        node = fetchNextBytecodeNode(conditionNodes, pc, startIndex);
                        continue;
                    }
                }
                final Object result = stackPopNode.executeRead(frame);
                return handleConditionResultNode.execute(frame, isIfTrue, result);
            }

            @Override
            @ExplodeLoop(kind = LoopExplosionKind.MERGE_EXPLODE)
            public boolean executeRepeating(final VirtualFrame frame) {
                if (conditionNodes == null || !executeCondition(frame)) {
                    if (bodyNodes == null) {
                        assert conditionNodes != null;
                        return true;
                    }
                    AbstractBytecodeNode node = bodyNodes[0];
                    final int bodyStartIndex = node.getIndex();
                    int pc = bodyStartIndex;
                    CompilerAsserts.compilationConstant(bodyNodes.length);
                    while (pc >= 0 && node != null) {
                        CompilerAsserts.partialEvaluationConstant(pc);
                        if (node instanceof ConditionalJumpNode) {
                            final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                            if (jumpNode.executeCondition(frame)) {
                                final int successor = jumpNode.getJumpSuccessor();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                                pc = successor;
                                node = fetchNextBytecodeNode(bodyNodes, pc, bodyStartIndex);
                                continue;
                            } else {
                                final int successor = jumpNode.getSuccessorIndex();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                                pc = successor;
                                node = fetchNextBytecodeNode(bodyNodes, pc, bodyStartIndex);
                                continue;
                            }
                        } else if (node instanceof UnconditionalJumpNode) {
                            final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
// if (CompilerDirectives.inInterpreter() && successor <= pc) {
// backJumpCounter++;
// }
                            pc = successor;
                            node = fetchNextBytecodeNode(bodyNodes, pc, bodyStartIndex);
                            continue;
                        } else {
                            final int successor = getSuccessorNode.executeGeneric(frame, node);
                            FrameAccess.setInstructionPointer(frame, code, successor);
                            node.executeVoid(frame);
                            pc = successor;
                            node = fetchNextBytecodeNode(bodyNodes, pc, bodyStartIndex);
                            continue;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }

            private AbstractBytecodeNode fetchNextBytecodeNode(final AbstractBytecodeNode[] bytecodeNodes, final int pc, final int offset) {
                if (pc - offset >= bytecodeNodes.length) {
                    return null;
                }
                if (bytecodeNodes[pc - offset] == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    bytecodeNodes[pc - offset] = insert(SqueakBytecodeDecoder.decodeBytecodeDetectLoops(code, pc));
                    // bytecodeNodes[pc] = insert(SqueakBytecodeDecoder.decodeBytecode(code, pc));
                }
                return bytecodeNodes[pc - offset];
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

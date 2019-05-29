package de.hpi.swa.graal.squeak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteContextNodeGen.TriggerInterruptHandlerNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnConstantNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnReceiverNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnTopFromBlockNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnTopFromMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public abstract class ExecuteContextNode extends AbstractNodeWithCode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, CallPrimitiveNode.class);
    private static final boolean DECODE_BYTECODE_ON_DEMAND = true;
    private static final int STACK_DEPTH_LIMIT = 25000;

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private TriggerInterruptHandlerNode triggerInterruptHandlerNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    @Child private FrameStackReadAndClearNode readAndClearNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;

    private static int stackDepth = 0;

    protected ExecuteContextNode(final CompiledCodeObject code) {
        super(code);
        if (DECODE_BYTECODE_ON_DEMAND) {
            bytecodeNodes = new AbstractBytecodeNode[SqueakBytecodeDecoder.trailerPosition(code)];
        } else {
            bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        }
        triggerInterruptHandlerNode = TriggerInterruptHandlerNode.create(code);
        readAndClearNode = FrameStackReadAndClearNode.create(code);
    }

    public static ExecuteContextNode create(final CompiledCodeObject code) {
        return ExecuteContextNodeGen.create(code);
    }

    public abstract Object executeContext(VirtualFrame frame, ContextObject context);

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Specialization(guards = "context == null")
    protected final Object doVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject context,
                    @Cached("create(code)") final MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode) {
        try {
            if (stackDepth++ > STACK_DEPTH_LIMIT) {
                throw ProcessSwitch.createWithBoundary(getGetOrCreateContextNode().executeGet(frame));
            }
            triggerInterruptHandlerNode.executeGeneric(frame, code.hasPrimitive(), bytecodeNodes.length);
            return startBytecode(frame);
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } catch (final NonVirtualReturn nvr) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } catch (final ProcessSwitch ps) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw ps;
        } finally {
            stackDepth--;
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @Fallback
    protected final Object doNonVirtualized(final VirtualFrame frame, final ContextObject context) {
        // maybe persist newContext, so there's no need to lookup the context to update its pc.
        assert code == context.getBlockOrMethod();
        assert context.getMethod() == FrameAccess.getMethod(frame);
        assert frame.getFrameDescriptor() == code.getFrameDescriptor();
        try {
            triggerInterruptHandlerNode.executeGeneric(frame, code.hasPrimitive(), bytecodeNodes.length);
            final long initialPC = context.getInstructionPointerForBytecodeLoop();
            assert initialPC >= 0 : "Trying to execute a terminated/illegal context";
            if (initialPC == 0) {
                return startBytecode(frame);
            } else {
                // Avoid optimizing cases in which a context is resumed.
                CompilerDirectives.transferToInterpreter();
                return resumeBytecode(frame, initialPC);
            }
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } finally {
            MaterializeContextOnMethodExitNode.stopMaterializationHere();
        }
    }

    public static int getStackDepth() {
        return stackDepth;
    }

    private GetOrCreateContextNode getGetOrCreateContextNode() {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(code));
        }
        return getOrCreateContextNode;
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object startBytecode(final VirtualFrame frame) {
        int pc = 0;
        int backJumpCounter = 0;
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        Object returnValue = null;
        bytecode_loop: while (true) {
            CompilerAsserts.partialEvaluationConstant(pc);
            final AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
            if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame)) {
                    final int successor = jumpNode.getJumpSuccessorIndex();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    continue bytecode_loop;
                } else {
                    final int successor = jumpNode.getSuccessorIndex();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    continue bytecode_loop;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                if (CompilerDirectives.inInterpreter() && successor <= pc) {
                    backJumpCounter++;
                }
                pc = successor;
                continue bytecode_loop;
            } else if (node instanceof CallPrimitiveNode) {
                final AbstractPrimitiveNode primitiveNode = ((CallPrimitiveNode) node).primitiveNode;
                if (primitiveNode != null) {
                    try {
                        returnValue = primitiveNode.executePrimitive(frame);
                        break bytecode_loop;
                    } catch (final PrimitiveFailed e) {
                        /** getHandlePrimitiveFailedNode() acts as branch profile. */
                        getHandlePrimitiveFailedNode().executeHandle(frame, e);
                        LOG.log(Level.FINE, () -> (primitiveNode instanceof PrimitiveFailedNode ? FrameAccess.getMethod(frame) : primitiveNode) +
                                        " (" + ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                        /** continue with fallback code. */
                    }
                }
                pc = node.getSuccessorIndex();
                continue bytecode_loop;
            } else if (node instanceof AbstractReturnNode) {
                if (node instanceof ReturnConstantNode) {
                    returnValue = ((ReturnConstantNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop;
                } else if (node instanceof ReturnReceiverNode) {
                    returnValue = ((ReturnReceiverNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop;
                } else if (node instanceof ReturnTopFromBlockNode) {
                    returnValue = ((ReturnTopFromBlockNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop;
                } else if (node instanceof ReturnTopFromMethodNode) {
                    returnValue = ((ReturnTopFromMethodNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop;
                }
            } else if (node instanceof PushClosureNode) {
                node.executeVoid(frame);
                pc = ((PushClosureNode) node).getClosureSuccessorIndex();
                continue bytecode_loop;
            } else {
                final int successor = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, code, successor);
                node.executeVoid(frame);
                pc = successor;
                continue bytecode_loop;
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        assert backJumpCounter >= 0;
        LoopNode.reportLoopCount(this, backJumpCounter);
        return returnValue;
    }

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    /*
     * Non-optimized version of startBytecode which is used to resume contexts.
     */
    private Object resumeBytecode(final VirtualFrame frame, final long initialPC) {
        int pc = (int) initialPC;
        Object returnValue = null;
        bytecode_loop_slow: while (true) {
            final AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
            if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame)) {
                    pc = jumpNode.getJumpSuccessorIndex();
                    continue bytecode_loop_slow;
                } else {
                    pc = jumpNode.getSuccessorIndex();
                    continue bytecode_loop_slow;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                pc = ((UnconditionalJumpNode) node).getJumpSuccessor();
                continue bytecode_loop_slow;
            } else if (node instanceof CallPrimitiveNode) {
                final AbstractPrimitiveNode primitiveNode = ((CallPrimitiveNode) node).primitiveNode;
                if (primitiveNode != null) {
                    try {
                        returnValue = primitiveNode.executePrimitive(frame);
                        break bytecode_loop_slow;
                    } catch (final PrimitiveFailed e) {
                        /** getHandlePrimitiveFailedNode() acts as branch profile. */
                        getHandlePrimitiveFailedNode().executeHandle(frame, e);
                        LOG.log(Level.FINE, () -> (primitiveNode instanceof PrimitiveFailedNode ? FrameAccess.getMethod(frame) : primitiveNode) +
                                        " (" + ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                        /** continue with fallback code. */
                    }
                }
                pc = node.getSuccessorIndex();
                continue bytecode_loop_slow;
            } else if (node instanceof AbstractReturnNode) {
                if (node instanceof ReturnConstantNode) {
                    returnValue = ((ReturnConstantNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop_slow;
                } else if (node instanceof ReturnReceiverNode) {
                    returnValue = ((ReturnReceiverNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop_slow;
                } else if (node instanceof ReturnTopFromBlockNode) {
                    returnValue = ((ReturnTopFromBlockNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop_slow;
                } else if (node instanceof ReturnTopFromMethodNode) {
                    returnValue = ((ReturnTopFromMethodNode) node).executeReturn(frame, readAndClearNode);
                    break bytecode_loop_slow;
                }
            } else if (node instanceof PushClosureNode) {
                node.executeVoid(frame);
                pc = ((PushClosureNode) node).getClosureSuccessorIndex();
                continue bytecode_loop_slow;
            } else {
                final int successor = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, code, successor);
                node.executeVoid(frame);
                pc = successor;
                continue bytecode_loop_slow;
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        return returnValue;
    }

    /*
     * Fetch next bytecode and insert AST nodes on demand if enabled.
     */
    @SuppressWarnings("unused")
    private AbstractBytecodeNode fetchNextBytecodeNode(final int pc) {
        if (DECODE_BYTECODE_ON_DEMAND && bytecodeNodes[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytecodeNodes[pc] = insert(SqueakBytecodeDecoder.decodeBytecode(code, pc));
        }
        return bytecodeNodes[pc];
    }

    @NodeInfo(cost = NodeCost.NONE)
    protected abstract static class TriggerInterruptHandlerNode extends AbstractNodeWithCode {
        protected static final int BYTECODE_LENGTH_THRESHOLD = 32;

        protected TriggerInterruptHandlerNode(final CompiledCodeObject code) {
            super(code);
        }

        private static TriggerInterruptHandlerNode create(final CompiledCodeObject code) {
            return TriggerInterruptHandlerNodeGen.create(code);
        }

        protected abstract void executeGeneric(VirtualFrame frame, boolean hasPrimitive, int bytecodeLength);

        @SuppressWarnings("unused")
        @Specialization(guards = {"!code.image.interrupt.disabled()", "!hasPrimitive", "bytecodeLength > BYTECODE_LENGTH_THRESHOLD"})
        protected final void doTrigger(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength,
                        @Cached("createCountingProfile()") final ConditionProfile triggerProfile,
                        @Cached("create(code)") final InterruptHandlerNode interruptNode) {
            if (triggerProfile.profile(code.image.interrupt.shouldTrigger())) {
                interruptNode.executeTrigger(frame);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"code.image.interrupt.disabled() || (hasPrimitive || bytecodeLength <= BYTECODE_LENGTH_THRESHOLD)"})
        protected final void doNothing(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength) {
            // Do not trigger.
        }
    }

    private HandleNonLocalReturnNode getHandleNonLocalReturnNode() {
        if (handleNonLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonLocalReturnNode = insert(HandleNonLocalReturnNode.create(code));
        }
        return handleNonLocalReturnNode;
    }
}

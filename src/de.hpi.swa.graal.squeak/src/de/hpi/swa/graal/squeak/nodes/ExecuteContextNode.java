package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteContextNodeGen.GetSuccessorNodeGen;
import de.hpi.swa.graal.squeak.nodes.ExecuteContextNodeGen.TriggerInterruptHandlerNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.context.UpdateInstructionPointerNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public abstract class ExecuteContextNode extends AbstractNodeWithCode {
    private static final boolean DECODE_BYTECODE_ON_DEMAND = false;

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleLocalReturnNode handleLocalReturnNode;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private HandleNonVirtualReturnNode handleNonVirtualReturnNode;
    @Child private TriggerInterruptHandlerNode triggerInterruptHandlerNode;

    private final BranchProfile nonVirtualReturnProfile = BranchProfile.create();
    private final BranchProfile processSwitchProfile = BranchProfile.create();

    @Child private UpdateInstructionPointerNode updateInstructionPointerNode;
    @Child private GetSuccessorNode getSuccessorNode;
    @Child private StackPushNode pushStackNode;
    @Child private CalculcatePCOffsetNode calculcatePCOffsetNode;

    public static ExecuteContextNode create(final CompiledCodeObject code) {
        return ExecuteContextNodeGen.create(code);
    }

    public abstract Object executeContext(VirtualFrame frame, ContextObject context);

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    protected ExecuteContextNode(final CompiledCodeObject code) {
        super(code);
        if (DECODE_BYTECODE_ON_DEMAND) {
            bytecodeNodes = new AbstractBytecodeNode[SqueakBytecodeDecoder.trailerPosition(code)];
        } else {
            bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        }
        triggerInterruptHandlerNode = TriggerInterruptHandlerNode.create(code);
    }

    @Specialization(guards = "context == null")
    protected final Object doVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject context,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode,
                    @Cached("create(code)") final MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode) {
        try {
            triggerInterruptHandlerNode.executeGeneric(frame, code.hasPrimitive(), bytecodeNodes.length);
            startBytecode(frame);
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            /** {@link getHandleLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleLocalReturnNode().executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            nonVirtualReturnProfile.enter();
            getOrCreateContextNode.executeGet(frame).markEscaped();
            return getHandleNonVirtualReturnNode().executeHandle(frame, nvr);
        } catch (ProcessSwitch ps) {
            processSwitchProfile.enter();
            getOrCreateContextNode.executeGet(frame).markEscaped();
            throw ps;
        } finally {
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @Fallback
    protected final Object doNonVirtualized(final VirtualFrame frame, final ContextObject context) {
        // maybe persist newContext, so there's no need to lookup the context to update its pc.
        assert context.getClosureOrMethod() == frame.getArguments()[FrameAccess.METHOD];

        try {
            triggerInterruptHandlerNode.executeGeneric(frame, code.hasPrimitive(), bytecodeNodes.length);
            final long initialPC = getAndDecodeSqueakPC(context);
            if (initialPC == 0) {
                startBytecode(frame);
            } else {
                // Avoid optimizing cases in which a context is resumed.
                CompilerDirectives.transferToInterpreter();
                resumeBytecode(frame, initialPC);
            }
            CompilerAsserts.neverPartOfCompilation();
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            /** {@link getHandleLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleLocalReturnNode().executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            nonVirtualReturnProfile.enter();
            return getHandleNonVirtualReturnNode().executeHandle(frame, nvr);
        } finally {
            MaterializeContextOnMethodExitNode.stopMaterializationHere();
        }
    }

    private long getAndDecodeSqueakPC(final ContextObject newContext) {
        return newContext.getInstructionPointer() - getCalculcatePCOffsetNode().execute(newContext.getClosureOrMethod());
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private void startBytecode(final VirtualFrame frame) {
        int pc = 0;
        int backJumpCounter = 0;
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                if (node instanceof ConditionalJumpNode) {
                    final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    if (jumpNode.executeCondition(frame)) {
                        final int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter() && successor <= pc) {
                            backJumpCounter++;
                        }
                        pc = successor;
                        node = fetchNextBytecodeNode(pc);
                        continue;
                    } else {
                        final int successor = jumpNode.getSuccessorIndex();
                        if (CompilerDirectives.inInterpreter() && successor <= pc) {
                            backJumpCounter++;
                        }
                        pc = successor;
                        node = fetchNextBytecodeNode(pc);
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    node = fetchNextBytecodeNode(pc);
                    continue;
                } else {
                    final int successor = getGetSuccessorNode().executeGeneric(frame, node);
                    getUpdateInstructionPointerNode().executeUpdate(frame, successor);
                    try {
                        node.executeVoid(frame);
                    } catch (NonLocalReturn nlr) {
                        if (nlr.hasArrivedAtTargetContext()) {
                            getStackPushNode().executeWrite(frame, nlr.getReturnValue());
                        } else {
                            throw nlr;
                        }
                    }
                    pc = successor;
                    node = fetchNextBytecodeNode(pc);
                    continue;
                }
            }
        } finally {
            assert backJumpCounter >= 0;
            LoopNode.reportLoopCount(this, backJumpCounter);
        }
    }

    /*
     * Non-optimized version of startBytecode which is used to resume contexts.
     */
    private void resumeBytecode(final VirtualFrame frame, final long initialPC) {
        int pc = (int) initialPC;
        AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
        while (pc >= 0) {
            final int successor = getGetSuccessorNode().executeGeneric(frame, node);
            getUpdateInstructionPointerNode().executeUpdate(frame, successor);
            try {
                node.executeVoid(frame);
            } catch (NonLocalReturn nlr) {
                if (nlr.hasArrivedAtTargetContext()) {
                    getStackPushNode().executeWrite(frame, nlr.getReturnValue());
                } else {
                    throw nlr;
                }
            }
            pc = successor;
            node = fetchNextBytecodeNode(pc);
        }
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

        private final ConditionProfile countingProfile = ConditionProfile.createCountingProfile();

        private static TriggerInterruptHandlerNode create(final CompiledCodeObject code) {
            return TriggerInterruptHandlerNodeGen.create(code);
        }

        protected TriggerInterruptHandlerNode(final CompiledCodeObject code) {
            super(code);
        }

        protected abstract void executeGeneric(VirtualFrame frame, boolean hasPrimitive, int bytecodeLength);

        @SuppressWarnings("unused")
        @Specialization(guards = {"!code.image.interrupt.disabled()", "!hasPrimitive", "bytecodeLength > BYTECODE_LENGTH_THRESHOLD"})
        protected final void doTrigger(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength,
                        @Cached("create(code)") final InterruptHandlerNode interruptNode) {
            if (countingProfile.profile(code.image.interrupt.shouldTrigger())) {
                interruptNode.executeTrigger(frame);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"code.image.interrupt.disabled() || (hasPrimitive || bytecodeLength <= BYTECODE_LENGTH_THRESHOLD)"})
        protected final void doNothing(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength) {
            // Do not trigger.
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final void doFail(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength) {
            throw new SqueakException("Should not happen");
        }
    }

    protected abstract static class GetSuccessorNode extends Node {
        private static GetSuccessorNode create() {
            return GetSuccessorNodeGen.create();
        }

        protected abstract int executeGeneric(VirtualFrame frame, AbstractBytecodeNode node);

        @Specialization
        protected static final int doClosureNode(final PushClosureNode node) {
            return node.getSuccessorIndex() + node.getBockSize();
        }

        @Specialization
        protected static final int doConditionalJump(final VirtualFrame frame, final ConditionalJumpNode node) {
            // `executeCondition` should only be called once because it pops value off the stack.
            if (node.executeCondition(frame)) {
                return node.getJumpSuccessor();
            } else {
                return node.getSuccessorIndex();
            }
        }

        @Specialization
        protected static final int doUnconditionalJump(final UnconditionalJumpNode node) {
            return node.getJumpSuccessor();
        }

        @Fallback
        protected static final int doNormal(final AbstractBytecodeNode node) {
            return node.getSuccessorIndex();
        }
    }

    private HandleLocalReturnNode getHandleLocalReturnNode() {
        if (handleLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleLocalReturnNode = insert(HandleLocalReturnNode.create(code));
        }
        return handleLocalReturnNode;
    }

    private HandleNonLocalReturnNode getHandleNonLocalReturnNode() {
        if (handleNonLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonLocalReturnNode = insert(HandleNonLocalReturnNode.create(code));
        }
        return handleNonLocalReturnNode;
    }

    private HandleNonVirtualReturnNode getHandleNonVirtualReturnNode() {
        if (handleNonVirtualReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonVirtualReturnNode = insert(HandleNonVirtualReturnNode.create());
        }
        return handleNonVirtualReturnNode;
    }

    private StackPushNode getStackPushNode() {
        if (pushStackNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pushStackNode = insert(StackPushNode.create(code));
        }
        return pushStackNode;
    }

    private CalculcatePCOffsetNode getCalculcatePCOffsetNode() {
        if (calculcatePCOffsetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            calculcatePCOffsetNode = insert(CalculcatePCOffsetNode.create());
        }
        return calculcatePCOffsetNode;
    }

    private UpdateInstructionPointerNode getUpdateInstructionPointerNode() {
        if (updateInstructionPointerNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateInstructionPointerNode = insert(UpdateInstructionPointerNode.create(code));
        }
        return updateInstructionPointerNode;
    }

    private GetSuccessorNode getGetSuccessorNode() {
        if (getSuccessorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSuccessorNode = insert(GetSuccessorNode.create());
        }
        return getSuccessorNode;
    }
}

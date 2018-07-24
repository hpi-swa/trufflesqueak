package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
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
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public abstract class ExecuteContextNode extends AbstractNodeWithCode {
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleLocalReturnNode handleLocalReturnNode;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private HandleNonVirtualReturnNode handleNonVirtualReturnNode = HandleNonVirtualReturnNode.create();
    @Child private TriggerInterruptHandlerNode triggerInterruptHandlerNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create();

    @Child private UpdateInstructionPointerNode updateInstructionPointerNode;
    @Child private GetSuccessorNode getSuccessorNode;
    @Child private StackPushNode pushStackNode;
    @Child private CalculcatePCOffsetNode calculcatePCOffsetNode;

    public static ExecuteContextNode create(final CompiledCodeObject code) {
        return ExecuteContextNodeGen.create(code);
    }

    public abstract Object executeContext(VirtualFrame frame, ContextObject context);

    @Override
    @TruffleBoundary
    public final String toString() {
        return code.toString();
    }

    protected ExecuteContextNode(final CompiledCodeObject code) {
        super(code);
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        handleLocalReturnNode = HandleLocalReturnNode.create(code);
        handleNonLocalReturnNode = HandleNonLocalReturnNode.create(code);
        triggerInterruptHandlerNode = TriggerInterruptHandlerNode.create(code.image);
    }

    @Specialization(guards = "context == null")
    protected final Object doVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final ContextObject context,
                    @Cached("create()") final GetOrCreateContextNode getOrCreateContextNode) {
        try {
            triggerInterruptHandlerNode.executeGeneric(frame, code.hasPrimitive(), bytecodeNodes.length);
            startBytecode(frame);
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            getOrCreateContextNode.executeGet(frame).markEscaped();
            return handleNonVirtualReturnNode.executeHandle(frame, nvr);
        } catch (ProcessSwitch ps) {
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
                // avoid optimizing the cases in which a context is resumed
                CompilerDirectives.transferToInterpreter();
                resumeBytecode(frame, initialPC);
            }
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
        } catch (NonVirtualReturn nvr) {
            return handleNonVirtualReturnNode.executeHandle(frame, nvr);
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
        AbstractBytecodeNode node = bytecodeNodes[pc];
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                if (node instanceof ConditionalJumpNode) {
                    final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    if (jumpNode.executeCondition(frame)) {
                        final int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    } else {
                        final int successor = jumpNode.getSuccessorIndex();
                        if (CompilerDirectives.inInterpreter()) {
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    node = bytecodeNodes[pc];
                    continue;
                } else {
                    final int successor = getGetSuccessorNode().executeGeneric(node);
                    getUpdateInstructionPointerNode().executeUpdate(frame, successor);
                    try {
                        pc = node.executeInt(frame);
                    } catch (NonLocalReturn nlr) {
                        if (nlr.hasArrivedAtTargetContext()) {
                            getStackPushNode().executeWrite(frame, nlr.getReturnValue());
                            pc = successor;
                        } else {
                            throw nlr;
                        }
                    }
                    node = bytecodeNodes[pc];
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
        AbstractBytecodeNode node = bytecodeNodes[pc];
        while (pc >= 0) {
            final int successor = getGetSuccessorNode().executeGeneric(node);
            getUpdateInstructionPointerNode().executeUpdate(frame, successor);
            try {
                pc = node.executeInt(frame);
            } catch (NonLocalReturn nlr) {
                if (nlr.hasArrivedAtTargetContext()) {
                    getStackPushNode().executeWrite(frame, nlr.getReturnValue());
                    pc = successor;
                } else {
                    throw nlr;
                }
            }
            node = bytecodeNodes[pc];
        }
    }

    protected abstract static class TriggerInterruptHandlerNode extends AbstractNodeWithImage {
        protected static final int BYTECODE_LENGTH_THRESHOLD = 32;

        protected static TriggerInterruptHandlerNode create(final SqueakImageContext image) {
            return TriggerInterruptHandlerNodeGen.create(image);
        }

        protected TriggerInterruptHandlerNode(final SqueakImageContext image) {
            super(image);
        }

        protected abstract void executeGeneric(VirtualFrame frame, boolean hasPrimitive, int bytecodeLength);

        @SuppressWarnings("unused")
        @Specialization(guards = {"!hasPrimitive", "bytecodeLength > BYTECODE_LENGTH_THRESHOLD"})
        protected final void doTrigger(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength) {
            image.interrupt.sendOrBackwardJumpTrigger(frame);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final void doNothing(final VirtualFrame frame, final boolean hasPrimitive, final int bytecodeLength) {
            // do not trigger
        }
    }

    protected abstract static class GetSuccessorNode extends Node {
        protected static GetSuccessorNode create() {
            return GetSuccessorNodeGen.create();
        }

        protected abstract int executeGeneric(AbstractBytecodeNode node);

        @Specialization
        protected static final int doClosureNode(final PushClosureNode node) {
            return node.getSuccessorIndex() + node.getBockSize();
        }

        @Fallback
        protected static final int doNormal(final AbstractBytecodeNode node) {
            return node.getSuccessorIndex();
        }
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

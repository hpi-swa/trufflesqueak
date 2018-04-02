package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;

import de.hpi.swa.trufflesqueak.exceptions.Returns.FreshReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackPushNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class ExecuteContextNode extends AbstractNodeWithCode {
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleLocalReturnNode handleLocalReturnNode;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private HandleNonVirtualReturnNode handleNonVirtualReturnNode;
    @Child private FrameSlotReadNode contextReadNode;
    @Child private FrameSlotReadNode instructionPointerReadNode;
    @Child private FrameSlotWriteNode instructionPointerWriteNode;
    @Child private StackPushNode pushStackNode;

    public static ExecuteContextNode create(CompiledCodeObject code) {
        return new ExecuteContextNode(code);
    }

    protected ExecuteContextNode(CompiledCodeObject code) {
        super(code);
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        handleLocalReturnNode = HandleLocalReturnNode.create(code);
        handleNonLocalReturnNode = HandleNonLocalReturnNode.create(code);
        handleNonVirtualReturnNode = HandleNonVirtualReturnNode.create(code);
        contextReadNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
        instructionPointerReadNode = FrameSlotReadNode.create(code.instructionPointerSlot);
        instructionPointerWriteNode = FrameSlotWriteNode.create(code.instructionPointerSlot);
        pushStackNode = StackPushNode.create(code);
    }

    public Object executeVirtualized(VirtualFrame frame) {
        code.image.interrupt.sendOrBackwardJumpTrigger(frame);
        try {
            startBytecode(frame);
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
// TODO: use handleNonVirtualReturnNode again
// } catch (NonVirtualReturn nvr) {
// return handleNonVirtualReturnNode.executeHandle(frame, nvr);
        }
    }

    public Object executeNonVirtualized(VirtualFrame frame, ContextObject newContext) {
        // maybe persist newContext, so there's no need to lookup the context to update its pc.
        assert newContext.getCodeObject() == FrameAccess.getMethod(frame);
        code.image.interrupt.sendOrBackwardJumpTrigger(frame);
        try {
            long initialPC = newContext.getInstructionPointer();
            if (initialPC == 0) {
                startBytecode(frame);
            } else {
                // avoid optimizing the cases in which a context is resumed
                CompilerDirectives.transferToInterpreter();
                resumeBytecode(frame.materialize(), initialPC);
            }
            throw new SqueakException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
// TODO: use handleNonVirtualReturnNode again
// } catch (NonVirtualReturn nvr) {
// return handleNonVirtualReturnNode.executeHandle(frame, nvr);
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void startBytecode(VirtualFrame frame) {
        int pc = 0;
        int backJumpCounter = 0;
        AbstractBytecodeNode node = bytecodeNodes[pc];
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                storeInstructionPointer(frame, node);
                if (node instanceof ConditionalJumpNode) {
                    ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    final boolean condition = jumpNode.executeCondition(frame);
                    if (CompilerDirectives.injectBranchProbability(jumpNode.getBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR), condition)) {
                        int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    } else {
                        int successor = jumpNode.getSuccessorIndex();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.FALSE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                            }
                        }
                        pc = successor;
                        node = bytecodeNodes[pc];
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter()) {
                        if (successor <= pc) {
                            backJumpCounter++;
                        }
                    }
                    pc = successor;
                    node = bytecodeNodes[pc];
                    continue;
                } else {
                    try {
                        pc = node.executeInt(frame);
                    } catch (FreshReturn fr) {
                        throw fr.getReturnException();
                    } catch (LocalReturn lr) {
                        pushStackNode.executeWrite(frame, lr.getReturnValue());
                    } catch (NonLocalReturn nlr) {
                        if (nlr.hasArrivedAtTargetContext()) {
                            pushStackNode.executeWrite(frame, nlr.getReturnValue());
                            pc = node.getSuccessorIndex();
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
     * Non-optimized version of startBytecode that is used to resume contexts.
     */
    protected void resumeBytecode(VirtualFrame frame, long initialPC) {
        int pc = (int) initialPC;
        AbstractBytecodeNode node = bytecodeNodes[pc];
        while (pc >= 0) {
            storeInstructionPointer(frame, node);
            try {
                pc = node.executeInt(frame);
            } catch (FreshReturn fr) {
                throw fr.getReturnException();
            } catch (LocalReturn lr) {
                pushStackNode.executeWrite(frame, lr.getReturnValue());
            } catch (NonLocalReturn nlr) {
                if (nlr.hasArrivedAtTargetContext()) {
                    pushStackNode.executeWrite(frame, nlr.getReturnValue());
                    pc = node.getSuccessorIndex();
                } else {
                    throw nlr;
                }
            }
            node = bytecodeNodes[pc];
        }
    }

    private void storeInstructionPointer(VirtualFrame frame, AbstractBytecodeNode node) {
        Object contextOrMarker = contextReadNode.executeRead(frame);
        if (contextOrMarker instanceof ContextObject) {
            ((ContextObject) contextOrMarker).setInstructionPointer(node.getSuccessorIndex());
        } else {
            instructionPointerWriteNode.executeWrite(frame, (long) node.getSuccessorIndex());
        }
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return code.toString();
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.RootTag.class;
    }
}

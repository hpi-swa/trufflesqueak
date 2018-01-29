package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class ExecuteContextNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;
    @Children private final AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleLocalReturnNode handleLocalReturnNode;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private GetOrCreateContextNode getContextNode;
    private int pcAfterNonLocalReturn;

    public static ExecuteContextNode create(CompiledCodeObject code) {
        return new ExecuteContextNode(code);
    }

    public ExecuteContextNode(CompiledCodeObject code) {
        this.code = code;
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        handleLocalReturnNode = HandleLocalReturnNode.create(code);
        handleNonLocalReturnNode = HandleNonLocalReturnNode.create(code);
        getContextNode = GetOrCreateContextNode.create(code);
    }

    public Object execute(VirtualFrame frame) {
        assert FrameAccess.getMethod(frame) == code;
        try {
            executeBytecode(frame);
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Method did not return");
        } catch (LocalReturn lr) {
            return handleLocalReturnNode.executeHandle(frame, lr);
        } catch (NonLocalReturn nlr) {
            return handleNonLocalReturnNode.executeHandle(frame, nlr);
        }
    }

    protected void executeBytecode(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int initialPC = initialPC(frame);
        if (initialPC == 0) {
            startBytecode(frame);
        } else {
            // avoid optimizing the cases in which a context is resumed
            CompilerDirectives.transferToInterpreter();
            resumeBytecode(frame.materialize(), initialPC);
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void startBytecode(VirtualFrame frame) {
        int pc = 0;
        int backJumpCounter = 0;
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                AbstractBytecodeNode node = bytecodeNodes[pc];
                if (node instanceof ConditionalJumpNode) {
                    ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    boolean condition = jumpNode.executeCondition(frame);
                    if (CompilerDirectives.injectBranchProbability(jumpNode.getBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR), condition)) {
                        int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                                code.image.interrupt.sendOrBackwardJumpTrigger(frame);
                            }
                        }
                        pc = successor;
                        continue;
                    } else {
                        int successor = jumpNode.getNoJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.FALSE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                                code.image.interrupt.sendOrBackwardJumpTrigger(frame);
                            }
                        }
                        pc = successor;
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter()) {
                        if (successor <= pc) {
                            backJumpCounter++;
                            code.image.interrupt.sendOrBackwardJumpTrigger(frame);
                        }
                    }
                    pc = successor;
                    continue;
                } else {
                    pc = node.executeInt(frame);
                    continue;
                }
            }
        } catch (NonLocalReturn nlr) {
            pcAfterNonLocalReturn = pc;
            throw nlr;
        } finally {
            assert backJumpCounter >= 0;
            LoopNode.reportLoopCount(this, backJumpCounter);
        }
    }

    /*
     * Non-optimized version of startBytecode that is used to resume contexts.
     */
    protected void resumeBytecode(VirtualFrame frame, int initialPC) {
        int pc = initialPC;
        try {
            while (pc >= 0) {
                AbstractBytecodeNode node = bytecodeNodes[pc];
                pc = node.executeInt(frame);
            }
        } catch (NonLocalReturn nlr) {
            pcAfterNonLocalReturn = pc;
            throw nlr;
        }
    }

    private int initialPC(VirtualFrame frame) {
        Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        if (contextOrMarker instanceof FrameMarker) {
            return 0; // start at the beginning
        }
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        if (closure != null) {
            int rawPC = closure.getPC();
            CompiledBlockObject block = closure.getCompiledBlock();
            assert code == block;
            return rawPC - block.getMethod().getInitialPC() - block.getOffset();
        }
        int rawPC = (int) ((ContextObject) contextOrMarker).at0(CONTEXT.INSTRUCTION_POINTER);
        return rawPC - code.getInitialPC();
    }

    public int getPCAfterNonLocalReturn() { // TODO: this maybe be needed
        return pcAfterNonLocalReturn;
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.RootTag.class;
    }
}

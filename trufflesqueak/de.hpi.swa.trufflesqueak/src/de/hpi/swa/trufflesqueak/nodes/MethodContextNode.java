package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.util.KnownClasses.BLOCK_CONTEXT;
import de.hpi.swa.trufflesqueak.util.KnownClasses.CONTEXT;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class MethodContextNode extends RootNode {
    @CompilationFinal private final ContextObject context;
    @CompilationFinal private final CompiledCodeObject code;
    @Children private final AbstractBytecodeNode[] bytecodeNodes;

    public MethodContextNode(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        this.context = context;
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
    }

    private void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setObject(code.markerSlot, new FrameMarker());
        frame.setObject(code.methodSlot, code);
        frame.setObject(code.thisContextSlot, context);
        // sp points to the last temp slot
        int sp = initialSP();
        assert sp >= -1;
        frame.setInt(code.stackPointerSlot, sp);
        if (code instanceof CompiledBlockObject) {
            frame.setObject(code.closureSlot, context.at0(BLOCK_CONTEXT.HOME));
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        try {
            executeBytecode(frame);
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Method did not return");
        } catch (LocalReturn lr) {
            if (context.isDirty()) {
                ContextObject sender = context.getSender();
                throw new NonVirtualReturn(lr.getReturnValue(), sender, sender);
            }
            context.activateUnwindContext();
            return lr.getReturnValue();
        } catch (NonLocalReturn nlr) {
            if (context.isDirty()) {
                throw new NonVirtualReturn(nlr.getReturnValue(), nlr.getTargetContext(), context.getSender());
            }
            context.activateUnwindContext();
            if (nlr.getTargetContext() == context.getSender()) {
                nlr.setArrivedAtTargetContext();
            }
            throw nlr;
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void executeBytecode(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int pc = initialPC();
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
                        }
                    }
                    pc = successor;
                    continue;
                } else {
                    pc = node.executeInt(frame);
                    continue;
                }
            }
        } finally {
            assert backJumpCounter >= 0;
            LoopNode.reportLoopCount(this, backJumpCounter);
        }
    }

    protected int initialPC() {
        int rawPC = (int) context.at0(CONTEXT.INSTRUCTION_POINTER);
        return rawPC - code.getBytecodeOffset() - 1;
    }

    protected int initialSP() {
        // no need to read context.at0(CONTEXT.STACKPOINTER)
        return code.getNumTemps() - 1;
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public String getName() {
        return code.toString();
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.RootTag.class;
    }
}

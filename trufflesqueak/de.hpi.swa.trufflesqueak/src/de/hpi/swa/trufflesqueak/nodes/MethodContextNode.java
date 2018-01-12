package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class MethodContextNode extends Node {
    @CompilationFinal private final CompiledCodeObject code;
    @CompilationFinal private static BaseSqueakObject aboutToReturnSelector;
    @Children private final AbstractBytecodeNode[] bytecodeNodes;
    @Child private PushStackNode pushNode;
    @Child private AboutToReturnNode aboutToReturnNode;
    @Child private TerminateContextNode terminateNode;
    @Child private GetMethodContextNode getContextNode;

    public static MethodContextNode create(CompiledCodeObject code) {
        return new MethodContextNode(code);
    }

    public MethodContextNode(CompiledCodeObject codeObject) {
        code = codeObject;
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        pushNode = PushStackNode.create(code);
        terminateNode = TerminateContextNode.create(code);
        getContextNode = GetMethodContextNode.create(code);
        if (code instanceof CompiledMethodObject) {
            aboutToReturnNode = AboutToReturnNode.create((CompiledMethodObject) code);
        }
    }

    public Object execute(VirtualFrame frame) {
        MethodContextObject context;
        try {
            executeBytecode(frame);
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Method did not return");
        } catch (LocalReturn lr) {
            context = getContextObject(frame);
            if (context != null && context.isDirty()) {
                MethodContextObject sender = context.getSender();
                terminateNode.executeTerminate(frame);
                throw new NonVirtualReturn(lr.getReturnValue(), sender, sender);
            } else {
                terminateNode.executeTerminate(frame);
                return lr.getReturnValue();
            }
        } catch (NonLocalReturn nlr) {
            if (aboutToReturnNode != null && code.isUnwindMarked()) { // handle ensure: or ifCurtailed:
                aboutToReturnNode.executeAboutToReturn(frame, nlr);
            }
            context = getContextObject(frame);
            if (context != null && context.isDirty()) {
                MethodContextObject sender = context.getSender();
                terminateNode.executeTerminate(frame);
                throw new NonVirtualReturn(nlr.getReturnValue(), nlr.getTargetContext(code.image), sender);
            } else {
                terminateNode.executeTerminate(frame);
                if (nlr.getTargetContext(code.image).hasSameMethodObject(context)) {
                    return nlr.getReturnValue();
                } else {
                    throw nlr;
                }
            }
        }
    }

    private MethodContextObject getContextObject(VirtualFrame frame) {
        return (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
    }

    protected void executeBytecode(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int initialPC = initialPC(frame);
        if (initialPC == 0) {
            startBytecode(frame);
        } else {
            // avoid optimizing the cases in which a context is resumed
            resumeBytecode(frame, initialPC);
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
                                code.image.interrupt.executeCheck(frame);
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
                                code.image.interrupt.executeCheck(frame);
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
                            code.image.interrupt.executeCheck(frame);
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
            getContextNode.executeGetMethodContext(frame, pc); // ensure context is there
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
            getContextNode.executeGetMethodContext(frame, pc); // ensure context is there
            throw nlr;
        }
    }

    private int initialPC(VirtualFrame frame) {
        MethodContextObject context = getContextObject(frame);
        if (context == null) {
            return 0; // start at the beginning
        }
        int rawPC = (int) context.at0(CONTEXT.INSTRUCTION_POINTER);
        return rawPC - code.getBytecodeOffset() - 1;
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

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameUtil;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class MethodContextNode extends RootNode {
    @CompilationFinal private final ContextObject context;
    @CompilationFinal private final CompiledCodeObject code;
    @Children private final AbstractBytecodeNode[] bytecodeNodes;

    public static MethodContextNode create(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        return new MethodContextNode(language, context, code);
    }

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
        // sp points to the last temp slot
        int sp = initialSP();
        assert sp >= -1;
        frame.setInt(code.stackPointerSlot, sp);
        if (code instanceof CompiledBlockObject) {
            frame.setInt(code.closureSlot, 1 + code.getNumCopiedValues()); // rcvr + args + copied
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        try {
            return executeLoop(frame);
        } catch (LocalReturn e) {
            return e.returnValue;
        } catch (NonLocalReturn e) {
            Object targetMarker = e.getTarget();
            Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
            if (targetMarker == frameMarker) {
                return e.returnValue;
            } else {
                throw e;
            }
        } catch (NonVirtualReturn e) {
            // TODO: unwind context chain towards e.targetContext
        }
        throw new RuntimeException("unimplemented exit from activation");
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected Object executeLoop(VirtualFrame frame) {
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
        CompilerDirectives.transferToInterpreter();
        throw new RuntimeException("Method did not return");
    }

    protected int initialPC() {
        int rawPC = (int) context.at0(CONTEXT.INSTRUCTION_POINTER);
        if (rawPC < 0) {
            return 0;
        }
        return rawPC - code.getBytecodeOffset() - 1;
    }

    protected int initialSP() {
        // no need to read context.at0(CONTEXT.STACKPOINTER)
        return code.getNumTemps() - 1;
    }

    private static ContextObject getSender(ContextObject context) {
        Object sender = context.at0(CONTEXT.SENDER);
        if (sender instanceof ContextObject) {
            return (ContextObject) sender;
        } else {
            throw new RuntimeException("sender chain ended");
        }
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

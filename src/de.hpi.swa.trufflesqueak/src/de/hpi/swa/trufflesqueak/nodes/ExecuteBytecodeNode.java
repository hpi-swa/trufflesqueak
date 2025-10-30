/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.AbstractUnconditionalBackJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ExecuteBytecodeNode extends AbstractExecuteContextNode implements BytecodeOSRNode {
    private static final int LOCAL_RETURN_PC = -2;

    private final CompiledCodeObject code;
    private final BranchProfile nonLocalReturnProfile = BranchProfile.create();

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @CompilationFinal private Object osrMetadata;

    public ExecuteBytecodeNode(final CompiledCodeObject code) {
        this.code = code;
        bytecodeNodes = code.asBytecodeNodesEmpty();
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC) {
        CompilerAsserts.partialEvaluationConstant(startPC);
        int pc = startPC;
        Object returnValue = null;
        /*
         * Maintain LoopCounter object so that the compiler does not confuse it with the pc because
         * both are constant within the loop.
         */
        final LoopCounter loopCounter = new LoopCounter();
        try {
            while (pc != LOCAL_RETURN_PC) {
                CompilerAsserts.partialEvaluationConstant(pc);
                final AbstractBytecodeNode node = fetchNextBytecodeNode(frame, pc);
                CompilerAsserts.partialEvaluationConstant(node);
                pc = node.getSuccessorIndex();
                if (node instanceof final AbstractSendNode sendNode) {
                    FrameAccess.setInstructionPointer(frame, pc);
                    sendNode.executeVoid(frame);
                    final int actualNextPc = FrameAccess.getInstructionPointer(frame);
                    if (pc != actualNextPc) {
                        /*
                         * pc has changed, which can happen if a context is restarted (e.g. as part
                         * of Exception>>retry). For now, we continue in the interpreter to avoid
                         * confusing the Graal compiler.
                         */
                        CompilerDirectives.transferToInterpreter();
                        pc = actualNextPc;
                    }
                } else if (node instanceof final ConditionalJumpNode jumpNode) {
                    if (jumpNode.executeCondition(frame)) {
                        pc = jumpNode.getJumpSuccessorIndex();
                    }
                } else if (node instanceof final AbstractUnconditionalBackJumpNode jumpNode) {
                    final int loopCount = ++loopCounter.value;
                    if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, loopCount >= LoopCounter.CHECK_LOOP_STRIDE)) {
                        LoopNode.reportLoopCount(this, loopCount);
                        if (CompilerDirectives.inInterpreter() && !FrameAccess.hasClosure(frame) && BytecodeOSRNode.pollOSRBackEdge(this, loopCount)) {
                            returnValue = BytecodeOSRNode.tryOSR(this, pc, null, null, frame);
                            if (returnValue != null) {
                                assert !FrameAccess.hasModifiedSender(frame);
                                FrameAccess.terminateFrame(frame);
                                break;
                            }
                        } else {
                            jumpNode.executeCheck(frame);
                        }
                        loopCounter.value = 0;
                    }
                } else if (node instanceof final AbstractReturnNode returnNode) {
                    /*
                     * Save pc in frame since ReturnFromClosureNode could send aboutToReturn or
                     * cannotReturn.
                     */
                    FrameAccess.setInstructionPointer(frame, pc);
                    returnValue = returnNode.executeReturn(frame);
                    assert returnValue != null && !FrameAccess.hasModifiedSender(frame);
                    FrameAccess.terminateFrame(frame);
                    pc = LOCAL_RETURN_PC;
                } else { /* All other bytecode nodes. */
                    node.executeVoid(frame);
                }
            }
        } catch (final NonLocalReturn nlr) {
            nonLocalReturnProfile.enter();
            FrameAccess.terminateContextAndFrame(frame);
            throw nlr;
        } catch (final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            throw getContext().tryToSignalLowSpace(frame, e);
        } finally {
            if (loopCounter.value > 0) {
                LoopNode.reportLoopCount(this, loopCounter.value);
            }
        }
        return returnValue;
    }

    /**
     * Smaller than int[1], does not kill int[] on write and doesn't need bounds checks.
     */
    private static final class LoopCounter {
        private static final int CHECK_LOOP_STRIDE = 1 << 14;
        private static final double CHECK_LOOP_PROBABILITY = 1.0D / CHECK_LOOP_STRIDE;
        private int value;
    }

    /*
     * Fetch next bytecode and insert AST nodes on demand if enabled.
     */
    private AbstractBytecodeNode fetchNextBytecodeNode(final VirtualFrame frame, final int pcZeroBased) {
        if (bytecodeNodes[pcZeroBased] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytecodeNodes[pcZeroBased] = insert(code.bytecodeNodeAt(frame, bytecodeNodes, pcZeroBased));
            notifyInserted(bytecodeNodes[pcZeroBased]);
        }
        return bytecodeNodes[pcZeroBased];
    }

    @Override
    public CompiledCodeObject getCodeObject() {
        return code;
    }

    /*
     * Bytecode OSR support
     */

    @Override
    public Object executeOSR(final VirtualFrame osrFrame, final int target, final Object interpreterState) {
        return execute(osrFrame, target);
    }

    @Override
    public Object getOSRMetadata() {
        return osrMetadata;
    }

    @Override
    public void setOSRMetadata(final Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }

    @Override
    public Object[] storeParentFrameInArguments(final VirtualFrame parentFrame) {
        return FrameAccess.storeParentFrameInArguments(parentFrame);
    }

    @Override
    public Frame restoreParentFrameFromArguments(final Object[] arguments) {
        return FrameAccess.restoreParentFrameFromArguments(arguments);
    }

    /*
     * Node metadata
     */

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public boolean hasTag(final Class<? extends Tag> tag) {
        return StandardTags.RootTag.class == tag;
    }

    @Override
    public String getDescription() {
        return code.toString();
    }

    @Override
    public SourceSection getSourceSection() {
        final Source source = code.getSource();
        return source.createSection(1, 1, source.getLength());
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }
}

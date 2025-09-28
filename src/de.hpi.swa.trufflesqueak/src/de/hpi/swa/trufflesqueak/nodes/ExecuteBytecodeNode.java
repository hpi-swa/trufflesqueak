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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.AbstractUnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ExecuteBytecodeNode extends AbstractExecuteContextNode implements BytecodeOSRNode {
    private static final int LOCAL_RETURN_PC = -2;
    private static final int BACKJUMP_THRESHOLD = 1 << 14;

    private final CompiledCodeObject code;
    private final BranchProfile nonLocalReturnProfile = BranchProfile.create();

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @CompilationFinal private Object osrMetadata;

    public ExecuteBytecodeNode(final CompiledCodeObject code) {
        this.code = code;
        bytecodeNodes = code.asBytecodeNodesEmpty();
    }

    @Override
    public Object execute(final VirtualFrame frame, final int startPC) {
        CompilerAsserts.partialEvaluationConstant(startPC);
        try {
            return interpretBytecode(frame, startPC);
        } catch (final NonLocalReturn nlr) {
            nonLocalReturnProfile.enter();
            FrameAccess.terminateContextOrFrame(frame);
            throw nlr;
        } catch (final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            throw getContext().tryToSignalLowSpace(frame, e);
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://git.io/fjEDw).
     */
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object interpretBytecode(final VirtualFrame frame, final int startPC) {
        final int initialPC = code.getInitialPC();
        int pc = startPC;
        /*
         * Maintain backJumpCounter in a Counter so that the compiler does not confuse it with the
         * pc because both are constant within the loop.
         */
        final Counter backJumpCounter = new Counter();
        Object returnValue = null;
        bytecode_loop: while (pc != LOCAL_RETURN_PC) {
            CompilerAsserts.partialEvaluationConstant(pc);
            final AbstractBytecodeNode node = fetchNextBytecodeNode(frame, pc - initialPC);
            CompilerAsserts.partialEvaluationConstant(node);
            if (node instanceof final AbstractSendNode sendNode) {
                pc = sendNode.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, pc);
                sendNode.executeVoid(frame);
                final int actualNextPc = FrameAccess.getInstructionPointer(frame);
                if (pc != actualNextPc) {
                    /*
                     * pc has changed, which can happen if a context is restarted (e.g. as part of
                     * Exception>>retry). For now, we continue in the interpreter to avoid confusing
                     * the Graal compiler.
                     */
                    CompilerDirectives.transferToInterpreter();
                    pc = actualNextPc;
                }
                continue bytecode_loop;
            } else if (node instanceof final ConditionalJumpNode jumpNode) {
                if (jumpNode.executeCondition(frame)) {
                    pc = jumpNode.getJumpSuccessorIndex();
                    continue bytecode_loop;
                } else {
                    pc = jumpNode.getSuccessorIndex();
                    continue bytecode_loop;
                }
            } else if (node instanceof final AbstractUnconditionalJumpNode jumpNode) {
                final int successor = jumpNode.getSuccessorIndex();
                if (successor <= pc) {
                    backJumpCounter.value++;
                    if (backJumpCounter.value % BACKJUMP_THRESHOLD == 0) {
                        if (CompilerDirectives.inInterpreter() && !FrameAccess.hasClosure(frame) && BytecodeOSRNode.pollOSRBackEdge(this, BACKJUMP_THRESHOLD)) {
                            returnValue = BytecodeOSRNode.tryOSR(this, successor, null, null, frame);
                            if (returnValue != null) {
                                break bytecode_loop;
                            }
                        } else {
                            jumpNode.executeCheck(frame);
                        }
                    }
                }
                pc = successor;
                continue bytecode_loop;
            } else if (node instanceof final AbstractReturnNode returnNode) {
                /*
                 * Save pc in frame since ReturnFromClosureNode could send aboutToReturn or
                 * cannotReturn.
                 */
                FrameAccess.setInstructionPointer(frame, returnNode.getSuccessorIndex());
                returnValue = returnNode.executeReturn(frame);
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else {
                /* All other bytecode nodes. */
                node.executeVoid(frame);
                pc = node.getSuccessorIndex();
                continue bytecode_loop;
            }
        }
        assert returnValue != null && !FrameAccess.hasModifiedSender(frame);
        FrameAccess.terminateFrame(frame);
        // only report non-zero counters to reduce interpreter overhead
        if (CompilerDirectives.hasNextTier() && backJumpCounter.value != 0) {
            LoopNode.reportLoopCount(this, backJumpCounter.value > 0 ? backJumpCounter.value : Integer.MAX_VALUE);
        }
        return returnValue;
    }

    /**
     * Smaller than int[1], does not kill int[] on write and doesn't need bounds checks.
     */
    private static final class Counter {
        int value;
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

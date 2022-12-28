/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class ExecuteBytecodeNode extends AbstractExecuteContextNode implements BytecodeOSRNode {
    private static final int LOCAL_RETURN_PC = -2;

    private final CompiledCodeObject code;
    private final int initialPC;
    private SourceSection section;

    @Child private AbstractPrimitiveNode primitiveNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @CompilationFinal private Object osrMetadata;

    public ExecuteBytecodeNode(final CompiledCodeObject code) {
        this.code = code;
        initialPC = code.getInitialPC();
        bytecodeNodes = code.asBytecodeNodesEmpty();
        if (code.hasPrimitive()) {
            primitiveNode = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(code, ArgumentsLocation.IN_FRAME_ARGUMENTS);
            if (primitiveNode == null) {
                final int primitiveIndex = code.primitiveIndex();
                if (primitiveIndex == PrimitiveNodeFactory.PRIMITIVE_EXTERNAL_CALL_INDEX) {
                    LogUtils.PRIMITIVES.fine(() -> "Named primitive not found for " + code);
                } else if (primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_SIMULATION_GUARD_INDEX &&
                                primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ENSURE_MARKER_INDEX &&
                                primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ON_DO_MARKER_INDEX) {
                    LogUtils.PRIMITIVES.fine(() -> "Primitive #" + code.primitiveIndex() + " not found for " + code);
                }
            }
        }
    }

    @Override
    public Object execute(final VirtualFrame frame, final int startPC) {
        CompilerAsserts.partialEvaluationConstant(startPC);
        try {
            if (primitiveNode != null && startPC == initialPC) {
                try {
                    return primitiveNode.execute(frame);
                } catch (final PrimitiveFailed e) {
                    /* getHandlePrimitiveFailedNode() also acts as a BranchProfile. */
                    getHandlePrimitiveFailedNode().executeHandle(frame, e.getReasonCode());
                    LogUtils.PRIMITIVES.finer(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
                                    ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                    /* continue with fallback code. */
                }
            }
            return interpretBytecode(frame, startPC);
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://git.io/fjEDw).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object interpretBytecode(final VirtualFrame frame, final int startPC) {
        CompilerAsserts.partialEvaluationConstant(bytecodeNodes.length);
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
            if (node instanceof AbstractSendNode) {
                pc = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, pc);
                node.executeVoid(frame);
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
            } else if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame)) {
                    pc = jumpNode.getJumpSuccessorIndex();
                    continue bytecode_loop;
                } else {
                    pc = jumpNode.getSuccessorIndex();
                    continue bytecode_loop;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                final int successor = ((UnconditionalJumpNode) node).getSuccessorIndex();
                if (CompilerDirectives.hasNextTier() && successor <= pc) {
                    backJumpCounter.value++;
                    if (CompilerDirectives.inInterpreter() && !FrameAccess.hasClosure(frame) && BytecodeOSRNode.pollOSRBackEdge(this)) {
                        returnValue = BytecodeOSRNode.tryOSR(this, successor, null, null, frame);
                        if (returnValue != null) {
                            break bytecode_loop;
                        }
                    }
                }
                pc = successor;
                continue bytecode_loop;
            } else if (node instanceof AbstractReturnNode) {
                returnValue = ((AbstractReturnNode) node).executeReturn(frame);
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
        FrameAccess.terminate(frame);
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
            bytecodeNodes[pcZeroBased] = insert(code.bytecodeNodeAt(frame, pcZeroBased));
            notifyInserted(bytecodeNodes[pcZeroBased]);
        }
        return bytecodeNodes[pcZeroBased];
    }

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    private HandleNonLocalReturnNode getHandleNonLocalReturnNode() {
        if (handleNonLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonLocalReturnNode = insert(HandleNonLocalReturnNode.create(code));
        }
        return handleNonLocalReturnNode;
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
        if (section == null) {
            final Source source = code.getSource();
            section = source.createSection(1, 1, source.getLength());
        }
        return section;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }
}

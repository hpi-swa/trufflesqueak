/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class ExecuteBytecodeNode extends AbstractExecuteContextNode {
    private static final int LOCAL_RETURN_PC = -2;

    protected final CompiledCodeObject code;
    @CompilationFinal private int initialPC = -1;
    private SourceSection section;

    @Child private AbstractPrimitiveNode primitiveNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;

    public ExecuteBytecodeNode(final CompiledCodeObject code) {
        this.code = code;
        initialPC = code.getInitialPC();
        bytecodeNodes = code.asBytecodeNodesEmpty();
        if (code.hasPrimitive()) {
            primitiveNode = PrimitiveNodeFactory.forIndex(code, false, code.primitiveIndex(), false);
            if (primitiveNode == null) {
                final Supplier<String> messageSupplier;
                if (code.primitiveIndex() == PrimitiveNodeFactory.PRIMITIVE_EXTERNAL_CALL_INDEX) {
                    messageSupplier = () -> "Named primitive not found for " + code;
                } else {
                    messageSupplier = () -> "Primitive #" + code.primitiveIndex() + " not found for " + code;
                }
                LogUtils.PRIMITIVES.warning(messageSupplier);
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
                    LogUtils.PRIMITIVES.fine(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
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
        int backJumpCounter = 0;
        Object returnValue = null;
        bytecode_loop: while (pc != LOCAL_RETURN_PC) {
            CompilerAsserts.partialEvaluationConstant(pc);
            final AbstractBytecodeNode node = fetchNextBytecodeNode(frame, pc - initialPC);
            if (node instanceof AbstractSendNode) {
                pc = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, code, pc);
                node.executeVoid(frame);
                final int actualNextPc = FrameAccess.getInstructionPointer(frame, code);
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
                    final int successor = jumpNode.getJumpSuccessorIndex();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    continue bytecode_loop;
                } else {
                    final int successor = jumpNode.getSuccessorIndex();
                    if (CompilerDirectives.inInterpreter() && successor <= pc) {
                        backJumpCounter++;
                    }
                    pc = successor;
                    continue bytecode_loop;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                final int successor = ((UnconditionalJumpNode) node).getSuccessorIndex();
                if (CompilerDirectives.inInterpreter() && successor <= pc) {
                    backJumpCounter++;
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
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code.getInstructionPointerSlot());
        assert backJumpCounter >= 0;
        LoopNode.reportLoopCount(this, backJumpCounter);
        return returnValue;
    }

    protected boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame, code);
        return context != null && context.hasModifiedSender();
    }

    /*
     * Fetch next bytecode and insert AST nodes on demand if enabled.
     */
    @SuppressWarnings("unused")
    private AbstractBytecodeNode fetchNextBytecodeNode(final VirtualFrame frame, final int pcZeroBased) {
        assert 0 <= pcZeroBased && pcZeroBased < bytecodeNodes.length : "PC out of bounds for " + code;
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

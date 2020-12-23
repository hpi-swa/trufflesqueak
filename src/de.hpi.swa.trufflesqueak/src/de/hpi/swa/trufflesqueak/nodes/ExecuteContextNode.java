/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractSqueakBytecodeDecoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackInitializationNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class ExecuteContextNode extends AbstractExecuteContextNode {
    private static final int LOCAL_RETURN_PC = -2;
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    protected final CompiledCodeObject code;
    @CompilationFinal private int initialPC = -1;
    @CompilationFinal private int startPC = -1;

    @Child private AbstractPrimitiveNode primitiveNode;
    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    @Child private FrameStackInitializationNode frameInitializationNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Child private InterruptHandlerNode interruptHandlerNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode;
    @CompilationFinal private ContextReference<SqueakImageContext> contextReference;

    private SourceSection section;

    protected ExecuteContextNode(final CompiledCodeObject code, final boolean resume) {
        this.code = code;
        primitiveNode = code.hasPrimitive() ? PrimitiveNodeFactory.forIndex(code, false, code.primitiveIndex()) : null;
        bytecodeNodes = code.asBytecodeNodesEmpty();
        frameInitializationNode = resume ? null : FrameStackInitializationNode.create();
        /*
         * Only check for interrupts if method is relatively large. Avoid check if a closure is
         * activated (effectively what #primitiveClosureValueNoContextSwitch is for). Also, skip
         * timer interrupts here as they trigger too often, which causes a lot of context switches
         * and therefore materialization and deopts. Timer inputs are currently handled in
         * primitiveRelinquishProcessor (#230) only.
         */
        interruptHandlerNode = code.isCompiledBlock() || bytecodeNodes.length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS ? null : InterruptHandlerNode.createOrNull(false);
        materializeContextOnMethodExitNode = resume ? null : MaterializeContextOnMethodExitNode.create();
    }

    public static ExecuteContextNode create(final CompiledCodeObject code, final boolean resume) {
        return new ExecuteContextNode(code, resume);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Override
    public Object executeFresh(final VirtualFrame frame) {
        FrameAccess.setInstructionPointer(frame, code, getStartPC(frame));
        try {
            frameInitializationNode.executeInitialize(frame);
            if (interruptHandlerNode != null) {
                interruptHandlerNode.executeTrigger(frame);
            }
            if (primitiveNode != null) {
                try {
                    return primitiveNode.executePrimitive(frame);
                } catch (final PrimitiveFailed e) {
                    /* getHandlePrimitiveFailedNode() also acts as a BranchProfile. */
                    getHandlePrimitiveFailedNode().executeHandle(frame, e.getReasonCode());
                    /*
                     * Same toString() methods may throw compilation warnings, this is expected and
                     * ok for primitive failure logging purposes. Note that primitives that are not
                     * implemented are also not logged.
                     */
                    LogUtils.PRIMITIVES.fine(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
                                    ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                    /* continue with fallback code. */
                }
            }
            return executeBytecode(frame, getStartPC(frame));
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } catch (final NonVirtualReturn | ProcessSwitch nvr) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } finally {
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    @Override
    public Object executeResumeAtStart(final VirtualFrame frame) {
        try {
            if (primitiveNode != null) {
                try {
                    return primitiveNode.executePrimitive(frame);
                } catch (final PrimitiveFailed e) {
                    /* getHandlePrimitiveFailedNode() also acts as a BranchProfile. */
                    getHandlePrimitiveFailedNode().executeHandle(frame, e.getReasonCode());
                    /*
                     * Same toString() methods may throw compilation warnings, this is expected and
                     * ok for primitive failure logging purposes. Note that primitives that are not
                     * implemented are also not logged.
                     */
                    LogUtils.PRIMITIVES.fine(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
                                    ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                    /* continue with fallback code. */
                }
            }
            return executeBytecode(frame, getStartPC(frame));
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } finally {
            getSqueakImageContext().lastSeenContext = null; // Stop materialization here.
        }
    }

    @Override
    public Object executeResumeInMiddle(final VirtualFrame frame, final long resumptionPC) {
        try {
            getStartPC(frame); // FIXME: hack to ensure initialPC is set
            return executeBytecode(frame, (int) resumptionPC);
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } finally {
            getSqueakImageContext().lastSeenContext = null; // Stop materialization here.
        }
    }

    private SqueakImageContext getSqueakImageContext() {
        if (contextReference == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextReference = lookupContextReference(SqueakLanguage.class);
        }
        return contextReference.get();
    }

    private GetOrCreateContextNode getGetOrCreateContextNode() {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(false));
        }
        return getOrCreateContextNode;
    }

    private int getStartPC(final VirtualFrame frame) {
        if (initialPC < 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final BlockClosureObject closure = FrameAccess.getClosure(frame);
            if (closure == null) {
                initialPC = code.getInitialPC();
                startPC = initialPC;
                if (code.hasPrimitive()) {
                    // skip callPrimitive bytecode if any
                    startPC += AbstractSqueakBytecodeDecoder.CALL_PRIMITIVE_NUM_BYTECODES;
                }
            } else {
                assert !code.hasPrimitive();
                initialPC = closure.getCompiledBlock().getInitialPC();
                startPC = (int) closure.getStartPC();
            }
        }
        return startPC;
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://git.io/fjEDw).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object executeBytecode(final VirtualFrame frame, final int firstPC) {
        CompilerAsserts.partialEvaluationConstant(bytecodeNodes.length);
        assert firstPC > 0;
        int pc = firstPC;
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

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    private boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame, code);
        return context != null && context.hasModifiedSender();
    }

    /*
     * Fetch next bytecode and insert AST nodes on demand if enabled.
     */
    @SuppressWarnings("unused")
    private AbstractBytecodeNode fetchNextBytecodeNode(final VirtualFrame frame, final int pcZeroBased) {
        if (bytecodeNodes[pcZeroBased] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytecodeNodes[pcZeroBased] = insert(code.bytecodeNodeAt(frame, pcZeroBased));
            notifyInserted(bytecodeNodes[pcZeroBased]);
        }
        return bytecodeNodes[pcZeroBased];
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

    private HandleNonLocalReturnNode getHandleNonLocalReturnNode() {
        if (handleNonLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonLocalReturnNode = insert(HandleNonLocalReturnNode.create(code));
        }
        return handleNonLocalReturnNode;
    }
}

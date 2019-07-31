package de.hpi.swa.graal.squeak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteContextNodeFactory.TriggerInterruptHandlerNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnConstantNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromBlockNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodesFactory.ReturnTopFromMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

@GenerateWrapper
public class ExecuteContextNode extends AbstractNodeWithCode implements InstrumentableNode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, CallPrimitiveNode.class);
    private static final boolean DECODE_BYTECODE_ON_DEMAND = true;
    private static final int STACK_DEPTH_LIMIT = 25000;

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    @Child private FrameStackWriteNode pushNode;
    @Child private FrameStackReadAndClearNode readAndClearNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode;

    private SourceSection section;

    protected ExecuteContextNode(final CompiledCodeObject code) {
        super(code);
        if (DECODE_BYTECODE_ON_DEMAND) {
            bytecodeNodes = new AbstractBytecodeNode[SqueakBytecodeDecoder.trailerPosition(code)];
        } else {
            bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        }
        pushNode = FrameStackWriteNode.create(code);
        materializeContextOnMethodExitNode = MaterializeContextOnMethodExitNode.create(code);
    }

    protected ExecuteContextNode(final ExecuteContextNode executeContextNode) {
        this(executeContextNode.code);
    }

    public static ExecuteContextNode create(final CompiledCodeObject code) {
        return new ExecuteContextNode(code);
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    public Object executeFresh(final VirtualFrame frame) {
        initializeSlots(code, frame);
        initializeArgumentsAndTemps(frame);
        final boolean enableStackDepthProtection = enableStackDepthProtection();
        try {
            if (enableStackDepthProtection && code.image.stackDepth++ > STACK_DEPTH_LIMIT) {
                throw ProcessSwitch.createWithBoundary(getGetOrCreateContextNode().executeGet(frame));
            }
            return startBytecode(frame);
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } catch (final NonVirtualReturn nvr) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw nvr;
        } catch (final ProcessSwitch ps) {
            /** {@link getGetOrCreateContextNode()} acts as {@link BranchProfile} */
            getGetOrCreateContextNode().executeGet(frame).markEscaped();
            throw ps;
        } finally {
            if (enableStackDepthProtection) {
                code.image.stackDepth--;
            }
            materializeContextOnMethodExitNode.execute(frame);
        }
    }

    private static void initializeSlots(final CompiledCodeObject code, final VirtualFrame frame) {
        FrameAccess.initializeMarker(frame, code);
        FrameAccess.setInstructionPointer(frame, code, 0);
        FrameAccess.setStackPointer(frame, code, 0);
    }

    @ExplodeLoop
    private void initializeArgumentsAndTemps(final VirtualFrame frame) {
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(code.getNumArgsAndCopied());
        for (int i = 0; i < code.getNumArgsAndCopied(); i++) {
            pushNode.executePush(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            pushNode.executePush(frame, NilObject.SINGLETON);
        }
        assert FrameAccess.getStackPointer(frame, code) >= remainingTemps;
    }

    public Object executeResume(final VirtualFrame frame, final ContextObject context) {
        // maybe persist newContext, so there's no need to lookup the context to update its pc.
        try {
            final long initialPC = context.getInstructionPointerForBytecodeLoop();
            assert initialPC >= 0 : "Trying to execute a terminated/illegal context";
            if (initialPC == 0) {
                return startBytecode(frame);
            } else {
                // Avoid optimizing cases in which a context is resumed.
                return resumeBytecode(frame, initialPC);
            }
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } finally {
            code.image.lastSeenContext = null; // Stop materialization here.
        }
    }

    private GetOrCreateContextNode getGetOrCreateContextNode() {
        if (getOrCreateContextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(code));
        }
        return getOrCreateContextNode;
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://git.io/fjEDw).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object startBytecode(final VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int pc = 0;
        if (code.hasPrimitive()) {
            final CallPrimitiveNode callPrimitiveNode = (CallPrimitiveNode) fetchNextBytecodeNode(0);
            if (callPrimitiveNode.primitiveNode != null) {
                try {
                    return callPrimitiveNode.primitiveNode.executePrimitive(frame);
                } catch (final PrimitiveFailed e) {
                    /** getHandlePrimitiveFailedNode() acts as branch profile. */
                    getHandlePrimitiveFailedNode().executeHandle(frame, e);
                    LOG.log(Level.FINE, () -> callPrimitiveNode.primitiveNode +
                                    " (" + ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                    /** continue with fallback code. */
                }
            }
            pc = callPrimitiveNode.getSuccessorIndex();
            assert pc == CallPrimitiveNode.NUM_BYTECODES;
        }
        int backJumpCounter = 0;
        Object returnValue = null;
        bytecode_loop: while (true) {
            CompilerAsserts.partialEvaluationConstant(pc);
            final AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
            if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame, getFrameStackReadAndClearNode())) {
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
                final int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                if (CompilerDirectives.inInterpreter() && successor <= pc) {
                    backJumpCounter++;
                }
                pc = successor;
                continue bytecode_loop;
            } else if (node instanceof ReturnConstantNodeGen) {
                returnValue = ((ReturnConstantNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop;
            } else if (node instanceof ReturnReceiverNodeGen) {
                returnValue = ((ReturnReceiverNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop;
            } else if (node instanceof ReturnTopFromBlockNodeGen) {
                returnValue = ((ReturnTopFromBlockNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop;
            } else if (node instanceof ReturnTopFromMethodNodeGen) {
                returnValue = ((ReturnTopFromMethodNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop;
            } else if (node instanceof PushClosureNode) {
                final PushClosureNode pushClosureNode = (PushClosureNode) node;
                pushClosureNode.executePush(frame, getFrameStackReadAndClearNode());
                pc = pushClosureNode.getClosureSuccessorIndex();
                continue bytecode_loop;
            } else {
                pc = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, code, pc);
                node.executeVoid(frame);
                continue bytecode_loop;
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        assert backJumpCounter >= 0;
        LoopNode.reportLoopCount(this, backJumpCounter);
        return returnValue;
    }

    private FrameStackReadAndClearNode getFrameStackReadAndClearNode() {
        /* Lazily insert node because it is not needed if primitive succeeds. */
        if (readAndClearNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readAndClearNode = insert(FrameStackReadAndClearNode.create(code));
        }
        return readAndClearNode;
    }

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    /*
     * Non-optimized version of startBytecode which is used to resume contexts.
     */
    private Object resumeBytecode(final VirtualFrame frame, final long initialPC) {
        assert initialPC > 0;
        int pc = (int) initialPC;
        Object returnValue = null;
        bytecode_loop_slow: while (true) {
            final AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
            if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame, getFrameStackReadAndClearNode())) {
                    pc = jumpNode.getJumpSuccessorIndex();
                    continue bytecode_loop_slow;
                } else {
                    pc = jumpNode.getSuccessorIndex();
                    continue bytecode_loop_slow;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                pc = ((UnconditionalJumpNode) node).getJumpSuccessor();
                continue bytecode_loop_slow;
            } else if (node instanceof ReturnConstantNodeGen) {
                returnValue = ((ReturnConstantNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop_slow;
            } else if (node instanceof ReturnReceiverNodeGen) {
                returnValue = ((ReturnReceiverNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop_slow;
            } else if (node instanceof ReturnTopFromBlockNodeGen) {
                returnValue = ((ReturnTopFromBlockNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop_slow;
            } else if (node instanceof ReturnTopFromMethodNodeGen) {
                returnValue = ((ReturnTopFromMethodNodeGen) node).executeReturn(frame, getFrameStackReadAndClearNode());
                break bytecode_loop_slow;
            } else if (node instanceof PushClosureNode) {
                final PushClosureNode pushClosureNode = (PushClosureNode) node;
                pushClosureNode.executePush(frame, getFrameStackReadAndClearNode());
                pc = pushClosureNode.getClosureSuccessorIndex();
                continue bytecode_loop_slow;
            } else {
                final int successor = node.getSuccessorIndex();
                FrameAccess.setInstructionPointer(frame, code, successor);
                node.executeVoid(frame);
                pc = successor;
                continue bytecode_loop_slow;
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        return returnValue;
    }

    /*
     * Fetch next bytecode and insert AST nodes on demand if enabled.
     */
    @SuppressWarnings("unused")
    private AbstractBytecodeNode fetchNextBytecodeNode(final int pc) {
        if (DECODE_BYTECODE_ON_DEMAND && bytecodeNodes[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytecodeNodes[pc] = insert(SqueakBytecodeDecoder.decodeBytecode(code, pc));
            notifyInserted(bytecodeNodes[pc]);
        }
        return bytecodeNodes[pc];
    }

    /* Only use stackDepthProtection in interpreter or once per compilation unit (if at all). */
    private boolean enableStackDepthProtection() {
        return code.image.options.enableStackDepthProtection && (CompilerDirectives.inInterpreter() || CompilerDirectives.inCompilationRoot());
    }

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new ExecuteContextNodeWrapper(this, this, probe);
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return StandardTags.RootTag.class == tag;
    }

    @Override
    public String getDescription() {
        return code.toString();
    }

    @Override
    public SourceSection getSourceSection() {
        if (section == null) {
            if (code.image.isTesting()) {
                // Cannot provide source section in case of AbstractSqueakTestCaseWithDummyImage.
                return null;
            }
            final Source source = code.getSource();
            section = source.createSection(1, 1, source.getLength());
        }
        return section;
    }

    // FIXME: Trigger interrupt check on sends and in loops.
    @NodeInfo(cost = NodeCost.NONE)
    protected abstract static class TriggerInterruptHandlerNode extends AbstractNodeWithCode {
        protected static final int BYTECODE_LENGTH_THRESHOLD = 32;

        protected TriggerInterruptHandlerNode(final CompiledCodeObject code) {
            super(code);
        }

        @SuppressWarnings("unused")
        private static TriggerInterruptHandlerNode create(final CompiledCodeObject code) {
            return TriggerInterruptHandlerNodeGen.create(code);
        }

        protected abstract void executeGeneric(VirtualFrame frame);

        @SuppressWarnings("unused")
        @Specialization(guards = {"shouldTrigger(frame)"})
        protected static final void doTrigger(final VirtualFrame frame, @Cached("create(code)") final InterruptHandlerNode interruptNode) {
            interruptNode.executeTrigger(frame);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final void doNothing() {
            // Do not trigger.
        }

        protected final boolean shouldTrigger(@SuppressWarnings("unused") final VirtualFrame frame) {
            if (CompilerDirectives.inCompiledCode() && !CompilerDirectives.inCompilationRoot()) {
                return false; // never trigger in inlined code
            }
            return code.image.interrupt.isActiveAndShouldTrigger();
        }
    }

    private HandleNonLocalReturnNode getHandleNonLocalReturnNode() {
        if (handleNonLocalReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handleNonLocalReturnNode = insert(HandleNonLocalReturnNode.create(code));
        }
        return handleNonLocalReturnNode;
    }
}

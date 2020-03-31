/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.LogUtils;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

@GenerateWrapper
public class ExecuteContextNode extends AbstractNodeWithCode implements InstrumentableNode {
    private static final boolean DECODE_BYTECODE_ON_DEMAND = true;
    private static final int STACK_DEPTH_LIMIT = 25000;
    private static final int LOCAL_RETURN_PC = -2;
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 64;

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Child private InterruptHandlerNode interruptHandlerNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode;
    @Child private AbstractPrimitiveNode primitiveNode;
    @CompilationFinal private boolean primitiveNodeInitialized = false;

    @CompilationFinal(dimensions = 1) private final Object[] specialSelectors;

    @Children private FrameSlotReadNode[] slotReadNodes;
    @Children private FrameSlotWriteNode[] slotWriteNodes;
    @Children private Node[] pcNodes;
    @CompilationFinal(dimensions = 1) private final ConditionProfile[] pcProfiles;

    @Child private SendSelectorNode cannotReturnNode;

    private SourceSection section;

    protected ExecuteContextNode(final CompiledCodeObject code, final boolean resume) {
        super(code);
        if (DECODE_BYTECODE_ON_DEMAND) {
            bytecodeNodes = new AbstractBytecodeNode[SqueakBytecodeDecoder.trailerPosition(code)];
        } else {
            bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        }
        /*
         * Only check for interrupts if method is relatively large. Also, skip timer interrupts here
         * as they trigger too often, which causes a lot of context switches and therefore
         * materialization and deopts. Timer inputs are currently handled in
         * primitiveRelinquishProcessor (#230) only.
         */
        interruptHandlerNode = bytecodeNodes.length >= MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS ? InterruptHandlerNode.create(code, false) : null;
        materializeContextOnMethodExitNode = resume ? null : MaterializeContextOnMethodExitNode.create(code);
        slotReadNodes = new FrameSlotReadNode[code.getNumStackSlots()];
        slotWriteNodes = new FrameSlotWriteNode[code.getNumStackSlots()];
        pcNodes = new Node[SqueakBytecodeDecoder.trailerPosition(code)];
        pcProfiles = new ConditionProfile[pcNodes.length];
        specialSelectors = code.image.getSpecialSelectors().getObjectStorage();
    }

    protected ExecuteContextNode(final ExecuteContextNode executeContextNode) {
        this(executeContextNode.code, executeContextNode.materializeContextOnMethodExitNode == null);
    }

    public static ExecuteContextNode create(final CompiledCodeObject code, final boolean resume) {
        return new ExecuteContextNode(code, resume);
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    public Object executeFresh(final VirtualFrame frame) {
        FrameAccess.setInstructionPointer(frame, code, 0);
        final boolean enableStackDepthProtection = enableStackDepthProtection();
        try {
            if (enableStackDepthProtection && code.image.stackDepth++ > STACK_DEPTH_LIMIT) {
                final ContextObject context = getGetOrCreateContextNode().executeGet(frame);
                context.setProcess(code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached()));
                throw ProcessSwitch.createWithBoundary(context);
            }
            initialize(frame); // TODO
            if (interruptHandlerNode != null) {
                interruptHandlerNode.executeTrigger(frame);
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

    @ExplodeLoop
    private void initialize(final VirtualFrame frame) {
        final int initialSP = code instanceof CompiledBlockObject ? code.getNumArgsAndCopied() : code.getNumTemps();
        CompilerDirectives.isCompilationConstant(initialSP);
        final Object[] arguments = frame.getArguments();
        final int numArgs = code.getNumArgsAndCopied();
        CompilerDirectives.isCompilationConstant(numArgs);
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        for (int i = 0; i < numArgs; i++) {
            push(frame, i, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = numArgs; i < initialSP; i++) {
            push(frame, i, NilObject.SINGLETON);
        }
        FrameAccess.setStackPointer(frame, code, initialSP);
    }

    public final Object executeResumeAtStart(final VirtualFrame frame) {
        try {
            return startBytecode(frame);
        } catch (final NonLocalReturn nlr) {
            /** {@link getHandleNonLocalReturnNode()} acts as {@link BranchProfile} */
            return getHandleNonLocalReturnNode().executeHandle(frame, nlr);
        } finally {
            code.image.lastSeenContext = null; // Stop materialization here.
        }
    }

    public final Object executeResumeInMiddle(final VirtualFrame frame, final long initialPC) {
        try {
            return resumeBytecode(frame, initialPC);
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
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(code, true));
        }
        return getOrCreateContextNode;
    }

    private void push(final VirtualFrame frame, final int slot, final Object value) {
        getStackWriteNode(slot).executeWrite(frame, value);
    }

    private Object pop(final VirtualFrame frame, final int slot) {
        return getStackReadNode(slot).executeRead(frame);
    }

    private Object peek(final VirtualFrame frame, final int slot) {
        return getStackReadNode(slot).executeRead(frame);
    }

    private Object top(final VirtualFrame frame, final int stackPointer) {
        return pop(frame, stackPointer - 1);
    }

    private FrameSlotWriteNode getStackWriteNode(final int slot) {
        if (slotWriteNodes[slot] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            slotWriteNodes[slot] = insert(FrameSlotWriteNode.create(code.getStackSlot(slot)));
        }
        return slotWriteNodes[slot];
    }

    private FrameSlotReadNode getStackReadNode(final int slot) {
        if (slotReadNodes[slot] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            slotReadNodes[slot] = insert(FrameSlotReadNode.create(code.getStackSlot(slot)));
        }
        return slotReadNodes[slot];
    }

    private SendSelectorNode getCannotReturnNode() {
        if (cannotReturnNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cannotReturnNode = insert(SendSelectorNode.create(code, code.image.cannotReturn));
        }
        return cannotReturnNode;
    }

    private static byte variableIndex(final int i) {
        return (byte) (i & 63);
    }

    private static byte variableType(final int i) {
        return (byte) (i >> 6 & 3);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private Object startBytecode(final VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int pc = 0;
        int stackPointer = code instanceof CompiledBlockObject ? code.getNumArgsAndCopied() : code.getNumTemps();
        int backJumpCounter = 0;
        Object returnValue = null;
        bytecode_loop: while (pc != LOCAL_RETURN_PC) {
            CompilerAsserts.partialEvaluationConstant(pc);
            CompilerAsserts.partialEvaluationConstant(stackPointer);
            final int opcode = code.getBytes()[pc] & 0xFF;
            CompilerAsserts.partialEvaluationConstant(opcode);
            switch (opcode) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                    push(frame, stackPointer++, getAt0Node(pc).execute(FrameAccess.getReceiver(frame), opcode & 15));
                    pc++;
                    continue bytecode_loop;
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                    push(frame, stackPointer++, pop(frame, opcode & 15));
                    pc++;
                    continue bytecode_loop;
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                    push(frame, stackPointer++, code.getLiteral(opcode & 31));
                    pc++;
                    continue bytecode_loop;
                case 64:
                case 65:
                case 66:
                case 67:
                case 68:
                case 69:
                case 70:
                case 71:
                case 72:
                case 73:
                case 74:
                case 75:
                case 76:
                case 77:
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 83:
                case 84:
                case 85:
                case 86:
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                case 92:
                case 93:
                case 94:
                case 95:
                    push(frame, stackPointer++, getAt0Node(pc++).execute(code.getLiteral(opcode & 31), ASSOCIATION.VALUE));
                    continue bytecode_loop;
                case 96:
                case 97:
                case 98:
                case 99:
                case 100:
                case 101:
                case 102:
                case 103:
                    getAtPut0Node(pc++).execute(FrameAccess.getReceiver(frame), opcode & 7, pop(frame, --stackPointer));
                    continue bytecode_loop;
                case 104:
                case 105:
                case 106:
                case 107:
                case 108:
                case 109:
                case 110:
                case 111:
                    push(frame, opcode & 7, pop(frame, --stackPointer));
                    pc++;
                    continue bytecode_loop;
                case 112:
                    push(frame, stackPointer++, FrameAccess.getReceiver(frame));
                    pc++;
                    continue bytecode_loop;
                case 113:
                    push(frame, stackPointer++, BooleanObject.TRUE);
                    pc++;
                    continue bytecode_loop;
                case 114:
                    push(frame, stackPointer++, BooleanObject.FALSE);
                    pc++;
                    continue bytecode_loop;
                case 115:
                    push(frame, stackPointer++, NilObject.SINGLETON);
                    pc++;
                    continue bytecode_loop;
                case 116:
                    push(frame, stackPointer++, -1L);
                    pc++;
                    continue bytecode_loop;
                case 117:
                    push(frame, stackPointer++, 0L);
                    pc++;
                    continue bytecode_loop;
                case 118:
                    push(frame, stackPointer++, 1L);
                    pc++;
                    continue bytecode_loop;
                case 119:
                    push(frame, stackPointer++, 2L);
                    pc++;
                    continue bytecode_loop;
                case 120:
                    if (code instanceof CompiledMethodObject) {
                        if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
                            throw new NonLocalReturn(FrameAccess.getReceiver(frame), FrameAccess.getSender(frame));
                        } else {
                            returnValue = FrameAccess.getReceiver(frame);
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        }
                    } else {
                        assert code instanceof CompiledBlockObject;
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        final Object caller = homeContext.getFrameSender();
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            /** {@link getCannotReturnNode()} acts as {@link BranchProfile} */
                            getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), FrameAccess.getReceiver(frame));
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(FrameAccess.getReceiver(frame), caller);
                        }
                    }
                case 121:
                    if (code instanceof CompiledMethodObject) {
                        if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
                            throw new NonLocalReturn(BooleanObject.TRUE, FrameAccess.getSender(frame));
                        } else {
                            returnValue = BooleanObject.TRUE;
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        }
                    } else {
                        assert code instanceof CompiledBlockObject;
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        final Object caller = homeContext.getFrameSender();
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            /** {@link getCannotReturnNode()} acts as {@link BranchProfile} */
                            getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), BooleanObject.TRUE);
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(BooleanObject.TRUE, caller);
                        }
                    }
                case 122:
                    if (code instanceof CompiledMethodObject) {
                        if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
                            throw new NonLocalReturn(BooleanObject.FALSE, FrameAccess.getSender(frame));
                        } else {
                            returnValue = BooleanObject.FALSE;
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        }
                    } else {
                        assert code instanceof CompiledBlockObject;
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        final Object caller = homeContext.getFrameSender();
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            /** {@link getCannotReturnNode()} acts as {@link BranchProfile} */
                            getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), BooleanObject.FALSE);
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(BooleanObject.FALSE, caller);
                        }
                    }
                case 123:
                    if (code instanceof CompiledMethodObject) {
                        if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
                            throw new NonLocalReturn(NilObject.SINGLETON, FrameAccess.getSender(frame));
                        } else {
                            returnValue = NilObject.SINGLETON;
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        }
                    } else {
                        assert code instanceof CompiledBlockObject;
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        final Object caller = homeContext.getFrameSender();
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            /** {@link getCannotReturnNode()} acts as {@link BranchProfile} */
                            getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), NilObject.SINGLETON);
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(NilObject.SINGLETON, caller);
                        }
                    }
                case 124:
                    if (code instanceof CompiledMethodObject) {
                        if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                            assert FrameAccess.getSender(frame) instanceof ContextObject : "Sender must be a materialized ContextObject";
                            throw new NonLocalReturn(pop(frame, --stackPointer), FrameAccess.getSender(frame));
                        } else {
                            returnValue = pop(frame, --stackPointer);
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        }
                    } else {
                        assert code instanceof CompiledBlockObject;
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        final Object caller = homeContext.getFrameSender();
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            /** {@link getCannotReturnNode()} acts as {@link BranchProfile} */
                            getCannotReturnNode().executeSend(frame, getGetOrCreateContextNode().executeGet(frame), pop(frame, --stackPointer));
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(pop(frame, --stackPointer), caller);
                        }
                    }
                case 125: {
                    assert code instanceof CompiledBlockObject;
                    if (getConditionProfile(pc).profile(hasModifiedSender(frame))) {
                        // Target is sender of closure's home context.
                        final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                        assert homeContext.getProcess() != null;
                        // FIXME
                        final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                        final Object caller = homeContext.getFrameSender();
                        if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                            CompilerDirectives.transferToInterpreter(); // FIXME
                            final ContextObject currentContext = FrameAccess.getContext(frame);
                            assert currentContext != null;
                            getCannotReturnNode().executeSend(frame, currentContext, pop(frame, --stackPointer));
                            throw SqueakException.create("Should not reach");
                        } else {
                            throw new NonLocalReturn(pop(frame, --stackPointer), caller);
                        }
                    } else {
                        returnValue = pop(frame, --stackPointer);
                        pc = LOCAL_RETURN_PC;
                        continue bytecode_loop;
                    }
                }
                case 128: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final byte variableType = variableType(nextByte);
                    CompilerAsserts.partialEvaluationConstant(variableType);
                    final int variableIndex = variableIndex(nextByte);
                    switch (variableType) {
                        case 0:
                            push(frame, stackPointer++, getAt0Node(pc).execute(FrameAccess.getReceiver(frame), variableIndex));
                            pc += 2;
                            continue bytecode_loop;
                        case 1:
                            push(frame, stackPointer++, pop(frame, variableIndex));
                            pc += 2;
                            continue bytecode_loop;
                        case 2:
                            push(frame, stackPointer++, code.getLiteral(variableIndex));
                            pc += 2;
                            continue bytecode_loop;
                        case 3:
                            push(frame, stackPointer++, getAt0Node(pc).execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE));
                            pc += 2;
                            continue bytecode_loop;
                        default:
                            throw SqueakException.create("unexpected type for ExtendedPush");
                    }
                }
                case 129: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final byte variableType = variableType(nextByte);
                    CompilerAsserts.partialEvaluationConstant(variableType);
                    final int variableIndex = variableIndex(nextByte);
                    switch (variableType) {
                        case 0:
                            getAtPut0Node(pc).execute(FrameAccess.getReceiver(frame), variableIndex, top(frame, stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        case 1:
                            push(frame, variableIndex, top(frame, stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        case 2:
                            throw SqueakException.create("Unknown/uninterpreted bytecode:", nextByte);
                        case 3:
                            getAtPut0Node(pc).execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE, top(frame, stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        default:
                            throw SqueakException.create("illegal ExtendedStore bytecode");
                    }
                }
                case 130: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final byte variableType = variableType(nextByte);
                    CompilerAsserts.partialEvaluationConstant(variableType);
                    final int variableIndex = variableIndex(nextByte);
                    switch (variableType) {
                        case 0:
                            getAtPut0Node(pc).execute(FrameAccess.getReceiver(frame), variableIndex, pop(frame, --stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        case 1:
                            push(frame, variableIndex, pop(frame, --stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        case 2:
                            throw SqueakException.create("Unknown/uninterpreted bytecode:", nextByte);
                        case 3:
                            getAtPut0Node(pc).execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE, pop(frame, --stackPointer));
                            pc += 2;
                            continue bytecode_loop;
                        default:
                            throw SqueakException.create("illegal ExtendedStore bytecode");
                    }
                }
                case 131: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 31);
                    final int numRcvrAndArgs = 1 + (nextByte >> 5);
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 2, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null;
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 132: {
                    final int second = code.getBytes()[pc + 1] & 0xFF;
                    final int third = code.getBytes()[pc + 2] & 0xFF;
                    final int opType = second >> 5;
                    CompilerAsserts.partialEvaluationConstant(opType);
                    switch (opType) {
                        case 0: {
                            final NativeObject selector = (NativeObject) code.getLiteral(third);
                            final int numRcvrAndArgs = 1 + (second & 31);
                            final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                            final int currentPC = pc;
                            storePCandSP(frame, pc += 3, stackPointer -= numRcvrAndArgs);
                            final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                            assert result != null;
                            if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                                CompilerDirectives.transferToInterpreter();
                                pc = FrameAccess.getInstructionPointer(frame, code);
                            }
                            if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                                push(frame, stackPointer++, result);
                                continue bytecode_loop;
                            } else {
                                stackPointer++;
                                assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                                continue bytecode_loop;
                            }
                        }
                        case 1: {
                            final NativeObject selector = (NativeObject) code.getLiteral(third);
                            final int numRcvrAndArgs = 1 + (second & 31);
                            final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                            final int currentPC = pc;
                            storePCandSP(frame, pc += 3, stackPointer -= numRcvrAndArgs);
                            final Object result = getExecuteSendNode(currentPC, true).execute(frame, selector, receiverAndArguments);
                            assert result != null;
                            if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                                CompilerDirectives.transferToInterpreter();
                                pc = FrameAccess.getInstructionPointer(frame, code);
                            }
                            if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                                push(frame, stackPointer++, result);
                                continue bytecode_loop;
                            } else {
                                stackPointer++;
                                assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                                continue bytecode_loop;
                            }
                        }
                        case 2:
                            push(frame, stackPointer++, getAt0Node(pc).execute(FrameAccess.getReceiver(frame), third));
                            pc += 3;
                            continue bytecode_loop;
                        case 3:
                            push(frame, stackPointer++, code.getLiteral(third));
                            pc += 3;
                            continue bytecode_loop;
                        case 4:
                            push(frame, stackPointer++, getAt0Node(pc).execute(code.getLiteral(third), ASSOCIATION.VALUE));
                            pc += 3;
                            continue bytecode_loop;
                        case 5:
                            getAtPut0Node(pc).execute(FrameAccess.getReceiver(frame), third, top(frame, stackPointer));
                            pc += 3;
                            continue bytecode_loop;
                        case 6:
                            getAtPut0Node(pc).execute(FrameAccess.getReceiver(frame), third, pop(frame, --stackPointer));
                            pc += 3;
                            continue bytecode_loop;
                        case 7:
                            getAtPut0Node(pc).execute(code.getLiteral(third), ASSOCIATION.VALUE, top(frame, stackPointer));
                            pc += 3;
                            continue bytecode_loop;
                        default:
                            throw SqueakException.create("Unknown/uninterpreted bytecode:", opType);
                    }
                }
                case 133: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 31);
                    final int numRcvrAndArgs = 1 + (nextByte >> 5);
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 2, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, true).execute(frame, selector, receiverAndArguments);
                    assert result != null;
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 134: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 63);
                    final int numRcvrAndArgs = 1 + (nextByte >> 6);
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 2, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null;
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 135:
                    pop(frame, --stackPointer);
                    pc++;
                    continue bytecode_loop;
                case 136: {
                    final Object value = top(frame, stackPointer);
                    push(frame, stackPointer++, value);
                    pc++;
                    continue bytecode_loop;
                }
                case 137:
                    push(frame, stackPointer++, getGetOrCreateContextNode().executeGet(frame));
                    pc++;
                    continue bytecode_loop;
                case 138: {
                    final int nextByte = code.getBytes()[pc + 1] & 0xFF;
                    CompilerAsserts.partialEvaluationConstant(nextByte);
                    final int arraySize = nextByte & 127;
                    ArrayObject array;
                    if (arraySize == 0) {
                        // TODO: always use same ArrayObject?
                        array = code.image.asArrayOfObjects(ArrayUtils.EMPTY_ARRAY);
                    } else {
                        if (nextByte > 127) {
                            final Object[] values = createArgumentsForCall(frame, arraySize, stackPointer);
                            stackPointer -= arraySize;
                            array = code.image.asArrayOfObjects(values);
                        } else {
                            /**
                             * Pushing an ArrayObject with object strategy. Contents likely to be
                             * mixed values and therefore unlikely to benefit from storage strategy.
                             */
                            array = ArrayObject.createObjectStrategy(code.image, code.image.arrayClass, arraySize);
                        }
                    }
                    push(frame, stackPointer++, array);
                    pc += 2;
                    continue bytecode_loop;
                }
                case 139: {
                    assert pc == 0;
                    if (!primitiveNodeInitialized) {
                        assert primitiveNode == null;
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        assert code instanceof CompiledMethodObject && code.hasPrimitive();
                        final int byte1 = code.getBytes()[pc + 1] & 0xFF;
                        final int byte2 = code.getBytes()[pc + 2] & 0xFF;
                        final int primitiveIndex = byte1 + (byte2 << 8);
                        primitiveNode = insert(PrimitiveNodeFactory.forIndex((CompiledMethodObject) code, primitiveIndex));
                        primitiveNodeInitialized = true;
                    }
                    if (primitiveNode != null) {
                        try {
                            returnValue = primitiveNode.executePrimitive(frame);
                            pc = LOCAL_RETURN_PC;
                            continue bytecode_loop;
                        } catch (final PrimitiveFailed e) {
                            /* getHandlePrimitiveFailedNode() also acts as a BranchProfile. */
                            getHandlePrimitiveFailedNode().executeHandle(frame, e.getReasonCode());
                            /*
                             * Same toString() methods may throw compilation warnings, this is
                             * expected and ok for primitive failure logging purposes. Note that
                             * primitives that are not implemented are also not logged.
                             */
                            LogUtils.PRIMITIVES.fine(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
                                            ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                            /* continue with fallback code. */
                        }
                    }
                    pc = 3;
                    continue bytecode_loop;
                }
                case 140: {
                    final int indexInArray = code.getBytes()[pc + 1] & 0xFF;
                    final int indexOfArray = code.getBytes()[pc + 2] & 0xFF;
                    push(frame, stackPointer++, getAt0Node(pc).execute(peek(frame, indexOfArray), indexInArray));
                    pc += 3;
                    continue bytecode_loop;
                }
                case 141: {
                    final int indexInArray = code.getBytes()[pc + 1] & 0xFF;
                    final int indexOfArray = code.getBytes()[pc + 2] & 0xFF;
                    getAtPut0Node(pc).execute(peek(frame, indexOfArray), indexInArray, top(frame, stackPointer));
                    pc += 3;
                    continue bytecode_loop;
                }
                case 142: {
                    final int indexInArray = code.getBytes()[pc + 1] & 0xFF;
                    final int indexOfArray = code.getBytes()[pc + 2] & 0xFF;
                    getAtPut0Node(pc).execute(peek(frame, indexOfArray), indexInArray, pop(frame, --stackPointer));
                    pc += 3;
                    continue bytecode_loop;
                }
                case 143: {
                    final int byte1 = code.getBytes()[pc + 1] & 0xFF;
                    final int byte2 = code.getBytes()[pc + 2] & 0xFF;
                    final int byte3 = code.getBytes()[pc + 3] & 0xFF;
                    final int numArgs = byte1 & 0xF;
                    final int numCopied = byte1 >> 4 & 0xF;
                    final int blockSize = byte2 << 8 | byte3;
                    final Object receiver = FrameAccess.getReceiver(frame);
                    final Object[] copiedValues = createArgumentsForCall(frame, numCopied, stackPointer);
                    stackPointer -= numCopied;
                    final ContextObject outerContext = getGetOrCreateContextNode().executeGet(frame);
                    final CompiledBlockObject cachedBlock = code.findBlock(FrameAccess.getMethod(frame), numArgs, numCopied, pc + 4, blockSize);
                    final int cachedStartPC = cachedBlock.getInitialPC();
                    final BlockClosureObject closure = new BlockClosureObject(code.image, cachedBlock, cachedStartPC, numArgs, receiver, copiedValues, outerContext);
                    push(frame, stackPointer++, closure);
                    pc += 4 + blockSize;
                    continue bytecode_loop;
                }
                case 144:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 150:
                case 151: {
                    final int offset = (opcode & 7) + 1;
                    if (CompilerDirectives.inInterpreter() && offset < 0) {
                        backJumpCounter++;
                    }
                    pc += offset + 1;
                    continue bytecode_loop;
                }
                case 152:
                case 153:
                case 154:
                case 155:
                case 156:
                case 157:
                case 158:
                case 159: {
                    final Object value = pop(frame, --stackPointer);
                    if (value instanceof Boolean) {
                        if (getConditionProfile(pc, true).profile((boolean) value)) {
                            pc++;
                            continue bytecode_loop;
                        } else {
                            final int offset = (opcode & 7) + 1;
                            if (CompilerDirectives.inInterpreter() && offset < 0) {
                                backJumpCounter++;
                            }
                            pc += offset + 1;
                            continue bytecode_loop;
                        }
                    } else {
                        // TODO;
                        throw SqueakException.create("not yet implemented");
                    }
                }
                case 160:
                case 161:
                case 162:
                case 163:
                case 164:
                case 165:
                case 166:
                case 167: {
                    final int offset = ((opcode & 7) - 4 << 8) + (code.getBytes()[pc + 1] & 0xFF);
                    if (CompilerDirectives.inInterpreter() && offset < 0) {
                        backJumpCounter++;
                    }
                    pc += offset + 2;
                    continue bytecode_loop;
                }
                case 168:
                case 169:
                case 170:
                case 171: {
                    final Object value = pop(frame, --stackPointer);
                    if (value instanceof Boolean) {
                        if (getConditionProfile(pc, true).profile((boolean) value)) {
                            final int offset = ((opcode & 3) << 8) + (code.getBytes()[pc + 1] & 0xFF);
                            if (CompilerDirectives.inInterpreter() && offset < 0) {
                                backJumpCounter++;
                            }
                            pc += offset + 2;
                            continue bytecode_loop;
                        } else {
                            pc += 2;
                            continue bytecode_loop;
                        }
                    } else {
                        // TODO;
                        throw SqueakException.create("not yet implemented");
                    }
                }
                case 172:
                case 173:
                case 174:
                case 175: {
                    final Object value = pop(frame, --stackPointer);
                    if (value instanceof Boolean) {
                        if (getConditionProfile(pc, true).profile((boolean) value)) {
                            pc += 2;
                            continue bytecode_loop;
                        } else {
                            final int offset = ((opcode & 7) - 4 << 8) + (code.getBytes()[pc + 1] & 0xFF);
                            if (CompilerDirectives.inInterpreter() && offset < 0) {
                                backJumpCounter++;
                            }
                            pc += offset + 2;
                            continue bytecode_loop;
                        }
                    } else {
                        // TODO;
                        throw SqueakException.create("not yet implemented");
                    }
                }
                case 176:
                case 177:
                case 178:
                case 179:
                case 180:
                case 181:
                case 182:
                case 183:
                case 184:
                case 185:
                case 186:
                case 187:
                case 188:
                case 189:
                case 190:
                case 191:
                case 192:
                case 193:
                case 194:
                case 195:
                case 196:
                case 197:
                case 198:
                case 199:
                case 200:
                case 201:
                case 202:
                case 203:
                case 204:
                case 205:
                case 206:
                case 207: {
                    final NativeObject selector = (NativeObject) specialSelectors[(opcode - 176) * 2];
                    final int numRcvrAndArgs = 1 + (int) (long) specialSelectors[(opcode - 176) * 2 + 1];
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 1, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null : "Result of a message send should not be null";
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 208:
                case 209:
                case 210:
                case 211:
                case 212:
                case 213:
                case 214:
                case 215:
                case 216:
                case 217:
                case 218:
                case 219:
                case 220:
                case 221:
                case 222:
                case 223: {
                    final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                    final int numRcvrAndArgs = 1;
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 1, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null : "Result of a message send should not be null";
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 224:
                case 225:
                case 226:
                case 227:
                case 228:
                case 229:
                case 230:
                case 231:
                case 232:
                case 233:
                case 234:
                case 235:
                case 236:
                case 237:
                case 238:
                case 239: {
                    final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                    final int numRcvrAndArgs = 2;
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 1, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null : "Result of a message send should not be null";
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                case 240:
                case 241:
                case 242:
                case 243:
                case 244:
                case 245:
                case 246:
                case 247:
                case 248:
                case 249:
                case 250:
                case 251:
                case 252:
                case 253:
                case 254:
                case 255: {
                    final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                    final int numRcvrAndArgs = 3;
                    final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                    final int currentPC = pc;
                    storePCandSP(frame, pc += 1, stackPointer -= numRcvrAndArgs);
                    final Object result = getExecuteSendNode(currentPC, false).execute(frame, selector, receiverAndArguments);
                    assert result != null : "Result of a message send should not be null";
                    if (pc != FrameAccess.getInstructionPointer(frame, code)) {
                        CompilerDirectives.transferToInterpreter();
                        pc = FrameAccess.getInstructionPointer(frame, code);
                    }
                    if (getConditionProfile(currentPC).profile(result != AbstractSendNode.NO_RESULT)) {
                        push(frame, stackPointer++, result);
                        continue bytecode_loop;
                    } else {
                        stackPointer++;
                        assert stackPointer == FrameAccess.getStackPointer(frame, code) : "Stack inconsistent";
                        continue bytecode_loop;
                    }
                }
                default:
                    throw SqueakException.create("Unknown bytecode:", opcode);
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        assert backJumpCounter >= 0;
        LoopNode.reportLoopCount(this, backJumpCounter);
        return returnValue;
    }

    private ExecuteSendNode getExecuteSendNode(final int pc, final boolean isSuper) {
        if (pcNodes[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pcNodes[pc] = insert(ExecuteSendNode.create(code, isSuper));
        }
        return (ExecuteSendNode) pcNodes[pc];
    }

    private SqueakObjectAt0Node getAt0Node(final int pc) {
        if (pcNodes[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pcNodes[pc] = insert(SqueakObjectAt0Node.create());
        }
        return (SqueakObjectAt0Node) pcNodes[pc];
    }

    private SqueakObjectAtPut0Node getAtPut0Node(final int pc) {
        if (pcNodes[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pcNodes[pc] = insert(SqueakObjectAtPut0Node.create());
        }
        return (SqueakObjectAtPut0Node) pcNodes[pc];
    }

    private ConditionProfile getConditionProfile(final int pc) {
        return getConditionProfile(pc, false);
    }

    private ConditionProfile getConditionProfile(final int pc, final boolean isCounting) {
        if (pcProfiles[pc] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (isCounting) {
                pcProfiles[pc] = ConditionProfile.createCountingProfile();
            } else {
                pcProfiles[pc] = ConditionProfile.createBinaryProfile();
            }
        }
        return pcProfiles[pc];
    }

    private void storePCandSP(final VirtualFrame frame, final int pc, final int sp) {
        FrameAccess.setInstructionPointer(frame, code, pc);
        FrameAccess.setStackPointer(frame, code, sp);
    }

    @ExplodeLoop
    private Object[] createArgumentsForCall(final VirtualFrame frame, final int numArgs, final int stackPointerOffset) {
        CompilerAsserts.partialEvaluationConstant(numArgs);
        if (numArgs == 0) {
            return ArrayUtils.EMPTY_ARRAY;
        } else {
            final Object[] args = new Object[numArgs];
            int stackPointer = stackPointerOffset;
            for (int i = numArgs - 1; i >= 0; --i) {
                args[i] = pop(frame, --stackPointer);
            }
            return args;
        }
    }

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    /*
     * Non-optimized version of startBytecode used to resume contexts.
     */
    private Object resumeBytecode(final VirtualFrame frame, final long initialPC) {
        assert initialPC > 0 : "Trying to resume a fresh/terminated/illegal context";
        int pc = (int) initialPC;
        Object returnValue = null;
        bytecode_loop_slow: while (pc != LOCAL_RETURN_PC) {
            final AbstractBytecodeNode node = fetchNextBytecodeNode(pc);
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
                continue bytecode_loop_slow;
            } else if (node instanceof ConditionalJumpNode) {
                final ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                if (jumpNode.executeCondition(frame)) {
                    pc = jumpNode.getJumpSuccessorIndex();
                    continue bytecode_loop_slow;
                } else {
                    pc = jumpNode.getSuccessorIndex();
                    continue bytecode_loop_slow;
                }
            } else if (node instanceof UnconditionalJumpNode) {
                pc = ((UnconditionalJumpNode) node).getJumpSuccessor();
                continue bytecode_loop_slow;
            } else if (node instanceof AbstractReturnNode) {
                returnValue = ((AbstractReturnNode) node).executeReturn(frame);
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop_slow;
            } else if (node instanceof PushClosureNode) {
                final PushClosureNode pushClosureNode = (PushClosureNode) node;
                pushClosureNode.executePush(frame);
                pc = pushClosureNode.getClosureSuccessorIndex();
                continue bytecode_loop_slow;
            } else {
                /* All other bytecode nodes. */
                node.executeVoid(frame);
                pc = node.getSuccessorIndex();
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

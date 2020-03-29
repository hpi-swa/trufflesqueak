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
import de.hpi.swa.graal.squeak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.AbstractReturnNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackInitializationNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.Bytecodes;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.LogUtils;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

@GenerateWrapper
public class ExecuteContextNode extends AbstractNodeWithCode implements InstrumentableNode {
    private static final boolean DECODE_BYTECODE_ON_DEMAND = true;
    private static final int STACK_DEPTH_LIMIT = 25000;
    private static final int LOCAL_RETURN_PC = -2;
    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;

    @Children private AbstractBytecodeNode[] bytecodeNodes;
    @Child private HandleNonLocalReturnNode handleNonLocalReturnNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    @Child private FrameStackInitializationNode frameInitializationNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;
    @Child private InterruptHandlerNode interruptHandlerNode;
    @Child private MaterializeContextOnMethodExitNode materializeContextOnMethodExitNode;
    @Child private AbstractPrimitiveNode primitiveNode;
    @CompilationFinal private boolean primitiveNodeInitialized = false;

    @Children private FrameSlotReadNode[] slotReadNodes;
    @Children private FrameSlotWriteNode[] slotWriteNodes;
    @Children private Node[] opcodeNodes;

    @Child private SqueakObjectAt0Node at0Node;
    @Child private SqueakObjectAtPut0Node atPut0Node;
    @Child private SendSelectorNode cannotReturnNode;

    private SourceSection section;

    protected ExecuteContextNode(final CompiledCodeObject code, final boolean resume) {
        super(code);
        if (DECODE_BYTECODE_ON_DEMAND) {
            bytecodeNodes = new AbstractBytecodeNode[SqueakBytecodeDecoder.trailerPosition(code)];
        } else {
            bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        }
        frameInitializationNode = resume ? null : FrameStackInitializationNode.create(code);
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
        opcodeNodes = new Node[SqueakBytecodeDecoder.trailerPosition(code)];
    }

    protected ExecuteContextNode(final ExecuteContextNode executeContextNode) {
        this(executeContextNode.code, executeContextNode.frameInitializationNode == null);
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
            frameInitializationNode.executeInitialize(frame);
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
            getOrCreateContextNode = insert(GetOrCreateContextNode.create(code, false));
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

    private SqueakObjectAt0Node getAt0Node() {
        if (at0Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            at0Node = insert(SqueakObjectAt0Node.create());
        }
        return at0Node;
    }

    private SqueakObjectAtPut0Node getAtPut0Node() {
        if (atPut0Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            atPut0Node = insert(SqueakObjectAtPut0Node.create());
        }
        return atPut0Node;
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
        int backJumpCounter = 0;
        int stackPointer = code.getNumTemps();
        Object returnValue = null;
        bytecode_loop: while (pc != LOCAL_RETURN_PC) {
            CompilerAsserts.partialEvaluationConstant(pc);
            final int opcode = code.getBytes()[pc] & 0xFF;
            if (Bytecodes.PUSH_RECEIVER_VARIABLE_START <= opcode && opcode <= Bytecodes.PUSH_RECEIVER_VARIABLE_END) {
                push(frame, stackPointer++, getAt0Node().execute(FrameAccess.getReceiver(frame), opcode & 15));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_TEMPORARY_LOCATION_START <= opcode && opcode <= Bytecodes.PUSH_TEMPORARY_LOCATION_END) {
                push(frame, stackPointer++, pop(frame, opcode & 15));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_LITERAL_CONSTANT_START <= opcode && opcode <= Bytecodes.PUSH_LITERAL_CONSTANT_END) {
                push(frame, stackPointer++, code.getLiteral(opcode & 31));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_LITERAL_VARIABLE_START <= opcode && opcode <= Bytecodes.PUSH_LITERAL_VARIABLE_END) {
                push(frame, stackPointer++, getAt0Node().execute(code.getLiteral(opcode & 31), ASSOCIATION.VALUE));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.POP_INTO_RECEIVER_VARIABLE_START <= opcode && opcode <= Bytecodes.POP_INTO_RECEIVER_VARIABLE_END) {
                getAtPut0Node().execute(FrameAccess.getReceiver(frame), opcode & 7, pop(frame, --stackPointer));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.POP_INTO_TEMPORARY_LOCATION_START <= opcode && opcode <= Bytecodes.POP_INTO_TEMPORARY_LOCATION_END) {
                push(frame, opcode & 7, pop(frame, --stackPointer));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_RECEIVER == opcode) {
                push(frame, stackPointer++, FrameAccess.getReceiver(frame));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_TRUE == opcode) {
                push(frame, stackPointer++, BooleanObject.TRUE);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_FALSE == opcode) {
                push(frame, stackPointer++, BooleanObject.FALSE);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_NIL == opcode) {
                push(frame, stackPointer++, NilObject.SINGLETON);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_MINUS_ONE == opcode) {
                push(frame, stackPointer++, -1L);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_ZERO == opcode) {
                push(frame, stackPointer++, 0L);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_ONE == opcode) {
                push(frame, stackPointer++, 1L);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_TWO == opcode) {
                push(frame, stackPointer++, 2L);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_RECEIVER == opcode) {
                returnValue = FrameAccess.getReceiver(frame);
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_TRUE == opcode) {
                returnValue = BooleanObject.TRUE;
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_FALSE == opcode) {
                returnValue = BooleanObject.FALSE;
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_NIL == opcode) {
                returnValue = NilObject.SINGLETON;
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_TOP_FROM_METHOD == opcode) {
                returnValue = pop(frame, --stackPointer);
                pc = LOCAL_RETURN_PC;
                continue bytecode_loop;
            } else if (Bytecodes.RETURN_TOP_FROM_BLOCK == opcode) {
                if (hasModifiedSender(frame)) {
                    // Target is sender of closure's home context.
                    final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
                    assert homeContext.getProcess() != null;
                    // FIXME
                    final boolean homeContextNotOnTheStack = homeContext.getProcess() != code.image.getActiveProcess(AbstractPointersObjectReadNode.getUncached());
                    final Object caller = homeContext.getFrameSender();
                    if (caller == NilObject.SINGLETON || homeContextNotOnTheStack) {
                        final ContextObject currentContext = FrameAccess.getContext(frame);
                        assert currentContext != null;
                        getCannotReturnNode().executeSend(frame, currentContext, pop(frame, --stackPointer));
                        throw SqueakException.create("Should not reach");
                    }
                    throw new NonLocalReturn(pop(frame, --stackPointer), caller);
                } else {
                    returnValue = pop(frame, --stackPointer);
                    pc = LOCAL_RETURN_PC;
                    continue bytecode_loop;
                }
            } else if (Bytecodes.EXTENDED_PUSH == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final int variableIndex = variableIndex(nextByte);
                switch (variableType(nextByte)) {
                    case 0:
                        push(frame, stackPointer++, getAt0Node().execute(FrameAccess.getReceiver(frame), variableIndex));
                        pc++;
                        continue bytecode_loop;
                    case 1:
                        push(frame, stackPointer++, pop(frame, variableIndex));
                        pc++;
                        continue bytecode_loop;
                    case 2:
                        push(frame, stackPointer++, code.getLiteral(variableIndex));
                        pc++;
                        continue bytecode_loop;
                    case 3:
                        push(frame, stackPointer++, getAt0Node().execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE));
                        pc++;
                        continue bytecode_loop;
                    default:
                        throw SqueakException.create("unexpected type for ExtendedPush");
                }
            } else if (Bytecodes.EXTENDED_STORE == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final int variableIndex = variableIndex(nextByte);
                switch (variableType(nextByte)) {
                    case 0:
                        getAtPut0Node().execute(FrameAccess.getReceiver(frame), variableIndex, top(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 1:
                        push(frame, variableIndex, top(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 2:
                        throw SqueakException.create("Unknown/uninterpreted bytecode:", nextByte);
                    case 3:
                        getAtPut0Node().execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE, top(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    default:
                        throw SqueakException.create("illegal ExtendedStore bytecode");
                }
            } else if (Bytecodes.EXTENDED_POP == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final int variableIndex = variableIndex(nextByte);
                switch (variableType(nextByte)) {
                    case 0:
                        getAtPut0Node().execute(FrameAccess.getReceiver(frame), variableIndex, pop(frame, --stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 1:
                        push(frame, variableIndex, pop(frame, --stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 2:
                        throw SqueakException.create("Unknown/uninterpreted bytecode:", nextByte);
                    case 3:
                        getAtPut0Node().execute(code.getLiteral(variableIndex), ASSOCIATION.VALUE, pop(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    default:
                        throw SqueakException.create("illegal ExtendedStore bytecode");
                }
            } else if (Bytecodes.SINGLE_EXTENDED_SEND == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 31);
                final int numRcvrAndArgs = 1 + (nextByte >> 5);
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.DOUBLE_EXTENDED_DO_ANYTHING == opcode) {
                final int second = code.getBytes()[++pc] & 0xFF;
                final int third = code.getBytes()[++pc] & 0xFF;
                final int opType = second >> 5;
                switch (opType) {
                    case 0: {
                        final NativeObject selector = (NativeObject) code.getLiteral(third);
                        final int numRcvrAndArgs = 1 + (second & 31);
                        final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                        stackPointer -= numRcvrAndArgs;
                        final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                        if (result != AbstractSendNode.NO_RESULT) {
                            push(frame, stackPointer++, result);
                        }
                        pc++;
                        continue bytecode_loop;
                    }
                    case 1: {
                        final NativeObject selector = (NativeObject) code.getLiteral(third);
                        final int numRcvrAndArgs = 1 + (second & 31);
                        final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                        stackPointer -= numRcvrAndArgs;
                        final Object result = getExecuteSendNode(pc, true).execute(frame, selector, receiverAndArguments);
                        if (result != AbstractSendNode.NO_RESULT) {
                            push(frame, stackPointer++, result);
                        }
                        pc++;
                        continue bytecode_loop;
                    }
                    case 2:
                        push(frame, stackPointer++, getAt0Node().execute(FrameAccess.getReceiver(frame), third));
                        pc++;
                        continue bytecode_loop;
                    case 3:
                        push(frame, stackPointer++, code.getLiteral(third));
                        pc++;
                        continue bytecode_loop;
                    case 4:
                        push(frame, stackPointer++, getAt0Node().execute(code.getLiteral(third), ASSOCIATION.VALUE));
                        pc++;
                        continue bytecode_loop;
                    case 5:
                        getAtPut0Node().execute(FrameAccess.getReceiver(frame), third, top(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 6:
                        getAtPut0Node().execute(FrameAccess.getReceiver(frame), third, pop(frame, --stackPointer));
                        pc++;
                        continue bytecode_loop;
                    case 7:
                        getAtPut0Node().execute(code.getLiteral(third), ASSOCIATION.VALUE, top(frame, stackPointer));
                        pc++;
                        continue bytecode_loop;
                    default:
                        throw SqueakException.create("Unknown/uninterpreted bytecode:", opType);
                }
            } else if (Bytecodes.SINGLE_EXTENDED_SUPER == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 31);
                final int numRcvrAndArgs = 1 + (nextByte >> 5);
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, true).execute(frame, selector, receiverAndArguments);
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.SECOND_EXTENDED_SEND == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final NativeObject selector = (NativeObject) code.getLiteral(nextByte & 63);
                final int numRcvrAndArgs = 1 + (nextByte >> 6);
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.POP == opcode) {
                pop(frame, --stackPointer);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.DUP == opcode) {
                final Object value = top(frame, stackPointer);
                push(frame, stackPointer++, value);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_THIS_CONTEXT == opcode) {
                push(frame, stackPointer++, getGetOrCreateContextNode().executeGet(frame));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_NEW_ARRAY == opcode) {
                final int nextByte = code.getBytes()[++pc] & 0xFF;
                final int arraySize = nextByte & 127;
                ArrayObject array;
                if (opcode > 127) {
                    final Object[] values = new Object[arraySize];
                    for (int i = 0; i < arraySize; i++) {
                        values[i] = pop(frame, stackPointer - i);
                    }
                    stackPointer -= arraySize;
                    array = code.image.asArrayOfObjects(values);
                } else {
                    /**
                     * Pushing an ArrayObject with object strategy. Contents likely to be mixed
                     * values and therefore unlikely to benefit from storage strategy.
                     */
                    array = ArrayObject.createObjectStrategy(code.image, code.image.arrayClass, arraySize);
                }
                push(frame, stackPointer++, array);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.CALL_PRIMITIVE == opcode) {
                if (!primitiveNodeInitialized) {
                    assert primitiveNode == null;
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    assert code instanceof CompiledMethodObject && code.hasPrimitive();
                    final int byte1 = code.getBytes()[++pc] & 0xFF;
                    final int byte2 = code.getBytes()[++pc] & 0xFF;
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
                         * Same toString() methods may throw compilation warnings, this is expected
                         * and ok for primitive failure logging purposes. Note that primitives that
                         * are not implemented are also not logged.
                         */
                        LogUtils.PRIMITIVES.fine(() -> primitiveNode.getClass().getSimpleName() + " failed (arguments: " +
                                        ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                        /* continue with fallback code. */
                    }
                }
                pc = CallPrimitiveNode.NUM_BYTECODES;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_REMOTE_TEMP == opcode) {
                final int indexInArray = code.getBytes()[++pc] & 0xFF;
                final int indexOfArray = code.getBytes()[++pc] & 0xFF;
                push(frame, stackPointer++, getAt0Node().execute(peek(frame, indexOfArray), indexInArray));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.STORE_INTO_REMOTE_TEMP == opcode) {
                final int indexInArray = code.getBytes()[++pc] & 0xFF;
                final int indexOfArray = code.getBytes()[++pc] & 0xFF;
                getAtPut0Node().execute(peek(frame, indexOfArray), indexInArray, top(frame, stackPointer));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.POP_INTO_REMOTE_TEMP == opcode) {
                final int indexInArray = code.getBytes()[++pc] & 0xFF;
                final int indexOfArray = code.getBytes()[++pc] & 0xFF;
                getAtPut0Node().execute(peek(frame, indexOfArray), indexInArray, pop(frame, --stackPointer));
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.PUSH_CLOSURE == opcode) {
                final int byte1 = code.getBytes()[++pc] & 0xFF;
                final int byte2 = code.getBytes()[++pc] & 0xFF;
                final int byte3 = code.getBytes()[++pc] & 0xFF;
                final int numArgs = byte1 & 0xF;
                final int numCopied = byte1 >> 4 & 0xF;
                final int blockSize = byte2 << 8 | byte3;
                final Object receiver = FrameAccess.getReceiver(frame);
                final Object[] copiedValues = createArgumentsForCall(frame, numArgs, stackPointer);
                stackPointer -= numArgs;
                final ContextObject outerContext = getGetOrCreateContextNode().executeGet(frame);
                final CompiledBlockObject cachedBlock = code.findBlock(FrameAccess.getMethod(frame), numArgs, numCopied, pc, blockSize);
                final int cachedStartPC = cachedBlock.getInitialPC();
                final BlockClosureObject closure = new BlockClosureObject(code.image, cachedBlock, cachedStartPC, numArgs, receiver, copiedValues, outerContext);
                push(frame, stackPointer++, closure);
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.UNCONDITIONAL_JUMP_START <= opcode && opcode <= Bytecodes.UNCONDITIONAL_JUMP_END) {
                final int offset = (opcode & 7) + 1;
                if (CompilerDirectives.inInterpreter() && offset < 0) {
                    backJumpCounter++;
                }
                pc += offset + 1;
                continue bytecode_loop;
            } else if (Bytecodes.CONDITIONAL_JUMP_START <= opcode && opcode <= Bytecodes.CONDITIONAL_JUMP_END) {
                final Object value = pop(frame, --stackPointer);
                if (value instanceof Boolean) {
                    if (!(boolean) value) {
                        final int offset = (opcode & 7) + 1;
                        if (CompilerDirectives.inInterpreter() && offset < 0) {
                            backJumpCounter++;
                        }
                        pc += offset;
                    }
                    pc++;
                } else {
                    // TODO;
                    throw SqueakException.create("not yet implemented");
                }
                continue bytecode_loop;
            } else if (Bytecodes.UNCONDITIONAL_JUMP_LONG_START <= opcode && opcode <= Bytecodes.UNCONDITIONAL_JUMP_LONG_END) {
                final int offset = ((opcode & 7) - 4 << 8) + (code.getBytes()[pc + 1] & 0xFF);
                if (CompilerDirectives.inInterpreter() && offset < 0) {
                    backJumpCounter++;
                }
                pc += offset + 1;
                continue bytecode_loop;
            } else if (Bytecodes.CONDITIONAL_JUMP_LONG_TRUE_START <= opcode && opcode <= Bytecodes.CONDITIONAL_JUMP_LONG_TRUE_END) {
                final Object value = pop(frame, --stackPointer);
                if (value instanceof Boolean) {
                    if (!(boolean) value) {
                        final int offset = ((opcode & 3) << 8) + (code.getBytes()[pc + 1] & 0xFF);
                        if (CompilerDirectives.inInterpreter() && offset < 0) {
                            backJumpCounter++;
                        }
                        pc += offset;
                    }
                    pc += 2;
                } else {
                    // TODO;
                    throw SqueakException.create("not yet implemented");
                }
                continue bytecode_loop;
            } else if (Bytecodes.CONDITIONAL_JUMP_LONG_FALSE_START <= opcode && opcode <= Bytecodes.CONDITIONAL_JUMP_LONG_FALSE_END) {
                final Object value = pop(frame, --stackPointer);
                if (value instanceof Boolean) {
                    if ((boolean) value) {
                        final int offset = ((opcode & 7) - 4 << 8) + (code.getBytes()[pc + 1] & 0xFF);
                        if (CompilerDirectives.inInterpreter() && offset < 0) {
                            backJumpCounter++;
                        }
                        pc += offset;
                    }
                    pc += 2;
                } else {
                    // TODO;
                    throw SqueakException.create("not yet implemented");
                }
                continue bytecode_loop;
            } else if (Bytecodes.SEND_SPECIAL_SELECTOR_START <= opcode && opcode <= Bytecodes.SEND_SPECIAL_SELECTOR_END) {
                final NativeObject selector = code.image.getSpecialSelector(opcode - 176);
                final int numRcvrAndArgs = 1 + code.image.getSpecialSelectorNumArgs(opcode - 176);
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                assert result != null : "Result of a message send should not be null";
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.SEND_LITERAL_SELECTOR_0_START <= opcode && opcode <= Bytecodes.SEND_LITERAL_SELECTOR_0_END) {
                final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                final int numRcvrAndArgs = 1;
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                assert result != null : "Result of a message send should not be null";
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.SEND_LITERAL_SELECTOR_1_START <= opcode && opcode <= Bytecodes.SEND_LITERAL_SELECTOR_1_END) {
                final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                final int numRcvrAndArgs = 2;
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                assert result != null : "Result of a message send should not be null";
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else if (Bytecodes.SEND_LITERAL_SELECTOR_2_START <= opcode && opcode <= Bytecodes.SEND_LITERAL_SELECTOR_2_END) {
                final NativeObject selector = (NativeObject) code.getLiteral(opcode & 0xF);
                final int numRcvrAndArgs = 3;
                final Object[] receiverAndArguments = createArgumentsForCall(frame, numRcvrAndArgs, stackPointer);
                stackPointer -= numRcvrAndArgs;
                final Object result = getExecuteSendNode(pc, false).execute(frame, selector, receiverAndArguments);
                assert result != null : "Result of a message send should not be null";
                if (result != AbstractSendNode.NO_RESULT) {
                    push(frame, stackPointer++, result);
                }
                pc++;
                continue bytecode_loop;
            } else {
                throw SqueakException.create("Unknown/uninterpreted bytecode:", opcode);
            }
        }
        assert returnValue != null && !hasModifiedSender(frame);
        FrameAccess.terminate(frame, code);
        assert backJumpCounter >= 0;
        LoopNode.reportLoopCount(this, backJumpCounter);
        return returnValue;
    }

    private ExecuteSendNode getExecuteSendNode(final int opcode, final boolean isSuper) {
        if (opcodeNodes[opcode] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            opcodeNodes[opcode] = insert(ExecuteSendNode.create(code, isSuper));
        }
        return (ExecuteSendNode) opcodeNodes[opcode];
    }

    @ExplodeLoop
    private Object[] createArgumentsForCall(final VirtualFrame frame, final int numArgs, final int stackPointerOffset) {
        CompilerAsserts.partialEvaluationConstant(numArgs);
        final Object[] args = new Object[numArgs];
        int stackPointer = stackPointerOffset;
        for (int i = numArgs - 1; i >= 0; --i) {
            args[i] = pop(frame, --stackPointer);
        }
        return args;
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

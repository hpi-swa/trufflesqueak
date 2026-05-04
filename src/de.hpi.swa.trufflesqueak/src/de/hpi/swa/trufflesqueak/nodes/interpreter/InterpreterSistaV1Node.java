/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import static com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig.Argument.ExpansionKind.MATERIALIZED;
import static com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig.Argument.ExpansionKind.VIRTUAL;
import static de.hpi.swa.trufflesqueak.nodes.SqueakGuards.isOverflowDivision;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.EarlyEscapeAnalysis;
import com.oracle.truffle.api.CompilerDirectives.EarlyInline;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.HostCompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterFetchOpcode;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandler;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig.Argument;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.impl.FrameWithoutBoxing;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.CountingConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.AbstractStandardSendReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectedSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsInLoopNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitAndNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitOrNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimNotEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatAddNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatNotEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatSubtractNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class InterpreterSistaV1Node extends AbstractInterpreterNode {
    public InterpreterSistaV1Node(final CompiledCodeObject code) {
        super(code);
    }

    public InterpreterSistaV1Node(final InterpreterSistaV1Node original) {
        super(original);
    }

    @EarlyInline
    static long computeExtA(final long currentExtBA, final int bytecode) {
        final int newExtA = ((int) currentExtBA << 8) | bytecode;
        return (currentExtBA >>> 32 << 32) | Integer.toUnsignedLong(newExtA);
    }

    @EarlyInline
    static long computeExtB(final long currentExtBA, final int bytecode) {
        /*
         * OSVM maintains a counter (numExtB) to determine whether an EXT_B byte code is the first
         * in a series. Here, we use extB == 0 to indicate that the byte code is the first. At the
         * moment, Squeak only emits single EXT_B byte codes, so this method will always work.
         * However, when the image starts using sequences of EXT_B byte codes and the value being
         * encoded is a positive integer with positions 7, 15 or 23 as the highest set bit, the
         * decoded value will be interpreted as a negative integer. For example, the emitted
         * sequence for the value 0x8765 would be 0x00, 0x87, 0x65. If we assume that the encoder
         * will never try to encode the value 0 (since that is the default value without any emitted
         * byte codes), we can detect the case of the leading zero byte by setting the upper byte of
         * extB and relying on the next byte to shift that byte out of the extB register.
         */
        final int extB = (int) (currentExtBA >> 32);
        final int newExtB;
        if (extB == 0) {
            /* leading byte is signed */
            /* make sure newExtB is non-zero for next byte processing */
            newExtB = bytecode == 0 ? 0x80000000 : (byte) bytecode;
        } else {
            /* subsequent bytes are unsigned */
            newExtB = (extB << 8) | bytecode;
        }
        return ((long) newExtB << 32) | Integer.toUnsignedLong((int) currentExtBA);
    }

    @ValueType
    private static final class VirtualState {
        int sp;
        long extBA;

        @EarlyInline
        VirtualState(final int sp) {
            this.sp = sp;
        }

        @EarlyInline
        int getExtA() {
            return (int) extBA;
        }

        @EarlyInline
        int getExtB() {
            return (int) (extBA >> 32);
        }

        @EarlyInline
        void updateExtA(final int bytecode) {
            extBA = computeExtA(extBA, bytecode);
        }

        @EarlyInline
        void updateExtB(final int bytecode) {
            extBA = computeExtB(extBA, bytecode);
        }

        @EarlyInline
        void resetExtAB() {
            extBA = 0;
        }
    }

    private static final class State {
        private final byte[] bytecode;
        private final LoopCounter loopCounter;
        private int interpreterLoopCounter;

        @EarlyInline
        State(final byte[] bytecode, final LoopCounter loopCounter) {
            this.bytecode = bytecode;
            this.loopCounter = loopCounter;
            this.interpreterLoopCounter = 0;
        }

        @EarlyInline
        private void reportLoopCountOnReturn(final Node source) {
            if (CompilerDirectives.hasNextTier()) {
                final int count = CompilerDirectives.inInterpreter() ? interpreterLoopCounter : loopCounter.value;
                if (count > 0) {
                    LoopNode.reportLoopCount(source, count);
                }
            }
        }
    }

    @Override
    protected void processBytecode(final int startPC, final int endPC) {
        final byte[] bc = code.getBytes();
        final SqueakImageContext image = SqueakImageContext.getSlow();

        int pc = startPC;
        assert pc < endPC;
        final VirtualState vstate = new VirtualState(0);

        while (pc < endPC) {
            final int currentPC = pc++;
            final int b = currentBytecode(bc, currentPC);
            switch (b) {
                /* 1 byte bytecodes */
                case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    break;
                }
                case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                    BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                    setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(b & 0xF)));
                    break;
                }
                case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                    BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                    BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                    BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F, //
                    BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                    BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, //
                    BC.PUSH_RECEIVER, BC.PUSH_CONSTANT_TRUE, BC.PUSH_CONSTANT_FALSE, BC.PUSH_CONSTANT_NIL, BC.PUSH_CONSTANT_ZERO, BC.PUSH_CONSTANT_ONE, //
                    BC.RETURN_RECEIVER, BC.RETURN_TRUE, BC.RETURN_FALSE, BC.RETURN_NIL, BC.RETURN_TOP_FROM_METHOD, BC.RETURN_NIL_FROM_BLOCK, BC.RETURN_TOP_FROM_BLOCK, //
                    BC.DUPLICATE_TOP, //
                    BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7, //
                    BC.POP_STACK: {
                    break;
                }
                case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                    if (vstate.getExtB() == 0) {
                        break;
                    } else {
                        throw unknownBytecode();
                    }
                }
                case BC.EXT_NOP:
                    vstate.resetExtAB();
                    break;
                case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y: {
                    setData(currentPC, insert(Dispatch0NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD))));
                    break;
                }
                case BC.BYTECODE_PRIM_CLASS: {
                    setData(currentPC, insert(SqueakObjectClassNodeGen.create()));
                    break;
                }
                case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, //
                    BC.BYTECODE_PRIM_EQUAL, BC.BYTECODE_PRIM_NOT_EQUAL, BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_DIV, //
                    BC.BYTECODE_PRIM_BIT_AND, BC.BYTECODE_PRIM_BIT_OR, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                    setData(currentPC, insert(Dispatch1NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD))));
                    break;
                }
                case BC.BYTECODE_PRIM_IDENTICAL, BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                    setData(currentPC, insert(SqueakObjectIdentityNodeGen.create()));
                    break;
                }
                case BC.BYTECODE_PRIM_AT_PUT: {
                    setData(currentPC, insert(Dispatch2NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD))));
                    break;
                }
                case BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                    BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    setData(currentPC, insert(Dispatch0NodeGen.create(selector)));
                    break;
                }
                case BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                    BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    setData(currentPC, insert(Dispatch1NodeGen.create(selector)));
                    break;
                }
                case BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                    BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    setData(currentPC, insert(Dispatch2NodeGen.create(selector)));
                    break;
                }
                case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                    final int jumpOffset = calculateShortOffset(b);
                    if (jumpOffset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(pc, 1, jumpOffset)));
                    }
                    break;
                }
                case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7, //
                    BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                    setData(currentPC, CountingConditionProfile.create());
                    break;
                }
                case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    break;
                }
                /* 2 byte bytecodes */
                case BC.EXT_A: {
                    vstate.updateExtA(getUnsignedInt(bc, pc++));
                    break;
                }
                case BC.EXT_B: {
                    vstate.updateExtB(getUnsignedInt(bc, pc++));
                    break;
                }
                case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(getByteExtended(bc, pc, vstate.getExtA()))));
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_CHARACTER: {
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                case BC.LONG_PUSH_TEMPORARY_VARIABLE, BC.PUSH_NEW_ARRAY, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE, BC.LONG_STORE_TEMPORARY_VARIABLE: {
                    pc++;
                    break;
                }
                case BC.EXT_PUSH_INTEGER: {
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_SEND: {
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (vstate.getExtA() << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final boolean isDirected = vstate.getExtB() >= 64;
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (vstate.getExtA() << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    final Node node;
                    if (isDirected) {
                        node = DispatchDirectedSuperNaryNodeGen.create(selector);
                    } else {
                        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                        node = DispatchSuperNaryNodeGen.create(methodClass, selector);
                    }
                    setData(currentPC, insert(node));
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int jumpOffset = calculateLongExtendedOffset(getByte(bc, pc++), vstate.getExtB());
                    if (jumpOffset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(currentPC, 2, jumpOffset)));
                    }
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE: {
                    setData(currentPC, CountingConditionProfile.create());
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_LITERAL_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    pc++;
                    vstate.resetExtAB();
                    break;
                }
                /* 3 byte bytecodes */
                case BC.CALL_PRIMITIVE: {
                    pc += 2;
                    break;
                }
                case BC.EXT_PUSH_FULL_CLOSURE: {
                    pc += 2;
                    vstate.resetExtAB();
                    break;
                }
                case BC.EXT_PUSH_CLOSURE: {
                    final int byteA = getUnsignedInt(bc, pc++);
                    final int extA = vstate.getExtA();
                    final int numArgs = (byteA & 0x07) + (extA & 0x0F) * 8;
                    final int numCopied = (byteA >> 3 & 0x7) + (extA >> 4) * 8;
                    final int blockSize = getByteExtended(bc, pc++, vstate.getExtB());
                    setData(currentPC, createBlock(code, pc, numArgs, numCopied, blockSize));
                    pc += blockSize;
                    vstate.resetExtAB();
                    break;
                }
                case BC.PUSH_REMOTE_TEMP_LONG: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    pc += 2;
                    break;
                }
                case BC.STORE_REMOTE_TEMP_LONG, BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    pc += 2;
                    break;
                }
                default: {
                    throw unknownBytecode();
                }
            }
        }
    }

    @Override
    @BytecodeInterpreterSwitch
    @BytecodeInterpreterHandlerConfig(maximumOperationCode = BC.STORE_AND_POP_REMOTE_TEMP_LONG, arguments = {
                    @Argument, // this
                    @Argument(expand = MATERIALIZED, fields = { // frame
                                    @Argument.Field(name = "indexedTags"),
                                    @Argument.Field(name = "indexedPrimitiveLocals"),
                                    @Argument.Field(name = "indexedLocals")}),
                    @Argument(returnValue = true), // pc
                    @Argument(expand = VIRTUAL), // vstate
                    @Argument(expand = Argument.ExpansionKind.MATERIALIZED, fields = { // state
                                    @Argument.Field(name = "bytecode")}),
    })
    @EarlyEscapeAnalysis
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame_, final int startPC, final int startSP) {
        final FrameWithoutBoxing frame = ACCESS.uncheckedCast(frame_, FrameWithoutBoxing.class);
        final byte[] bc = ACCESS.uncheckedCast(code.getBytes(), byte[].class);
        assert isBlock == FrameAccess.hasClosure(frame);

        final LoopCounter loopCounter = CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? new LoopCounter() : null;
        int pc = startPC;
        final State state = new State(bc, loopCounter);
        final VirtualState vstate = new VirtualState(startSP);

        Object returnValue = null;
        while (pc != LOCAL_RETURN_PC) {
            CompilerAsserts.partialEvaluationConstant(pc);
            CompilerAsserts.partialEvaluationConstant(vstate.sp);
            CompilerAsserts.partialEvaluationConstant(vstate.extBA);
            try {
                switch (HostCompilerDirectives.markThreadedSwitch(currentBytecode(bc, pc))) {
                    /* 1 byte bytecodes */
                    case BC.PUSH_RCVR_VAR_0: {
                        pc = handlePushReceiverVariable0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_1: {
                        pc = handlePushReceiverVariable1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_2: {
                        pc = handlePushReceiverVariable2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_3: {
                        pc = handlePushReceiverVariable3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_4: {
                        pc = handlePushReceiverVariable4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_5: {
                        pc = handlePushReceiverVariable5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_6: {
                        pc = handlePushReceiverVariable6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_7: {
                        pc = handlePushReceiverVariable7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_8: {
                        pc = handlePushReceiverVariable8(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_9: {
                        pc = handlePushReceiverVariable9(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_A: {
                        pc = handlePushReceiverVariableA(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_B: {
                        pc = handlePushReceiverVariableB(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_C: {
                        pc = handlePushReceiverVariableC(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_D: {
                        pc = handlePushReceiverVariableD(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_E: {
                        pc = handlePushReceiverVariableE(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RCVR_VAR_F: {
                        pc = handlePushReceiverVariableF(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_0: {
                        pc = handlePushLiteralVariable0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_1: {
                        pc = handlePushLiteralVariable1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_2: {
                        pc = handlePushLiteralVariable2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_3: {
                        pc = handlePushLiteralVariable3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_4: {
                        pc = handlePushLiteralVariable4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_5: {
                        pc = handlePushLiteralVariable5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_6: {
                        pc = handlePushLiteralVariable6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_7: {
                        pc = handlePushLiteralVariable7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_8: {
                        pc = handlePushLiteralVariable8(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_9: {
                        pc = handlePushLiteralVariable9(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_A: {
                        pc = handlePushLiteralVariableA(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_B: {
                        pc = handlePushLiteralVariableB(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_C: {
                        pc = handlePushLiteralVariableC(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_D: {
                        pc = handlePushLiteralVariableD(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_E: {
                        pc = handlePushLiteralVariableE(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_F: {
                        pc = handlePushLiteralVariableF(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00: {
                        pc = handlePushLiteralConstant00(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_01: {
                        pc = handlePushLiteralConstant01(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_02: {
                        pc = handlePushLiteralConstant02(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_03: {
                        pc = handlePushLiteralConstant03(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_04: {
                        pc = handlePushLiteralConstant04(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_05: {
                        pc = handlePushLiteralConstant05(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_06: {
                        pc = handlePushLiteralConstant06(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_07: {
                        pc = handlePushLiteralConstant07(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_08: {
                        pc = handlePushLiteralConstant08(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_09: {
                        pc = handlePushLiteralConstant09(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0A: {
                        pc = handlePushLiteralConstant0A(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0B: {
                        pc = handlePushLiteralConstant0B(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0C: {
                        pc = handlePushLiteralConstant0C(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0D: {
                        pc = handlePushLiteralConstant0D(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0E: {
                        pc = handlePushLiteralConstant0E(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_0F: {
                        pc = handlePushLiteralConstant0F(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_10: {
                        pc = handlePushLiteralConstant10(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_11: {
                        pc = handlePushLiteralConstant11(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_12: {
                        pc = handlePushLiteralConstant12(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_13: {
                        pc = handlePushLiteralConstant13(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_14: {
                        pc = handlePushLiteralConstant14(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_15: {
                        pc = handlePushLiteralConstant15(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_16: {
                        pc = handlePushLiteralConstant16(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_17: {
                        pc = handlePushLiteralConstant17(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_18: {
                        pc = handlePushLiteralConstant18(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_19: {
                        pc = handlePushLiteralConstant19(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1A: {
                        pc = handlePushLiteralConstant1A(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1B: {
                        pc = handlePushLiteralConstant1B(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1C: {
                        pc = handlePushLiteralConstant1C(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1D: {
                        pc = handlePushLiteralConstant1D(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1E: {
                        pc = handlePushLiteralConstant1E(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_1F: {
                        pc = handlePushLiteralConstant1F(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0: {
                        pc = handlePushTemporaryVariable0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_1: {
                        pc = handlePushTemporaryVariable1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_2: {
                        pc = handlePushTemporaryVariable2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_3: {
                        pc = handlePushTemporaryVariable3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_4: {
                        pc = handlePushTemporaryVariable4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_5: {
                        pc = handlePushTemporaryVariable5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_6: {
                        pc = handlePushTemporaryVariable6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_7: {
                        pc = handlePushTemporaryVariable7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_8: {
                        pc = handlePushTemporaryVariable8(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_9: {
                        pc = handlePushTemporaryVariable9(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_A: {
                        pc = handlePushTemporaryVariableA(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_B: {
                        pc = handlePushTemporaryVariableB(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_RECEIVER: {
                        pc = handlePushReceiver(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_CONSTANT_TRUE: {
                        pc = handlePushConstantTrue(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_CONSTANT_FALSE: {
                        pc = handlePushConstantFalse(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_CONSTANT_NIL: {
                        pc = handlePushConstantNil(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ZERO: {
                        pc = handlePushConstantZero(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ONE: {
                        pc = handlePushConstantOne(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                        pc = handlePushPseudoVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        pc = handleDuplicateTop(frame, pc, vstate, state);
                        break;
                    }
                    case BC.RETURN_RECEIVER: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleReturn(frame, pc, pc + 1, vstate.sp, FrameAccess.getReceiver(frame));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TRUE: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleReturn(frame, pc, pc + 1, vstate.sp, BooleanObject.TRUE);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_FALSE: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleReturn(frame, pc, pc + 1, vstate.sp, BooleanObject.FALSE);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleReturn(frame, pc, pc + 1, vstate.sp, NilObject.SINGLETON);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_METHOD: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleReturn(frame, pc, pc + 1, vstate.sp - 1, top(frame, vstate.sp));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL_FROM_BLOCK: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleNormalReturn(frame, pc, NilObject.SINGLETON);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_BLOCK: {
                        state.reportLoopCountOnReturn(this);
                        returnValue = handleNormalReturn(frame, pc, top(frame, vstate.sp));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.EXT_NOP: {
                        pc = handleNoOperation(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_ADD: {
                        pc = handlePrimitiveAdd(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SUBTRACT: {
                        pc = handlePrimitiveSubtract(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        pc = handlePrimitiveLessThan(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        pc = handlePrimitiveGreaterThan(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        pc = handlePrimitiveLessOrEqual(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        pc = handlePrimitiveGreaterOrEqual(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        pc = handlePrimitiveEqual(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        pc = handlePrimitiveNotEqual(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_MULTIPLY: {
                        pc = handlePrimitiveMultiply(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_DIV: {
                        pc = handlePrimitiveDiv(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_AND: {
                        pc = handlePrimitiveBitAnd(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_OR: {
                        pc = handlePrimitiveBitOr(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SIZE: {
                        pc = handlePrimitiveSize(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        pc = handlePrimitiveIdentical(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        pc = handlePrimitiveClass(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        pc = handlePrimitiveNotIdentical(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        pc = handleSend0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG, //
                        BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        pc = handleSend1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        pc = handleSend2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_0: {
                        pc = handleShortUnconditionalJump0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_1: {
                        pc = handleShortUnconditionalJump1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_2: {
                        pc = handleShortUnconditionalJump2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_3: {
                        pc = handleShortUnconditionalJump3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_4: {
                        pc = handleShortUnconditionalJump4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_5: {
                        pc = handleShortUnconditionalJump5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_6: {
                        pc = handleShortUnconditionalJump6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_UJUMP_7: {
                        pc = handleShortUnconditionalJump7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0: {
                        pc = handleShortConditionalJumpTrue0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_1: {
                        pc = handleShortConditionalJumpTrue1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_2: {
                        pc = handleShortConditionalJumpTrue2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_3: {
                        pc = handleShortConditionalJumpTrue3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_4: {
                        pc = handleShortConditionalJumpTrue4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_5: {
                        pc = handleShortConditionalJumpTrue5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_6: {
                        pc = handleShortConditionalJumpTrue6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_7: {
                        pc = handleShortConditionalJumpTrue7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_0: {
                        pc = handleShortConditionalJumpFalse0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_1: {
                        pc = handleShortConditionalJumpFalse1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_2: {
                        pc = handleShortConditionalJumpFalse2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_3: {
                        pc = handleShortConditionalJumpFalse3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_4: {
                        pc = handleShortConditionalJumpFalse4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_5: {
                        pc = handleShortConditionalJumpFalse5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_6: {
                        pc = handleShortConditionalJumpFalse6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_7: {
                        pc = handleShortConditionalJumpFalse7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_0: {
                        pc = handlePopIntoReceiverVariable0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_1: {
                        pc = handlePopIntoReceiverVariable1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_2: {
                        pc = handlePopIntoReceiverVariable2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_3: {
                        pc = handlePopIntoReceiverVariable3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_4: {
                        pc = handlePopIntoReceiverVariable4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_5: {
                        pc = handlePopIntoReceiverVariable5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_6: {
                        pc = handlePopIntoReceiverVariable6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_7: {
                        pc = handlePopIntoReceiverVariable7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_0: {
                        pc = handlePopIntoTemporaryVariable0(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_1: {
                        pc = handlePopIntoTemporaryVariable1(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_2: {
                        pc = handlePopIntoTemporaryVariable2(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_3: {
                        pc = handlePopIntoTemporaryVariable3(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_4: {
                        pc = handlePopIntoTemporaryVariable4(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_5: {
                        pc = handlePopIntoTemporaryVariable5(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_6: {
                        pc = handlePopIntoTemporaryVariable6(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_7: {
                        pc = handlePopIntoTemporaryVariable7(frame, pc, vstate, state);
                        break;
                    }
                    case BC.POP_STACK: {
                        pc = handlePopStack(frame, pc, vstate, state);
                        break;
                    }
                    /* 2 byte bytecodes */
                    case BC.EXT_A: {
                        pc = handleExtA(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_B: {
                        pc = handleExtB(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                        pc = handleExtendedPushReceiverVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL_VARIABLE: {
                        pc = handleExtendedPushLiteralVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL: {
                        pc = handleExtendedPushLiteralConstant(frame, pc, vstate, state);
                        break;
                    }
                    case BC.LONG_PUSH_TEMPORARY_VARIABLE: {
                        pc = handleLongPushTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_NEW_ARRAY: {
                        pc = handlePushNewArray(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_INTEGER: {
                        pc = handleExtendedPushInteger(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_CHARACTER: {
                        pc = handleExtendedPushCharacter(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_SEND: {
                        pc = handleExtendedSend(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_SEND_SUPER: {
                        pc = handleExtendedSuperSend(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_UNCONDITIONAL_JUMP: {
                        pc = handleExtendedUnconditionalJump(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_JUMP_IF_TRUE: {
                        pc = handleExtendedConditionalJumpTrue(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_JUMP_IF_FALSE: {
                        pc = handleExtendedConditionalJumpFalse(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                        pc = handleExtendedStoreAndPopReceiverVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        pc = handleExtendedStoreAndPopLiteralVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        pc = handleLongStoreAndPopTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        pc = handleExtendedStoreReceiverVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        pc = handleExtendedStoreLiteralVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.LONG_STORE_TEMPORARY_VARIABLE: {
                        pc = handleLongStoreTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    /* 3 byte bytecodes */
                    case BC.CALL_PRIMITIVE: {
                        pc = handleCallPrimitive(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_FULL_CLOSURE: {
                        pc = handleExtendedPushFullClosure(frame, pc, vstate, state);
                        break;
                    }
                    case BC.EXT_PUSH_CLOSURE: {
                        pc = handleExtendedPushClosure(frame, pc, vstate, state);
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        pc = handleLongPushRemoteTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        pc = handleLongStoreRemoteTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        pc = handleLongStoreAndPopRemoteTemporaryVariable(frame, pc, vstate, state);
                        break;
                    }
                    default: {
                        throw unknownBytecode(pc, currentBytecode(bc, pc));
                    }
                }
            } catch (final OSRException e) {
                return e.osrResult;
            } catch (final StackOverflowError e) {
                CompilerDirectives.transferToInterpreter();
                throw getContext().tryToSignalLowSpace(frame, e);
            }
        }
        assert returnValue != null;
        return returnValue;
    }

    @SuppressWarnings("serial")
    private static final class OSRException extends ControlFlowException {
        private final Object osrResult;

        OSRException(final Object osrResult) {
            this.osrResult = osrResult;
        }
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterFetchOpcode
    private int currentBytecode(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return currentBytecode(state.bytecode, pc);
    }

    @EarlyInline
    private static int currentBytecode(final byte[] bytecode, final int pc) {
        return getUnsignedInt(bytecode, pc);
    }

    // =========================================================================
    // SECTION: PUSH BYTECODES
    // =========================================================================

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_0, safepoint = false)
    private int handlePushReceiverVariable0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x0);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_1, safepoint = false)
    private int handlePushReceiverVariable1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_2, safepoint = false)
    private int handlePushReceiverVariable2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_3, safepoint = false)
    private int handlePushReceiverVariable3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_4, safepoint = false)
    private int handlePushReceiverVariable4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_5, safepoint = false)
    private int handlePushReceiverVariable5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_6, safepoint = false)
    private int handlePushReceiverVariable6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_7, safepoint = false)
    private int handlePushReceiverVariable7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x7);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_8, safepoint = false)
    private int handlePushReceiverVariable8(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x8);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_9, safepoint = false)
    private int handlePushReceiverVariable9(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0x9);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_A, safepoint = false)
    private int handlePushReceiverVariableA(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xA);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_B, safepoint = false)
    private int handlePushReceiverVariableB(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xB);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_C, safepoint = false)
    private int handlePushReceiverVariableC(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xC);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_D, safepoint = false)
    private int handlePushReceiverVariableD(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xD);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_E, safepoint = false)
    private int handlePushReceiverVariableE(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xE);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RCVR_VAR_F, safepoint = false)
    private int handlePushReceiverVariableF(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushReceiverVariable(frame, pc, vstate, 0xF);
    }

    @EarlyInline
    private int handlePushReceiverVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final int index) {
        pushFollowed(frame, pc, vstate.sp++, ACCESS.uncheckedCast(getData(pc), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), index));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_0, safepoint = false)
    private int handlePushLiteralVariable0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x0);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_1, safepoint = false)
    private int handlePushLiteralVariable1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_2, safepoint = false)
    private int handlePushLiteralVariable2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_3, safepoint = false)
    private int handlePushLiteralVariable3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_4, safepoint = false)
    private int handlePushLiteralVariable4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_5, safepoint = false)
    private int handlePushLiteralVariable5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_6, safepoint = false)
    private int handlePushLiteralVariable6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_7, safepoint = false)
    private int handlePushLiteralVariable7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x7);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_8, safepoint = false)
    private int handlePushLiteralVariable8(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x8);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_9, safepoint = false)
    private int handlePushLiteralVariable9(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0x9);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_A, safepoint = false)
    private int handlePushLiteralVariableA(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xA);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_B, safepoint = false)
    private int handlePushLiteralVariableB(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xB);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_C, safepoint = false)
    private int handlePushLiteralVariableC(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xC);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_D, safepoint = false)
    private int handlePushLiteralVariableD(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xD);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_E, safepoint = false)
    private int handlePushLiteralVariableE(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xE);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_VAR_F, safepoint = false)
    private int handlePushLiteralVariableF(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralVariable(frame, pc, vstate, 0xF);
    }

    @EarlyInline
    private int handlePushLiteralVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final int index) {
        push(frame, vstate.sp++, readLiteralVariable(pc, index));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_00, safepoint = false)
    private int handlePushLiteralConstant00(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x00);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_01, safepoint = false)
    private int handlePushLiteralConstant01(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x01);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_02, safepoint = false)
    private int handlePushLiteralConstant02(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x02);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_03, safepoint = false)
    private int handlePushLiteralConstant03(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x03);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_04, safepoint = false)
    private int handlePushLiteralConstant04(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x04);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_05, safepoint = false)
    private int handlePushLiteralConstant05(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x05);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_06, safepoint = false)
    private int handlePushLiteralConstant06(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x06);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_07, safepoint = false)
    private int handlePushLiteralConstant07(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x07);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_08, safepoint = false)
    private int handlePushLiteralConstant08(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x08);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_09, safepoint = false)
    private int handlePushLiteralConstant09(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x09);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0A, safepoint = false)
    private int handlePushLiteralConstant0A(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0A);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0B, safepoint = false)
    private int handlePushLiteralConstant0B(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0B);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0C, safepoint = false)
    private int handlePushLiteralConstant0C(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0C);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0D, safepoint = false)
    private int handlePushLiteralConstant0D(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0D);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0E, safepoint = false)
    private int handlePushLiteralConstant0E(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0E);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_0F, safepoint = false)
    private int handlePushLiteralConstant0F(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x0F);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_10, safepoint = false)
    private int handlePushLiteralConstant10(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x10);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_11, safepoint = false)
    private int handlePushLiteralConstant11(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x11);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_12, safepoint = false)
    private int handlePushLiteralConstant12(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x12);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_13, safepoint = false)
    private int handlePushLiteralConstant13(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x13);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_14, safepoint = false)
    private int handlePushLiteralConstant14(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x14);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_15, safepoint = false)
    private int handlePushLiteralConstant15(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x15);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_16, safepoint = false)
    private int handlePushLiteralConstant16(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x16);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_17, safepoint = false)
    private int handlePushLiteralConstant17(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x17);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_18, safepoint = false)
    private int handlePushLiteralConstant18(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x18);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_19, safepoint = false)
    private int handlePushLiteralConstant19(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x19);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1A, safepoint = false)
    private int handlePushLiteralConstant1A(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1A);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1B, safepoint = false)
    private int handlePushLiteralConstant1B(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1B);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1C, safepoint = false)
    private int handlePushLiteralConstant1C(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1C);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1D, safepoint = false)
    private int handlePushLiteralConstant1D(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1D);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1E, safepoint = false)
    private int handlePushLiteralConstant1E(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1E);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_LIT_CONST_1F, safepoint = false)
    private int handlePushLiteralConstant1F(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushLiteralConstant(frame, pc, vstate, 0x1F);
    }

    @EarlyInline
    private int handlePushLiteralConstant(final VirtualFrame frame, final int pc, final VirtualState vstate, final int index) {
        push(frame, vstate.sp++, getAndResolveLiteral(pc, index));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_0, safepoint = false)
    private int handlePushTemporaryVariable0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x0);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_1, safepoint = false)
    private int handlePushTemporaryVariable1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_2, safepoint = false)
    private int handlePushTemporaryVariable2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_3, safepoint = false)
    private int handlePushTemporaryVariable3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_4, safepoint = false)
    private int handlePushTemporaryVariable4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_5, safepoint = false)
    private int handlePushTemporaryVariable5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_6, safepoint = false)
    private int handlePushTemporaryVariable6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_7, safepoint = false)
    private int handlePushTemporaryVariable7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x7);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_8, safepoint = false)
    private int handlePushTemporaryVariable8(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x8);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_9, safepoint = false)
    private int handlePushTemporaryVariable9(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0x9);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_A, safepoint = false)
    private int handlePushTemporaryVariableA(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0xA);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_TEMP_VAR_B, safepoint = false)
    private int handlePushTemporaryVariableB(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePushTemporaryVariable(frame, pc, vstate, 0xB);
    }

    @EarlyInline
    private int handlePushTemporaryVariable(final VirtualFrame frame, final int pc, @SuppressWarnings("unused") final VirtualState vstate, final int index) {
        pushFollowed(frame, pc, vstate.sp++, FrameAccess.getStackValue(frame, index));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_RECEIVER, safepoint = false)
    private int handlePushReceiver(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        pushFollowed(frame, pc, vstate.sp++, FrameAccess.getReceiver(frame));
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.PUSH_CONSTANT_TRUE, safepoint = false)
    private int handlePushConstantTrue(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        push(frame, vstate.sp++, BooleanObject.TRUE);
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.PUSH_CONSTANT_FALSE, safepoint = false)
    private int handlePushConstantFalse(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        push(frame, vstate.sp++, BooleanObject.FALSE);
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.PUSH_CONSTANT_NIL, safepoint = false)
    private int handlePushConstantNil(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        push(frame, vstate.sp++, NilObject.SINGLETON);
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.PUSH_CONSTANT_ZERO, safepoint = false)
    private int handlePushConstantZero(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        push(frame, vstate.sp++, BOXED_ZERO);
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.PUSH_CONSTANT_ONE, safepoint = false)
    private int handlePushConstantOne(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        push(frame, vstate.sp++, BOXED_ONE);
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_PSEUDO_VARIABLE, safepoint = false)
    private int handlePushPseudoVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        if (vstate.getExtB() == 0) {
            push(frame, vstate.sp++, getOrCreateContext(frame, pc));
        } else {
            throw unknownBytecode(pc, getByte(state.bytecode, pc));
        }
        assert vstate.getExtA() == 0;
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.DUPLICATE_TOP, safepoint = false)
    private int handleDuplicateTop(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        pushFollowed(frame, pc, vstate.sp, top(frame, vstate.sp));
        vstate.sp++;
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_RECEIVER_VARIABLE, safepoint = false)
    private int handleExtendedPushReceiverVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        pushFollowed(frame, pc, vstate.sp++, ACCESS.uncheckedCast(getData(pc), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), index));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_LITERAL_VARIABLE, safepoint = false)
    private int handleExtendedPushLiteralVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        push(frame, vstate.sp++, readLiteralVariable(pc, index));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_LITERAL, safepoint = false)
    private int handleExtendedPushLiteralConstant(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        push(frame, vstate.sp++, getAndResolveLiteral(pc, index));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.LONG_PUSH_TEMPORARY_VARIABLE, safepoint = false)
    private int handleLongPushTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        pushFollowed(frame, pc, vstate.sp++, FrameAccess.getStackValue(frame, getUnsignedInt(state.bytecode, pc + 1)));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_NEW_ARRAY, safepoint = false)
    private int handlePushNewArray(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int param = getByte(state.bytecode, pc + 1);
        final int arraySize = param & 127;
        CompilerAsserts.partialEvaluationConstant(arraySize);
        final Object[] values;
        if (param < 0) {
            values = popN(frame, vstate.sp, arraySize);
            vstate.sp -= arraySize;
        } else {
            values = ArrayUtils.withAll(arraySize, NilObject.SINGLETON);
        }
        push(frame, vstate.sp++, ArrayObject.createWithStorage(getContext().arrayClass, values));
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_INTEGER, safepoint = false)
    private int handleExtendedPushInteger(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        push(frame, vstate.sp++, (long) getByteExtended(state.bytecode, pc + 1, vstate.getExtB()));
        vstate.resetExtAB();
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_CHARACTER, safepoint = false)
    private int handleExtendedPushCharacter(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int intValue = getByteExtendedWithExtA(pc, vstate, state);
        push(frame, vstate.sp++, CharacterObject.valueOf(intValue));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_FULL_CLOSURE, safepoint = false)
    private int handleExtendedPushFullClosure(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int literalIndex = getByteExtendedWithExtA(pc, vstate, state);
        final CompiledCodeObject block = (CompiledCodeObject) code.getLiteral(literalIndex);
        assert block.assertNotForwarded();
        CompilerAsserts.partialEvaluationConstant(block);
        final byte byteB = getByte(state.bytecode, pc + 2);
        final int numCopied = Byte.toUnsignedInt(byteB) & 63;
        CompilerAsserts.partialEvaluationConstant(numCopied);
        final Object[] copiedValues = popN(frame, vstate.sp, numCopied);
        vstate.sp -= numCopied;
        final boolean ignoreContext = (byteB & 0x40) != 0;
        final boolean receiverOnStack = (byteB & 0x80) != 0;
        final ContextObject outerContext = ignoreContext ? null : getOrCreateContext(frame, pc);
        final Object receiver = receiverOnStack ? pop(frame, --vstate.sp) : FrameAccess.getReceiver(frame);
        push(frame, vstate.sp++, new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext));
        return pc + 3;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_PUSH_CLOSURE, safepoint = false)
    private int handleExtendedPushClosure(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int byteA = getUnsignedInt(state.bytecode, pc + 1);
        final int numCopied = (byteA >> 3 & 0x7) + (vstate.getExtA() >> 4) * 8;
        CompilerAsserts.partialEvaluationConstant(numCopied);
        final Object[] copiedValues = popN(frame, vstate.sp, numCopied);
        vstate.sp -= numCopied;
        push(frame, vstate.sp++, createBlockClosure(frame, ACCESS.uncheckedCast(getData(pc), CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, pc)));
        final int blockSize = getByteExtended(state.bytecode, pc + 2, vstate.getExtB());
        CompilerAsserts.partialEvaluationConstant(blockSize);
        vstate.resetExtAB();
        return pc + 3 + blockSize;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.PUSH_REMOTE_TEMP_LONG, safepoint = false)
    private int handleLongPushRemoteTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        pushFollowed(frame, pc, vstate.sp++, ACCESS.uncheckedCast(getData(pc), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex));
        return pc + 3;
    }

    // =========================================================================
    // SECTION: POP BYTECODES
    // =========================================================================

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_0, safepoint = false)
    private int handlePopIntoReceiverVariable0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 0);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_1, safepoint = false)
    private int handlePopIntoReceiverVariable1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_2, safepoint = false)
    private int handlePopIntoReceiverVariable2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_3, safepoint = false)
    private int handlePopIntoReceiverVariable3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_4, safepoint = false)
    private int handlePopIntoReceiverVariable4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_5, safepoint = false)
    private int handlePopIntoReceiverVariable5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_6, safepoint = false)
    private int handlePopIntoReceiverVariable6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_RCVR_VAR_7, safepoint = false)
    private int handlePopIntoReceiverVariable7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoReceiverVariable(frame, pc, vstate, 7);
    }

    @EarlyInline
    private int handlePopIntoReceiverVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final int index) {
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --vstate.sp));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_0, safepoint = false)
    private int handlePopIntoTemporaryVariable0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 0);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_1, safepoint = false)
    private int handlePopIntoTemporaryVariable1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_2, safepoint = false)
    private int handlePopIntoTemporaryVariable2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_3, safepoint = false)
    private int handlePopIntoTemporaryVariable3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_4, safepoint = false)
    private int handlePopIntoTemporaryVariable4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_5, safepoint = false)
    private int handlePopIntoTemporaryVariable5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_6, safepoint = false)
    private int handlePopIntoTemporaryVariable6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.POP_INTO_TEMP_VAR_7, safepoint = false)
    private int handlePopIntoTemporaryVariable7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handlePopIntoTemporaryVariable(frame, pc, vstate, 7);
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    private int handlePopIntoTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final int index) {
        FrameAccess.setStackValue(frame, index, pop(frame, --vstate.sp));
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.POP_STACK, safepoint = false)
    private int handlePopStack(@SuppressWarnings("unused") final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        --vstate.sp;
        return pc + 1;
    }

    // =========================================================================
    // SECTION: STORE BYTECODES
    // =========================================================================

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, safepoint = false)
    private int handleExtendedStoreAndPopReceiverVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, safepoint = false)
    private int handleExtendedStoreAndPopLiteralVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(pc, index), ASSOCIATION.VALUE, pop(frame, --vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE, safepoint = false)
    private int handleLongStoreAndPopTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        FrameAccess.setStackValue(frame, getUnsignedInt(state.bytecode, pc + 1), pop(frame, --vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_STORE_RECEIVER_VARIABLE, safepoint = false)
    private int handleExtendedStoreReceiverVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, top(frame, vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_STORE_LITERAL_VARIABLE, safepoint = false)
    private int handleExtendedStoreLiteralVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtendedWithExtA(pc, vstate, state);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(pc, index), ASSOCIATION.VALUE, top(frame, vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings("static-method")
    @BytecodeInterpreterHandler(value = BC.LONG_STORE_TEMPORARY_VARIABLE, safepoint = false)
    private int handleLongStoreTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        FrameAccess.setStackValue(frame, getUnsignedInt(state.bytecode, pc + 1), top(frame, vstate.sp));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.STORE_REMOTE_TEMP_LONG, safepoint = false)
    private int handleLongStoreRemoteTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, top(frame, vstate.sp));
        return pc + 3;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.STORE_AND_POP_REMOTE_TEMP_LONG, safepoint = false)
    private int handleLongStoreAndPopRemoteTemporaryVariable(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, pop(frame, --vstate.sp));
        return pc + 3;
    }

    // =========================================================================
    // SECTION: SEND BYTECODES
    // =========================================================================

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_ADD, safepoint = false)
    private int handlePrimitiveAdd(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            /* Profiled version of LargeIntegers.add(image, lhs, rhs). */
            final long r = lhs + rhs;
            if (((lhs ^ r) & (rhs ^ r)) < 0) {
                enter(pc, profile, BRANCH3);
                result = LargeIntegers.addLarge(getContext(), lhs, rhs);
            } else {
                enter(pc, profile, BRANCH4);
                result = r;
            }
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH5);
            result = PrimSmallFloatAddNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_SUBTRACT, safepoint = false)
    private int handlePrimitiveSubtract(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            /* Profiled version of LargeIntegers.subtract(image, lhs, rhs). */
            final long r = lhs - rhs;
            if (((lhs ^ rhs) & (lhs ^ r)) < 0) {
                enter(pc, profile, BRANCH3);
                result = LargeIntegers.subtractLarge(getContext(), lhs, rhs);
            } else {
                enter(pc, profile, BRANCH4);
                result = r;
            }
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH5);
            result = PrimSmallFloatSubtractNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_LESS_THAN, safepoint = false)
    private int handlePrimitiveLessThan(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimLessThanNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatLessThanNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_GREATER_THAN, safepoint = false)
    private int handlePrimitiveGreaterThan(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimGreaterThanNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatGreaterThanNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_LESS_OR_EQUAL, safepoint = false)
    private int handlePrimitiveLessOrEqual(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimLessOrEqualNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatLessOrEqualNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_GREATER_OR_EQUAL, safepoint = false)
    private int handlePrimitiveGreaterOrEqual(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimGreaterOrEqualNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatGreaterOrEqualNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_EQUAL, safepoint = false)
    private int handlePrimitiveEqual(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimEqualNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatEqualNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_NOT_EQUAL, safepoint = false)
    private int handlePrimitiveNotEqual(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimNotEqualNode.doLong(lhs, rhs);
        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
            enter(pc, profile, BRANCH3);
            result = PrimSmallFloatNotEqualNode.doDouble(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_MULTIPLY, safepoint = false)
    private int handlePrimitiveMultiply(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = multiply(pc, profile, lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    private Object multiply(final int pc, final byte profile, final long lhs, final long rhs) {
        /* Inlined version of Math.multiplyExact(x, y) with large integer fallback. */
        final long result = lhs * rhs;
        final long ax = Math.abs(lhs);
        final long ay = Math.abs(rhs);
        if ((ax | ay) >>> 31 != 0) {
            if (rhs != 0 && result / rhs != lhs || lhs == Long.MIN_VALUE && rhs == -1) {
                enter(pc, profile, BRANCH3);
                return LargeIntegers.multiplyLarge(getContext(), lhs, rhs);
            }
        }
        return result;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_DIV, safepoint = false)
    private int handlePrimitiveDiv(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs && rhs != 0 && !isOverflowDivision(lhs, rhs)) {
            enter(pc, profile, BRANCH2);
            final long q = lhs / rhs;
            if ((lhs ^ rhs) < 0 && (q * rhs != lhs)) {
                enter(pc, profile, BRANCH3);
                result = q - 1;
            } else {
                enter(pc, profile, BRANCH4);
                result = q;
            }
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_BIT_AND, safepoint = false)
    private int handlePrimitiveBitAnd(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimBitAndNode.doLong(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_BIT_OR, safepoint = false)
    private int handlePrimitiveBitOr(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimBitOrNode.doLong(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_SIZE, safepoint = false)
    private int handlePrimitiveSize(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object receiver = pop(frame, --vstate.sp);
        final byte profile = getProfile(pc);
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final NativeObject nativeObject && getContext().isByteString(nativeObject)) {
            enter(pc, profile, BRANCH2);
            result = (long) nativeObject.getByteLength();
        } else { // TODO: OSVM also special cases arrays
            enter(pc, profile, BRANCH1);
            FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
            result = send(frame, pc, receiver);
            nextPC = FrameAccess.internalizePC(frame, nextPC);
        }
        push(frame, vstate.sp++, result);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_IDENTICAL, safepoint = false)
    private int handlePrimitiveIdentical(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        push(frame, vstate.sp++, ACCESS.uncheckedCast(getData(pc), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_CLASS, safepoint = false)
    private int handlePrimitiveClass(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object receiver = pop(frame, --vstate.sp);
        push(frame, vstate.sp++, ACCESS.uncheckedCast(getData(pc), SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.BYTECODE_PRIM_NOT_IDENTICAL, safepoint = false)
    private int handlePrimitiveNotIdentical(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        push(frame, vstate.sp++, !ACCESS.uncheckedCast(getData(pc), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
        return pc + 1;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE,
                    BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y,
                    BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3,
                    BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7,
                    BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B,
                    BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F}, safepoint = false)
    private int handleSend0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        int nextPC = pc + 1;
        final Object receiver = pop(frame, --vstate.sp);
        FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
        push(frame, vstate.sp++, send(frame, pc, receiver));
        nextPC = FrameAccess.internalizePC(frame, nextPC);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT,
                    BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT,
                    BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG,
                    BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3,
                    BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7,
                    BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B,
                    BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F}, safepoint = false)
    private int handleSend1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        int nextPC = pc + 1;
        final Object arg = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
        push(frame, vstate.sp++, send(frame, pc, receiver, arg));
        nextPC = FrameAccess.internalizePC(frame, nextPC);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_AT_PUT,
                    BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3,
                    BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7,
                    BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B,
                    BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F}, safepoint = false)
    private int handleSend2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        int nextPC = pc + 1;
        final Object arg2 = pop(frame, --vstate.sp);
        final Object arg1 = pop(frame, --vstate.sp);
        final Object receiver = pop(frame, --vstate.sp);
        FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
        push(frame, vstate.sp++, send(frame, pc, receiver, arg1, arg2));
        nextPC = FrameAccess.internalizePC(frame, nextPC);
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_SEND, safepoint = false)
    private int handleExtendedSend(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        int nextPC = pc + 2;
        final int byte1 = getUnsignedInt(state.bytecode, pc + 1);
        final int numArgs = (byte1 & 7) + (vstate.getExtB() << 3);
        CompilerAsserts.partialEvaluationConstant(numArgs);
        final Object[] arguments = popN(frame, vstate.sp, numArgs);
        vstate.sp -= numArgs;
        final Object receiver = pop(frame, --vstate.sp);
        FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
        push(frame, vstate.sp++, sendNary(frame, pc, receiver, arguments));
        nextPC = FrameAccess.internalizePC(frame, nextPC);
        vstate.resetExtAB();
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_SEND_SUPER, safepoint = false)
    private int handleExtendedSuperSend(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        int nextPC = pc + 2;
        final boolean isDirected;
        final int extB = vstate.getExtB();
        final int extBValue;
        if (extB >= 64) {
            isDirected = true;
            extBValue = extB & 63;
        } else {
            isDirected = false;
            extBValue = extB;
        }
        final int byte1 = getUnsignedInt(state.bytecode, pc + 1);
        final int numArgs = (byte1 & 7) + (extBValue << 3);
        CompilerAsserts.partialEvaluationConstant(numArgs);
        final ClassObject lookupClass = isDirected ? ((ClassObject) pop(frame, --vstate.sp)).getResolvedSuperclass() : null;
        final Object[] arguments = popN(frame, vstate.sp, numArgs);
        vstate.sp -= numArgs;
        final Object receiver = pop(frame, --vstate.sp);
        FrameAccess.externalizePCAndSP(frame, nextPC, vstate.sp);
        CompilerAsserts.partialEvaluationConstant(isDirected);
        pushFollowed(frame, pc, vstate.sp++, sendSuper(frame, isDirected, pc, lookupClass, receiver, arguments));
        nextPC = FrameAccess.internalizePC(frame, nextPC);
        vstate.resetExtAB();
        return nextPC;
    }

    // =========================================================================
    // SECTION: JUMP BYTECODES
    // =========================================================================

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_0, safepoint = false)
    private int handleShortUnconditionalJump0(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_1, safepoint = false)
    private int handleShortUnconditionalJump1(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 3;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_2, safepoint = false)
    private int handleShortUnconditionalJump2(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 4;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_3, safepoint = false)
    private int handleShortUnconditionalJump3(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 5;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_4, safepoint = false)
    private int handleShortUnconditionalJump4(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 6;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_5, safepoint = false)
    private int handleShortUnconditionalJump5(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 7;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_6, safepoint = false)
    private int handleShortUnconditionalJump6(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 8;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.SHORT_UJUMP_7, safepoint = false)
    private int handleShortUnconditionalJump7(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        return pc + 9;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_0, safepoint = false)
    private int handleShortConditionalJumpTrue0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_1, safepoint = false)
    private int handleShortConditionalJumpTrue1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_2, safepoint = false)
    private int handleShortConditionalJumpTrue2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_3, safepoint = false)
    private int handleShortConditionalJumpTrue3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_4, safepoint = false)
    private int handleShortConditionalJumpTrue4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_5, safepoint = false)
    private int handleShortConditionalJumpTrue5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_6, safepoint = false)
    private int handleShortConditionalJumpTrue6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 7);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_TRUE_7, safepoint = false)
    private int handleShortConditionalJumpTrue7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpTrue(frame, pc, vstate, 8);
    }

    @EarlyInline
    private int handleShortConditionalJumpTrue(final VirtualFrame frame, final int pc, final VirtualState vstate, final int jumpOffset) {
        final Object stackValue = pop(frame, --vstate.sp);
        final int nextPC = pc + 1;
        if (stackValue instanceof final Boolean condition) {
            if (ACCESS.uncheckedCast(getData(pc), CountingConditionProfile.class).profile(condition)) {
                return nextPC + jumpOffset;
            } else {
                return nextPC;
            }
        } else {
            throw sendMustBeBooleanInInterpreter(frame, nextPC, vstate.sp, stackValue);
        }
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_0, safepoint = false)
    private int handleShortConditionalJumpFalse0(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 1);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_1, safepoint = false)
    private int handleShortConditionalJumpFalse1(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 2);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_2, safepoint = false)
    private int handleShortConditionalJumpFalse2(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 3);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_3, safepoint = false)
    private int handleShortConditionalJumpFalse3(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 4);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_4, safepoint = false)
    private int handleShortConditionalJumpFalse4(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 5);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_5, safepoint = false)
    private int handleShortConditionalJumpFalse5(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 6);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_6, safepoint = false)
    private int handleShortConditionalJumpFalse6(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 7);
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.SHORT_CJUMP_FALSE_7, safepoint = false)
    private int handleShortConditionalJumpFalse7(final VirtualFrame frame, final int pc, final VirtualState vstate, @SuppressWarnings("unused") final State state) {
        return handleShortConditionalJumpFalse(frame, pc, vstate, 8);
    }

    @EarlyInline
    private int handleShortConditionalJumpFalse(final VirtualFrame frame, final int pc, final VirtualState vstate, final int jumpOffset) {
        final Object stackValue = pop(frame, --vstate.sp);
        final int nextPC = pc + 1;
        if (stackValue instanceof final Boolean condition) {
            if (ACCESS.uncheckedCast(getData(pc), CountingConditionProfile.class).profile(!condition)) {
                return nextPC + jumpOffset;
            } else {
                return nextPC;
            }
        } else {
            throw sendMustBeBooleanInInterpreter(frame, nextPC, vstate.sp, stackValue);
        }
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_UNCONDITIONAL_JUMP, safepoint = true)
    private int handleExtendedUnconditionalJump(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final int jumpOffset = calculateLongExtendedOffset(getByte(state.bytecode, pc + 1), vstate.getExtB());
        final int nextPC = pc + 2 + jumpOffset;
        if (jumpOffset < 0) {
            if (CompilerDirectives.hasNextTier()) {
                final int counter = CompilerDirectives.inCompiledCode() ? ++state.loopCounter.value : ++state.interpreterLoopCounter;
                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, counter >= LoopCounter.CHECK_LOOP_STRIDE)) {
                    LoopNode.reportLoopCount(this, counter);
                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, counter)) {
                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((vstate.sp & 0xFF) << 16) | nextPC, null, null, frame);
                        if (osrReturnValue != null) {
                            assert !FrameAccess.hasModifiedSender(frame);
                            FrameAccess.terminateFrame(frame);
                            throw new OSRException(osrReturnValue);
                        }
                    }
                    if (CompilerDirectives.inCompiledCode()) {
                        state.loopCounter.value = 0;
                    } else {
                        state.interpreterLoopCounter = 0;
                    }
                }
                if (CompilerDirectives.inCompiledCode()) {
                    state.interpreterLoopCounter = 0;
                }
            }
            if (getData(pc) instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                checkForInterruptsNode.execute(frame, nextPC, vstate.sp);
            }
        }
        vstate.resetExtAB();
        return nextPC;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_JUMP_IF_TRUE, safepoint = false)
    private int handleExtendedConditionalJumpTrue(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final Object stackValue = pop(frame, --vstate.sp);
        final int jumpOffset = getByteExtended(state.bytecode, pc + 1, vstate.getExtB());
        final int nextPC = pc + 2;
        if (stackValue instanceof final Boolean condition) {
            vstate.resetExtAB();
            if (ACCESS.uncheckedCast(getData(pc), CountingConditionProfile.class).profile(condition)) {
                return nextPC + jumpOffset;
            } else {
                return nextPC;
            }
        } else {
            throw sendMustBeBooleanInInterpreter(frame, nextPC, vstate.sp, stackValue);
        }
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.EXT_JUMP_IF_FALSE, safepoint = false)
    private int handleExtendedConditionalJumpFalse(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        final Object stackValue = pop(frame, --vstate.sp);
        final int jumpOffset = getByteExtended(state.bytecode, pc + 1, vstate.getExtB());
        final int nextPC = pc + 2;
        if (stackValue instanceof final Boolean condition) {
            vstate.resetExtAB();
            if (ACCESS.uncheckedCast(getData(pc), CountingConditionProfile.class).profile(!condition)) {
                return nextPC + jumpOffset;
            } else {
                return nextPC;
            }
        } else {
            throw sendMustBeBooleanInInterpreter(frame, nextPC, vstate.sp, stackValue);
        }
    }

    // =========================================================================
    // SECTION: MISCELLANEOUS BYTECODES
    // =========================================================================

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.EXT_NOP, safepoint = false)
    private int handleNoOperation(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        vstate.resetExtAB();
        return pc + 1;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.EXT_A, safepoint = false)
    private int handleExtA(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        vstate.updateExtA(getUnsignedInt(state.bytecode, pc + 1));
        return pc + 2;
    }

    @EarlyInline
    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(value = BC.EXT_B, safepoint = false)
    private int handleExtB(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        vstate.updateExtB(getUnsignedInt(state.bytecode, pc + 1));
        return pc + 2;
    }

    @EarlyInline
    @BytecodeInterpreterHandler(value = BC.CALL_PRIMITIVE, safepoint = false)
    private int handleCallPrimitive(final VirtualFrame frame, final int pc, final VirtualState vstate, final State state) {
        if (getUnsignedInt(state.bytecode, pc + 3) == BC.LONG_STORE_TEMPORARY_VARIABLE) {
            assert vstate.sp > 0;
            FrameAccess.setStackValue(frame, vstate.sp - 1, getErrorObject());
        }
        return pc + 3;
    }

    // =========================================================================

    @EarlyInline
    private static int getByteExtendedWithExtA(final int pc, final VirtualState vstate, final State state) {
        final int index = getByteExtended(state.bytecode, pc + 1, vstate.getExtA());
        CompilerAsserts.partialEvaluationConstant(index);
        vstate.resetExtAB();
        return index;
    }

    @EarlyInline
    private static int getByteExtended(final byte[] bc, final int pc, final int extend) {
        return getUnsignedInt(bc, pc) + (extend << 8);
    }

    private Object sendSuper(final VirtualFrame frame, final boolean isDirected, final int currentPC, final ClassObject lookupClass, final Object receiver, final Object[] arguments) {
        try {
            if (isDirected) {
                return ACCESS.uncheckedCast(getData(currentPC), DispatchDirectedSuperNaryNodeGen.class).execute(frame, lookupClass, receiver, arguments);
            } else {
                return ACCESS.uncheckedCast(getData(currentPC), DispatchSuperNaryNodeGen.class).execute(frame, receiver, arguments);
            }
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    public static int calculateLongExtendedOffset(final byte bytecode, final int extB) {
        return Byte.toUnsignedInt(bytecode) + (extB << 8);
    }

    static class BC {
        /* 1 byte bytecodes */
        static final int PUSH_RCVR_VAR_0 = 0;
        static final int PUSH_RCVR_VAR_1 = 1;
        static final int PUSH_RCVR_VAR_2 = 2;
        static final int PUSH_RCVR_VAR_3 = 3;
        static final int PUSH_RCVR_VAR_4 = 4;
        static final int PUSH_RCVR_VAR_5 = 5;
        static final int PUSH_RCVR_VAR_6 = 6;
        static final int PUSH_RCVR_VAR_7 = 7;
        static final int PUSH_RCVR_VAR_8 = 8;
        static final int PUSH_RCVR_VAR_9 = 9;
        static final int PUSH_RCVR_VAR_A = 10;
        static final int PUSH_RCVR_VAR_B = 11;
        static final int PUSH_RCVR_VAR_C = 12;
        static final int PUSH_RCVR_VAR_D = 13;
        static final int PUSH_RCVR_VAR_E = 14;
        static final int PUSH_RCVR_VAR_F = 15;
        static final int PUSH_LIT_VAR_0 = 16;
        static final int PUSH_LIT_VAR_1 = 17;
        static final int PUSH_LIT_VAR_2 = 18;
        static final int PUSH_LIT_VAR_3 = 19;
        static final int PUSH_LIT_VAR_4 = 20;
        static final int PUSH_LIT_VAR_5 = 21;
        static final int PUSH_LIT_VAR_6 = 22;
        static final int PUSH_LIT_VAR_7 = 23;
        static final int PUSH_LIT_VAR_8 = 24;
        static final int PUSH_LIT_VAR_9 = 25;
        static final int PUSH_LIT_VAR_A = 26;
        static final int PUSH_LIT_VAR_B = 27;
        static final int PUSH_LIT_VAR_C = 28;
        static final int PUSH_LIT_VAR_D = 29;
        static final int PUSH_LIT_VAR_E = 30;
        static final int PUSH_LIT_VAR_F = 31;
        static final int PUSH_LIT_CONST_00 = 32;
        static final int PUSH_LIT_CONST_01 = 33;
        static final int PUSH_LIT_CONST_02 = 34;
        static final int PUSH_LIT_CONST_03 = 35;
        static final int PUSH_LIT_CONST_04 = 36;
        static final int PUSH_LIT_CONST_05 = 37;
        static final int PUSH_LIT_CONST_06 = 38;
        static final int PUSH_LIT_CONST_07 = 39;
        static final int PUSH_LIT_CONST_08 = 40;
        static final int PUSH_LIT_CONST_09 = 41;
        static final int PUSH_LIT_CONST_0A = 42;
        static final int PUSH_LIT_CONST_0B = 43;
        static final int PUSH_LIT_CONST_0C = 44;
        static final int PUSH_LIT_CONST_0D = 45;
        static final int PUSH_LIT_CONST_0E = 46;
        static final int PUSH_LIT_CONST_0F = 47;
        static final int PUSH_LIT_CONST_10 = 48;
        static final int PUSH_LIT_CONST_11 = 49;
        static final int PUSH_LIT_CONST_12 = 50;
        static final int PUSH_LIT_CONST_13 = 51;
        static final int PUSH_LIT_CONST_14 = 52;
        static final int PUSH_LIT_CONST_15 = 53;
        static final int PUSH_LIT_CONST_16 = 54;
        static final int PUSH_LIT_CONST_17 = 55;
        static final int PUSH_LIT_CONST_18 = 56;
        static final int PUSH_LIT_CONST_19 = 57;
        static final int PUSH_LIT_CONST_1A = 58;
        static final int PUSH_LIT_CONST_1B = 59;
        static final int PUSH_LIT_CONST_1C = 60;
        static final int PUSH_LIT_CONST_1D = 61;
        static final int PUSH_LIT_CONST_1E = 62;
        static final int PUSH_LIT_CONST_1F = 63;
        static final int PUSH_TEMP_VAR_0 = 64;
        static final int PUSH_TEMP_VAR_1 = 65;
        static final int PUSH_TEMP_VAR_2 = 66;
        static final int PUSH_TEMP_VAR_3 = 67;
        static final int PUSH_TEMP_VAR_4 = 68;
        static final int PUSH_TEMP_VAR_5 = 69;
        static final int PUSH_TEMP_VAR_6 = 70;
        static final int PUSH_TEMP_VAR_7 = 71;
        static final int PUSH_TEMP_VAR_8 = 72;
        static final int PUSH_TEMP_VAR_9 = 73;
        static final int PUSH_TEMP_VAR_A = 74;
        static final int PUSH_TEMP_VAR_B = 75;
        static final int PUSH_RECEIVER = 76;
        static final int PUSH_CONSTANT_TRUE = 77;
        static final int PUSH_CONSTANT_FALSE = 78;
        static final int PUSH_CONSTANT_NIL = 79;
        static final int PUSH_CONSTANT_ZERO = 80;
        static final int PUSH_CONSTANT_ONE = 81;
        static final int EXT_PUSH_PSEUDO_VARIABLE = 82;
        static final int DUPLICATE_TOP = 83;
        static final int RETURN_RECEIVER = 88;
        static final int RETURN_TRUE = 89;
        static final int RETURN_FALSE = 90;
        static final int RETURN_NIL = 91;
        static final int RETURN_TOP_FROM_METHOD = 92;
        static final int RETURN_NIL_FROM_BLOCK = 93;
        static final int RETURN_TOP_FROM_BLOCK = 94;
        static final int EXT_NOP = 95;
        static final int BYTECODE_PRIM_ADD = 96;
        static final int BYTECODE_PRIM_SUBTRACT = 97;
        static final int BYTECODE_PRIM_LESS_THAN = 98;
        static final int BYTECODE_PRIM_GREATER_THAN = 99;
        static final int BYTECODE_PRIM_LESS_OR_EQUAL = 100;
        static final int BYTECODE_PRIM_GREATER_OR_EQUAL = 101;
        static final int BYTECODE_PRIM_EQUAL = 102;
        static final int BYTECODE_PRIM_NOT_EQUAL = 103;
        static final int BYTECODE_PRIM_MULTIPLY = 104;
        static final int BYTECODE_PRIM_DIVIDE = 105;
        static final int BYTECODE_PRIM_MOD = 106;
        static final int BYTECODE_PRIM_MAKE_POINT = 107;
        static final int BYTECODE_PRIM_BIT_SHIFT = 108;
        static final int BYTECODE_PRIM_DIV = 109;
        static final int BYTECODE_PRIM_BIT_AND = 110;
        static final int BYTECODE_PRIM_BIT_OR = 111;
        static final int BYTECODE_PRIM_AT = 112;
        static final int BYTECODE_PRIM_AT_PUT = 113;
        static final int BYTECODE_PRIM_SIZE = 114;
        static final int BYTECODE_PRIM_NEXT = 115;
        static final int BYTECODE_PRIM_NEXT_PUT = 116;
        static final int BYTECODE_PRIM_AT_END = 117;
        static final int BYTECODE_PRIM_IDENTICAL = 118;
        static final int BYTECODE_PRIM_CLASS = 119;
        static final int BYTECODE_PRIM_NOT_IDENTICAL = 120;
        static final int BYTECODE_PRIM_VALUE = 121;
        static final int BYTECODE_PRIM_VALUE_WITH_ARG = 122;
        static final int BYTECODE_PRIM_DO = 123;
        static final int BYTECODE_PRIM_NEW = 124;
        static final int BYTECODE_PRIM_NEW_WITH_ARG = 125;
        static final int BYTECODE_PRIM_POINT_X = 126;
        static final int BYTECODE_PRIM_POINT_Y = 127;
        static final int SEND_LIT_SEL0_0 = 128;
        static final int SEND_LIT_SEL0_1 = 129;
        static final int SEND_LIT_SEL0_2 = 130;
        static final int SEND_LIT_SEL0_3 = 131;
        static final int SEND_LIT_SEL0_4 = 132;
        static final int SEND_LIT_SEL0_5 = 133;
        static final int SEND_LIT_SEL0_6 = 134;
        static final int SEND_LIT_SEL0_7 = 135;
        static final int SEND_LIT_SEL0_8 = 136;
        static final int SEND_LIT_SEL0_9 = 137;
        static final int SEND_LIT_SEL0_A = 138;
        static final int SEND_LIT_SEL0_B = 139;
        static final int SEND_LIT_SEL0_C = 140;
        static final int SEND_LIT_SEL0_D = 141;
        static final int SEND_LIT_SEL0_E = 142;
        static final int SEND_LIT_SEL0_F = 143;
        static final int SEND_LIT_SEL1_0 = 144;
        static final int SEND_LIT_SEL1_1 = 145;
        static final int SEND_LIT_SEL1_2 = 146;
        static final int SEND_LIT_SEL1_3 = 147;
        static final int SEND_LIT_SEL1_4 = 148;
        static final int SEND_LIT_SEL1_5 = 149;
        static final int SEND_LIT_SEL1_6 = 150;
        static final int SEND_LIT_SEL1_7 = 151;
        static final int SEND_LIT_SEL1_8 = 152;
        static final int SEND_LIT_SEL1_9 = 153;
        static final int SEND_LIT_SEL1_A = 154;
        static final int SEND_LIT_SEL1_B = 155;
        static final int SEND_LIT_SEL1_C = 156;
        static final int SEND_LIT_SEL1_D = 157;
        static final int SEND_LIT_SEL1_E = 158;
        static final int SEND_LIT_SEL1_F = 159;
        static final int SEND_LIT_SEL2_0 = 160;
        static final int SEND_LIT_SEL2_1 = 161;
        static final int SEND_LIT_SEL2_2 = 162;
        static final int SEND_LIT_SEL2_3 = 163;
        static final int SEND_LIT_SEL2_4 = 164;
        static final int SEND_LIT_SEL2_5 = 165;
        static final int SEND_LIT_SEL2_6 = 166;
        static final int SEND_LIT_SEL2_7 = 167;
        static final int SEND_LIT_SEL2_8 = 168;
        static final int SEND_LIT_SEL2_9 = 169;
        static final int SEND_LIT_SEL2_A = 170;
        static final int SEND_LIT_SEL2_B = 171;
        static final int SEND_LIT_SEL2_C = 172;
        static final int SEND_LIT_SEL2_D = 173;
        static final int SEND_LIT_SEL2_E = 174;
        static final int SEND_LIT_SEL2_F = 175;
        static final int SHORT_UJUMP_0 = 176;
        static final int SHORT_UJUMP_1 = 177;
        static final int SHORT_UJUMP_2 = 178;
        static final int SHORT_UJUMP_3 = 179;
        static final int SHORT_UJUMP_4 = 180;
        static final int SHORT_UJUMP_5 = 181;
        static final int SHORT_UJUMP_6 = 182;
        static final int SHORT_UJUMP_7 = 183;
        static final int SHORT_CJUMP_TRUE_0 = 184;
        static final int SHORT_CJUMP_TRUE_1 = 185;
        static final int SHORT_CJUMP_TRUE_2 = 186;
        static final int SHORT_CJUMP_TRUE_3 = 187;
        static final int SHORT_CJUMP_TRUE_4 = 188;
        static final int SHORT_CJUMP_TRUE_5 = 189;
        static final int SHORT_CJUMP_TRUE_6 = 190;
        static final int SHORT_CJUMP_TRUE_7 = 191;
        static final int SHORT_CJUMP_FALSE_0 = 192;
        static final int SHORT_CJUMP_FALSE_1 = 193;
        static final int SHORT_CJUMP_FALSE_2 = 194;
        static final int SHORT_CJUMP_FALSE_3 = 195;
        static final int SHORT_CJUMP_FALSE_4 = 196;
        static final int SHORT_CJUMP_FALSE_5 = 197;
        static final int SHORT_CJUMP_FALSE_6 = 198;
        static final int SHORT_CJUMP_FALSE_7 = 199;
        static final int POP_INTO_RCVR_VAR_0 = 200;
        static final int POP_INTO_RCVR_VAR_1 = 201;
        static final int POP_INTO_RCVR_VAR_2 = 202;
        static final int POP_INTO_RCVR_VAR_3 = 203;
        static final int POP_INTO_RCVR_VAR_4 = 204;
        static final int POP_INTO_RCVR_VAR_5 = 205;
        static final int POP_INTO_RCVR_VAR_6 = 206;
        static final int POP_INTO_RCVR_VAR_7 = 207;
        static final int POP_INTO_TEMP_VAR_0 = 208;
        static final int POP_INTO_TEMP_VAR_1 = 209;
        static final int POP_INTO_TEMP_VAR_2 = 210;
        static final int POP_INTO_TEMP_VAR_3 = 211;
        static final int POP_INTO_TEMP_VAR_4 = 212;
        static final int POP_INTO_TEMP_VAR_5 = 213;
        static final int POP_INTO_TEMP_VAR_6 = 214;
        static final int POP_INTO_TEMP_VAR_7 = 215;
        static final int POP_STACK = 216;

        /* 2 byte bytecodes */
        static final int EXT_A = 224;
        static final int EXT_B = 225;
        static final int EXT_PUSH_RECEIVER_VARIABLE = 226;
        static final int EXT_PUSH_LITERAL_VARIABLE = 227;
        static final int EXT_PUSH_LITERAL = 228;
        static final int LONG_PUSH_TEMPORARY_VARIABLE = 229;
        static final int PUSH_NEW_ARRAY = 231;
        static final int EXT_PUSH_INTEGER = 232;
        static final int EXT_PUSH_CHARACTER = 233;
        static final int EXT_SEND = 234;
        static final int EXT_SEND_SUPER = 235;
        static final int EXT_UNCONDITIONAL_JUMP = 237;
        static final int EXT_JUMP_IF_TRUE = 238;
        static final int EXT_JUMP_IF_FALSE = 239;
        static final int EXT_STORE_AND_POP_RECEIVER_VARIABLE = 240;
        static final int EXT_STORE_AND_POP_LITERAL_VARIABLE = 241;
        static final int LONG_STORE_AND_POP_TEMPORARY_VARIABLE = 242;
        static final int EXT_STORE_RECEIVER_VARIABLE = 243;
        static final int EXT_STORE_LITERAL_VARIABLE = 244;
        static final int LONG_STORE_TEMPORARY_VARIABLE = 245;

        /* 3 byte bytecodes */
        static final int CALL_PRIMITIVE = 248;
        static final int EXT_PUSH_FULL_CLOSURE = 249;
        static final int EXT_PUSH_CLOSURE = 250;
        static final int PUSH_REMOTE_TEMP_LONG = 251;
        static final int STORE_REMOTE_TEMP_LONG = 252;
        static final int STORE_AND_POP_REMOTE_TEMP_LONG = 253;
    }

    /*
     * Copying
     */

    @Override
    public Node copy() {
        return new InterpreterSistaV1Node(this);
    }

    @Override
    public Node deepCopy() {
        return new InterpreterSistaV1Node(this);
    }
}

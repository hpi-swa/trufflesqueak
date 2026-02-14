/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import static com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig.Argument.ExpansionKind.MATERIALIZED;
import static com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterHandlerConfig.Argument.ExpansionKind.VIRTUAL;
import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

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
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
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

    @ValueType
    private static final class VirtualState {
        int sp;
        long extBA;

        @EarlyInline
        VirtualState(final int sp) {
            this.sp = sp;
            this.extBA = 0;
        }

        @EarlyInline
        int getExtB() {
            return (int) (extBA >> 32);
        }

        @EarlyInline
        int getExtA() {
            return (int) extBA;
        }

        @EarlyInline
        void resetExtensions() {
            this.extBA = 0;
        }
    }

    private static final class State {
        @EarlyInline
        State(final byte[] bytecode, final LoopCounter loopCounter, final int interpreterLoopCounter) {
            this.bytecode = bytecode;
            this.loopCounter = loopCounter;
            this.interpreterLoopCounter = interpreterLoopCounter;
        }

        final byte[] bytecode;
        final LoopCounter loopCounter;
        int interpreterLoopCounter;

        @EarlyInline
        private void reportLoopCountOnReturn(final Node source) {
            if (CompilerDirectives.hasNextTier()) {
                int count = CompilerDirectives.inInterpreter() ? interpreterLoopCounter : loopCounter.value;
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
        long extBA = 0;

        while (pc < endPC) {
            final int currentPC = pc++;
            final int b = getUnsignedInt(bc, currentPC);
            switch (b) {
                /* 1 byte bytecodes */
                case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    break;
                }
                case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                    BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                    data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(b & 0xF));
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
                    if (getExtB(extBA) == 0) {
                        break;
                    } else {
                        throw unknownBytecode(pc, b);
                    }
                }
                case BC.EXT_NOP:
                    extBA = 0;
                    break;
                case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y: {
                    data[currentPC] = insert(Dispatch0NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD)));
                    break;
                }
                case BC.BYTECODE_PRIM_CLASS: {
                    data[currentPC] = insert(SqueakObjectClassNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, //
                    BC.BYTECODE_PRIM_EQUAL, BC.BYTECODE_PRIM_NOT_EQUAL, BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_DIV, //
                    BC.BYTECODE_PRIM_BIT_AND, BC.BYTECODE_PRIM_BIT_OR, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD)));
                    break;
                }
                case BC.BYTECODE_PRIM_IDENTICAL, BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                    data[currentPC] = insert(SqueakObjectIdentityNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_AT_PUT: {
                    data[currentPC] = insert(Dispatch2NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD)));
                    break;
                }
                case BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                    BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(Dispatch0NodeGen.create(selector));
                    break;
                }
                case BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                    BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(Dispatch1NodeGen.create(selector));
                    break;
                }
                case BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                    BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(Dispatch2NodeGen.create(selector));
                    break;
                }
                case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                    final int offset = calculateShortOffset(b);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsInLoopNode.createForLoop(data, pc, 1, offset));
                    }
                    break;
                }
                case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7, //
                    BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                    data[currentPC] = CountingConditionProfile.create();
                    break;
                }
                case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    break;
                }
                /* 2 byte bytecodes */
                case BC.EXT_A: {
                    final int extA = getExtA(extBA);
                    final int newExtA = (extA << 8) | getUnsignedInt(bc, pc++);
                    extBA = (extBA & 0xFFFFFFFF00000000L) | Integer.toUnsignedLong(newExtA);
                    break;
                }
                case BC.EXT_B: {
                    /// Search for: BC_EXT_B_LOGIC for more details
                    final int extB = getExtB(extBA);
                    final int newExtB;
                    if (extB == 0) {
                        /* leading byte is signed */
                        final int byteValue = getByte(bc, pc++);
                        /* make sure newExtB is non-zero for next byte processing */
                        newExtB = byteValue == 0 ? 0x80000000 : byteValue;
                    } else {
                        /* subsequent bytes are unsigned */
                        final int byteValue = getUnsignedInt(bc, pc++);
                        newExtB = (extB << 8) | byteValue;
                    }
                    extBA = ((long) newExtB << 32) | (extBA & 0xFFFFFFFFL);
                    break;
                }
                case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    pc++;
                    extBA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(getByteExtended(bc, pc, getExtA(extBA))));
                    pc++;
                    extBA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_INTEGER, BC.EXT_PUSH_CHARACTER: {
                    pc++;
                    extBA = 0;
                    break;
                }
                case BC.LONG_PUSH_TEMPORARY_VARIABLE, BC.PUSH_NEW_ARRAY, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE, BC.LONG_STORE_TEMPORARY_VARIABLE: {
                    pc++;
                    break;
                }
                case BC.EXT_SEND: {
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (getExtA(extBA) << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    data[currentPC] = insert(DispatchNaryNodeGen.create(selector));
                    extBA = 0;
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final boolean isDirected = getExtB(extBA) >= 64;
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (getExtA(extBA) << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    if (isDirected) {
                        data[currentPC] = insert(DispatchDirectedSuperNaryNodeGen.create(selector));
                    } else {
                        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                        data[currentPC] = insert(DispatchSuperNaryNodeGen.create(methodClass, selector));
                    }
                    extBA = 0;
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int offset = calculateLongExtendedOffset(getByte(bc, pc++), getExtB(extBA));
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsInLoopNode.createForLoop(data, currentPC, 2, offset));
                    }
                    extBA = 0;
                    break;
                }
                case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE: {
                    data[currentPC] = CountingConditionProfile.create();
                    pc++;
                    extBA = 0;
                    break;
                }
                case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_LITERAL_VARIABLE: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    pc++;
                    extBA = 0;
                    break;
                }
                /* 3 byte bytecodes */
                case BC.CALL_PRIMITIVE: {
                    pc += 2;
                    break;
                }
                case BC.EXT_PUSH_FULL_CLOSURE: {
                    pc += 2;
                    extBA = 0;
                    break;
                }
                case BC.EXT_PUSH_CLOSURE: {
                    final int byteA = getUnsignedInt(bc, pc++);
                    final int extA = getExtA(extBA);
                    final int numArgs = (byteA & 0x07) + (extA & 0x0F) * 8;
                    final int numCopied = (byteA >> 3 & 0x7) + (extA >> 4) * 8;
                    final int blockSize = getByteExtended(bc, pc++, getExtB(extBA));
                    data[currentPC] = createBlock(code, pc, numArgs, numCopied, blockSize);
                    pc += blockSize;
                    extBA = 0;
                    break;
                }
                case BC.PUSH_REMOTE_TEMP_LONG: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    pc += 2;
                    break;
                }
                case BC.STORE_REMOTE_TEMP_LONG, BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    pc += 2;
                    break;
                }
                default: {
                    throw unknownBytecode(pc, b);
                }
            }
        }
    }

    @Override
    @BytecodeInterpreterSwitch
    @BytecodeInterpreterHandlerConfig(maximumOperationCode = BC.STORE_AND_POP_REMOTE_TEMP_LONG, arguments = {
                    @Argument, // Denotes `this' pointer
                    @Argument(returnValue = true),  // pc
                    @Argument(expand = Argument.ExpansionKind.MATERIALIZED, fields = {@Argument.Field(name = "bytecode")}),
                    @Argument(expand = VIRTUAL),    // virtualState
                    @Argument(expand = MATERIALIZED),  // frame
    })
    @EarlyEscapeAnalysis
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC, final int startSP) {
        assert isBlock == FrameAccess.hasClosure(frame);

        if (numArguments == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArguments = FrameAccess.getNumArguments(frame);
        }

        LoopCounter loopCounter = null;
        if (CompilerDirectives.hasNextTier() && !CompilerDirectives.inInterpreter()) {
            loopCounter = new LoopCounter();
        }
        int interpreterLoopCounter = 0;

        int pc = startPC;
        final State state = new State(code.getBytes(), loopCounter, interpreterLoopCounter);
        final VirtualState virtualState = new VirtualState(startSP);

        hoistState(state.interpreterLoopCounter, virtualState.sp);

        Object returnValue = null;
        try {
            while (pc != LOCAL_RETURN_PC) {
                final int opcode = nextOpcode(pc, state, virtualState, frame);
                switch (HostCompilerDirectives.markThreadedSwitch(opcode)) {
                    /* 1 byte bytecodes */
                    case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                        BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                        pc = handlePushReceiverVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                        BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                        pc = handlePushLiteralVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                        BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                        BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                        BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F: {
                        pc = handlePushLiteralConstant(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                        BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B: {
                        pc = handlePushTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_RECEIVER: {
                        pc = handlePushReceiver(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_CONSTANT_TRUE: {
                        pc = handlePushConstantTrue(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_CONSTANT_FALSE: {
                        pc = handlePushConstantFalse(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_CONSTANT_NIL: {
                        pc = handlePushConstantNil(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ZERO: {
                        pc = handlePushConstantZero(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ONE: {
                        pc = handlePushConstantOne(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                        pc = handlePushPseudoVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        pc = handleDuplicateTop(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.RETURN_RECEIVER, BC.RETURN_TRUE, BC.RETURN_FALSE, BC.RETURN_NIL, BC.RETURN_TOP_FROM_METHOD: {
                        state.reportLoopCountOnReturn(this);
                        final Object value = switch (opcode) {
                            case BC.RETURN_RECEIVER -> FrameAccess.getReceiver(frame);
                            case BC.RETURN_TRUE -> BooleanObject.TRUE;
                            case BC.RETURN_FALSE -> BooleanObject.FALSE;
                            case BC.RETURN_NIL -> NilObject.SINGLETON;
                            case BC.RETURN_TOP_FROM_METHOD -> top(frame, virtualState.sp);
                            default -> throw CompilerDirectives.shouldNotReachHere();
                        };
                        returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp, value);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL_FROM_BLOCK, BC.RETURN_TOP_FROM_BLOCK: {
                        state.reportLoopCountOnReturn(this);
                        final Object value = opcode == BC.RETURN_NIL_FROM_BLOCK ? NilObject.SINGLETON : top(frame, virtualState.sp);
                        returnValue = handleReturnFromBlock(frame, pc, value);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.EXT_NOP: {
                        pc = handleNoOperation(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_ADD: {
                        pc = handlePrimitiveAdd(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SUBTRACT: {
                        pc = handlePrimitiveSubtract(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        pc = handlePrimitiveLessThan(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        pc = handlePrimitiveGreaterThan(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        pc = handlePrimitiveLessOrEqual(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        pc = handlePrimitiveGreaterOrEqual(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        pc = handlePrimitiveEqual(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        pc = handlePrimitiveNotEqual(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_AND: {
                        pc = handlePrimitiveBitAnd(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_OR: {
                        pc = handlePrimitiveBitOr(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        pc = handlePrimitiveIdentical(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        pc = handlePrimitiveClass(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        pc = handlePrimitiveNotIdentical(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        pc = handleSend0(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG, //
                        BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        pc = handleSend1(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        pc = handleSend2(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        pc = handleShortUnconditionalJump(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7: {
                        pc = handleShortConditionalJumpTrue(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                        pc = handleShortConditionalJumpFalse(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                        pc = handlePopIntoReceiverVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7: {
                        pc = handlePopIntoTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.POP_STACK: {
                        pc = handlePopStack(pc, state, virtualState, frame);
                        break;
                    }
                    /* 2 byte bytecodes */
                    case BC.EXT_A: {
                        pc = handleExtA(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_B: {
                        pc = handleExtB(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                        pc = handleExtendedPushReceiverVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL_VARIABLE: {
                        pc = handleExtendedPushLiteralVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL: {
                        pc = handleExtendedPushLiteralConstant(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.LONG_PUSH_TEMPORARY_VARIABLE: {
                        pc = handleLongPushTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_NEW_ARRAY: {
                        pc = handlePushNewArray(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_INTEGER: {
                        pc = handleExtendedPushInteger(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_CHARACTER: {
                        pc = handleExtendedPushCharacter(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_SEND: {
                        pc = handleExtendedSend(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_SEND_SUPER: {
                        pc = handleExtendedSuperSend(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_UNCONDITIONAL_JUMP: {
                        pc = handleExtendedUnconditionalJump(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_JUMP_IF_TRUE: {
                        pc = handleExtendedConditionalJumpTrue(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_JUMP_IF_FALSE: {
                        pc = handleExtendedConditionalJumpFalse(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                        pc = handleExtendedStoreAndPopReceiverVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        pc = handleExtendedStoreAndPopLiteralVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        pc = handleLongStoreAndPopTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        pc = handleExtendedStoreReceiverVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        pc = handleExtendedStoreLiteralVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.LONG_STORE_TEMPORARY_VARIABLE: {
                        pc = handleLongStoreTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    /* 3 byte bytecodes */
                    case BC.CALL_PRIMITIVE: {
                        pc = handleCallPrimitive(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_FULL_CLOSURE: {
                        pc = handleExtendedPushFullClosure(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.EXT_PUSH_CLOSURE: {
                        pc = handleExtendedPushClosure(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        pc = handleLongPushRemoteTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        pc = handleLongStoreRemoteTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        pc = handleLongStoreAndPopRemoteTemporaryVariable(pc, state, virtualState, frame);
                        break;
                    }
                    default: {
                        throw unknownBytecode(pc, getUnsignedInt(state.bytecode, pc));
                    }
                }
            }
            assert returnValue != null;
            return returnValue;
        } catch (final OSRException e) {
            return e.osrResult;
        } catch (final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            throw getContext().tryToSignalLowSpace(frame, e);
        }
    }

    @SuppressWarnings("serial")
    private static class OSRException extends RuntimeException {
        private final Object osrResult;

        OSRException(Object osrResult) {
            this.osrResult = osrResult;
        }
    }

    @CompilerDirectives.TruffleBoundary(allowInlining = true)
    private void hoistState(int i1, int i2) {
        // required
    }

    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterFetchOpcode
    @EarlyInline
    private int nextOpcode(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        return getUnsignedInt(state.bytecode, pc);
    }

    // =========================================================================
    // SECTION: PUSH BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3,
                    BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7,
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B,
                    BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F})
    @EarlyInline
    private int handlePushReceiverVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // externalizePCAndSP(frame, virtualState.pc, virtualState.sp); // for ContextObject access
        final int index = getUnsignedInt(state.bytecode, pc) & 0x0F;
        pushFollowed(frame, pc, virtualState.sp++, uncheckedCast(data[pc], SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), index));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3,
                    BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7,
                    BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B,
                    BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F})
    @EarlyInline
    private int handlePushLiteralVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getUnsignedInt(state.bytecode, pc) & 0x0F;
        push(frame, virtualState.sp++, readLiteralVariable(pc, index));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03,
                    BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07,
                    BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B,
                    BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F,
                    BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13,
                    BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17,
                    BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B,
                    BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F})
    @EarlyInline
    private int handlePushLiteralConstant(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getUnsignedInt(state.bytecode, pc) & 0x1F;
        push(frame, virtualState.sp++, getAndResolveLiteral(pc, index));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3,
                    BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7,
                    BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B})
    @EarlyInline
    private int handlePushTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getUnsignedInt(state.bytecode, pc) & 0x0F;
        pushFollowed(frame, pc, virtualState.sp++, getTemp(frame, index));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_RECEIVER)
    @EarlyInline
    private int handlePushReceiver(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        pushFollowed(frame, pc, virtualState.sp++, FrameAccess.getReceiver(frame));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_CONSTANT_TRUE)
    @EarlyInline
    private int handlePushConstantTrue(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, BooleanObject.TRUE);
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_CONSTANT_FALSE)
    @EarlyInline
    private int handlePushConstantFalse(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, BooleanObject.FALSE);
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_CONSTANT_NIL)
    @EarlyInline
    private int handlePushConstantNil(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, NilObject.SINGLETON);
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_CONSTANT_ZERO)
    @EarlyInline
    private int handlePushConstantZero(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, 0L);
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_CONSTANT_ONE)
    @EarlyInline
    private int handlePushConstantOne(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, 1L);
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_PSEUDO_VARIABLE)
    @EarlyInline
    private int handlePushPseudoVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        if (virtualState.getExtB() == 0) {
            push(frame, virtualState.sp++, getOrCreateContext(frame, pc));
        } else {
            throw unknownBytecode(pc, getByte(state.bytecode, pc));
        }
        virtualState.resetExtensions();
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.DUPLICATE_TOP)
    @EarlyInline
    private int handleDuplicateTop(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        pushFollowed(frame, pc, virtualState.sp, top(frame, virtualState.sp));
        virtualState.sp++;
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_RECEIVER_VARIABLE)
    @EarlyInline
    private int handleExtendedPushReceiverVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        pushFollowed(frame, pc, virtualState.sp++,
                        uncheckedCast(data[pc], SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(state.bytecode, pc + 1, virtualState.getExtA())));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_LITERAL_VARIABLE)
    @EarlyInline
    private int handleExtendedPushLiteralVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, readLiteralVariable(pc, getByteExtended(state.bytecode, pc + 1, virtualState.getExtA())));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_LITERAL)
    @EarlyInline
    private int handleExtendedPushLiteralConstant(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, getAndResolveLiteral(pc, getByteExtended(state.bytecode, pc + 1, virtualState.getExtA())));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.LONG_PUSH_TEMPORARY_VARIABLE)
    @EarlyInline
    private int handleLongPushTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        pushFollowed(frame, pc, virtualState.sp++, getTemp(frame, getUnsignedInt(state.bytecode, pc + 1)));
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_NEW_ARRAY)
    @EarlyInline
    private int handlePushNewArray(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int param = getByte(state.bytecode, pc + 1);
        final int arraySize = param & 127;
        CompilerAsserts.partialEvaluationConstant(arraySize);
        final Object[] values;
        if (param < 0) {
            values = popN(frame, virtualState.sp, arraySize);
            virtualState.sp -= arraySize;
        } else {
            values = ArrayUtils.withAll(arraySize, NilObject.SINGLETON);
        }
        push(frame, virtualState.sp++, ArrayObject.createWithStorage(getContext().arrayClass, values));
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_INTEGER)
    @EarlyInline
    private int handleExtendedPushInteger(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, (long) getByteExtended(state.bytecode, pc + 1, virtualState.getExtB()));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_CHARACTER)
    @EarlyInline
    private int handleExtendedPushCharacter(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        push(frame, virtualState.sp++, CharacterObject.valueOf(getByteExtended(state.bytecode, pc + 1, virtualState.getExtA())));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_FULL_CLOSURE)
    @EarlyInline
    private int handleExtendedPushFullClosure(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int literalIndex = getByteExtended(state.bytecode, pc + 1, virtualState.getExtA());
        final CompiledCodeObject block = (CompiledCodeObject) code.getLiteral(literalIndex);
        assert block.assertNotForwarded();
        CompilerAsserts.partialEvaluationConstant(block);
        final byte byteB = getByte(state.bytecode, pc + 2);
        final int numCopied = Byte.toUnsignedInt(byteB) & 63;
        CompilerAsserts.partialEvaluationConstant(numCopied);
        final Object[] copiedValues = popN(frame, virtualState.sp, numCopied);
        virtualState.sp -= numCopied;
        final boolean ignoreContext = (byteB & 0x40) != 0;
        final boolean receiverOnStack = (byteB & 0x80) != 0;
        final ContextObject outerContext = ignoreContext ? null : getOrCreateContext(frame, pc);
        final Object receiver = receiverOnStack ? pop(frame, --virtualState.sp) : FrameAccess.getReceiver(frame);
        push(frame, virtualState.sp++, new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext));
        virtualState.resetExtensions();
        return pc + 3;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_PUSH_CLOSURE)
    @EarlyInline
    private int handleExtendedPushClosure(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int byteA = getUnsignedInt(state.bytecode, pc + 1);
        final int numCopied = (byteA >> 3 & 0x7) + (virtualState.getExtA() >> 4) * 8;
        CompilerAsserts.partialEvaluationConstant(numCopied);
        final Object[] copiedValues = popN(frame, virtualState.sp, numCopied);
        virtualState.sp -= numCopied;
        push(frame, virtualState.sp++, createBlockClosure(frame, uncheckedCast(data[pc], CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, pc)));
        final int blockSize = getByteExtended(state.bytecode, pc + 2, virtualState.getExtB());
        CompilerAsserts.partialEvaluationConstant(blockSize);
        virtualState.resetExtensions();
        return pc + 3 + blockSize;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.PUSH_REMOTE_TEMP_LONG)
    @EarlyInline
    private int handleLongPushRemoteTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        pushFollowed(frame, pc, virtualState.sp++, uncheckedCast(data[pc], SqueakObjectAt0NodeGen.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex));
        return pc + 3;
    }

    // =========================================================================
    // SECTION: POP BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3,
                    BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7})
    @EarlyInline
    private int handlePopIntoReceiverVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getUnsignedInt(state.bytecode, pc) & 0x07;
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --virtualState.sp));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3,
                    BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7})
    @EarlyInline
    private int handlePopIntoTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getUnsignedInt(state.bytecode, pc) & 0x07;
        setStackValue(frame, index, pop(frame, --virtualState.sp));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.POP_STACK)
    @EarlyInline
    private int handlePopStack(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        pop(frame, --virtualState.sp);
        return pc + 1;
    }

    // =========================================================================
    // SECTION: STORE BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE)
    @EarlyInline
    private int handleExtendedStoreAndPopReceiverVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getByteExtended(state.bytecode, pc + 1, virtualState.getExtA());
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --virtualState.sp));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_STORE_AND_POP_LITERAL_VARIABLE)
    @EarlyInline
    private int handleExtendedStoreAndPopLiteralVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getByteExtended(state.bytecode, pc + 1, virtualState.getExtA());
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(pc, index), ASSOCIATION.VALUE, pop(frame, --virtualState.sp));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE)
    @EarlyInline
    private int handleLongStoreAndPopTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        setStackValue(frame, getUnsignedInt(state.bytecode, pc + 1), pop(frame, --virtualState.sp));
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_STORE_RECEIVER_VARIABLE)
    @EarlyInline
    private int handleExtendedStoreReceiverVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getByteExtended(state.bytecode, pc + 1, virtualState.getExtA());
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, top(frame, virtualState.sp));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_STORE_LITERAL_VARIABLE)
    @EarlyInline
    private int handleExtendedStoreLiteralVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int index = getByteExtended(state.bytecode, pc + 1, virtualState.getExtA());
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(pc, index), ASSOCIATION.VALUE, top(frame, virtualState.sp));
        virtualState.resetExtensions();
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.LONG_STORE_TEMPORARY_VARIABLE)
    @EarlyInline
    private int handleLongStoreTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        setStackValue(frame, getUnsignedInt(state.bytecode, pc + 1), top(frame, virtualState.sp));
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.STORE_REMOTE_TEMP_LONG)
    @EarlyInline
    private int handleLongStoreRemoteTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, top(frame, virtualState.sp));
        return pc + 3;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.STORE_AND_POP_REMOTE_TEMP_LONG)
    @EarlyInline
    private int handleLongStoreAndPopRemoteTemporaryVariable(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int remoteTempIndex = getUnsignedInt(state.bytecode, pc + 1);
        final int tempVectorIndex = getUnsignedInt(state.bytecode, pc + 2);
        uncheckedCast(data[pc], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, pop(frame, --virtualState.sp));
        return pc + 3;
    }

    // =========================================================================
    // SECTION: RETURN BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_RECEIVER)
    @EarlyInline
    private int handleReturnReceiver(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp,
        // FrameAccess.getReceiver(frame));
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_TRUE)
    @EarlyInline
    private int handleReturnTrue(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp,
        // BooleanObject.TRUE,
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_FALSE)
    @EarlyInline
    private int handleReturnFalse(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp,
        // BooleanObject.FALSE,
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_NIL)
    @EarlyInline
    private int handleReturnNil(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp,
        // NilObject.SINGLETON,
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_TOP_FROM_METHOD)
    @EarlyInline
    private int handleReturnTopFromMethod(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturn(frame, pc, pc + 1, virtualState.sp, top(frame,
        // virtualState.sp),
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_NIL_FROM_BLOCK)
    @EarlyInline
    private int handleReturnNilFromBlock(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturnFromBlock(frame, pc, NilObject.SINGLETON,
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.RETURN_TOP_FROM_BLOCK)
    @EarlyInline
    private int handleReturnTopFromBlock(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // virtualState.returnValue = handleReturnFromBlock(frame, pc, top(frame, virtualState.sp),
        // virtualState.getProfileCount());
        return LOCAL_RETURN_PC;
    }

    // =========================================================================
    // SECTION: SEND BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_ADD)
    @EarlyInline
    private int handlePrimitiveAdd(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_SUBTRACT)
    @EarlyInline
    private int handlePrimitiveSubtract(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_LESS_THAN)
    @EarlyInline
    private int handlePrimitiveLessThan(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_GREATER_THAN)
    @EarlyInline
    private int handlePrimitiveGreaterThan(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_LESS_OR_EQUAL)
    @EarlyInline
    private int handlePrimitiveLessOrEqual(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_GREATER_OR_EQUAL)
    @EarlyInline
    private int handlePrimitiveGreaterOrEqual(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_EQUAL)
    @EarlyInline
    private int handlePrimitiveEqual(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_NOT_EQUAL)
    @EarlyInline
    private int handlePrimitiveNotEqual(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
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
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_BIT_AND)
    @EarlyInline
    private int handlePrimitiveBitAnd(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimBitAndNode.doLong(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_BIT_OR)
    @EarlyInline
    private int handlePrimitiveBitOr(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        final byte profile = profiles[pc];
        int nextPC = pc + 1;
        final Object result;
        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
            enter(pc, profile, BRANCH2);
            result = PrimBitOrNode.doLong(lhs, rhs);
        } else {
            enter(pc, profile, BRANCH1);
            externalizePCAndSP(frame, nextPC, virtualState.sp);
            result = send(frame, pc, receiver, arg);
            nextPC = internalizePC(frame, nextPC);
        }
        push(frame, virtualState.sp++, result);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_IDENTICAL)
    @EarlyInline
    private int handlePrimitiveIdentical(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        push(frame, virtualState.sp++, uncheckedCast(data[pc], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_CLASS)
    @EarlyInline
    private int handlePrimitiveClass(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object receiver = popReceiver(frame, --virtualState.sp);
        push(frame, virtualState.sp++, uncheckedCast(data[pc], SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.BYTECODE_PRIM_NOT_IDENTICAL)
    @EarlyInline
    private int handlePrimitiveNotIdentical(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        push(frame, virtualState.sp++, !uncheckedCast(data[pc], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
        return pc + 1;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE,
                    BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y,
                    BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3,
                    BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7,
                    BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B,
                    BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F})
    @EarlyInline
    private int handleSend0(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 1;
        final Object receiver = popReceiver(frame, --virtualState.sp);
        externalizePCAndSP(frame, nextPC, virtualState.sp);
        push(frame, virtualState.sp++, send(frame, pc, receiver));
        nextPC = internalizePC(frame, nextPC);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT,
                    BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT,
                    BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG,
                    BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3,
                    BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7,
                    BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B,
                    BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F})
    @EarlyInline
    private int handleSend1(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 1;
        final Object arg = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        externalizePCAndSP(frame, nextPC, virtualState.sp);
        push(frame, virtualState.sp++, send(frame, pc, receiver, arg));
        nextPC = internalizePC(frame, nextPC);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.BYTECODE_PRIM_AT_PUT,
                    BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3,
                    BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7,
                    BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B,
                    BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F})
    @EarlyInline
    private int handleSend2(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 1;
        final Object arg2 = pop(frame, --virtualState.sp);
        final Object arg1 = pop(frame, --virtualState.sp);
        final Object receiver = popReceiver(frame, --virtualState.sp);
        externalizePCAndSP(frame, nextPC, virtualState.sp);
        push(frame, virtualState.sp++, send(frame, pc, receiver, arg1, arg2));
        nextPC = internalizePC(frame, nextPC);
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_SEND)
    @EarlyInline
    private int handleExtendedSend(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 2;
        final int byte1 = getUnsignedInt(state.bytecode, pc + 1);
        final int numArgs = (byte1 & 7) + (virtualState.getExtB() << 3);
        CompilerAsserts.partialEvaluationConstant(numArgs);
        final Object[] arguments = popN(frame, virtualState.sp, numArgs);
        virtualState.sp -= numArgs;
        final Object receiver = popReceiver(frame, --virtualState.sp);
        externalizePCAndSP(frame, nextPC, virtualState.sp);
        push(frame, virtualState.sp++, sendNary(frame, pc, receiver, arguments));
        nextPC = internalizePC(frame, nextPC);
        virtualState.resetExtensions();
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_SEND_SUPER)
    @EarlyInline
    private int handleExtendedSuperSend(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 2;
        final boolean isDirected;
        final int extB = virtualState.getExtB();
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
        final ClassObject lookupClass = isDirected ? ((ClassObject) pop(frame, --virtualState.sp)).getResolvedSuperclass() : null;
        final Object[] arguments = popN(frame, virtualState.sp, numArgs);
        virtualState.sp -= numArgs;
        final Object receiver = popReceiver(frame, --virtualState.sp);
        externalizePCAndSP(frame, nextPC, virtualState.sp);
        CompilerAsserts.partialEvaluationConstant(isDirected);
        pushFollowed(frame, pc, virtualState.sp++, sendSuper(frame, isDirected, pc, lookupClass, receiver, arguments));
        nextPC = internalizePC(frame, nextPC);
        virtualState.resetExtensions();
        return nextPC;
    }

    // =========================================================================
    // SECTION: JUMP BYTECODES
    // =========================================================================

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3,
                    BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7})
    @EarlyInline
    private int handleShortUnconditionalJump(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int offset = calculateShortOffset(getUnsignedInt(state.bytecode, pc));
        final int nextPC = pc + 1 + offset;
        if (offset < 0) {
            if (CompilerDirectives.hasNextTier()) {
                int count = CompilerDirectives.inInterpreter() ? ++state.interpreterLoopCounter : ++state.loopCounter.value;
                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, count >= LoopCounter.CHECK_LOOP_STRIDE)) {
                    LoopNode.reportLoopCount(this, count);
                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, count)) {
                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((virtualState.sp & 0xFF) << 16) | nextPC, null, null, frame);
                        if (osrReturnValue != null) {
                            assert !FrameAccess.hasModifiedSender(frame);
                            FrameAccess.terminateFrame(frame);
                            throw new OSRException(osrReturnValue);
                        }
                    }
                    if (CompilerDirectives.inInterpreter()) {
                        state.interpreterLoopCounter = 0;
                    } else {
                        state.loopCounter.value = 0;
                    }
                }
            }
            if (data[pc] instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                checkForInterruptsNode.execute(frame, nextPC);
            }
        }
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3,
                    BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7})
    @EarlyInline
    private int handleShortConditionalJumpTrue(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 1;
        final Object stackValue = pop(frame, --virtualState.sp);
        if (stackValue instanceof final Boolean condition) {
            if (uncheckedCast(data[pc], CountingConditionProfile.class).profile(condition)) {
                nextPC += calculateShortOffset(getUnsignedInt(state.bytecode, pc));
            }
        } else {
            sendMustBeBooleanInInterpreter(frame, nextPC, stackValue);
        }
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(value = {BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3,
                    BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7})
    @EarlyInline
    private int handleShortConditionalJumpFalse(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        int nextPC = pc + 1;
        final Object stackValue = pop(frame, --virtualState.sp);
        if (stackValue instanceof final Boolean condition) {
            if (uncheckedCast(data[pc], CountingConditionProfile.class).profile(!condition)) {
                nextPC += calculateShortOffset(getUnsignedInt(state.bytecode, pc));
            }
        } else {
            sendMustBeBooleanInInterpreter(frame, nextPC, stackValue);
        }
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_UNCONDITIONAL_JUMP)
    @EarlyInline
    private int handleExtendedUnconditionalJump(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int offset = calculateLongExtendedOffset(getByte(state.bytecode, pc + 1), virtualState.getExtB());
        final int nextPC = pc + 2 + offset;
        if (offset < 0) {
            if (CompilerDirectives.hasNextTier()) {
                int count = CompilerDirectives.inInterpreter() ? ++state.interpreterLoopCounter : ++state.loopCounter.value;
                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, count >= LoopCounter.CHECK_LOOP_STRIDE)) {
                    LoopNode.reportLoopCount(this, count);
                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, count)) {
                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((virtualState.sp & 0xFF) << 16) | nextPC, null, null, frame);
                        if (osrReturnValue != null) {
                            assert !FrameAccess.hasModifiedSender(frame);
                            FrameAccess.terminateFrame(frame);
                            throw new OSRException(osrReturnValue);
                        }
                    }
                    if (CompilerDirectives.inInterpreter()) {
                        state.interpreterLoopCounter = 0;
                    } else {
                        state.loopCounter.value = 0;
                    }
                }
            }
            if (data[pc] instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                checkForInterruptsNode.execute(frame, nextPC);
            }
        }
        virtualState.resetExtensions();
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_JUMP_IF_TRUE)
    @EarlyInline
    private int handleExtendedConditionalJumpTrue(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object stackValue = pop(frame, --virtualState.sp);
        final int offset = getByteExtended(state.bytecode, pc + 1, virtualState.getExtB());
        int nextPC = pc + 2;
        if (stackValue instanceof final Boolean condition) {
            if (uncheckedCast(data[pc], CountingConditionProfile.class).profile(condition)) {
                nextPC += offset;
            }
        } else {
            sendMustBeBooleanInInterpreter(frame, nextPC, stackValue);
        }
        virtualState.resetExtensions();
        return nextPC;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.EXT_JUMP_IF_FALSE)
    @EarlyInline
    private int handleExtendedConditionalJumpFalse(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final Object stackValue = pop(frame, --virtualState.sp);
        final int offset = getByteExtended(state.bytecode, pc + 1, virtualState.getExtB());
        int nextPC = pc + 2;
        if (stackValue instanceof final Boolean condition) {
            if (uncheckedCast(data[pc], CountingConditionProfile.class).profile(!condition)) {
                nextPC += offset;
            }
        } else {
            sendMustBeBooleanInInterpreter(frame, nextPC, stackValue);
        }
        virtualState.resetExtensions();
        return nextPC;
    }

    // =========================================================================
    // SECTION: MISCELLANEOUS BYTECODES
    // =========================================================================

    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(BC.EXT_NOP)
    @EarlyInline
    private int handleNoOperation(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        virtualState.resetExtensions();
        return pc + 1;
    }

    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(BC.EXT_A)
    @EarlyInline
    private int handleExtA(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        final int extA = virtualState.getExtA();
        final int newExtA = (extA << 8) | getUnsignedInt(state.bytecode, pc + 1);
        virtualState.extBA = (virtualState.extBA & 0xFFFFFFFF00000000L) | Integer.toUnsignedLong(newExtA);
        return pc + 2;
    }

    @SuppressWarnings({"unused", "static-method"})
    @BytecodeInterpreterHandler(BC.EXT_B)
    @EarlyInline
    private int handleExtB(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        // {editor-fold desc="BC_EXT_B_LOGIC"}
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
         * extB and relying on the next byte to shift that byte out of the register.
         */
        // {/editor-fold}
        final int extB = virtualState.getExtB();
        final int newExtB;
        if (extB == 0) {
            /* leading byte is signed */
            final int byteValue = getByte(state.bytecode, pc + 1);
            /* make sure newExtB is non-zero for next byte processing */
            newExtB = byteValue == 0 ? 0x80000000 : byteValue;
        } else {
            /* subsequent bytes are unsigned */
            final int byteValue = getUnsignedInt(state.bytecode, pc + 1);
            newExtB = (extB << 8) | byteValue;
        }
        virtualState.extBA = ((long) newExtB << 32) | (virtualState.extBA & 0xFFFFFFFFL);
        return pc + 2;
    }

    @SuppressWarnings("unused")
    @BytecodeInterpreterHandler(BC.CALL_PRIMITIVE)
    @EarlyInline
    private int handleCallPrimitive(final int pc, final State state, final VirtualState virtualState, final VirtualFrame frame) {
        if (getUnsignedInt(state.bytecode, pc + 3) == BC.LONG_STORE_TEMPORARY_VARIABLE) {
            assert virtualState.sp > 0;
            // ToDo: should this push instead of setting the top of the stack
            setStackValue(frame, virtualState.sp - 1, getErrorObject());
        }
        return pc + 3;
    }

    // =========================================================================

    private static int getByteExtended(final byte[] bc, final int pc, final int extend) {
        return getUnsignedInt(bc, pc) + (extend << 8);
    }

    private Object sendSuper(final VirtualFrame frame, final boolean isDirected, final int currentPC, final ClassObject lookupClass, final Object receiver, final Object[] arguments) {
        try {
            if (isDirected) {
                return uncheckedCast(data[currentPC], DispatchDirectedSuperNaryNodeGen.class).execute(frame, lookupClass, receiver, arguments);
            } else {
                return uncheckedCast(data[currentPC], DispatchSuperNaryNodeGen.class).execute(frame, receiver, arguments);
            }
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    public static int calculateLongExtendedOffset(final byte bytecode, final int extB) {
        return Byte.toUnsignedInt(bytecode) + (extB << 8);
    }

    /**
     * Extracts the signed 32-bit extB from the high 32 bits of extBA.
     */
    private static int getExtB(final long extBA) {
        return (int) (extBA >> 32);
    }

    /**
     * Extracts the unsigned 32-bit extA from the low 32 bits of extBA.
     */
    private static int getExtA(final long extBA) {
        return (int) extBA;
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

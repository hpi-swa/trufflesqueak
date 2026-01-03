/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
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

    @Override
    protected void processBytecode(final int startPC, final int endPC) {
        final byte[] bc = code.getBytes();
        final SqueakImageContext image = SqueakImageContext.getSlow();

        int pc = startPC;
        assert pc < endPC;
        int extA = 0;
        int extB = 0;

        while (pc < endPC) {
            final int currentPC = pc++;
            final byte b = getByte(bc, currentPC);
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
                    if (extB == 0) {
                        break;
                    } else {
                        throw unknownBytecode();
                    }
                }
                case BC.EXT_NOP:
                    extA = extB = 0;
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
                    extA = (extA << 8) + getUnsignedInt(bc, pc++);
                    break;
                }
                case BC.EXT_B: {
                    final int byteValue = getUnsignedInt(bc, pc++);
                    extB = extB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue;
                    assert extB != 0 : "should use numExtB?";
                    break;
                }
                case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(getByteExtended(bc, pc, extA)));
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_CHARACTER: {
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.LONG_PUSH_TEMPORARY_VARIABLE, BC.PUSH_NEW_ARRAY, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE, BC.LONG_STORE_TEMPORARY_VARIABLE: {
                    pc++;
                    break;
                }
                case BC.EXT_PUSH_INTEGER: {
                    pc++;
                    extB = 0;
                    break;
                }
                case BC.EXT_SEND: {
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (extA << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    data[currentPC] = insert(DispatchNaryNodeGen.create(selector));
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final boolean isDirected = extB >= 64;
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (extA << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    if (isDirected) {
                        data[currentPC] = insert(DispatchDirectedSuperNaryNodeGen.create(selector));
                    } else {
                        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                        data[currentPC] = insert(DispatchSuperNaryNodeGen.create(methodClass, selector));
                    }
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsInLoopNode.createForLoop(data, currentPC, 2, offset));
                    }
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE: {
                    data[currentPC] = CountingConditionProfile.create();
                    pc++;
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_LITERAL_VARIABLE: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    pc++;
                    extA = 0;
                    break;
                }
                /* 3 byte bytecodes */
                case BC.CALL_PRIMITIVE: {
                    pc += 2;
                    break;
                }
                case BC.EXT_PUSH_FULL_CLOSURE: {
                    pc += 2;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_CLOSURE: {
                    final int byteA = getUnsignedInt(bc, pc++);
                    final int numArgs = (byteA & 0x07) + (extA & 0x0F) * 8;
                    final int numCopied = (byteA >> 3 & 0x7) + (extA >> 4) * 8;
                    final int blockSize = getByteExtended(bc, pc++, extB);
                    data[currentPC] = createBlock(code, pc, numArgs, numCopied, blockSize);
                    pc += blockSize;
                    extA = extB = 0;
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
                    throw unknownBytecode();
                }
            }
        }
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC, final int startSP) {
        assert isBlock == FrameAccess.hasClosure(frame);

        if (numArguments == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArguments = FrameAccess.getNumArguments(frame);
        }

        final SqueakImageContext image = getContext();
        final byte[] bc = uncheckedCast(code.getBytes(), byte[].class);

        int pc = startPC;
        int sp = startSP;
        int extA = 0;
        int extB = 0;

        int counter = 0;
        final LoopCounter loopCounter = CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? new LoopCounter() : null;

        Object returnValue = null;
        try {
            while (pc != LOCAL_RETURN_PC) {
                CompilerAsserts.partialEvaluationConstant(pc);
                CompilerAsserts.partialEvaluationConstant(sp);
                CompilerAsserts.partialEvaluationConstant(extA);
                CompilerAsserts.partialEvaluationConstant(extB);
                final int currentPC = pc++;
                final byte b = getByte(bc, currentPC);
                CompilerAsserts.partialEvaluationConstant(b);
                switch (b) {
                    /* 1 byte bytecodes */
                    case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                        BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                        externalizePCAndSP(frame, pc, sp); // for ContextObject access
                        pushFollowed(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                        BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                        push(frame, sp++, readLiteralVariable(currentPC, b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                        BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                        BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                        BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F: {
                        push(frame, sp++, getAndResolveLiteral(currentPC, b & 0x1F));
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                        BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B: {
                        pushFollowed(frame, currentPC, sp++, getTemp(frame, b & 0xF));
                        break;
                    }
                    case BC.PUSH_RECEIVER: {
                        pushFollowed(frame, currentPC, sp++, FrameAccess.getReceiver(frame));
                        break;
                    }
                    case BC.PUSH_CONSTANT_TRUE: {
                        push(frame, sp++, BooleanObject.TRUE);
                        break;
                    }
                    case BC.PUSH_CONSTANT_FALSE: {
                        push(frame, sp++, BooleanObject.FALSE);
                        break;
                    }
                    case BC.PUSH_CONSTANT_NIL: {
                        push(frame, sp++, NilObject.SINGLETON);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ZERO: {
                        push(frame, sp++, 0L);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ONE: {
                        push(frame, sp++, 1L);
                        break;
                    }
                    case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                        if (extB == 0) {
                            push(frame, sp++, getOrCreateContext(frame, currentPC));
                        } else {
                            throw unknownBytecode();
                        }
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        pushFollowed(frame, currentPC, sp, top(frame, sp));
                        sp++;
                        break;
                    }
                    case BC.RETURN_RECEIVER: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, FrameAccess.getReceiver(frame),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TRUE: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, BooleanObject.TRUE,
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_FALSE: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, BooleanObject.FALSE,
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, NilObject.SINGLETON,
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_METHOD: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, top(frame, sp),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL_FROM_BLOCK: {
                        returnValue = handleReturnFromBlock(frame, currentPC, NilObject.SINGLETON,
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_BLOCK: {
                        returnValue = handleReturnFromBlock(frame, currentPC, top(frame, sp),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.EXT_NOP: {
                        extA = extB = 0;
                        break;
                    }
                    case BC.BYTECODE_PRIM_ADD: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            /* Profiled version of LargeIntegers.add(image, lhs, rhs). */
                            final long r = lhs + rhs;
                            if (((lhs ^ r) & (rhs ^ r)) < 0) {
                                enter(currentPC, state, BRANCH3);
                                result = LargeIntegers.addLarge(image, lhs, rhs);
                            } else {
                                enter(currentPC, state, BRANCH4);
                                result = r;
                            }
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH5);
                            result = PrimSmallFloatAddNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SUBTRACT: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            /* Profiled version of LargeIntegers.subtract(image, lhs, rhs). */
                            final long r = lhs - rhs;
                            if (((lhs ^ rhs) & (lhs ^ r)) < 0) {
                                enter(currentPC, state, BRANCH3);
                                result = LargeIntegers.subtractLarge(image, lhs, rhs);
                            } else {
                                enter(currentPC, state, BRANCH4);
                                result = r;
                            }
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH5);
                            result = PrimSmallFloatSubtractNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimLessThanNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatLessThanNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimGreaterThanNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatGreaterThanNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimLessOrEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatLessOrEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimGreaterOrEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatGreaterOrEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimNotEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, state, BRANCH3);
                            result = PrimSmallFloatNotEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_AND: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimBitAndNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_OR: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, state, BRANCH2);
                            result = PrimBitOrNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, state, BRANCH1);
                            externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, uncheckedCast(data[currentPC], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, uncheckedCast(data[currentPC], SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, !uncheckedCast(data[currentPC], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver));
                        pc = internalizePC(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG, //
                        BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg));
                        pc = internalizePC(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        final Object arg2 = pop(frame, --sp);
                        final Object arg1 = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg1, arg2));
                        pc = internalizePC(frame, pc);
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        final int offset = calculateShortOffset(b);
                        pc += offset;
                        if (offset < 0) {
                            if (CompilerDirectives.hasNextTier()) {
                                if (CompilerDirectives.inCompiledCode()) {
                                    counter = ++loopCounter.value;
                                } else {
                                    counter++;
                                }
                                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, counter >= LoopCounter.CHECK_LOOP_STRIDE)) {
                                    LoopNode.reportLoopCount(this, counter);
                                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, counter)) {
                                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((sp & 0xFF) << 16) | pc, null, null, frame);
                                        if (osrReturnValue != null) {
                                            assert !FrameAccess.hasModifiedSender(frame);
                                            FrameAccess.terminateFrame(frame);
                                            return osrReturnValue;
                                        }
                                    }
                                    if (CompilerDirectives.inCompiledCode()) {
                                        loopCounter.value = 0;
                                    } else {
                                        counter = 0;
                                    }
                                }
                                if (CompilerDirectives.inCompiledCode()) {
                                    counter = 0;
                                }
                            }
                            if (data[currentPC] instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                                checkForInterruptsNode.execute(frame, pc);
                            }
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(!condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), b & 7, pop(frame, --sp));
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7: {
                        setStackValue(frame, b & 7, pop(frame, --sp));
                        break;
                    }
                    case BC.POP_STACK: {
                        pop(frame, --sp);
                        break;
                    }
                    /* 2 byte bytecodes */
                    case BC.EXT_A: {
                        extA = (extA << 8) + getUnsignedInt(bc, pc++);
                        break;
                    }
                    case BC.EXT_B: {
                        final int byteValue = getUnsignedInt(bc, pc++);
                        extB = extB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue;
                        assert extB != 0 : "is numExtB needed?";
                        break;
                    }
                    case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                        pushFollowed(frame, currentPC, sp++,
                                        uncheckedCast(data[currentPC], SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL_VARIABLE: {
                        push(frame, sp++, readLiteralVariable(currentPC, getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL: {
                        push(frame, sp++, getAndResolveLiteral(currentPC, getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_PUSH_TEMPORARY_VARIABLE: {
                        pushFollowed(frame, currentPC, sp++, getTemp(frame, getUnsignedInt(bc, pc++)));
                        break;
                    }
                    case BC.PUSH_NEW_ARRAY: {
                        final int param = getByte(bc, pc++);
                        final int arraySize = param & 127;
                        final Object[] values;
                        if (param < 0) {
                            values = popN(frame, sp, arraySize);
                            sp -= arraySize;
                        } else {
                            values = ArrayUtils.withAll(arraySize, NilObject.SINGLETON);
                        }
                        push(frame, sp++, ArrayObject.createWithStorage(image.arrayClass, values));
                        break;
                    }
                    case BC.EXT_PUSH_INTEGER: {
                        push(frame, sp++, (long) getByteExtended(bc, pc++, extB));
                        extB = 0;
                        break;
                    }
                    case BC.EXT_PUSH_CHARACTER: {
                        push(frame, sp++, CharacterObject.valueOf(getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_SEND: {
                        final int byte1 = getUnsignedInt(bc, pc++);
                        final int numArgs = (byte1 & 7) + (extB << 3);
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = internalizePC(frame, pc);
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_SEND_SUPER: {
                        final boolean isDirected;
                        final int extBValue;
                        if (extB >= 64) {
                            isDirected = true;
                            extBValue = extB & 63;
                        } else {
                            isDirected = false;
                            extBValue = extB;
                        }
                        final int byte1 = getUnsignedInt(bc, pc++);
                        final int numArgs = (byte1 & 7) + (extBValue << 3);
                        final ClassObject lookupClass = isDirected ? ((ClassObject) pop(frame, --sp)).getResolvedSuperclass() : null;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        CompilerAsserts.partialEvaluationConstant(isDirected);
                        pushFollowed(frame, currentPC, sp++, sendSuper(frame, isDirected, currentPC, lookupClass, receiver, arguments));
                        pc = internalizePC(frame, pc);
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_UNCONDITIONAL_JUMP: {
                        final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                        pc += offset;
                        if (offset < 0) {
                            if (CompilerDirectives.hasNextTier()) {
                                if (CompilerDirectives.inCompiledCode()) {
                                    counter = ++loopCounter.value;
                                } else {
                                    counter++;
                                }
                                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, counter >= LoopCounter.CHECK_LOOP_STRIDE)) {
                                    LoopNode.reportLoopCount(this, counter);
                                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, counter)) {
                                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((sp & 0xFF) << 16) | pc, null, null, frame);
                                        if (osrReturnValue != null) {
                                            assert !FrameAccess.hasModifiedSender(frame);
                                            FrameAccess.terminateFrame(frame);
                                            return osrReturnValue;
                                        }
                                    }
                                    if (CompilerDirectives.inCompiledCode()) {
                                        loopCounter.value = 0;
                                    } else {
                                        counter = 0;
                                    }
                                }
                                if (CompilerDirectives.inCompiledCode()) {
                                    counter = 0;
                                }
                            }
                            if (data[currentPC] instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                                checkForInterruptsNode.execute(frame, pc);
                            }
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_JUMP_IF_TRUE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = getByteExtended(bc, pc++, extB);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_JUMP_IF_FALSE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = getByteExtended(bc, pc++, extB);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(!condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        setStackValue(frame, getByte(bc, pc++), pop(frame, --sp));
                        break;
                    }
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, top(frame, sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, top(frame, sp));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_STORE_TEMPORARY_VARIABLE: {
                        setStackValue(frame, getByte(bc, pc++), top(frame, sp));
                        break;
                    }
                    /* 3 byte bytecodes */
                    case BC.CALL_PRIMITIVE: {
                        pc += 2;
                        if (getByte(bc, pc) == BC.LONG_STORE_TEMPORARY_VARIABLE) {
                            assert sp > 0;
                            setStackValue(frame, sp - 1, getErrorObject());
                        }
                        break;
                    }
                    case BC.EXT_PUSH_FULL_CLOSURE: {
                        final int literalIndex = getByteExtended(bc, pc++, extA);
                        final CompiledCodeObject block = (CompiledCodeObject) code.getLiteral(literalIndex);
                        assert block.assertNotForwarded();
                        CompilerAsserts.partialEvaluationConstant(block);
                        final byte byteB = getByte(bc, pc++);
                        final int numCopied = Byte.toUnsignedInt(byteB) & 63;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        final boolean ignoreContext = (byteB & 0x40) != 0;
                        final boolean receiverOnStack = (byteB & 0x80) != 0;
                        final ContextObject outerContext = ignoreContext ? null : getOrCreateContext(frame, currentPC);
                        final Object receiver = receiverOnStack ? pop(frame, --sp) : FrameAccess.getReceiver(frame);
                        push(frame, sp++, new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_CLOSURE: {
                        final int byteA = getUnsignedInt(bc, pc++);
                        final int numCopied = (byteA >> 3 & 0x7) + (extA >> 4) * 8;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        push(frame, sp++, createBlockClosure(frame, uncheckedCast(data[currentPC], CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, currentPC)));
                        final int blockSize = getByteExtended(bc, pc++, extB);
                        pc += blockSize;
                        extA = extB = 0;
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        pushFollowed(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0NodeGen.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex));
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, top(frame, sp));
                        break;
                    }
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
                        break;
                    }
                    default: {
                        throw unknownBytecode();
                    }
                }
            }
        } catch (final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            throw image.tryToSignalLowSpace(frame, e);
        }
        assert returnValue != null;
        return returnValue;
    }

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

    static class BC {
        /* 1 byte bytecodes */
        static final byte PUSH_RCVR_VAR_0 = (byte) 0;
        static final byte PUSH_RCVR_VAR_1 = (byte) 1;
        static final byte PUSH_RCVR_VAR_2 = (byte) 2;
        static final byte PUSH_RCVR_VAR_3 = (byte) 3;
        static final byte PUSH_RCVR_VAR_4 = (byte) 4;
        static final byte PUSH_RCVR_VAR_5 = (byte) 5;
        static final byte PUSH_RCVR_VAR_6 = (byte) 6;
        static final byte PUSH_RCVR_VAR_7 = (byte) 7;
        static final byte PUSH_RCVR_VAR_8 = (byte) 8;
        static final byte PUSH_RCVR_VAR_9 = (byte) 9;
        static final byte PUSH_RCVR_VAR_A = (byte) 10;
        static final byte PUSH_RCVR_VAR_B = (byte) 11;
        static final byte PUSH_RCVR_VAR_C = (byte) 12;
        static final byte PUSH_RCVR_VAR_D = (byte) 13;
        static final byte PUSH_RCVR_VAR_E = (byte) 14;
        static final byte PUSH_RCVR_VAR_F = (byte) 15;
        static final byte PUSH_LIT_VAR_0 = (byte) 16;
        static final byte PUSH_LIT_VAR_1 = (byte) 17;
        static final byte PUSH_LIT_VAR_2 = (byte) 18;
        static final byte PUSH_LIT_VAR_3 = (byte) 19;
        static final byte PUSH_LIT_VAR_4 = (byte) 20;
        static final byte PUSH_LIT_VAR_5 = (byte) 21;
        static final byte PUSH_LIT_VAR_6 = (byte) 22;
        static final byte PUSH_LIT_VAR_7 = (byte) 23;
        static final byte PUSH_LIT_VAR_8 = (byte) 24;
        static final byte PUSH_LIT_VAR_9 = (byte) 25;
        static final byte PUSH_LIT_VAR_A = (byte) 26;
        static final byte PUSH_LIT_VAR_B = (byte) 27;
        static final byte PUSH_LIT_VAR_C = (byte) 28;
        static final byte PUSH_LIT_VAR_D = (byte) 29;
        static final byte PUSH_LIT_VAR_E = (byte) 30;
        static final byte PUSH_LIT_VAR_F = (byte) 31;
        static final byte PUSH_LIT_CONST_00 = (byte) 32;
        static final byte PUSH_LIT_CONST_01 = (byte) 33;
        static final byte PUSH_LIT_CONST_02 = (byte) 34;
        static final byte PUSH_LIT_CONST_03 = (byte) 35;
        static final byte PUSH_LIT_CONST_04 = (byte) 36;
        static final byte PUSH_LIT_CONST_05 = (byte) 37;
        static final byte PUSH_LIT_CONST_06 = (byte) 38;
        static final byte PUSH_LIT_CONST_07 = (byte) 39;
        static final byte PUSH_LIT_CONST_08 = (byte) 40;
        static final byte PUSH_LIT_CONST_09 = (byte) 41;
        static final byte PUSH_LIT_CONST_0A = (byte) 42;
        static final byte PUSH_LIT_CONST_0B = (byte) 43;
        static final byte PUSH_LIT_CONST_0C = (byte) 44;
        static final byte PUSH_LIT_CONST_0D = (byte) 45;
        static final byte PUSH_LIT_CONST_0E = (byte) 46;
        static final byte PUSH_LIT_CONST_0F = (byte) 47;
        static final byte PUSH_LIT_CONST_10 = (byte) 48;
        static final byte PUSH_LIT_CONST_11 = (byte) 49;
        static final byte PUSH_LIT_CONST_12 = (byte) 50;
        static final byte PUSH_LIT_CONST_13 = (byte) 51;
        static final byte PUSH_LIT_CONST_14 = (byte) 52;
        static final byte PUSH_LIT_CONST_15 = (byte) 53;
        static final byte PUSH_LIT_CONST_16 = (byte) 54;
        static final byte PUSH_LIT_CONST_17 = (byte) 55;
        static final byte PUSH_LIT_CONST_18 = (byte) 56;
        static final byte PUSH_LIT_CONST_19 = (byte) 57;
        static final byte PUSH_LIT_CONST_1A = (byte) 58;
        static final byte PUSH_LIT_CONST_1B = (byte) 59;
        static final byte PUSH_LIT_CONST_1C = (byte) 60;
        static final byte PUSH_LIT_CONST_1D = (byte) 61;
        static final byte PUSH_LIT_CONST_1E = (byte) 62;
        static final byte PUSH_LIT_CONST_1F = (byte) 63;
        static final byte PUSH_TEMP_VAR_0 = (byte) 64;
        static final byte PUSH_TEMP_VAR_1 = (byte) 65;
        static final byte PUSH_TEMP_VAR_2 = (byte) 66;
        static final byte PUSH_TEMP_VAR_3 = (byte) 67;
        static final byte PUSH_TEMP_VAR_4 = (byte) 68;
        static final byte PUSH_TEMP_VAR_5 = (byte) 69;
        static final byte PUSH_TEMP_VAR_6 = (byte) 70;
        static final byte PUSH_TEMP_VAR_7 = (byte) 71;
        static final byte PUSH_TEMP_VAR_8 = (byte) 72;
        static final byte PUSH_TEMP_VAR_9 = (byte) 73;
        static final byte PUSH_TEMP_VAR_A = (byte) 74;
        static final byte PUSH_TEMP_VAR_B = (byte) 75;
        static final byte PUSH_RECEIVER = (byte) 76;
        static final byte PUSH_CONSTANT_TRUE = (byte) 77;
        static final byte PUSH_CONSTANT_FALSE = (byte) 78;
        static final byte PUSH_CONSTANT_NIL = (byte) 79;
        static final byte PUSH_CONSTANT_ZERO = (byte) 80;
        static final byte PUSH_CONSTANT_ONE = (byte) 81;
        static final byte EXT_PUSH_PSEUDO_VARIABLE = (byte) 82;
        static final byte DUPLICATE_TOP = (byte) 83;
        static final byte RETURN_RECEIVER = (byte) 88;
        static final byte RETURN_TRUE = (byte) 89;
        static final byte RETURN_FALSE = (byte) 90;
        static final byte RETURN_NIL = (byte) 91;
        static final byte RETURN_TOP_FROM_METHOD = (byte) 92;
        static final byte RETURN_NIL_FROM_BLOCK = (byte) 93;
        static final byte RETURN_TOP_FROM_BLOCK = (byte) 94;
        static final byte EXT_NOP = (byte) 95;
        static final byte BYTECODE_PRIM_ADD = (byte) 96;
        static final byte BYTECODE_PRIM_SUBTRACT = (byte) 97;
        static final byte BYTECODE_PRIM_LESS_THAN = (byte) 98;
        static final byte BYTECODE_PRIM_GREATER_THAN = (byte) 99;
        static final byte BYTECODE_PRIM_LESS_OR_EQUAL = (byte) 100;
        static final byte BYTECODE_PRIM_GREATER_OR_EQUAL = (byte) 101;
        static final byte BYTECODE_PRIM_EQUAL = (byte) 102;
        static final byte BYTECODE_PRIM_NOT_EQUAL = (byte) 103;
        static final byte BYTECODE_PRIM_MULTIPLY = (byte) 104;
        static final byte BYTECODE_PRIM_DIVIDE = (byte) 105;
        static final byte BYTECODE_PRIM_MOD = (byte) 106;
        static final byte BYTECODE_PRIM_MAKE_POINT = (byte) 107;
        static final byte BYTECODE_PRIM_BIT_SHIFT = (byte) 108;
        static final byte BYTECODE_PRIM_DIV = (byte) 109;
        static final byte BYTECODE_PRIM_BIT_AND = (byte) 110;
        static final byte BYTECODE_PRIM_BIT_OR = (byte) 111;
        static final byte BYTECODE_PRIM_AT = (byte) 112;
        static final byte BYTECODE_PRIM_AT_PUT = (byte) 113;
        static final byte BYTECODE_PRIM_SIZE = (byte) 114;
        static final byte BYTECODE_PRIM_NEXT = (byte) 115;
        static final byte BYTECODE_PRIM_NEXT_PUT = (byte) 116;
        static final byte BYTECODE_PRIM_AT_END = (byte) 117;
        static final byte BYTECODE_PRIM_IDENTICAL = (byte) 118;
        static final byte BYTECODE_PRIM_CLASS = (byte) 119;
        static final byte BYTECODE_PRIM_NOT_IDENTICAL = (byte) 120;
        static final byte BYTECODE_PRIM_VALUE = (byte) 121;
        static final byte BYTECODE_PRIM_VALUE_WITH_ARG = (byte) 122;
        static final byte BYTECODE_PRIM_DO = (byte) 123;
        static final byte BYTECODE_PRIM_NEW = (byte) 124;
        static final byte BYTECODE_PRIM_NEW_WITH_ARG = (byte) 125;
        static final byte BYTECODE_PRIM_POINT_X = (byte) 126;
        static final byte BYTECODE_PRIM_POINT_Y = (byte) 127;
        static final byte SEND_LIT_SEL0_0 = (byte) 128;
        static final byte SEND_LIT_SEL0_1 = (byte) 129;
        static final byte SEND_LIT_SEL0_2 = (byte) 130;
        static final byte SEND_LIT_SEL0_3 = (byte) 131;
        static final byte SEND_LIT_SEL0_4 = (byte) 132;
        static final byte SEND_LIT_SEL0_5 = (byte) 133;
        static final byte SEND_LIT_SEL0_6 = (byte) 134;
        static final byte SEND_LIT_SEL0_7 = (byte) 135;
        static final byte SEND_LIT_SEL0_8 = (byte) 136;
        static final byte SEND_LIT_SEL0_9 = (byte) 137;
        static final byte SEND_LIT_SEL0_A = (byte) 138;
        static final byte SEND_LIT_SEL0_B = (byte) 139;
        static final byte SEND_LIT_SEL0_C = (byte) 140;
        static final byte SEND_LIT_SEL0_D = (byte) 141;
        static final byte SEND_LIT_SEL0_E = (byte) 142;
        static final byte SEND_LIT_SEL0_F = (byte) 143;
        static final byte SEND_LIT_SEL1_0 = (byte) 144;
        static final byte SEND_LIT_SEL1_1 = (byte) 145;
        static final byte SEND_LIT_SEL1_2 = (byte) 146;
        static final byte SEND_LIT_SEL1_3 = (byte) 147;
        static final byte SEND_LIT_SEL1_4 = (byte) 148;
        static final byte SEND_LIT_SEL1_5 = (byte) 149;
        static final byte SEND_LIT_SEL1_6 = (byte) 150;
        static final byte SEND_LIT_SEL1_7 = (byte) 151;
        static final byte SEND_LIT_SEL1_8 = (byte) 152;
        static final byte SEND_LIT_SEL1_9 = (byte) 153;
        static final byte SEND_LIT_SEL1_A = (byte) 154;
        static final byte SEND_LIT_SEL1_B = (byte) 155;
        static final byte SEND_LIT_SEL1_C = (byte) 156;
        static final byte SEND_LIT_SEL1_D = (byte) 157;
        static final byte SEND_LIT_SEL1_E = (byte) 158;
        static final byte SEND_LIT_SEL1_F = (byte) 159;
        static final byte SEND_LIT_SEL2_0 = (byte) 160;
        static final byte SEND_LIT_SEL2_1 = (byte) 161;
        static final byte SEND_LIT_SEL2_2 = (byte) 162;
        static final byte SEND_LIT_SEL2_3 = (byte) 163;
        static final byte SEND_LIT_SEL2_4 = (byte) 164;
        static final byte SEND_LIT_SEL2_5 = (byte) 165;
        static final byte SEND_LIT_SEL2_6 = (byte) 166;
        static final byte SEND_LIT_SEL2_7 = (byte) 167;
        static final byte SEND_LIT_SEL2_8 = (byte) 168;
        static final byte SEND_LIT_SEL2_9 = (byte) 169;
        static final byte SEND_LIT_SEL2_A = (byte) 170;
        static final byte SEND_LIT_SEL2_B = (byte) 171;
        static final byte SEND_LIT_SEL2_C = (byte) 172;
        static final byte SEND_LIT_SEL2_D = (byte) 173;
        static final byte SEND_LIT_SEL2_E = (byte) 174;
        static final byte SEND_LIT_SEL2_F = (byte) 175;
        static final byte SHORT_UJUMP_0 = (byte) 176;
        static final byte SHORT_UJUMP_1 = (byte) 177;
        static final byte SHORT_UJUMP_2 = (byte) 178;
        static final byte SHORT_UJUMP_3 = (byte) 179;
        static final byte SHORT_UJUMP_4 = (byte) 180;
        static final byte SHORT_UJUMP_5 = (byte) 181;
        static final byte SHORT_UJUMP_6 = (byte) 182;
        static final byte SHORT_UJUMP_7 = (byte) 183;
        static final byte SHORT_CJUMP_TRUE_0 = (byte) 184;
        static final byte SHORT_CJUMP_TRUE_1 = (byte) 185;
        static final byte SHORT_CJUMP_TRUE_2 = (byte) 186;
        static final byte SHORT_CJUMP_TRUE_3 = (byte) 187;
        static final byte SHORT_CJUMP_TRUE_4 = (byte) 188;
        static final byte SHORT_CJUMP_TRUE_5 = (byte) 189;
        static final byte SHORT_CJUMP_TRUE_6 = (byte) 190;
        static final byte SHORT_CJUMP_TRUE_7 = (byte) 191;
        static final byte SHORT_CJUMP_FALSE_0 = (byte) 192;
        static final byte SHORT_CJUMP_FALSE_1 = (byte) 193;
        static final byte SHORT_CJUMP_FALSE_2 = (byte) 194;
        static final byte SHORT_CJUMP_FALSE_3 = (byte) 195;
        static final byte SHORT_CJUMP_FALSE_4 = (byte) 196;
        static final byte SHORT_CJUMP_FALSE_5 = (byte) 197;
        static final byte SHORT_CJUMP_FALSE_6 = (byte) 198;
        static final byte SHORT_CJUMP_FALSE_7 = (byte) 199;
        static final byte POP_INTO_RCVR_VAR_0 = (byte) 200;
        static final byte POP_INTO_RCVR_VAR_1 = (byte) 201;
        static final byte POP_INTO_RCVR_VAR_2 = (byte) 202;
        static final byte POP_INTO_RCVR_VAR_3 = (byte) 203;
        static final byte POP_INTO_RCVR_VAR_4 = (byte) 204;
        static final byte POP_INTO_RCVR_VAR_5 = (byte) 205;
        static final byte POP_INTO_RCVR_VAR_6 = (byte) 206;
        static final byte POP_INTO_RCVR_VAR_7 = (byte) 207;
        static final byte POP_INTO_TEMP_VAR_0 = (byte) 208;
        static final byte POP_INTO_TEMP_VAR_1 = (byte) 209;
        static final byte POP_INTO_TEMP_VAR_2 = (byte) 210;
        static final byte POP_INTO_TEMP_VAR_3 = (byte) 211;
        static final byte POP_INTO_TEMP_VAR_4 = (byte) 212;
        static final byte POP_INTO_TEMP_VAR_5 = (byte) 213;
        static final byte POP_INTO_TEMP_VAR_6 = (byte) 214;
        static final byte POP_INTO_TEMP_VAR_7 = (byte) 215;
        static final byte POP_STACK = (byte) 216;

        /* 2 byte bytecodes */
        static final byte EXT_A = (byte) 224;
        static final byte EXT_B = (byte) 225;
        static final byte EXT_PUSH_RECEIVER_VARIABLE = (byte) 226;
        static final byte EXT_PUSH_LITERAL_VARIABLE = (byte) 227;
        static final byte EXT_PUSH_LITERAL = (byte) 228;
        static final byte LONG_PUSH_TEMPORARY_VARIABLE = (byte) 229;
        static final byte PUSH_NEW_ARRAY = (byte) 231;
        static final byte EXT_PUSH_INTEGER = (byte) 232;
        static final byte EXT_PUSH_CHARACTER = (byte) 233;
        static final byte EXT_SEND = (byte) 234;
        static final byte EXT_SEND_SUPER = (byte) 235;
        static final byte EXT_UNCONDITIONAL_JUMP = (byte) 237;
        static final byte EXT_JUMP_IF_TRUE = (byte) 238;
        static final byte EXT_JUMP_IF_FALSE = (byte) 239;
        static final byte EXT_STORE_AND_POP_RECEIVER_VARIABLE = (byte) 240;
        static final byte EXT_STORE_AND_POP_LITERAL_VARIABLE = (byte) 241;
        static final byte LONG_STORE_AND_POP_TEMPORARY_VARIABLE = (byte) 242;
        static final byte EXT_STORE_RECEIVER_VARIABLE = (byte) 243;
        static final byte EXT_STORE_LITERAL_VARIABLE = (byte) 244;
        static final byte LONG_STORE_TEMPORARY_VARIABLE = (byte) 245;

        /* 3 byte bytecodes */
        static final byte CALL_PRIMITIVE = (byte) 248;
        static final byte EXT_PUSH_FULL_CLOSURE = (byte) 249;
        static final byte EXT_PUSH_CLOSURE = (byte) 250;
        static final byte PUSH_REMOTE_TEMP_LONG = (byte) 251;
        static final byte STORE_REMOTE_TEMP_LONG = (byte) 252;
        static final byte STORE_AND_POP_REMOTE_TEMP_LONG = (byte) 253;
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

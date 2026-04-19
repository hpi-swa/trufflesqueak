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
import com.oracle.truffle.api.HostCompilerDirectives;
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
            final int b = getUnsignedInt(bc, currentPC);
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
                    final int offset = calculateShortOffset(b);
                    if (offset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(pc, 1, offset)));
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
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(getByteExtended(bc, pc, extA))));
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
                    setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final boolean isDirected = extB >= 64;
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (extA << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    final Node node;
                    if (isDirected) {
                        node = DispatchDirectedSuperNaryNodeGen.create(selector);
                    } else {
                        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                        node = DispatchSuperNaryNodeGen.create(methodClass, selector);
                    }
                    setData(currentPC, insert(node));
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                    if (offset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(currentPC, 2, offset)));
                    }
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE: {
                    setData(currentPC, CountingConditionProfile.create());
                    pc++;
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE, BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_LITERAL_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
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
                    setData(currentPC, createBlock(code, pc, numArgs, numCopied, blockSize));
                    pc += blockSize;
                    extA = extB = 0;
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
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC, final int startSP) {
        assert isBlock == FrameAccess.hasClosure(frame);

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
                final int b = getUnsignedInt(bc, currentPC);
                CompilerAsserts.partialEvaluationConstant(b);
                switch (HostCompilerDirectives.markThreadedSwitch(b)) {
                    /* 1 byte bytecodes */
                    case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                        BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                        pushFollowed(frame, currentPC, sp++, uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
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
                        pushFollowed(frame, currentPC, sp++, FrameAccess.getStackValue(frame, b & 0xF));
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
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            /* Profiled version of LargeIntegers.add(image, lhs, rhs). */
                            final long r = lhs + rhs;
                            if (((lhs ^ r) & (rhs ^ r)) < 0) {
                                enter(currentPC, profile, BRANCH3);
                                result = LargeIntegers.addLarge(image, lhs, rhs);
                            } else {
                                enter(currentPC, profile, BRANCH4);
                                result = r;
                            }
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH5);
                            result = PrimSmallFloatAddNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SUBTRACT: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            /* Profiled version of LargeIntegers.subtract(image, lhs, rhs). */
                            final long r = lhs - rhs;
                            if (((lhs ^ rhs) & (lhs ^ r)) < 0) {
                                enter(currentPC, profile, BRANCH3);
                                result = LargeIntegers.subtractLarge(image, lhs, rhs);
                            } else {
                                enter(currentPC, profile, BRANCH4);
                                result = r;
                            }
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH5);
                            result = PrimSmallFloatSubtractNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimLessThanNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatLessThanNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimGreaterThanNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatGreaterThanNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimLessOrEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatLessOrEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimGreaterOrEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatGreaterOrEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimNotEqualNode.doLong(lhs, rhs);
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            enter(currentPC, profile, BRANCH3);
                            result = PrimSmallFloatNotEqualNode.doDouble(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_AND: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimBitAndNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_OR: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimBitOrNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                            pc = FrameAccess.internalizePC(frame, pc);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, uncheckedCast(getData(currentPC), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, uncheckedCast(getData(currentPC), SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, sp++, !uncheckedCast(getData(currentPC), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = popReceiver(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver));
                        pc = FrameAccess.internalizePC(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG, //
                        BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg));
                        pc = FrameAccess.internalizePC(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        final Object arg2 = pop(frame, --sp);
                        final Object arg1 = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg1, arg2));
                        pc = FrameAccess.internalizePC(frame, pc);
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
                            if (getData(currentPC) instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                                checkForInterruptsNode.execute(frame, pc, sp);
                            }
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(!condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), b & 7, pop(frame, --sp));
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7: {
                        FrameAccess.setStackValue(frame, b & 7, pop(frame, --sp));
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
                                        uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(bc, pc++, extA)));
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
                        pushFollowed(frame, currentPC, sp++, FrameAccess.getStackValue(frame, getUnsignedInt(bc, pc++)));
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
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = FrameAccess.internalizePC(frame, pc);
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
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        CompilerAsserts.partialEvaluationConstant(isDirected);
                        pushFollowed(frame, currentPC, sp++, sendSuper(frame, isDirected, currentPC, lookupClass, receiver, arguments));
                        pc = FrameAccess.internalizePC(frame, pc);
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
                            if (getData(currentPC) instanceof final CheckForInterruptsInLoopNode checkForInterruptsNode) {
                                checkForInterruptsNode.execute(frame, pc, sp);
                            }
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_JUMP_IF_TRUE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = getByteExtended(bc, pc++, extB);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_JUMP_IF_FALSE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = getByteExtended(bc, pc++, extB);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(!condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        FrameAccess.setStackValue(frame, getByte(bc, pc++), pop(frame, --sp));
                        break;
                    }
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), index, top(frame, sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        final int index = getByteExtended(bc, pc++, extA);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, top(frame, sp));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_STORE_TEMPORARY_VARIABLE: {
                        FrameAccess.setStackValue(frame, getByte(bc, pc++), top(frame, sp));
                        break;
                    }
                    /* 3 byte bytecodes */
                    case BC.CALL_PRIMITIVE: {
                        pc += 2;
                        if (getByte(bc, pc) == BC.LONG_STORE_TEMPORARY_VARIABLE) {
                            assert sp > 0;
                            FrameAccess.setStackValue(frame, sp - 1, getErrorObject());
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
                        push(frame, sp++, createBlockClosure(frame, uncheckedCast(getData(currentPC), CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, currentPC)));
                        final int blockSize = getByteExtended(bc, pc++, extB);
                        pc += blockSize;
                        extA = extB = 0;
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        pushFollowed(frame, currentPC, sp++,
                                        uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex));
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, top(frame, sp));
                        break;
                    }
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
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
                return uncheckedCast(getData(currentPC), DispatchDirectedSuperNaryNodeGen.class).execute(frame, lookupClass, receiver, arguments);
            } else {
                return uncheckedCast(getData(currentPC), DispatchSuperNaryNodeGen.class).execute(frame, receiver, arguments);
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

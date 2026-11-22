/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.EarlyInline;
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
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchNewspeakNode.DispatchNewspeakSendNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchNewspeakNode.NewspeakEnclosingObjectNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchNewspeakNodeFactory.DispatchNewspeakSendNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchNewspeakNodeFactory.NewspeakEnclosingObjectNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectedSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueWithArgNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.NewspeakLookup;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsInLoopNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitAndNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitOrNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimNotEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatNotEqualNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/**
 * Interpreter for the NewsqueakV4 bytecode set, the secondary bytecode set of Newspeak images. See
 * EncoderForNewsqueakV4 and StackInterpreter class>>initializeBytecodeTableForNewspeakV4.
 *
 * In addition to ordinary sends, the bytecode set provides sends with an absent receiver (implicit
 * receiver sends, self sends, outer sends, and super sends), see {@link NewspeakLookup}.
 */
public final class InterpreterNewsqueakV4Node extends AbstractInterpreterNode {
    public InterpreterNewsqueakV4Node(final SqueakImageContext image, final CompiledCodeObject code) {
        super(image, code);
    }

    public InterpreterNewsqueakV4Node(final InterpreterNewsqueakV4Node original) {
        super(original);
    }

    /** 224 11100000 aaaaaaaa Extend A (Ext A = Ext A prev * 256 + Ext A). */
    @EarlyInline
    static int computeExtA(final int extA, final int byteValue) {
        return (extA << 8) + byteValue;
    }

    /** 225 11100001 sbbbbbbb Extend B (Ext B = Ext B prev * 256 + Ext B), the first byte is signed. */
    @EarlyInline
    static int computeExtB(final int extB, final int numExtB, final int byteValue) {
        return numExtB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue;
    }

    public static int calculateLongExtendedOffset(final byte bytecode, final int extB) {
        return Byte.toUnsignedInt(bytecode) + (extB << 8);
    }

    @Override
    protected boolean processBytecode(final SqueakImageContext image, final int startPC, final int endPC) {
        final byte[] bc = code.getBytes();
        boolean hasPotentialContextIVStore = false;

        int pc = startPC;
        int extA = 0;
        int extB = 0;
        int numExtB = 0;

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
                    BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7, //
                    BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7, //
                    BC.PUSH_RECEIVER, BC.PUSH_CONSTANT_ZERO, BC.PUSH_CONSTANT_ONE, BC.RETURN_RECEIVER, BC.RETURN_TOP_FROM_METHOD, BC.DUPLICATE_TOP, BC.POP_STACK: {
                    break;
                }
                case BC.EXT_PUSH_PSEUDO_VARIABLE_OR_OUTER: {
                    if (extB < 0) {
                        setData(currentPC, insert(NewspeakEnclosingObjectNodeGen.create(code, -extB)));
                    } else if (extB > 3) {
                        throw unknownBytecode();
                    }
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.SEND_IMPLICIT_SEL0_0, BC.SEND_IMPLICIT_SEL0_1, BC.SEND_IMPLICIT_SEL0_2, BC.SEND_IMPLICIT_SEL0_3, BC.SEND_IMPLICIT_SEL0_4, BC.SEND_IMPLICIT_SEL0_5, BC.SEND_IMPLICIT_SEL0_6, BC.SEND_IMPLICIT_SEL0_7, //
                    BC.SEND_IMPLICIT_SEL0_8, BC.SEND_IMPLICIT_SEL0_9, BC.SEND_IMPLICIT_SEL0_A, BC.SEND_IMPLICIT_SEL0_B, BC.SEND_IMPLICIT_SEL0_C, BC.SEND_IMPLICIT_SEL0_D, BC.SEND_IMPLICIT_SEL0_E, BC.SEND_IMPLICIT_SEL0_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    setData(currentPC, insert(DispatchNewspeakSendNodeGen.create(code, selector, NewspeakLookup.KIND_IMPLICIT)));
                    break;
                }
                case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    if (b - BC.POP_INTO_RCVR_VAR_0 < CONTEXT.INST_SIZE) {
                        hasPotentialContextIVStore = true;
                    }
                    break;
                }
                case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7, //
                    BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                    setData(currentPC, CountingConditionProfile.create());
                    break;
                }
                case BC.EXT_RETURN_TOP_FROM_BLOCK: {
                    if (extA != 0) {
                        throw unknownBytecode(); /* Return from enclosing block N is not implemented. */
                    }
                    break;
                }
                case BC.EXT_NOP: {
                    extA = extB = numExtB = 0;
                    break;
                }
                /* 2 byte bytecodes */
                case BC.EXT_A: {
                    extA = computeExtA(extA, getUnsignedInt(bc, pc++));
                    break;
                }
                case BC.EXT_B: {
                    extB = computeExtB(extB, numExtB++, getUnsignedInt(bc, pc++));
                    break;
                }
                case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    pc++;
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(getUnsignedInt(bc, pc++) + (extA << 8))));
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_INTEGER: {
                    pc++;
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.LONG_PUSH_TEMPORARY_VARIABLE, BC.PUSH_NEW_ARRAY, BC.LONG_STORE_TEMPORARY_VARIABLE, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                    pc++;
                    break;
                }
                case BC.EXT_STORE_RECEIVER_VARIABLE, BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    if (getUnsignedInt(bc, pc++) + (extA << 8) < CONTEXT.INST_SIZE) {
                        hasPotentialContextIVStore = true;
                    }
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_STORE_LITERAL_VARIABLE, BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    pc++;
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    if (extB >= 64) {
                        setData(currentPC, insert(DispatchDirectedSuperNaryNodeGen.create(selector)));
                    } else {
                        setData(currentPC, insert(DispatchSuperNaryNodeGen.create(code, selector)));
                    }
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND_IMPLICIT: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    setData(currentPC, insert(DispatchNewspeakSendNodeGen.create(code, selector, NewspeakLookup.KIND_IMPLICIT)));
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND_DYNAMIC_SUPER: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    setData(currentPC, insert(DispatchNewspeakSendNodeGen.create(code, selector, NewspeakLookup.KIND_DYNAMIC_SUPER)));
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND_SELF: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    setData(currentPC, insert(DispatchNewspeakSendNodeGen.create(code, selector, NewspeakLookup.KIND_SELF)));
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                    if (offset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(pc + offset, currentPC)));
                    }
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_JUMP_IF_TRUE, BC.EXT_JUMP_IF_FALSE: {
                    setData(currentPC, CountingConditionProfile.create());
                    pc++;
                    extA = extB = numExtB = 0;
                    break;
                }
                /* 3 byte bytecodes */
                case BC.CALL_PRIMITIVE: {
                    pc += 2;
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
                case BC.EXT_PUSH_CLOSURE: {
                    final int byteA = getUnsignedInt(bc, pc++);
                    final int numArgs = (byteA & 0x07) + (extA & 0x0F) * 8;
                    final int numCopied = (byteA >> 3 & 0x7) + (extA >> 4) * 8;
                    final int blockSize = getUnsignedInt(bc, pc++) + (extB << 8);
                    setData(currentPC, createBlock(code, pc, numArgs, numCopied, blockSize));
                    pc += blockSize;
                    extA = extB = numExtB = 0;
                    break;
                }
                case BC.EXT_SEND_OUTER: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral((getUnsignedInt(bc, pc++) >> 3) + (extA << 5));
                    final int depth = getUnsignedInt(bc, pc++);
                    setData(currentPC, insert(DispatchNewspeakSendNodeGen.create(code, selector, depth)));
                    extA = extB = numExtB = 0;
                    break;
                }
                /* Special selector sends and sends with 0, 1, and 2 arguments (same as V3PlusClosures). */
                case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y: {
                    setData(currentPC, insert(Dispatch0NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD))));
                    break;
                }
                case BC.BYTECODE_PRIM_CLASS: {
                    setData(currentPC, insert(SqueakObjectClassNodeGen.create()));
                    break;
                }
                case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, //
                    BC.BYTECODE_PRIM_EQUAL, BC.BYTECODE_PRIM_NOT_EQUAL, BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_DIV, //
                    BC.BYTECODE_PRIM_BIT_AND, BC.BYTECODE_PRIM_BIT_OR, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                    setData(currentPC, insert(Dispatch1NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD))));
                    break;
                }
                case BC.BYTECODE_PRIM_IDENTICAL, BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                    setData(currentPC, insert(SqueakObjectIdentityNodeGen.create()));
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE: {
                    setData(currentPC, insert(DispatchValueNodeGen.create()));
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE_WITH_ARG: {
                    setData(currentPC, insert(DispatchValueWithArgNodeGen.create()));
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
                default: {
                    throw unknownBytecode(currentPC, b);
                }
            }
        }
        return hasPotentialContextIVStore;
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC, final int startSP) {
        assert isBlock == FrameAccess.hasClosure(frame);

        ensureContextReceiverIsNotActive(frame, startPC, startSP);

        final SqueakImageContext image = getContext();
        final byte[] bc = ACCESS.uncheckedCast(code.getBytes(), byte[].class);

        int pc = CompilerDirectives.mergeExplodeKey(startPC);
        int sp = startSP;
        /* Extensions are only set between an extension bytecode and the bytecode it extends. */
        int extA = 0;
        int extB = 0;
        int numExtB = 0;

        int counter = 0;
        final LoopCounter loopCounter = CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? new LoopCounter() : null;

        try {
            while (true) {
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
                        pushFollowed(frame, currentPC, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
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
                    case BC.EXT_PUSH_PSEUDO_VARIABLE_OR_OUTER: {
                        /* 77 Push false [* 1:true, 2:nil, 3:thisContext, ..., -N: pushExplicitOuter: N, N = Extend B] */
                        if (extB == 0) {
                            push(frame, sp++, BooleanObject.FALSE);
                        } else if (extB == 1) {
                            push(frame, sp++, BooleanObject.TRUE);
                        } else if (extB == 2) {
                            push(frame, sp++, NilObject.SINGLETON);
                        } else if (extB == 3) {
                            push(frame, sp++, getOrCreateContext(frame, currentPC));
                        } else {
                            assert extB < 0;
                            final Object methodReceiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(FrameAccess.getReceiver(frame));
                            pushFollowed(frame, currentPC, sp++, ACCESS.uncheckedCast(getData(currentPC), NewspeakEnclosingObjectNode.class).execute(methodReceiver));
                        }
                        extA = extB = numExtB = 0;
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
                    case BC.SEND_IMPLICIT_SEL0_0, BC.SEND_IMPLICIT_SEL0_1, BC.SEND_IMPLICIT_SEL0_2, BC.SEND_IMPLICIT_SEL0_3, BC.SEND_IMPLICIT_SEL0_4, BC.SEND_IMPLICIT_SEL0_5, BC.SEND_IMPLICIT_SEL0_6, BC.SEND_IMPLICIT_SEL0_7, //
                        BC.SEND_IMPLICIT_SEL0_8, BC.SEND_IMPLICIT_SEL0_9, BC.SEND_IMPLICIT_SEL0_A, BC.SEND_IMPLICIT_SEL0_B, BC.SEND_IMPLICIT_SEL0_C, BC.SEND_IMPLICIT_SEL0_D, BC.SEND_IMPLICIT_SEL0_E, BC.SEND_IMPLICIT_SEL0_F: {
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNewspeak(frame, currentPC, ArrayUtils.EMPTY_ARRAY));
                        break;
                    }
                    case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), b & 7, pop(frame, --sp));
                        break;
                    }
                    case BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7: {
                        FrameAccess.setStackValue(frame, b & 7, pop(frame, --sp));
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        pc += calculateShortOffset(b);
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(!condition)) {
                                pc += calculateShortOffset(b);
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.RETURN_RECEIVER: {
                        final Object result = handleReturn(frame, currentPC, pc, sp, FrameAccess.getReceiver(frame),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        if (isValidPC(pc)) {
                            return result;
                        }
                        break;
                    }
                    case BC.RETURN_TOP_FROM_METHOD: {
                        final Object result = handleReturn(frame, currentPC, pc, sp - 1, top(frame, sp),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        if (isValidPC(pc)) {
                            return result;
                        }
                        break;
                    }
                    case BC.EXT_RETURN_TOP_FROM_BLOCK: {
                        final Object result = handleReturnFromBlock(frame, currentPC, pc, sp - 1, top(frame, sp),
                                        CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? loopCounter.value : counter);
                        if (isValidPC(pc)) {
                            return result;
                        }
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        pushFollowed(frame, currentPC, sp, top(frame, sp));
                        sp++;
                        break;
                    }
                    case BC.POP_STACK: {
                        clearStackSlot(frame, --sp);
                        break;
                    }
                    case BC.EXT_NOP: {
                        extA = extB = numExtB = 0;
                        break;
                    }
                    /* 2 byte bytecodes */
                    case BC.EXT_A: {
                        extA = computeExtA(extA, getUnsignedInt(bc, pc++));
                        break;
                    }
                    case BC.EXT_B: {
                        extB = computeExtB(extB, numExtB++, getUnsignedInt(bc, pc++));
                        break;
                    }
                    case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        pushFollowed(frame, currentPC, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), index));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        push(frame, sp++, readLiteralVariable(currentPC, index));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        push(frame, sp++, getAndResolveLiteral(currentPC, index));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_PUSH_INTEGER: {
                        push(frame, sp++, (long) (getUnsignedInt(bc, pc++) + (extB << 8)));
                        extA = extB = numExtB = 0;
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
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        doStoreIntoReceiverVariable(frame, currentPC, index, top(frame, sp));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, top(frame, sp));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.LONG_STORE_TEMPORARY_VARIABLE: {
                        FrameAccess.setStackValue(frame, getUnsignedInt(bc, pc++), top(frame, sp));
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_RECEIVER_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        doStoreIntoReceiverVariable(frame, currentPC, index, pop(frame, --sp));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        final int index = getUnsignedInt(bc, pc++) + (extA << 8);
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, index), ASSOCIATION.VALUE, pop(frame, --sp));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        FrameAccess.setStackValue(frame, getUnsignedInt(bc, pc++), pop(frame, --sp));
                        break;
                    }
                    case BC.EXT_SEND: {
                        final int numArgs = (getUnsignedInt(bc, pc++) & 7) + (extB << 3);
                        CompilerAsserts.partialEvaluationConstant(numArgs);
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_SEND_SUPER: {
                        final boolean isDirected = extB >= 64;
                        final int numArgs = (getUnsignedInt(bc, pc++) & 7) + ((isDirected ? extB & 63 : extB) << 3);
                        CompilerAsserts.partialEvaluationConstant(numArgs);
                        final ClassObject lookupClass = isDirected ? ((ClassObject) pop(frame, --sp)).getResolvedSuperclass() : null;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(pop(frame, --sp));
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        pushFollowed(frame, currentPC, sp++, sendSuper(frame, isDirected, currentPC, lookupClass, receiver, arguments));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_SEND_IMPLICIT, BC.EXT_SEND_DYNAMIC_SUPER, BC.EXT_SEND_SELF: {
                        final int numArgs = (getUnsignedInt(bc, pc++) & 7) + (extB << 3);
                        CompilerAsserts.partialEvaluationConstant(numArgs);
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNewspeak(frame, currentPC, arguments));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_UNCONDITIONAL_JUMP: {
                        final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                        extA = extB = numExtB = 0;
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
                                    if (CompilerDirectives.inInterpreter() && BytecodeOSRNode.pollOSRBackEdge(this, counter)) {
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
                    case BC.EXT_JUMP_IF_TRUE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                        extA = extB = numExtB = 0;
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(condition)) {
                                pc += offset;
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.EXT_JUMP_IF_FALSE: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = calculateLongExtendedOffset(getByte(bc, pc++), extB);
                        extA = extB = numExtB = 0;
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(!condition)) {
                                pc += offset;
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    /* 3 byte bytecodes */
                    case BC.CALL_PRIMITIVE: {
                        pc += 2;
                        if (getUnsignedInt(bc, pc) == BC.LONG_STORE_TEMPORARY_VARIABLE) {
                            assert sp > 0;
                            FrameAccess.setStackValue(frame, sp - 1, getErrorObject());
                        }
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        pushFollowed(frame, currentPC, sp++,
                                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex));
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, top(frame, sp));
                        break;
                    }
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
                        break;
                    }
                    case BC.EXT_PUSH_CLOSURE: {
                        final int numCopied = (getUnsignedInt(bc, pc++) >> 3 & 0x7) + (extA >> 4) * 8;
                        final int blockSize = getUnsignedInt(bc, pc++) + (extB << 8);
                        CompilerAsserts.partialEvaluationConstant(numCopied);
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        push(frame, sp++, createBlockClosure(frame, ACCESS.uncheckedCast(getData(currentPC), CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, currentPC)));
                        pc += blockSize;
                        extA = extB = numExtB = 0;
                        break;
                    }
                    case BC.EXT_SEND_OUTER: {
                        final int numArgs = (getUnsignedInt(bc, pc++) & 7) + (extB << 3);
                        pc++; /* depth */
                        CompilerAsserts.partialEvaluationConstant(numArgs);
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNewspeak(frame, currentPC, arguments));
                        extA = extB = numExtB = 0;
                        break;
                    }
                    /* Special selector sends and sends with 0, 1, and 2 arguments (same as V3PlusClosures). */
                    case BC.BYTECODE_PRIM_ADD: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs && Double.isFinite(lhs + rhs)) {
                            enter(currentPC, profile, BRANCH5);
                            result = lhs + rhs;
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_SUBTRACT: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs && Double.isFinite(lhs - rhs)) {
                            enter(currentPC, profile, BRANCH5);
                            result = lhs - rhs;
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
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
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_AND: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimBitAndNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_BIT_OR: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        final byte profile = getProfile(currentPC);
                        final Object result;
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            enter(currentPC, profile, BRANCH2);
                            result = PrimBitOrNode.doLong(lhs, rhs);
                        } else {
                            enter(currentPC, profile, BRANCH1);
                            FrameAccess.externalizePCAndSP(frame, pc, sp);
                            result = send(frame, currentPC, receiver, arg);
                        }
                        push(frame, sp++, result);
                        break;
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        push(frame, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        final Object receiver = pop(frame, --sp);
                        push(frame, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        push(frame, sp++, !ACCESS.uncheckedCast(getData(currentPC), SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_VALUE: {
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendValue(frame, currentPC, receiver));
                        break;
                    }
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver));
                        break;
                    }
                    case BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG, //
                        BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_VALUE_WITH_ARG: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendValueWithArg(frame, currentPC, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        final Object arg2 = pop(frame, --sp);
                        final Object arg1 = pop(frame, --sp);
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg1, arg2));
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
    }

    /** Performs a send with absent receiver (implicit receiver, self, outer, or super send). */
    private Object sendNewspeak(final VirtualFrame frame, final int currentPC, final Object[] arguments) {
        try {
            /* The receiver of the method may have been forwarded by a become in the meantime. */
            final Object methodReceiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(FrameAccess.getReceiver(frame));
            return ACCESS.uncheckedCast(getData(currentPC), DispatchNewspeakSendNode.class).execute(frame, methodReceiver, arguments);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
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

    @EarlyInline
    private void doStoreIntoReceiverVariable(final VirtualFrame frame, final int pc, final int index, final Object value) {
        final Object receiver = FrameAccess.getReceiver(frame);
        ACCESS.uncheckedCast(getData(pc), SqueakObjectAtPut0Node.class).execute(this, receiver, index, value);
    }

    @Override
    protected boolean isFastPathedMessageSend(final int bytecode) {
        return switch (bytecode) {
            case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, //
                BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, BC.BYTECODE_PRIM_EQUAL, BC.BYTECODE_PRIM_NOT_EQUAL, //
                BC.BYTECODE_PRIM_BIT_AND, BC.BYTECODE_PRIM_BIT_OR -> true;
            default -> false;
        };
    }

    static final class BC {
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
        static final int EXT_PUSH_PSEUDO_VARIABLE_OR_OUTER = 77;
        static final int PUSH_CONSTANT_ZERO = 78;
        static final int PUSH_CONSTANT_ONE = 79;

        static final int BYTECODE_PRIM_ADD = 80;
        static final int BYTECODE_PRIM_SUBTRACT = 81;
        static final int BYTECODE_PRIM_LESS_THAN = 82;
        static final int BYTECODE_PRIM_GREATER_THAN = 83;
        static final int BYTECODE_PRIM_LESS_OR_EQUAL = 84;
        static final int BYTECODE_PRIM_GREATER_OR_EQUAL = 85;
        static final int BYTECODE_PRIM_EQUAL = 86;
        static final int BYTECODE_PRIM_NOT_EQUAL = 87;
        static final int BYTECODE_PRIM_MULTIPLY = 88;
        static final int BYTECODE_PRIM_DIVIDE = 89;
        static final int BYTECODE_PRIM_MOD = 90;
        static final int BYTECODE_PRIM_MAKE_POINT = 91;
        static final int BYTECODE_PRIM_BIT_SHIFT = 92;
        static final int BYTECODE_PRIM_DIV = 93;
        static final int BYTECODE_PRIM_BIT_AND = 94;
        static final int BYTECODE_PRIM_BIT_OR = 95;
        static final int BYTECODE_PRIM_AT = 96;
        static final int BYTECODE_PRIM_AT_PUT = 97;
        static final int BYTECODE_PRIM_SIZE = 98;
        static final int BYTECODE_PRIM_NEXT = 99;
        static final int BYTECODE_PRIM_NEXT_PUT = 100;
        static final int BYTECODE_PRIM_AT_END = 101;
        static final int BYTECODE_PRIM_IDENTICAL = 102;
        static final int BYTECODE_PRIM_CLASS = 103;
        static final int BYTECODE_PRIM_NOT_IDENTICAL = 104;
        static final int BYTECODE_PRIM_VALUE = 105;
        static final int BYTECODE_PRIM_VALUE_WITH_ARG = 106;
        static final int BYTECODE_PRIM_DO = 107;
        static final int BYTECODE_PRIM_NEW = 108;
        static final int BYTECODE_PRIM_NEW_WITH_ARG = 109;
        static final int BYTECODE_PRIM_POINT_X = 110;
        static final int BYTECODE_PRIM_POINT_Y = 111;

        static final int SEND_LIT_SEL0_0 = 112;
        static final int SEND_LIT_SEL0_1 = 113;
        static final int SEND_LIT_SEL0_2 = 114;
        static final int SEND_LIT_SEL0_3 = 115;
        static final int SEND_LIT_SEL0_4 = 116;
        static final int SEND_LIT_SEL0_5 = 117;
        static final int SEND_LIT_SEL0_6 = 118;
        static final int SEND_LIT_SEL0_7 = 119;
        static final int SEND_LIT_SEL0_8 = 120;
        static final int SEND_LIT_SEL0_9 = 121;
        static final int SEND_LIT_SEL0_A = 122;
        static final int SEND_LIT_SEL0_B = 123;
        static final int SEND_LIT_SEL0_C = 124;
        static final int SEND_LIT_SEL0_D = 125;
        static final int SEND_LIT_SEL0_E = 126;
        static final int SEND_LIT_SEL0_F = 127;
        static final int SEND_LIT_SEL1_0 = 128;
        static final int SEND_LIT_SEL1_1 = 129;
        static final int SEND_LIT_SEL1_2 = 130;
        static final int SEND_LIT_SEL1_3 = 131;
        static final int SEND_LIT_SEL1_4 = 132;
        static final int SEND_LIT_SEL1_5 = 133;
        static final int SEND_LIT_SEL1_6 = 134;
        static final int SEND_LIT_SEL1_7 = 135;
        static final int SEND_LIT_SEL1_8 = 136;
        static final int SEND_LIT_SEL1_9 = 137;
        static final int SEND_LIT_SEL1_A = 138;
        static final int SEND_LIT_SEL1_B = 139;
        static final int SEND_LIT_SEL1_C = 140;
        static final int SEND_LIT_SEL1_D = 141;
        static final int SEND_LIT_SEL1_E = 142;
        static final int SEND_LIT_SEL1_F = 143;
        static final int SEND_LIT_SEL2_0 = 144;
        static final int SEND_LIT_SEL2_1 = 145;
        static final int SEND_LIT_SEL2_2 = 146;
        static final int SEND_LIT_SEL2_3 = 147;
        static final int SEND_LIT_SEL2_4 = 148;
        static final int SEND_LIT_SEL2_5 = 149;
        static final int SEND_LIT_SEL2_6 = 150;
        static final int SEND_LIT_SEL2_7 = 151;
        static final int SEND_LIT_SEL2_8 = 152;
        static final int SEND_LIT_SEL2_9 = 153;
        static final int SEND_LIT_SEL2_A = 154;
        static final int SEND_LIT_SEL2_B = 155;
        static final int SEND_LIT_SEL2_C = 156;
        static final int SEND_LIT_SEL2_D = 157;
        static final int SEND_LIT_SEL2_E = 158;
        static final int SEND_LIT_SEL2_F = 159;
        static final int SEND_IMPLICIT_SEL0_0 = 160;
        static final int SEND_IMPLICIT_SEL0_1 = 161;
        static final int SEND_IMPLICIT_SEL0_2 = 162;
        static final int SEND_IMPLICIT_SEL0_3 = 163;
        static final int SEND_IMPLICIT_SEL0_4 = 164;
        static final int SEND_IMPLICIT_SEL0_5 = 165;
        static final int SEND_IMPLICIT_SEL0_6 = 166;
        static final int SEND_IMPLICIT_SEL0_7 = 167;
        static final int SEND_IMPLICIT_SEL0_8 = 168;
        static final int SEND_IMPLICIT_SEL0_9 = 169;
        static final int SEND_IMPLICIT_SEL0_A = 170;
        static final int SEND_IMPLICIT_SEL0_B = 171;
        static final int SEND_IMPLICIT_SEL0_C = 172;
        static final int SEND_IMPLICIT_SEL0_D = 173;
        static final int SEND_IMPLICIT_SEL0_E = 174;
        static final int SEND_IMPLICIT_SEL0_F = 175;

        static final int POP_INTO_RCVR_VAR_0 = 176;
        static final int POP_INTO_RCVR_VAR_1 = 177;
        static final int POP_INTO_RCVR_VAR_2 = 178;
        static final int POP_INTO_RCVR_VAR_3 = 179;
        static final int POP_INTO_RCVR_VAR_4 = 180;
        static final int POP_INTO_RCVR_VAR_5 = 181;
        static final int POP_INTO_RCVR_VAR_6 = 182;
        static final int POP_INTO_RCVR_VAR_7 = 183;
        static final int POP_INTO_TEMP_VAR_0 = 184;
        static final int POP_INTO_TEMP_VAR_1 = 185;
        static final int POP_INTO_TEMP_VAR_2 = 186;
        static final int POP_INTO_TEMP_VAR_3 = 187;
        static final int POP_INTO_TEMP_VAR_4 = 188;
        static final int POP_INTO_TEMP_VAR_5 = 189;
        static final int POP_INTO_TEMP_VAR_6 = 190;
        static final int POP_INTO_TEMP_VAR_7 = 191;
        static final int SHORT_UJUMP_0 = 192;
        static final int SHORT_UJUMP_1 = 193;
        static final int SHORT_UJUMP_2 = 194;
        static final int SHORT_UJUMP_3 = 195;
        static final int SHORT_UJUMP_4 = 196;
        static final int SHORT_UJUMP_5 = 197;
        static final int SHORT_UJUMP_6 = 198;
        static final int SHORT_UJUMP_7 = 199;
        static final int SHORT_CJUMP_TRUE_0 = 200;
        static final int SHORT_CJUMP_TRUE_1 = 201;
        static final int SHORT_CJUMP_TRUE_2 = 202;
        static final int SHORT_CJUMP_TRUE_3 = 203;
        static final int SHORT_CJUMP_TRUE_4 = 204;
        static final int SHORT_CJUMP_TRUE_5 = 205;
        static final int SHORT_CJUMP_TRUE_6 = 206;
        static final int SHORT_CJUMP_TRUE_7 = 207;
        static final int SHORT_CJUMP_FALSE_0 = 208;
        static final int SHORT_CJUMP_FALSE_1 = 209;
        static final int SHORT_CJUMP_FALSE_2 = 210;
        static final int SHORT_CJUMP_FALSE_3 = 211;
        static final int SHORT_CJUMP_FALSE_4 = 212;
        static final int SHORT_CJUMP_FALSE_5 = 213;
        static final int SHORT_CJUMP_FALSE_6 = 214;
        static final int SHORT_CJUMP_FALSE_7 = 215;

        static final int RETURN_RECEIVER = 216;
        static final int RETURN_TOP_FROM_METHOD = 217;
        static final int EXT_RETURN_TOP_FROM_BLOCK = 218;
        static final int DUPLICATE_TOP = 219;
        static final int POP_STACK = 220;
        static final int EXT_NOP = 221;

        /* 2 byte bytecodes */
        static final int EXT_A = 224;
        static final int EXT_B = 225;
        static final int EXT_PUSH_RECEIVER_VARIABLE = 226;
        static final int EXT_PUSH_LITERAL_VARIABLE = 227;
        static final int EXT_PUSH_LITERAL = 228;
        static final int EXT_PUSH_INTEGER = 229;
        static final int LONG_PUSH_TEMPORARY_VARIABLE = 230;
        static final int PUSH_NEW_ARRAY = 231;
        static final int EXT_STORE_RECEIVER_VARIABLE = 232;
        static final int EXT_STORE_LITERAL_VARIABLE = 233;
        static final int LONG_STORE_TEMPORARY_VARIABLE = 234;
        static final int EXT_STORE_AND_POP_RECEIVER_VARIABLE = 235;
        static final int EXT_STORE_AND_POP_LITERAL_VARIABLE = 236;
        static final int LONG_STORE_AND_POP_TEMPORARY_VARIABLE = 237;
        static final int EXT_SEND = 238;
        static final int EXT_SEND_SUPER = 239;
        static final int EXT_SEND_IMPLICIT = 240;
        static final int EXT_SEND_DYNAMIC_SUPER = 241;
        static final int EXT_UNCONDITIONAL_JUMP = 242;
        static final int EXT_JUMP_IF_TRUE = 243;
        static final int EXT_JUMP_IF_FALSE = 244;
        static final int EXT_SEND_SELF = 245;

        /* 3 byte bytecodes */
        static final int CALL_PRIMITIVE = 249;
        static final int PUSH_REMOTE_TEMP_LONG = 250;
        static final int STORE_REMOTE_TEMP_LONG = 251;
        static final int STORE_AND_POP_REMOTE_TEMP_LONG = 252;
        static final int EXT_PUSH_CLOSURE = 253;
        static final int EXT_SEND_OUTER = 254;
    }

    /*
     * Copying
     */

    @Override
    public Node copy() {
        return new InterpreterNewsqueakV4Node(this);
    }

    @Override
    public Node deepCopy() {
        return new InterpreterNewsqueakV4Node(this);
    }
}

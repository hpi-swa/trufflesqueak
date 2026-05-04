/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

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
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
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

public final class InterpreterV3PlusClosuresNode extends AbstractInterpreterNode {
    public InterpreterV3PlusClosuresNode(final CompiledCodeObject code) {
        super(code);
    }

    public InterpreterV3PlusClosuresNode(final InterpreterV3PlusClosuresNode original) {
        super(original);
    }

    @Override
    protected void processBytecode(final int startPC, final int endPC) {
        final byte[] bc = code.getBytes();
        final SqueakImageContext image = SqueakImageContext.getSlow();

        int pc = startPC;

        while (pc < endPC) {
            final int currentPC = pc++;
            final int b = getUnsignedInt(bc, currentPC);
            switch (b) {
                case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    break;
                }
                case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                    BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, BC.PUSH_TEMP_VAR_C, BC.PUSH_TEMP_VAR_D, BC.PUSH_TEMP_VAR_E, BC.PUSH_TEMP_VAR_F, //
                    BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                    BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                    BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                    BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F, //
                    BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7, //
                    BC.RETURN_RECEIVER, BC.RETURN_TRUE, BC.RETURN_FALSE, BC.RETURN_NIL, BC.RETURN_TOP_FROM_METHOD, BC.RETURN_TOP_FROM_BLOCK, //
                    BC.PUSH_RECEIVER, BC.PUSH_CONSTANT_TRUE, BC.PUSH_CONSTANT_FALSE, BC.PUSH_CONSTANT_NIL, BC.PUSH_CONSTANT_MINUS_ONE, BC.PUSH_CONSTANT_ZERO, BC.PUSH_CONSTANT_ONE, BC.PUSH_CONSTANT_TWO, //
                    BC.POP_STACK, BC.DUPLICATE_TOP, BC.PUSH_ACTIVE_CONTEXT: {
                    break;
                }
                case BC.PUSH_LIT_VAR_00, BC.PUSH_LIT_VAR_01, BC.PUSH_LIT_VAR_02, BC.PUSH_LIT_VAR_03, BC.PUSH_LIT_VAR_04, BC.PUSH_LIT_VAR_05, BC.PUSH_LIT_VAR_06, BC.PUSH_LIT_VAR_07, //
                    BC.PUSH_LIT_VAR_08, BC.PUSH_LIT_VAR_09, BC.PUSH_LIT_VAR_0A, BC.PUSH_LIT_VAR_0B, BC.PUSH_LIT_VAR_0C, BC.PUSH_LIT_VAR_0D, BC.PUSH_LIT_VAR_0E, BC.PUSH_LIT_VAR_0F, //
                    BC.PUSH_LIT_VAR_10, BC.PUSH_LIT_VAR_11, BC.PUSH_LIT_VAR_12, BC.PUSH_LIT_VAR_13, BC.PUSH_LIT_VAR_14, BC.PUSH_LIT_VAR_15, BC.PUSH_LIT_VAR_16, BC.PUSH_LIT_VAR_17, //
                    BC.PUSH_LIT_VAR_18, BC.PUSH_LIT_VAR_19, BC.PUSH_LIT_VAR_1A, BC.PUSH_LIT_VAR_1B, BC.PUSH_LIT_VAR_1C, BC.PUSH_LIT_VAR_1D, BC.PUSH_LIT_VAR_1E, BC.PUSH_LIT_VAR_1F: {
                    setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(b & 0x1F)));
                    break;
                }
                case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    break;
                }
                case BC.EXTENDED_PUSH: {
                    final byte descriptor = getByte(bc, pc++);
                    final int variableIndex = variableIndex(descriptor);
                    switch (variableType(descriptor)) {
                        case 0: {
                            setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                            break;
                        }
                        case 1, 2: {
                            break;
                        }
                        case 3: {
                            setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(variableIndex)));
                            break;
                        }
                    }
                    break;
                }
                case BC.EXTENDED_STORE, BC.EXTENDED_POP: {
                    final byte descriptor = getByte(bc, pc++);
                    switch (variableType(descriptor)) {
                        case 0, 3: {
                            setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                            break;
                        }
                        case 1: {
                            break;
                        }
                        case 2: {
                            throw unknownBytecode();
                        }
                    }
                    break;
                }
                case BC.SINGLE_EXTENDED_SEND: {
                    final NativeObject selector = (NativeObject) code.getLiteral(getByte(bc, pc++) & 0x1F);
                    setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                    break;
                }
                case BC.DOUBLE_EXTENDED_DO_ANYTHING: {
                    final int byte2 = getUnsignedInt(bc, pc++);
                    final int byte3 = getUnsignedInt(bc, pc++);
                    switch (byte2 >> 5) {
                        case 0: {
                            final NativeObject selector = (NativeObject) code.getLiteral(byte3);
                            setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                            break;
                        }
                        case 1: {
                            final NativeObject selector = (NativeObject) code.getLiteral(byte3);
                            final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                            setData(currentPC, insert(DispatchSuperNaryNodeGen.create(methodClass, selector)));
                            break;
                        }
                        case 2: {
                            setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                            break;
                        }
                        case 3: {
                            break;
                        }
                        case 4: {
                            setData(currentPC, getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(byte3)));
                            break;
                        }
                        case 5, 6, 7: {
                            setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                            break;
                        }
                        default: {
                            throw unknownBytecode();
                        }
                    }
                    break;
                }
                case BC.SINGLE_EXTENDED_SUPER: {
                    final NativeObject selector = (NativeObject) code.getLiteral(getByte(bc, pc++) & 0x1F);
                    final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                    setData(currentPC, insert(DispatchSuperNaryNodeGen.create(methodClass, selector)));
                    break;
                }
                case BC.SECOND_EXTENDED_SEND: {
                    final NativeObject selector = (NativeObject) code.getLiteral(getByte(bc, pc++) & 0x3F);
                    setData(currentPC, insert(DispatchNaryNodeGen.create(selector)));
                    break;
                }
                case BC.PUSH_NEW_ARRAY: {
                    pc++;
                    break;
                }
                case BC.CALL_PRIMITIVE: {
                    pc += 2;
                    break;
                }
                case BC.PUSH_REMOTE_TEMP_LONG: {
                    setData(currentPC, insert(SqueakObjectAt0NodeGen.create()));
                    pc += 2;
                    break;
                }
                case BC.STORE_REMOTE_TEMP_LONG, BC.POP_INTO_REMOTE_TEMP_LONG: {
                    setData(currentPC, insert(SqueakObjectAtPut0NodeGen.create()));
                    pc += 2;
                    break;
                }
                case BC.PUSH_CLOSURE_COPY_COPIED_VALUES: {
                    final int numArgsNumCopied = getUnsignedInt(bc, pc++);
                    final int numArgs = numArgsNumCopied & 0xF;
                    final int numCopied = numArgsNumCopied >> 4 & 0xF;
                    final int blockSize = getBlockSize(bc, pc++, pc++);
                    setData(currentPC, createBlock(code, pc, numArgs, numCopied, blockSize));
                    pc += blockSize;
                    break;
                }
                case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                    final int offset = calculateShortOffset(b);
                    if (offset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(pc, 1, offset)));
                    }
                    break;
                }
                case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                    setData(currentPC, CountingConditionProfile.create());
                    break;
                }

                case BC.LONG_UJUMP_0, BC.LONG_UJUMP_1, BC.LONG_UJUMP_2, BC.LONG_UJUMP_3, BC.LONG_UJUMP_4, BC.LONG_UJUMP_5, BC.LONG_UJUMP_6, BC.LONG_UJUMP_7: {
                    final int offset = ((b & 7) - 4 << 8) + getUnsignedInt(bc, pc++);
                    if (offset < 0) {
                        setData(currentPC, insert(createCheckForInterruptsInLoopNode(pc, 1, offset)));
                    }
                    break;
                }
                case BC.LONG_CJUMP_TRUE_0, BC.LONG_CJUMP_TRUE_1, BC.LONG_CJUMP_TRUE_2, BC.LONG_CJUMP_TRUE_3, //
                    BC.LONG_CJUMP_FALSE_0, BC.LONG_CJUMP_FALSE_1, BC.LONG_CJUMP_FALSE_2, BC.LONG_CJUMP_FALSE_3: {
                    setData(currentPC, CountingConditionProfile.create());
                    pc++;
                    break;
                }
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
        final byte[] bc = ACCESS.uncheckedCast(code.getBytes(), byte[].class);

        int pc = startPC;
        int sp = startSP;

        int counter = 0;
        final LoopCounter loopCounter = CompilerDirectives.inCompiledCode() && CompilerDirectives.hasNextTier() ? new LoopCounter() : null;

        Object returnValue = null;
        try {
            while (pc != LOCAL_RETURN_PC) {
                CompilerAsserts.partialEvaluationConstant(pc);
                CompilerAsserts.partialEvaluationConstant(sp);
                final int currentPC = pc++;
                final int b = getUnsignedInt(bc, currentPC);
                CompilerAsserts.partialEvaluationConstant(b);
                switch (HostCompilerDirectives.markThreadedSwitch(b)) {
                    case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                        BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                        FrameAccess.externalizePCAndSP(frame, pc, sp); // for ContextObject access
                        pushFollowed(frame, currentPC, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                        BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, BC.PUSH_TEMP_VAR_C, BC.PUSH_TEMP_VAR_D, BC.PUSH_TEMP_VAR_E, BC.PUSH_TEMP_VAR_F: {
                        pushFollowed(frame, currentPC, sp++, FrameAccess.getStackValue(frame, b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                        BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                        BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                        BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F: {
                        push(frame, sp++, getAndResolveLiteral(currentPC, b & 0x1F));
                        break;
                    }
                    case BC.PUSH_LIT_VAR_00, BC.PUSH_LIT_VAR_01, BC.PUSH_LIT_VAR_02, BC.PUSH_LIT_VAR_03, BC.PUSH_LIT_VAR_04, BC.PUSH_LIT_VAR_05, BC.PUSH_LIT_VAR_06, BC.PUSH_LIT_VAR_07, //
                        BC.PUSH_LIT_VAR_08, BC.PUSH_LIT_VAR_09, BC.PUSH_LIT_VAR_0A, BC.PUSH_LIT_VAR_0B, BC.PUSH_LIT_VAR_0C, BC.PUSH_LIT_VAR_0D, BC.PUSH_LIT_VAR_0E, BC.PUSH_LIT_VAR_0F, //
                        BC.PUSH_LIT_VAR_10, BC.PUSH_LIT_VAR_11, BC.PUSH_LIT_VAR_12, BC.PUSH_LIT_VAR_13, BC.PUSH_LIT_VAR_14, BC.PUSH_LIT_VAR_15, BC.PUSH_LIT_VAR_16, BC.PUSH_LIT_VAR_17, //
                        BC.PUSH_LIT_VAR_18, BC.PUSH_LIT_VAR_19, BC.PUSH_LIT_VAR_1A, BC.PUSH_LIT_VAR_1B, BC.PUSH_LIT_VAR_1C, BC.PUSH_LIT_VAR_1D, BC.PUSH_LIT_VAR_1E, BC.PUSH_LIT_VAR_1F: {
                        push(frame, sp++, readLiteralVariable(currentPC, b & 0x1F));
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
                    case BC.PUSH_CONSTANT_MINUS_ONE: {
                        push(frame, sp++, -1L);
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
                    case BC.PUSH_CONSTANT_TWO: {
                        push(frame, sp++, 2L);
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
                        returnValue = handleReturn(frame, currentPC, pc, sp - 1, top(frame, sp),
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
                    case BC.EXTENDED_PUSH: {
                        final byte descriptor = getByte(bc, pc++);
                        final int variableIndex = variableIndex(descriptor);
                        final byte variableType = variableType(descriptor);
                        CompilerAsserts.partialEvaluationConstant(variableType);
                        switch (variableType) {
                            case 0: {
                                // for ContextObject access
                                FrameAccess.externalizePCAndSP(frame, pc, sp);
                                pushFollowed(frame, currentPC, sp++,
                                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), variableIndex));
                                break;
                            }
                            case 1: {
                                pushFollowed(frame, currentPC, sp++, FrameAccess.getStackValue(frame, variableIndex));
                                break;
                            }
                            case 2: {
                                push(frame, sp++, getAndResolveLiteral(currentPC, variableIndex));
                                break;
                            }
                            case 3: {
                                push(frame, sp++, readLiteralVariable(currentPC, variableIndex));
                                break;
                            }
                        }
                        break;
                    }
                    case BC.EXTENDED_STORE: {
                        final byte descriptor = getByte(bc, pc++);
                        final Object stackTop = top(frame, sp);
                        final int variableIndex = variableIndex(descriptor);
                        final byte variableType = variableType(descriptor);
                        CompilerAsserts.partialEvaluationConstant(variableType);
                        switch (variableType) {
                            case 0: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), variableIndex, stackTop);
                                break;
                            }
                            case 1: {
                                FrameAccess.setStackValue(frame, variableIndex, stackTop);
                                break;
                            }
                            case 2: {
                                throw unknownBytecode();
                            }
                            case 3: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, variableIndex), ASSOCIATION.VALUE, stackTop);
                                break;
                            }
                        }
                        break;
                    }
                    case BC.EXTENDED_POP: {
                        final byte descriptor = getByte(bc, pc++);
                        final Object stackValue = pop(frame, --sp);
                        final int variableIndex = variableIndex(descriptor);
                        final byte variableType = variableType(descriptor);
                        CompilerAsserts.partialEvaluationConstant(variableType);
                        switch (variableType) {
                            case 0: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), variableIndex, stackValue);
                                break;
                            }
                            case 1: {
                                FrameAccess.setStackValue(frame, variableIndex, stackValue);
                                break;
                            }
                            case 2: {
                                throw unknownBytecode();
                            }
                            case 3: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, variableIndex), ASSOCIATION.VALUE, stackValue);
                                break;
                            }
                        }
                        break;
                    }
                    case BC.SINGLE_EXTENDED_SEND: {
                        final int numArgs = getUnsignedInt(bc, pc++) >> 5;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = FrameAccess.internalizePC(frame, pc);
                        break;
                    }
                    case BC.DOUBLE_EXTENDED_DO_ANYTHING: {
                        final int byte2 = getUnsignedInt(bc, pc++);
                        final int byte3 = getUnsignedInt(bc, pc++);
                        final int opType = byte2 >> 5;
                        CompilerAsserts.partialEvaluationConstant(opType);
                        switch (opType) {
                            case 0: {
                                final int numArgs = byte2 & 31;
                                final Object[] arguments = popN(frame, sp, numArgs);
                                sp -= numArgs;
                                final Object receiver = pop(frame, --sp);
                                FrameAccess.externalizePCAndSP(frame, pc, sp);
                                push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                                pc = FrameAccess.internalizePC(frame, pc);
                                break;
                            }
                            case 1: {
                                final int numArgs = byte2 & 31;
                                final Object[] arguments = popN(frame, sp, numArgs);
                                sp -= numArgs;
                                final Object receiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(pop(frame, --sp));
                                FrameAccess.externalizePCAndSP(frame, pc, sp);
                                pushFollowed(frame, currentPC, sp++, sendSuper(frame, currentPC, receiver, arguments));
                                pc = FrameAccess.internalizePC(frame, pc);
                                break;
                            }
                            case 2: {
                                // for ContextObject access
                                FrameAccess.externalizePCAndSP(frame, pc, sp);
                                pushFollowed(frame, currentPC, sp++, ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAt0NodeGen.class).execute(this, FrameAccess.getReceiver(frame), byte3));
                                break;
                            }
                            case 3: {
                                push(frame, sp++, getAndResolveLiteral(currentPC, byte3));
                                break;
                            }
                            case 4: {
                                push(frame, sp++, readLiteralVariable(currentPC, byte3));
                                break;
                            }
                            case 5: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), byte3, top(frame, sp));
                                break;
                            }
                            case 6: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), byte3, pop(frame, --sp));
                                break;
                            }
                            case 7: {
                                ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, byte3), ASSOCIATION.VALUE, top(frame, sp));
                                break;
                            }
                            default: {
                                throw unknownBytecode();
                            }
                        }
                        break;
                    }
                    case BC.SINGLE_EXTENDED_SUPER: {
                        final int numArgs = getUnsignedInt(bc, pc++) >> 5;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(pop(frame, --sp));
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        pushFollowed(frame, currentPC, sp++, sendSuper(frame, currentPC, receiver, arguments));
                        pc = FrameAccess.internalizePC(frame, pc);
                        break;
                    }
                    case BC.SECOND_EXTENDED_SEND: {
                        final int numArgs = getUnsignedInt(bc, pc++) >> 6;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = FrameAccess.internalizePC(frame, pc);
                        break;
                    }
                    case BC.POP_STACK: {
                        --sp;
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        pushFollowed(frame, currentPC, sp, top(frame, sp));
                        sp++;
                        break;
                    }
                    case BC.PUSH_ACTIVE_CONTEXT: {
                        push(frame, sp++, getOrCreateContext(frame, currentPC));
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
                    case BC.CALL_PRIMITIVE: {
                        pc += 2;
                        if (getByte(bc, pc) == BC.EXTENDED_STORE) {
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
                    case BC.POP_INTO_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        ACCESS.uncheckedCast(getData(currentPC), SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getStackValue(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
                        break;
                    }
                    case BC.PUSH_CLOSURE_COPY_COPIED_VALUES: {
                        final int numArgsNumCopied = getUnsignedInt(bc, pc++);
                        final int blockSize = getBlockSize(bc, pc++, pc++);
                        final int numCopied = numArgsNumCopied >> 4 & 0xF;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        push(frame, sp++, createBlockClosure(frame, ACCESS.uncheckedCast(getData(currentPC), CompiledCodeObject.class), copiedValues, getOrCreateContext(frame, currentPC)));
                        pc += blockSize;
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        pc += calculateShortOffset(b);
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
                    case BC.LONG_UJUMP_0, BC.LONG_UJUMP_1, BC.LONG_UJUMP_2, BC.LONG_UJUMP_3, BC.LONG_UJUMP_4, BC.LONG_UJUMP_5, BC.LONG_UJUMP_6, BC.LONG_UJUMP_7: {
                        final int offset = ((b & 7) - 4 << 8) + getUnsignedInt(bc, pc++);
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
                    case BC.LONG_CJUMP_TRUE_0, BC.LONG_CJUMP_TRUE_1, BC.LONG_CJUMP_TRUE_2, BC.LONG_CJUMP_TRUE_3: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = longJump(b, getUnsignedInt(bc, pc++));
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(condition)) {
                                pc += offset;
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
                    case BC.LONG_CJUMP_FALSE_0, BC.LONG_CJUMP_FALSE_1, BC.LONG_CJUMP_FALSE_2, BC.LONG_CJUMP_FALSE_3: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = longJump(b, getUnsignedInt(bc, pc++));
                        if (stackValue instanceof final Boolean condition) {
                            if (ACCESS.uncheckedCast(getData(currentPC), CountingConditionProfile.class).profile(!condition)) {
                                pc += offset;
                            }
                        } else {
                            throw sendMustBeBooleanInInterpreter(frame, pc, sp, stackValue);
                        }
                        break;
                    }
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                            pc = FrameAccess.internalizePC(frame, pc);
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
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y, //
                        BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = pop(frame, --sp);
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
                        final Object receiver = pop(frame, --sp);
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
                        final Object receiver = pop(frame, --sp);
                        FrameAccess.externalizePCAndSP(frame, pc, sp);
                        push(frame, sp++, send(frame, currentPC, receiver, arg1, arg2));
                        pc = FrameAccess.internalizePC(frame, pc);
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

    private static int getBlockSize(final byte[] bc, final int highIndex, final int lowIndex) {
        return getUnsignedInt(bc, highIndex) << 8 | getUnsignedInt(bc, lowIndex);
    }

    private Object sendSuper(final VirtualFrame frame, final int currentPC, final Object receiver, final Object[] arguments) {
        try {
            return ACCESS.uncheckedCast(getData(currentPC), DispatchSuperNaryNodeGen.class).execute(frame, receiver, arguments);
        } catch (final AbstractStandardSendReturn r) {
            return handleReturnException(frame, currentPC, r);
        }
    }

    static int longJump(final int b, final int nextByte) {
        return ((b & 3) << 8) + nextByte;
    }

    public static int variableIndex(final byte i) {
        return i & 63;
    }

    public static byte variableType(final byte i) {
        return (byte) (i >> 6 & 3);
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
        static final int PUSH_TEMP_VAR_0 = 16;
        static final int PUSH_TEMP_VAR_1 = 17;
        static final int PUSH_TEMP_VAR_2 = 18;
        static final int PUSH_TEMP_VAR_3 = 19;
        static final int PUSH_TEMP_VAR_4 = 20;
        static final int PUSH_TEMP_VAR_5 = 21;
        static final int PUSH_TEMP_VAR_6 = 22;
        static final int PUSH_TEMP_VAR_7 = 23;
        static final int PUSH_TEMP_VAR_8 = 24;
        static final int PUSH_TEMP_VAR_9 = 25;
        static final int PUSH_TEMP_VAR_A = 26;
        static final int PUSH_TEMP_VAR_B = 27;
        static final int PUSH_TEMP_VAR_C = 28;
        static final int PUSH_TEMP_VAR_D = 29;
        static final int PUSH_TEMP_VAR_E = 30;
        static final int PUSH_TEMP_VAR_F = 31;
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
        static final int PUSH_LIT_VAR_00 = 64;
        static final int PUSH_LIT_VAR_01 = 65;
        static final int PUSH_LIT_VAR_02 = 66;
        static final int PUSH_LIT_VAR_03 = 67;
        static final int PUSH_LIT_VAR_04 = 68;
        static final int PUSH_LIT_VAR_05 = 69;
        static final int PUSH_LIT_VAR_06 = 70;
        static final int PUSH_LIT_VAR_07 = 71;
        static final int PUSH_LIT_VAR_08 = 72;
        static final int PUSH_LIT_VAR_09 = 73;
        static final int PUSH_LIT_VAR_0A = 74;
        static final int PUSH_LIT_VAR_0B = 75;
        static final int PUSH_LIT_VAR_0C = 76;
        static final int PUSH_LIT_VAR_0D = 77;
        static final int PUSH_LIT_VAR_0E = 78;
        static final int PUSH_LIT_VAR_0F = 79;
        static final int PUSH_LIT_VAR_10 = 80;
        static final int PUSH_LIT_VAR_11 = 81;
        static final int PUSH_LIT_VAR_12 = 82;
        static final int PUSH_LIT_VAR_13 = 83;
        static final int PUSH_LIT_VAR_14 = 84;
        static final int PUSH_LIT_VAR_15 = 85;
        static final int PUSH_LIT_VAR_16 = 86;
        static final int PUSH_LIT_VAR_17 = 87;
        static final int PUSH_LIT_VAR_18 = 88;
        static final int PUSH_LIT_VAR_19 = 89;
        static final int PUSH_LIT_VAR_1A = 90;
        static final int PUSH_LIT_VAR_1B = 91;
        static final int PUSH_LIT_VAR_1C = 92;
        static final int PUSH_LIT_VAR_1D = 93;
        static final int PUSH_LIT_VAR_1E = 94;
        static final int PUSH_LIT_VAR_1F = 95;
        static final int POP_INTO_RCVR_VAR_0 = 96;
        static final int POP_INTO_RCVR_VAR_1 = 97;
        static final int POP_INTO_RCVR_VAR_2 = 98;
        static final int POP_INTO_RCVR_VAR_3 = 99;
        static final int POP_INTO_RCVR_VAR_4 = 100;
        static final int POP_INTO_RCVR_VAR_5 = 101;
        static final int POP_INTO_RCVR_VAR_6 = 102;
        static final int POP_INTO_RCVR_VAR_7 = 103;
        static final int POP_INTO_TEMP_VAR_0 = 104;
        static final int POP_INTO_TEMP_VAR_1 = 105;
        static final int POP_INTO_TEMP_VAR_2 = 106;
        static final int POP_INTO_TEMP_VAR_3 = 107;
        static final int POP_INTO_TEMP_VAR_4 = 108;
        static final int POP_INTO_TEMP_VAR_5 = 109;
        static final int POP_INTO_TEMP_VAR_6 = 110;
        static final int POP_INTO_TEMP_VAR_7 = 111;
        static final int PUSH_RECEIVER = 112;
        static final int PUSH_CONSTANT_TRUE = 113;
        static final int PUSH_CONSTANT_FALSE = 114;
        static final int PUSH_CONSTANT_NIL = 115;
        static final int PUSH_CONSTANT_MINUS_ONE = 116;
        static final int PUSH_CONSTANT_ZERO = 117;
        static final int PUSH_CONSTANT_ONE = 118;
        static final int PUSH_CONSTANT_TWO = 119;
        static final int RETURN_RECEIVER = 120;
        static final int RETURN_TRUE = 121;
        static final int RETURN_FALSE = 122;
        static final int RETURN_NIL = 123;
        static final int RETURN_TOP_FROM_METHOD = 124;
        static final int RETURN_TOP_FROM_BLOCK = 125;

        static final int EXTENDED_PUSH = 128;
        static final int EXTENDED_STORE = 129;
        static final int EXTENDED_POP = 130;
        static final int SINGLE_EXTENDED_SEND = 131;
        static final int DOUBLE_EXTENDED_DO_ANYTHING = 132;
        static final int SINGLE_EXTENDED_SUPER = 133;
        static final int SECOND_EXTENDED_SEND = 134;
        static final int POP_STACK = 135;
        static final int DUPLICATE_TOP = 136;

        static final int PUSH_ACTIVE_CONTEXT = 137;
        static final int PUSH_NEW_ARRAY = 138;
        static final int CALL_PRIMITIVE = 139;
        static final int PUSH_REMOTE_TEMP_LONG = 140;
        static final int STORE_REMOTE_TEMP_LONG = 141;
        static final int POP_INTO_REMOTE_TEMP_LONG = 142;
        static final int PUSH_CLOSURE_COPY_COPIED_VALUES = 143;

        static final int SHORT_UJUMP_0 = 144;
        static final int SHORT_UJUMP_1 = 145;
        static final int SHORT_UJUMP_2 = 146;
        static final int SHORT_UJUMP_3 = 147;
        static final int SHORT_UJUMP_4 = 148;
        static final int SHORT_UJUMP_5 = 149;
        static final int SHORT_UJUMP_6 = 150;
        static final int SHORT_UJUMP_7 = 151;
        static final int SHORT_CJUMP_FALSE_0 = 152;
        static final int SHORT_CJUMP_FALSE_1 = 153;
        static final int SHORT_CJUMP_FALSE_2 = 154;
        static final int SHORT_CJUMP_FALSE_3 = 155;
        static final int SHORT_CJUMP_FALSE_4 = 156;
        static final int SHORT_CJUMP_FALSE_5 = 157;
        static final int SHORT_CJUMP_FALSE_6 = 158;
        static final int SHORT_CJUMP_FALSE_7 = 159;
        static final int LONG_UJUMP_0 = 160;
        static final int LONG_UJUMP_1 = 161;
        static final int LONG_UJUMP_2 = 162;
        static final int LONG_UJUMP_3 = 163;
        static final int LONG_UJUMP_4 = 164;
        static final int LONG_UJUMP_5 = 165;
        static final int LONG_UJUMP_6 = 166;
        static final int LONG_UJUMP_7 = 167;
        static final int LONG_CJUMP_TRUE_0 = 168;
        static final int LONG_CJUMP_TRUE_1 = 169;
        static final int LONG_CJUMP_TRUE_2 = 170;
        static final int LONG_CJUMP_TRUE_3 = 171;
        static final int LONG_CJUMP_FALSE_0 = 172;
        static final int LONG_CJUMP_FALSE_1 = 173;
        static final int LONG_CJUMP_FALSE_2 = 174;
        static final int LONG_CJUMP_FALSE_3 = 175;

        // 176-191 were sendArithmeticSelectorBytecode
        static final int BYTECODE_PRIM_ADD = 176;
        static final int BYTECODE_PRIM_SUBTRACT = 177;
        static final int BYTECODE_PRIM_LESS_THAN = 178;
        static final int BYTECODE_PRIM_GREATER_THAN = 179;
        static final int BYTECODE_PRIM_LESS_OR_EQUAL = 180;
        static final int BYTECODE_PRIM_GREATER_OR_EQUAL = 181;
        static final int BYTECODE_PRIM_EQUAL = 182;
        static final int BYTECODE_PRIM_NOT_EQUAL = 183;
        static final int BYTECODE_PRIM_MULTIPLY = 184;
        static final int BYTECODE_PRIM_DIVIDE = 185;
        static final int BYTECODE_PRIM_MOD = 186;
        static final int BYTECODE_PRIM_MAKE_POINT = 187;
        static final int BYTECODE_PRIM_BIT_SHIFT = 188;
        static final int BYTECODE_PRIM_DIV = 189;
        static final int BYTECODE_PRIM_BIT_AND = 190;
        static final int BYTECODE_PRIM_BIT_OR = 191;

        // 192-207 were sendCommonSelectorBytecode
        static final int BYTECODE_PRIM_AT = 192;
        static final int BYTECODE_PRIM_AT_PUT = 193;
        static final int BYTECODE_PRIM_SIZE = 194;
        static final int BYTECODE_PRIM_NEXT = 195;
        static final int BYTECODE_PRIM_NEXT_PUT = 196;
        static final int BYTECODE_PRIM_AT_END = 197;
        static final int BYTECODE_PRIM_IDENTICAL = 198;
        static final int BYTECODE_PRIM_CLASS = 199;
        static final int BYTECODE_PRIM_NOT_IDENTICAL = 200;
        static final int BYTECODE_PRIM_VALUE = 201;
        static final int BYTECODE_PRIM_VALUE_WITH_ARG = 202;
        static final int BYTECODE_PRIM_DO = 203;
        static final int BYTECODE_PRIM_NEW = 204;
        static final int BYTECODE_PRIM_NEW_WITH_ARG = 205;
        static final int BYTECODE_PRIM_POINT_X = 206;
        static final int BYTECODE_PRIM_POINT_Y = 207;

        static final int SEND_LIT_SEL0_0 = 208;
        static final int SEND_LIT_SEL0_1 = 209;
        static final int SEND_LIT_SEL0_2 = 210;
        static final int SEND_LIT_SEL0_3 = 211;
        static final int SEND_LIT_SEL0_4 = 212;
        static final int SEND_LIT_SEL0_5 = 213;
        static final int SEND_LIT_SEL0_6 = 214;
        static final int SEND_LIT_SEL0_7 = 215;
        static final int SEND_LIT_SEL0_8 = 216;
        static final int SEND_LIT_SEL0_9 = 217;
        static final int SEND_LIT_SEL0_A = 218;
        static final int SEND_LIT_SEL0_B = 219;
        static final int SEND_LIT_SEL0_C = 220;
        static final int SEND_LIT_SEL0_D = 221;
        static final int SEND_LIT_SEL0_E = 222;
        static final int SEND_LIT_SEL0_F = 223;
        static final int SEND_LIT_SEL1_0 = 224;
        static final int SEND_LIT_SEL1_1 = 225;
        static final int SEND_LIT_SEL1_2 = 226;
        static final int SEND_LIT_SEL1_3 = 227;
        static final int SEND_LIT_SEL1_4 = 228;
        static final int SEND_LIT_SEL1_5 = 229;
        static final int SEND_LIT_SEL1_6 = 230;
        static final int SEND_LIT_SEL1_7 = 231;
        static final int SEND_LIT_SEL1_8 = 232;
        static final int SEND_LIT_SEL1_9 = 233;
        static final int SEND_LIT_SEL1_A = 234;
        static final int SEND_LIT_SEL1_B = 235;
        static final int SEND_LIT_SEL1_C = 236;
        static final int SEND_LIT_SEL1_D = 237;
        static final int SEND_LIT_SEL1_E = 238;
        static final int SEND_LIT_SEL1_F = 239;
        static final int SEND_LIT_SEL2_0 = 240;
        static final int SEND_LIT_SEL2_1 = 241;
        static final int SEND_LIT_SEL2_2 = 242;
        static final int SEND_LIT_SEL2_3 = 243;
        static final int SEND_LIT_SEL2_4 = 244;
        static final int SEND_LIT_SEL2_5 = 245;
        static final int SEND_LIT_SEL2_6 = 246;
        static final int SEND_LIT_SEL2_7 = 247;
        static final int SEND_LIT_SEL2_8 = 248;
        static final int SEND_LIT_SEL2_9 = 249;
        static final int SEND_LIT_SEL2_A = 250;
        static final int SEND_LIT_SEL2_B = 251;
        static final int SEND_LIT_SEL2_C = 252;
        static final int SEND_LIT_SEL2_D = 253;
        static final int SEND_LIT_SEL2_E = 254;
        static final int SEND_LIT_SEL2_F = 255;
    }

    /*
     * Copying
     */

    @Override
    public Node copy() {
        return new InterpreterV3PlusClosuresNode(this);
    }

    @Override
    public Node deepCopy() {
        return new InterpreterV3PlusClosuresNode(this);
    }
}

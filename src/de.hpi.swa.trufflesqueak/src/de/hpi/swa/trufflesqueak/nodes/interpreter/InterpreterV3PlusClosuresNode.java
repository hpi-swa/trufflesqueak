/*
 * Copyright (c) 2025-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2025 Oracle and/or its affiliates
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

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimBitAndNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimBitOrNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimBitShiftNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimDivNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimDivideNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimMakePointNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimModNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimMultiplyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimPointXNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimPointYNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimSubtractNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsInLoopNode;
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

public final class InterpreterV3PlusClosuresNode extends AbstractInterpreterNode {
    public InterpreterV3PlusClosuresNode(final CompiledCodeObject code) {
        super(code);
    }

    public InterpreterV3PlusClosuresNode(final InterpreterV3PlusClosuresNode original) {
        super(original);
    }

    @Override
    protected void processBytecode(final int maxPC) {
        final byte[] bc = code.getBytes();
        final SqueakImageContext image = SqueakImageContext.getSlow();

        int pc = code.hasOuterMethod() ? code.getOuterMethodStartPC() : 0;

        while (pc < maxPC) {
            final int currentPC = pc++;
            final byte b = getByte(bc, currentPC);
            switch (b) {
                case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    break;
                }
                case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                    BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, BC.PUSH_TEMP_VAR_C, BC.PUSH_TEMP_VAR_D, BC.PUSH_TEMP_VAR_E, BC.PUSH_TEMP_VAR_F, //
                    BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                    BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                    BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                    BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F, //
                    BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7, //
                    BC.PUSH_RECEIVER, BC.PUSH_CONSTANT_TRUE, BC.PUSH_CONSTANT_FALSE, BC.PUSH_CONSTANT_NIL, BC.PUSH_CONSTANT_MINUS_ONE, BC.PUSH_CONSTANT_ZERO, BC.PUSH_CONSTANT_ONE, BC.PUSH_CONSTANT_TWO, //
                    BC.POP_STACK, //
                    BC.DUPLICATE_TOP: {
                    break;
                }
                case BC.PUSH_LIT_VAR_00, BC.PUSH_LIT_VAR_01, BC.PUSH_LIT_VAR_02, BC.PUSH_LIT_VAR_03, BC.PUSH_LIT_VAR_04, BC.PUSH_LIT_VAR_05, BC.PUSH_LIT_VAR_06, BC.PUSH_LIT_VAR_07, //
                    BC.PUSH_LIT_VAR_08, BC.PUSH_LIT_VAR_09, BC.PUSH_LIT_VAR_0A, BC.PUSH_LIT_VAR_0B, BC.PUSH_LIT_VAR_0C, BC.PUSH_LIT_VAR_0D, BC.PUSH_LIT_VAR_0E, BC.PUSH_LIT_VAR_0F, //
                    BC.PUSH_LIT_VAR_10, BC.PUSH_LIT_VAR_11, BC.PUSH_LIT_VAR_12, BC.PUSH_LIT_VAR_13, BC.PUSH_LIT_VAR_14, BC.PUSH_LIT_VAR_15, BC.PUSH_LIT_VAR_16, BC.PUSH_LIT_VAR_17, //
                    BC.PUSH_LIT_VAR_18, BC.PUSH_LIT_VAR_19, BC.PUSH_LIT_VAR_1A, BC.PUSH_LIT_VAR_1B, BC.PUSH_LIT_VAR_1C, BC.PUSH_LIT_VAR_1D, BC.PUSH_LIT_VAR_1E, BC.PUSH_LIT_VAR_1F: {
                    data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(b & 0x1F));
                    break;
                }
                case BC.POP_INTO_RCVR_VAR_0, BC.POP_INTO_RCVR_VAR_1, BC.POP_INTO_RCVR_VAR_2, BC.POP_INTO_RCVR_VAR_3, BC.POP_INTO_RCVR_VAR_4, BC.POP_INTO_RCVR_VAR_5, BC.POP_INTO_RCVR_VAR_6, BC.POP_INTO_RCVR_VAR_7: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    break;
                }
                case BC.RETURN_RECEIVER, BC.RETURN_TRUE, BC.RETURN_FALSE, BC.RETURN_NIL, BC.RETURN_TOP_FROM_METHOD: {
                    data[currentPC] = isBlock ? new BlockReturnNode() : new NormalReturnNode();
                    break;
                }
                case BC.RETURN_TOP_FROM_BLOCK: {
                    data[currentPC] = new NormalReturnNode();
                    break;
                }
                case BC.EXTENDED_PUSH: {
                    final byte descriptor = getByte(bc, pc++);
                    final int variableIndex = variableIndex(descriptor);
                    switch (variableType(descriptor)) {
                        case 0: {
                            data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                            break;
                        }
                        case 1, 2: {
                            break;
                        }
                        case 3: {
                            data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(variableIndex));
                            break;
                        }
                    }
                    break;
                }
                case BC.EXTENDED_STORE, BC.EXTENDED_POP: {
                    final byte descriptor = getByte(bc, pc++);
                    switch (variableType(descriptor)) {
                        case 0, 3: {
                            data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
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
                    data[currentPC] = insert(DispatchNaryNodeGen.create(selector));
                    break;
                }
                case BC.DOUBLE_EXTENDED_DO_ANYTHING: {
                    final int byte2 = getUnsignedInt(bc, pc++);
                    final int byte3 = getUnsignedInt(bc, pc++);
                    switch (byte2 >> 5) {
                        case 0: {
                            final NativeObject selector = (NativeObject) code.getLiteral(byte3);
                            data[currentPC] = insert(DispatchNaryNodeGen.create(selector));
                            break;
                        }
                        case 1: {
                            final NativeObject selector = (NativeObject) code.getLiteral(byte3);
                            final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                            data[currentPC] = insert(DispatchSuperNaryNodeGen.create(methodClass, selector));
                            break;
                        }
                        case 2: {
                            data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                            break;
                        }
                        case 3: {
                            break;
                        }
                        case 4: {
                            data[currentPC] = getLiteralVariableOrCreateLiteralNode(code.getAndResolveLiteral(byte3));
                            break;
                        }
                        case 5, 6, 7: {
                            data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
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
                    data[currentPC] = insert(DispatchSuperNaryNodeGen.create(methodClass, selector));
                    break;
                }
                case BC.SECOND_EXTENDED_SEND: {
                    final NativeObject selector = (NativeObject) code.getLiteral(getByte(bc, pc++) & 0x3F);
                    data[currentPC] = insert(DispatchNaryNodeGen.create(selector));
                    break;
                }
                case BC.PUSH_ACTIVE_CONTEXT: {
                    data[currentPC] = insert(GetOrCreateContextWithFrameNode.create());
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
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    pc += 2;
                    break;
                }
                case BC.STORE_REMOTE_TEMP_LONG, BC.POP_INTO_REMOTE_TEMP_LONG: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    pc += 2;
                    break;
                }
                case BC.PUSH_CLOSURE_COPY_COPIED_VALUES: {
                    final int numArgsNumCopied = getUnsignedInt(bc, pc++);
                    final int numArgs = numArgsNumCopied & 0xF;
                    final int blockSize = getUnsignedInt(bc, pc++) << 8 | getUnsignedInt(bc, pc++);
                    data[currentPC] = insert(new PushClosureNode(code, pc, numArgs));
                    pc += blockSize;
                    break;
                }
                case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                    final int offset = calculateShortOffset(b);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsInLoopNode.createForLoop(data, pc, 1, offset));
                    }
                    break;
                }
                case BC.SHORT_CJUMP_FALSE_0, BC.SHORT_CJUMP_FALSE_1, BC.SHORT_CJUMP_FALSE_2, BC.SHORT_CJUMP_FALSE_3, BC.SHORT_CJUMP_FALSE_4, BC.SHORT_CJUMP_FALSE_5, BC.SHORT_CJUMP_FALSE_6, BC.SHORT_CJUMP_FALSE_7: {
                    data[currentPC] = CountingConditionProfile.create();
                    break;
                }

                case BC.LONG_UJUMP_0, BC.LONG_UJUMP_1, BC.LONG_UJUMP_2, BC.LONG_UJUMP_3, BC.LONG_UJUMP_4, BC.LONG_UJUMP_5, BC.LONG_UJUMP_6, BC.LONG_UJUMP_7: {
                    final int offset = ((b & 7) - 4 << 8) + getUnsignedInt(bc, pc++);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsInLoopNode.createForLoop(data, pc, 1, offset));
                    }
                    break;
                }
                case BC.LONG_CJUMP_TRUE_0, BC.LONG_CJUMP_TRUE_1, BC.LONG_CJUMP_TRUE_2, BC.LONG_CJUMP_TRUE_3, //
                    BC.LONG_CJUMP_FALSE_0, BC.LONG_CJUMP_FALSE_1, BC.LONG_CJUMP_FALSE_2, BC.LONG_CJUMP_FALSE_3: {
                    data[currentPC] = CountingConditionProfile.create();
                    pc++;
                    break;
                }
                case BC.BYTECODE_PRIM_ADD: {
                    data[currentPC] = insert(BytecodePrimAddNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_SUBTRACT: {
                    data[currentPC] = insert(BytecodePrimSubtractNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, BC.BYTECODE_PRIM_EQUAL, BC.BYTECODE_PRIM_NOT_EQUAL: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector(b - BC.BYTECODE_PRIM_ADD)));
                    break;
                }
                case BC.BYTECODE_PRIM_MULTIPLY: {
                    data[currentPC] = insert(BytecodePrimMultiplyNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_DIVIDE: {
                    data[currentPC] = insert(BytecodePrimDivideNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_MOD: {
                    data[currentPC] = insert(BytecodePrimModNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_MAKE_POINT: {
                    data[currentPC] = insert(BytecodePrimMakePointNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_SHIFT: {
                    data[currentPC] = insert(BytecodePrimBitShiftNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_DIV: {
                    data[currentPC] = insert(BytecodePrimDivNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_AND: {
                    data[currentPC] = insert(BytecodePrimBitAndNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_OR: {
                    data[currentPC] = insert(BytecodePrimBitOrNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_AT: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_AT_PUT: {
                    data[currentPC] = insert(Dispatch2NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_SIZE: {
                    data[currentPC] = insert(BytecodePrimSizeNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_NEXT: {
                    data[currentPC] = insert(Dispatch0NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_NEXT_PUT: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_AT_END: {
                    data[currentPC] = insert(Dispatch0NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_IDENTICAL, BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                    data[currentPC] = insert(SqueakObjectIdentityNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_CLASS: {
                    data[currentPC] = insert(SqueakObjectClassNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE: {
                    data[currentPC] = insert(Dispatch0NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE_WITH_ARG: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_DO: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_NEW: {
                    data[currentPC] = insert(Dispatch0NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                    data[currentPC] = insert(Dispatch1NodeGen.create(image.getSpecialSelector((b & 0xFF) - 0xB0)));
                    break;
                }
                case BC.BYTECODE_PRIM_POINT_X: {
                    data[currentPC] = insert(BytecodePrimPointXNodeGen.create());
                    break;
                }
                case BC.BYTECODE_PRIM_POINT_Y: {
                    data[currentPC] = insert(BytecodePrimPointYNodeGen.create());
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

        final byte[] bc = uncheckedCast(code.getBytes(), byte[].class);

        int pc = startPC;
        int sp = startSP;

        final LoopCounter loopCounter = new LoopCounter();

        Object returnValue = null;
        try {
            while (pc != LOCAL_RETURN_PC) {
                CompilerAsserts.partialEvaluationConstant(pc);
                CompilerAsserts.partialEvaluationConstant(sp);
                final int currentPC = pc++;
                final byte b = getByte(bc, currentPC);
                CompilerAsserts.partialEvaluationConstant(b);
                switch (b) {
                    case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                        BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                        externalizePCAndSP(frame, pc, sp); // for ContextObject access
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                        BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, BC.PUSH_TEMP_VAR_C, BC.PUSH_TEMP_VAR_D, BC.PUSH_TEMP_VAR_E, BC.PUSH_TEMP_VAR_F: {
                        push(frame, currentPC, sp++, getTemp(frame, b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                        BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                        BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                        BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F: {
                        push(frame, currentPC, sp++, getAndResolveLiteral(currentPC, b & 0x1F));
                        break;
                    }
                    case BC.PUSH_LIT_VAR_00, BC.PUSH_LIT_VAR_01, BC.PUSH_LIT_VAR_02, BC.PUSH_LIT_VAR_03, BC.PUSH_LIT_VAR_04, BC.PUSH_LIT_VAR_05, BC.PUSH_LIT_VAR_06, BC.PUSH_LIT_VAR_07, //
                        BC.PUSH_LIT_VAR_08, BC.PUSH_LIT_VAR_09, BC.PUSH_LIT_VAR_0A, BC.PUSH_LIT_VAR_0B, BC.PUSH_LIT_VAR_0C, BC.PUSH_LIT_VAR_0D, BC.PUSH_LIT_VAR_0E, BC.PUSH_LIT_VAR_0F, //
                        BC.PUSH_LIT_VAR_10, BC.PUSH_LIT_VAR_11, BC.PUSH_LIT_VAR_12, BC.PUSH_LIT_VAR_13, BC.PUSH_LIT_VAR_14, BC.PUSH_LIT_VAR_15, BC.PUSH_LIT_VAR_16, BC.PUSH_LIT_VAR_17, //
                        BC.PUSH_LIT_VAR_18, BC.PUSH_LIT_VAR_19, BC.PUSH_LIT_VAR_1A, BC.PUSH_LIT_VAR_1B, BC.PUSH_LIT_VAR_1C, BC.PUSH_LIT_VAR_1D, BC.PUSH_LIT_VAR_1E, BC.PUSH_LIT_VAR_1F: {
                        push(frame, currentPC, sp++, readLiteralVariable(currentPC, b & 0x1F));
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
                    case BC.PUSH_RECEIVER: {
                        push(frame, currentPC, sp++, FrameAccess.getReceiver(frame));
                        break;
                    }
                    case BC.PUSH_CONSTANT_TRUE: {
                        pushResolved(frame, sp++, BooleanObject.TRUE);
                        break;
                    }
                    case BC.PUSH_CONSTANT_FALSE: {
                        pushResolved(frame, sp++, BooleanObject.FALSE);
                        break;
                    }
                    case BC.PUSH_CONSTANT_NIL: {
                        pushResolved(frame, sp++, NilObject.SINGLETON);
                        break;
                    }
                    case BC.PUSH_CONSTANT_MINUS_ONE: {
                        pushResolved(frame, sp++, -1L);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ZERO: {
                        pushResolved(frame, sp++, 0L);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ONE: {
                        pushResolved(frame, sp++, 1L);
                        break;
                    }
                    case BC.PUSH_CONSTANT_TWO: {
                        pushResolved(frame, sp++, 2L);
                        break;
                    }
                    case BC.RETURN_RECEIVER: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, FrameAccess.getReceiver(frame));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TRUE: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, BooleanObject.TRUE);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_FALSE: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, BooleanObject.FALSE);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_NIL: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, NilObject.SINGLETON);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_METHOD: {
                        returnValue = handleReturn(frame, currentPC, pc, sp, top(frame, sp));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_BLOCK: {
                        returnValue = uncheckedCast(data[currentPC], NormalReturnNode.class).execute(frame, top(frame, sp));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.EXTENDED_PUSH: {
                        final byte descriptor = getByte(bc, pc++);
                        final int variableIndex = variableIndex(descriptor);
                        switch (variableType(descriptor)) {
                            case 0: {
                                externalizePCAndSP(frame, pc, sp); // for ContextObject access
                                push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, FrameAccess.getReceiver(frame), variableIndex));
                                break;
                            }
                            case 1: {
                                push(frame, currentPC, sp++, getTemp(frame, variableIndex));
                                break;
                            }
                            case 2: {
                                push(frame, currentPC, sp++, getAndResolveLiteral(currentPC, variableIndex));
                                break;
                            }
                            case 3: {
                                push(frame, currentPC, sp++, readLiteralVariable(currentPC, variableIndex));
                                break;
                            }
                        }
                        break;
                    }
                    case BC.EXTENDED_STORE: {
                        final byte descriptor = getByte(bc, pc++);
                        final int variableIndex = variableIndex(descriptor);
                        final Object stackTop = top(frame, sp);
                        switch (variableType(descriptor)) {
                            case 0: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), variableIndex, stackTop);
                                break;
                            }
                            case 1: {
                                setStackValue(frame, variableIndex, stackTop);
                                break;
                            }
                            case 2: {
                                throw unknownBytecode();
                            }
                            case 3: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, variableIndex), ASSOCIATION.VALUE, stackTop);
                                break;
                            }
                        }
                        break;
                    }
                    case BC.EXTENDED_POP: {
                        final byte descriptor = getByte(bc, pc++);
                        final int variableIndex = variableIndex(descriptor);
                        final Object stackValue = pop(frame, --sp);
                        switch (variableType(descriptor)) {
                            case 0: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), variableIndex, stackValue);
                                break;
                            }
                            case 1: {
                                setStackValue(frame, variableIndex, stackValue);
                                break;
                            }
                            case 2: {
                                throw unknownBytecode();
                            }
                            case 3: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, variableIndex), ASSOCIATION.VALUE, stackValue);
                                break;
                            }
                        }
                        break;
                    }
                    case BC.SINGLE_EXTENDED_SEND: {
                        final int numArgs = getUnsignedInt(bc, pc++) >> 5;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.DOUBLE_EXTENDED_DO_ANYTHING: {
                        final int byte2 = getUnsignedInt(bc, pc++);
                        final int byte3 = getUnsignedInt(bc, pc++);
                        switch (byte2 >> 5) {
                            case 0: {
                                final int numArgs = byte2 & 31;
                                final Object[] arguments = popN(frame, sp, numArgs);
                                sp -= numArgs;
                                final Object receiver = popReceiver(frame, --sp);
                                externalizePCAndSP(frame, pc, sp);
                                push(frame, currentPC, sp++, sendNary(frame, currentPC, receiver, arguments));
                                pc = checkPCAfterSend(frame, pc);
                                break;
                            }
                            case 1: {
                                final int numArgs = byte2 & 31;
                                final Object[] arguments = popN(frame, sp, numArgs);
                                sp -= numArgs;
                                final Object receiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(popReceiver(frame, --sp));
                                externalizePCAndSP(frame, pc, sp);
                                final Object result = sendSuper(frame, currentPC, receiver, arguments);
                                push(frame, currentPC, sp++, result);
                                pc = checkPCAfterSend(frame, pc);
                                break;
                            }
                            case 2: {
                                externalizePCAndSP(frame, pc, sp); // for ContextObject access
                                push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, FrameAccess.getReceiver(frame), byte3));
                                break;
                            }
                            case 3: {
                                push(frame, currentPC, sp++, getAndResolveLiteral(currentPC, byte3));
                                break;
                            }
                            case 4: {
                                push(frame, currentPC, sp++, readLiteralVariable(currentPC, byte3));
                                break;
                            }
                            case 5: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), byte3, top(frame, sp));
                                break;
                            }
                            case 6: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), byte3, pop(frame, --sp));
                                break;
                            }
                            case 7: {
                                uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getAndResolveLiteral(currentPC, byte3), ASSOCIATION.VALUE, top(frame, sp));
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
                        final Object receiver = AbstractSqueakObjectWithClassAndHash.resolveForwardingPointer(popReceiver(frame, --sp));
                        externalizePCAndSP(frame, pc, sp);
                        final Object result = sendSuper(frame, currentPC, receiver, arguments);
                        push(frame, currentPC, sp++, result);
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.SECOND_EXTENDED_SEND: {
                        final int numArgs = getUnsignedInt(bc, pc++) >> 6;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, sendNary(frame, currentPC, receiver, arguments));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.POP_STACK: {
                        pop(frame, --sp);
                        break;
                    }
                    case BC.DUPLICATE_TOP: {
                        push(frame, currentPC, sp, top(frame, sp));
                        sp++;
                        break;
                    }
                    case BC.PUSH_ACTIVE_CONTEXT: {
                        pushResolved(frame, sp++, uncheckedCast(data[currentPC], GetOrCreateContextWithFrameNode.class).executeGet(frame));
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
                        final SqueakImageContext image = getContext();
                        pushResolved(frame, sp++, ArrayObject.createWithStorage(image, image.arrayClass, values));
                        break;
                    }
                    case BC.CALL_PRIMITIVE: {
                        pc += 2;
                        if (getByte(bc, pc) == BC.EXTENDED_STORE) {
                            assert sp > 0;
                            setStackValue(frame, sp - 1, getErrorObject());
                        }
                        break;
                    }
                    case BC.PUSH_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex));
                        break;
                    }
                    case BC.STORE_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, top(frame, sp));
                        break;
                    }
                    case BC.POP_INTO_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
                        break;
                    }
                    case BC.PUSH_CLOSURE_COPY_COPIED_VALUES: {
                        final int numArgsNumCopied = getUnsignedInt(bc, pc++);
                        final int blockSizeHigh = getUnsignedInt(bc, pc++);
                        final int blockSizeLow = getUnsignedInt(bc, pc++);
                        final int numCopied = numArgsNumCopied >> 4 & 0xF;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        pushResolved(frame, sp++, uncheckedCast(data[currentPC], PushClosureNode.class).execute(frame, copiedValues));
                        final int blockSize = blockSizeHigh << 8 | blockSizeLow;
                        pc += blockSize;
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        final int offset = calculateShortOffset(b);
                        pc += offset;
                        if (offset < 0) {
                            if (CompilerDirectives.hasNextTier()) {
                                final int loopCount = ++loopCounter.value;
                                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, loopCount >= LoopCounter.CHECK_LOOP_STRIDE)) {
                                    LoopNode.reportLoopCount(this, loopCount);
                                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, loopCount)) {
                                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((sp & 0xFF) << 16) | pc, null, null, frame);
                                        if (osrReturnValue != null) {
                                            assert !FrameAccess.hasModifiedSender(frame);
                                            FrameAccess.terminateFrame(frame);
                                            return osrReturnValue;
                                        }
                                    }
                                    loopCounter.value = 0;
                                }
                            }
                            if (data[currentPC] != null) {
                                uncheckedCast(data[currentPC], CheckForInterruptsInLoopNode.class).execute(frame, pc);
                            }
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
                    case BC.LONG_UJUMP_0, BC.LONG_UJUMP_1, BC.LONG_UJUMP_2, BC.LONG_UJUMP_3, BC.LONG_UJUMP_4, BC.LONG_UJUMP_5, BC.LONG_UJUMP_6, BC.LONG_UJUMP_7: {
                        final int offset = ((b & 7) - 4 << 8) + getUnsignedInt(bc, pc++);
                        pc += offset;
                        if (offset < 0) {
                            if (CompilerDirectives.hasNextTier()) {
                                final int loopCount = ++loopCounter.value;
                                if (CompilerDirectives.injectBranchProbability(LoopCounter.CHECK_LOOP_PROBABILITY, loopCount >= LoopCounter.CHECK_LOOP_STRIDE)) {
                                    LoopNode.reportLoopCount(this, loopCount);
                                    if (CompilerDirectives.inInterpreter() && !isBlock && BytecodeOSRNode.pollOSRBackEdge(this, loopCount)) {
                                        final Object osrReturnValue = BytecodeOSRNode.tryOSR(this, ((sp & 0xFF) << 16) | pc, null, null, frame);
                                        if (osrReturnValue != null) {
                                            assert !FrameAccess.hasModifiedSender(frame);
                                            FrameAccess.terminateFrame(frame);
                                            return osrReturnValue;
                                        }
                                    }
                                    loopCounter.value = 0;
                                }
                            }
                            if (data[currentPC] != null) {
                                uncheckedCast(data[currentPC], CheckForInterruptsInLoopNode.class).execute(frame, pc);
                            }
                        }
                        break;
                    }
                    case BC.LONG_CJUMP_TRUE_0, BC.LONG_CJUMP_TRUE_1, BC.LONG_CJUMP_TRUE_2, BC.LONG_CJUMP_TRUE_3: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = longJump(b, getUnsignedInt(bc, pc++));
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        break;
                    }
                    case BC.LONG_CJUMP_FALSE_0, BC.LONG_CJUMP_FALSE_1, BC.LONG_CJUMP_FALSE_2, BC.LONG_CJUMP_FALSE_3: {
                        final Object stackValue = pop(frame, --sp);
                        final int offset = longJump(b, getUnsignedInt(bc, pc++));
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(!condition)) {
                                pc += offset;
                            }
                        } else {
                            sendMustBeBooleanInInterpreter(frame, pc, stackValue);
                        }
                        break;
                    }
                    /* bytecode prims with 0 args */
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y: {
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, sendBytecodePrim(frame, currentPC, receiver));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_CLASS: {
                        final Object receiver = popReceiver(frame, --sp);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectClassNodeGen.class).executeLookup(this, receiver));
                        break;
                    }
                    /* bytecode prims with 1 arg */
                    case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, //
                        BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_BIT_AND, BC.BYTECODE_PRIM_BIT_OR, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, sendBytecodePrim(frame, currentPC, receiver, arg));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_LESS_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimLessThanNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatLessThanNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_GREATER_THAN: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimGreaterThanNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatGreaterThanNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimLessOrEqualNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatLessOrEqualNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimGreaterOrEqualNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatGreaterOrEqualNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimEqualNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatEqualNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_NOT_EQUAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        final byte state = profiles[currentPC];
                        if (receiver instanceof final Long lhs && arg instanceof final Long rhs) {
                            if ((state & 0b100) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b100;
                            }
                            pushResolved(frame, sp++, PrimNotEqualNode.doLong(lhs, rhs));
                            break;
                        } else if (receiver instanceof final Double lhs && arg instanceof final Double rhs) {
                            if ((state & 0b1000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b1000;
                            }
                            pushResolved(frame, sp++, PrimSmallFloatNotEqualNode.doDouble(lhs, rhs));
                            break;
                        } else {
                            if ((state & 0b10000) == 0) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                profiles[currentPC] |= 0b10000;
                            }
                            externalizePCAndSP(frame, pc, sp);
                            final Object result = send(frame, currentPC, receiver, arg);
                            push(frame, currentPC, sp++, result);
                            pc = checkPCAfterSend(frame, pc);
                            break;
                        }
                    }
                    case BC.BYTECODE_PRIM_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        pushResolved(frame, sp++, uncheckedCast(data[currentPC], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        pushResolved(frame, sp++, !uncheckedCast(data[currentPC], SqueakObjectIdentityNodeGen.class).execute(this, receiver, arg));
                        break;
                    }
                    case BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, send(frame, currentPC, receiver));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, send(frame, currentPC, receiver, arg));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.BYTECODE_PRIM_AT_PUT, //
                        BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                        BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                        final Object arg2 = pop(frame, --sp);
                        final Object arg1 = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, send(frame, currentPC, receiver, arg1, arg2));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    default: {
                        throw unknownBytecode();
                    }
                }
            }
        } catch (final StackOverflowError e) {
            CompilerDirectives.transferToInterpreter();
            throw getContext().tryToSignalLowSpace(frame, e);
        } finally {
            if (CompilerDirectives.hasNextTier() && loopCounter.value > 0) {
                LoopNode.reportLoopCount(this, loopCounter.value);
            }
        }
        assert returnValue != null;
        return returnValue;
    }

    static int longJump(final byte b, final int nextByte) {
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
        static final byte PUSH_TEMP_VAR_0 = (byte) 16;
        static final byte PUSH_TEMP_VAR_1 = (byte) 17;
        static final byte PUSH_TEMP_VAR_2 = (byte) 18;
        static final byte PUSH_TEMP_VAR_3 = (byte) 19;
        static final byte PUSH_TEMP_VAR_4 = (byte) 20;
        static final byte PUSH_TEMP_VAR_5 = (byte) 21;
        static final byte PUSH_TEMP_VAR_6 = (byte) 22;
        static final byte PUSH_TEMP_VAR_7 = (byte) 23;
        static final byte PUSH_TEMP_VAR_8 = (byte) 24;
        static final byte PUSH_TEMP_VAR_9 = (byte) 25;
        static final byte PUSH_TEMP_VAR_A = (byte) 26;
        static final byte PUSH_TEMP_VAR_B = (byte) 27;
        static final byte PUSH_TEMP_VAR_C = (byte) 28;
        static final byte PUSH_TEMP_VAR_D = (byte) 29;
        static final byte PUSH_TEMP_VAR_E = (byte) 30;
        static final byte PUSH_TEMP_VAR_F = (byte) 31;
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
        static final byte PUSH_LIT_VAR_00 = (byte) 64;
        static final byte PUSH_LIT_VAR_01 = (byte) 65;
        static final byte PUSH_LIT_VAR_02 = (byte) 66;
        static final byte PUSH_LIT_VAR_03 = (byte) 67;
        static final byte PUSH_LIT_VAR_04 = (byte) 68;
        static final byte PUSH_LIT_VAR_05 = (byte) 69;
        static final byte PUSH_LIT_VAR_06 = (byte) 70;
        static final byte PUSH_LIT_VAR_07 = (byte) 71;
        static final byte PUSH_LIT_VAR_08 = (byte) 72;
        static final byte PUSH_LIT_VAR_09 = (byte) 73;
        static final byte PUSH_LIT_VAR_0A = (byte) 74;
        static final byte PUSH_LIT_VAR_0B = (byte) 75;
        static final byte PUSH_LIT_VAR_0C = (byte) 76;
        static final byte PUSH_LIT_VAR_0D = (byte) 77;
        static final byte PUSH_LIT_VAR_0E = (byte) 78;
        static final byte PUSH_LIT_VAR_0F = (byte) 79;
        static final byte PUSH_LIT_VAR_10 = (byte) 80;
        static final byte PUSH_LIT_VAR_11 = (byte) 81;
        static final byte PUSH_LIT_VAR_12 = (byte) 82;
        static final byte PUSH_LIT_VAR_13 = (byte) 83;
        static final byte PUSH_LIT_VAR_14 = (byte) 84;
        static final byte PUSH_LIT_VAR_15 = (byte) 85;
        static final byte PUSH_LIT_VAR_16 = (byte) 86;
        static final byte PUSH_LIT_VAR_17 = (byte) 87;
        static final byte PUSH_LIT_VAR_18 = (byte) 88;
        static final byte PUSH_LIT_VAR_19 = (byte) 89;
        static final byte PUSH_LIT_VAR_1A = (byte) 90;
        static final byte PUSH_LIT_VAR_1B = (byte) 91;
        static final byte PUSH_LIT_VAR_1C = (byte) 92;
        static final byte PUSH_LIT_VAR_1D = (byte) 93;
        static final byte PUSH_LIT_VAR_1E = (byte) 94;
        static final byte PUSH_LIT_VAR_1F = (byte) 95;
        static final byte POP_INTO_RCVR_VAR_0 = (byte) 96;
        static final byte POP_INTO_RCVR_VAR_1 = (byte) 97;
        static final byte POP_INTO_RCVR_VAR_2 = (byte) 98;
        static final byte POP_INTO_RCVR_VAR_3 = (byte) 99;
        static final byte POP_INTO_RCVR_VAR_4 = (byte) 100;
        static final byte POP_INTO_RCVR_VAR_5 = (byte) 101;
        static final byte POP_INTO_RCVR_VAR_6 = (byte) 102;
        static final byte POP_INTO_RCVR_VAR_7 = (byte) 103;
        static final byte POP_INTO_TEMP_VAR_0 = (byte) 104;
        static final byte POP_INTO_TEMP_VAR_1 = (byte) 105;
        static final byte POP_INTO_TEMP_VAR_2 = (byte) 106;
        static final byte POP_INTO_TEMP_VAR_3 = (byte) 107;
        static final byte POP_INTO_TEMP_VAR_4 = (byte) 108;
        static final byte POP_INTO_TEMP_VAR_5 = (byte) 109;
        static final byte POP_INTO_TEMP_VAR_6 = (byte) 110;
        static final byte POP_INTO_TEMP_VAR_7 = (byte) 111;
        static final byte PUSH_RECEIVER = (byte) 112;
        static final byte PUSH_CONSTANT_TRUE = (byte) 113;
        static final byte PUSH_CONSTANT_FALSE = (byte) 114;
        static final byte PUSH_CONSTANT_NIL = (byte) 115;
        static final byte PUSH_CONSTANT_MINUS_ONE = (byte) 116;
        static final byte PUSH_CONSTANT_ZERO = (byte) 117;
        static final byte PUSH_CONSTANT_ONE = (byte) 118;
        static final byte PUSH_CONSTANT_TWO = (byte) 119;
        static final byte RETURN_RECEIVER = (byte) 120;
        static final byte RETURN_TRUE = (byte) 121;
        static final byte RETURN_FALSE = (byte) 122;
        static final byte RETURN_NIL = (byte) 123;
        static final byte RETURN_TOP_FROM_METHOD = (byte) 124;
        static final byte RETURN_TOP_FROM_BLOCK = (byte) 125;

        static final byte EXTENDED_PUSH = (byte) 128;
        static final byte EXTENDED_STORE = (byte) 129;
        static final byte EXTENDED_POP = (byte) 130;
        static final byte SINGLE_EXTENDED_SEND = (byte) 131;
        static final byte DOUBLE_EXTENDED_DO_ANYTHING = (byte) 132;
        static final byte SINGLE_EXTENDED_SUPER = (byte) 133;
        static final byte SECOND_EXTENDED_SEND = (byte) 134;
        static final byte POP_STACK = (byte) 135;
        static final byte DUPLICATE_TOP = (byte) 136;

        static final byte PUSH_ACTIVE_CONTEXT = (byte) 137;
        static final byte PUSH_NEW_ARRAY = (byte) 138;
        static final byte CALL_PRIMITIVE = (byte) 139;
        static final byte PUSH_REMOTE_TEMP_LONG = (byte) 140;
        static final byte STORE_REMOTE_TEMP_LONG = (byte) 141;
        static final byte POP_INTO_REMOTE_TEMP_LONG = (byte) 142;
        static final byte PUSH_CLOSURE_COPY_COPIED_VALUES = (byte) 143;

        static final byte SHORT_UJUMP_0 = (byte) 144;
        static final byte SHORT_UJUMP_1 = (byte) 145;
        static final byte SHORT_UJUMP_2 = (byte) 146;
        static final byte SHORT_UJUMP_3 = (byte) 147;
        static final byte SHORT_UJUMP_4 = (byte) 148;
        static final byte SHORT_UJUMP_5 = (byte) 149;
        static final byte SHORT_UJUMP_6 = (byte) 150;
        static final byte SHORT_UJUMP_7 = (byte) 151;
        static final byte SHORT_CJUMP_FALSE_0 = (byte) 152;
        static final byte SHORT_CJUMP_FALSE_1 = (byte) 153;
        static final byte SHORT_CJUMP_FALSE_2 = (byte) 154;
        static final byte SHORT_CJUMP_FALSE_3 = (byte) 155;
        static final byte SHORT_CJUMP_FALSE_4 = (byte) 156;
        static final byte SHORT_CJUMP_FALSE_5 = (byte) 157;
        static final byte SHORT_CJUMP_FALSE_6 = (byte) 158;
        static final byte SHORT_CJUMP_FALSE_7 = (byte) 159;
        static final byte LONG_UJUMP_0 = (byte) 160;
        static final byte LONG_UJUMP_1 = (byte) 161;
        static final byte LONG_UJUMP_2 = (byte) 162;
        static final byte LONG_UJUMP_3 = (byte) 163;
        static final byte LONG_UJUMP_4 = (byte) 164;
        static final byte LONG_UJUMP_5 = (byte) 165;
        static final byte LONG_UJUMP_6 = (byte) 166;
        static final byte LONG_UJUMP_7 = (byte) 167;
        static final byte LONG_CJUMP_TRUE_0 = (byte) 168;
        static final byte LONG_CJUMP_TRUE_1 = (byte) 169;
        static final byte LONG_CJUMP_TRUE_2 = (byte) 170;
        static final byte LONG_CJUMP_TRUE_3 = (byte) 171;
        static final byte LONG_CJUMP_FALSE_0 = (byte) 172;
        static final byte LONG_CJUMP_FALSE_1 = (byte) 173;
        static final byte LONG_CJUMP_FALSE_2 = (byte) 174;
        static final byte LONG_CJUMP_FALSE_3 = (byte) 175;

        // 176-191 were sendArithmeticSelectorBytecode
        static final byte BYTECODE_PRIM_ADD = (byte) 176;
        static final byte BYTECODE_PRIM_SUBTRACT = (byte) 177;
        static final byte BYTECODE_PRIM_LESS_THAN = (byte) 178;
        static final byte BYTECODE_PRIM_GREATER_THAN = (byte) 179;
        static final byte BYTECODE_PRIM_LESS_OR_EQUAL = (byte) 180;
        static final byte BYTECODE_PRIM_GREATER_OR_EQUAL = (byte) 181;
        static final byte BYTECODE_PRIM_EQUAL = (byte) 182;
        static final byte BYTECODE_PRIM_NOT_EQUAL = (byte) 183;
        static final byte BYTECODE_PRIM_MULTIPLY = (byte) 184;
        static final byte BYTECODE_PRIM_DIVIDE = (byte) 185;
        static final byte BYTECODE_PRIM_MOD = (byte) 186;
        static final byte BYTECODE_PRIM_MAKE_POINT = (byte) 187;
        static final byte BYTECODE_PRIM_BIT_SHIFT = (byte) 188;
        static final byte BYTECODE_PRIM_DIV = (byte) 189;
        static final byte BYTECODE_PRIM_BIT_AND = (byte) 190;
        static final byte BYTECODE_PRIM_BIT_OR = (byte) 191;

        // 192-207 were sendCommonSelectorBytecode
        static final byte BYTECODE_PRIM_AT = (byte) 192;
        static final byte BYTECODE_PRIM_AT_PUT = (byte) 193;
        static final byte BYTECODE_PRIM_SIZE = (byte) 194;
        static final byte BYTECODE_PRIM_NEXT = (byte) 195;
        static final byte BYTECODE_PRIM_NEXT_PUT = (byte) 196;
        static final byte BYTECODE_PRIM_AT_END = (byte) 197;
        static final byte BYTECODE_PRIM_IDENTICAL = (byte) 198;
        static final byte BYTECODE_PRIM_CLASS = (byte) 199;
        static final byte BYTECODE_PRIM_NOT_IDENTICAL = (byte) 200;
        static final byte BYTECODE_PRIM_VALUE = (byte) 201;
        static final byte BYTECODE_PRIM_VALUE_WITH_ARG = (byte) 202;
        static final byte BYTECODE_PRIM_DO = (byte) 203;
        static final byte BYTECODE_PRIM_NEW = (byte) 204;
        static final byte BYTECODE_PRIM_NEW_WITH_ARG = (byte) 205;
        static final byte BYTECODE_PRIM_POINT_X = (byte) 206;
        static final byte BYTECODE_PRIM_POINT_Y = (byte) 207;

        static final byte SEND_LIT_SEL0_0 = (byte) 208;
        static final byte SEND_LIT_SEL0_1 = (byte) 209;
        static final byte SEND_LIT_SEL0_2 = (byte) 210;
        static final byte SEND_LIT_SEL0_3 = (byte) 211;
        static final byte SEND_LIT_SEL0_4 = (byte) 212;
        static final byte SEND_LIT_SEL0_5 = (byte) 213;
        static final byte SEND_LIT_SEL0_6 = (byte) 214;
        static final byte SEND_LIT_SEL0_7 = (byte) 215;
        static final byte SEND_LIT_SEL0_8 = (byte) 216;
        static final byte SEND_LIT_SEL0_9 = (byte) 217;
        static final byte SEND_LIT_SEL0_A = (byte) 218;
        static final byte SEND_LIT_SEL0_B = (byte) 219;
        static final byte SEND_LIT_SEL0_C = (byte) 220;
        static final byte SEND_LIT_SEL0_D = (byte) 221;
        static final byte SEND_LIT_SEL0_E = (byte) 222;
        static final byte SEND_LIT_SEL0_F = (byte) 223;
        static final byte SEND_LIT_SEL1_0 = (byte) 224;
        static final byte SEND_LIT_SEL1_1 = (byte) 225;
        static final byte SEND_LIT_SEL1_2 = (byte) 226;
        static final byte SEND_LIT_SEL1_3 = (byte) 227;
        static final byte SEND_LIT_SEL1_4 = (byte) 228;
        static final byte SEND_LIT_SEL1_5 = (byte) 229;
        static final byte SEND_LIT_SEL1_6 = (byte) 230;
        static final byte SEND_LIT_SEL1_7 = (byte) 231;
        static final byte SEND_LIT_SEL1_8 = (byte) 232;
        static final byte SEND_LIT_SEL1_9 = (byte) 233;
        static final byte SEND_LIT_SEL1_A = (byte) 234;
        static final byte SEND_LIT_SEL1_B = (byte) 235;
        static final byte SEND_LIT_SEL1_C = (byte) 236;
        static final byte SEND_LIT_SEL1_D = (byte) 237;
        static final byte SEND_LIT_SEL1_E = (byte) 238;
        static final byte SEND_LIT_SEL1_F = (byte) 239;
        static final byte SEND_LIT_SEL2_0 = (byte) 240;
        static final byte SEND_LIT_SEL2_1 = (byte) 241;
        static final byte SEND_LIT_SEL2_2 = (byte) 242;
        static final byte SEND_LIT_SEL2_3 = (byte) 243;
        static final byte SEND_LIT_SEL2_4 = (byte) 244;
        static final byte SEND_LIT_SEL2_5 = (byte) 245;
        static final byte SEND_LIT_SEL2_6 = (byte) 246;
        static final byte SEND_LIT_SEL2_7 = (byte) 247;
        static final byte SEND_LIT_SEL2_8 = (byte) 248;
        static final byte SEND_LIT_SEL2_9 = (byte) 249;
        static final byte SEND_LIT_SEL2_A = (byte) 250;
        static final byte SEND_LIT_SEL2_B = (byte) 251;
        static final byte SEND_LIT_SEL2_C = (byte) 252;
        static final byte SEND_LIT_SEL2_D = (byte) 253;
        static final byte SEND_LIT_SEL2_E = (byte) 254;
        static final byte SEND_LIT_SEL2_F = (byte) 255;
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

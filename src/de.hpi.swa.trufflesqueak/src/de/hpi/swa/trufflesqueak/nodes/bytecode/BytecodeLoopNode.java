/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecode;

import static de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushLiteralVariableNode.READONLY_CLASSES;
import static de.hpi.swa.trufflesqueak.util.UnsafeUtils.uncheckedCast;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.BytecodeInterpreterSwitch;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.Returns.AbstractStandardSendReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
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
import de.hpi.swa.trufflesqueak.nodes.AbstractExecuteContextNode;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractSqueakBytecodeDecoder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnFromClosureNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSpecialNode.SendSpecial0Node.SendBytecode0;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSpecialNode.SendSpecial1Node.SendBytecode1;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial0NodeFactory.BytecodePrimClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial0NodeFactory.BytecodePrimPointXNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial0NodeFactory.BytecodePrimPointYNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial0NodeFactory.BytecodePrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimBitAndNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimBitOrNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimBitShiftNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimDivNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimDivideNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimGreaterOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimGreaterThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimIdenticalSistaV1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimLessOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimLessThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimMakePointNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimModNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimMultiplyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimNotEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimNotIdenticalSistaV1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialNodeFactory.SendSpecial1NodeFactory.BytecodePrimSubtractNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.Dispatch0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectedSuperNaryNode.DirectedSuperDispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchSuperNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectedSuperNaryNodeFactory.DirectedSuperDispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class BytecodeLoopNode extends AbstractExecuteContextNode implements BytecodeOSRNode {
    private static final int LOCAL_RETURN_PC = -2;

    private final CompiledCodeObject code;
    private final boolean isBlock;

    @CompilationFinal private int numArguments;

    @CompilationFinal(dimensions = 1) private final Object[] data;
    @CompilationFinal(dimensions = 1) private final ConditionProfile[] resolveProfiles;
    @CompilationFinal private Object osrMetadata;

    public BytecodeLoopNode(final CompiledCodeObject code) {
        this.code = code;
        isBlock = code.isCompiledBlock();
        numArguments = -1;
        final int maxPC = AbstractSqueakBytecodeDecoder.trailerPosition(code);
        data = new Object[maxPC];
        resolveProfiles = new ConditionProfile[maxPC];
        processBytecode(maxPC);
    }

    public BytecodeLoopNode(final BytecodeLoopNode original) {
        // reusable fields
        code = original.code;
        isBlock = original.isBlock;
        numArguments = original.numArguments;
        // fresh fields
        final int maxPC = AbstractSqueakBytecodeDecoder.trailerPosition(code);
        data = new Object[maxPC];
        resolveProfiles = new ConditionProfile[maxPC];
        processBytecode(maxPC);
        osrMetadata = null;
    }

    private void processBytecode(final int maxPC) {
        final byte[] bc = code.getBytes();
        final SqueakImageContext image = SqueakImageContext.getSlow();

        int pc = 0;
        int extA = 0;
        int extB = 0;

        while (pc < maxPC) {
            final int currentPC = pc++;
            final byte b = getByte(bc, currentPC);
            switch (b) {
                /* 1 byte bytecodes */
                case BC.PUSH_RCVR_VAR_0, BC.PUSH_RCVR_VAR_1, BC.PUSH_RCVR_VAR_2, BC.PUSH_RCVR_VAR_3, BC.PUSH_RCVR_VAR_4, BC.PUSH_RCVR_VAR_5, BC.PUSH_RCVR_VAR_6, BC.PUSH_RCVR_VAR_7, //
                    BC.PUSH_RCVR_VAR_8, BC.PUSH_RCVR_VAR_9, BC.PUSH_RCVR_VAR_A, BC.PUSH_RCVR_VAR_B, BC.PUSH_RCVR_VAR_C, BC.PUSH_RCVR_VAR_D, BC.PUSH_RCVR_VAR_E, BC.PUSH_RCVR_VAR_F: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                    BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                    data[currentPC] = createLiteralNode(code.getAndResolveLiteral(b & 0xF));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                    BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                    BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                    BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F, //
                    BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                    BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B, //
                    BC.PUSH_RECEIVER, BC.DUPLICATE_TOP, //
                    BC.POP_INTO_TEMP_VAR_0, BC.POP_INTO_TEMP_VAR_1, BC.POP_INTO_TEMP_VAR_2, BC.POP_INTO_TEMP_VAR_3, BC.POP_INTO_TEMP_VAR_4, BC.POP_INTO_TEMP_VAR_5, BC.POP_INTO_TEMP_VAR_6, BC.POP_INTO_TEMP_VAR_7, //
                    BC.POP_STACK: {
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.PUSH_CONSTANT_TRUE, BC.PUSH_CONSTANT_FALSE, BC.PUSH_CONSTANT_NIL, BC.PUSH_CONSTANT_ZERO, BC.PUSH_CONSTANT_ONE: {
                    break;
                }
                case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                    if (extB == 0) {
                        data[currentPC] = insert(GetOrCreateContextWithFrameNode.create());
                        break;
                    } else {
                        throw CompilerDirectives.shouldNotReachHere();
                    }
                }
                case BC.RETURN_RECEIVER, BC.RETURN_TRUE, BC.RETURN_FALSE, BC.RETURN_NIL, BC.RETURN_TOP_FROM_METHOD: {
                    data[currentPC] = isBlock ? new BlockReturnNode() : new NormalReturnNode();
                    break;
                }
                case BC.RETURN_NIL_FROM_BLOCK, BC.RETURN_TOP_FROM_BLOCK: {
                    data[currentPC] = new NormalReturnNode();
                    break;
                }
                case BC.EXT_NOP:
                    extA = extB = 0;
                    break;
                case BC.BYTECODE_PRIM_ADD: {
                    data[currentPC] = insert(BytecodePrimAddNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_SUBTRACT: {
                    data[currentPC] = insert(BytecodePrimSubtractNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_LESS_THAN: {
                    data[currentPC] = insert(BytecodePrimLessThanNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_GREATER_THAN: {
                    data[currentPC] = insert(BytecodePrimGreaterThanNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_LESS_OR_EQUAL: {
                    data[currentPC] = insert(BytecodePrimLessOrEqualNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_GREATER_OR_EQUAL: {
                    data[currentPC] = insert(BytecodePrimGreaterOrEqualNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_EQUAL: {
                    data[currentPC] = insert(BytecodePrimEqualNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NOT_EQUAL: {
                    data[currentPC] = insert(BytecodePrimNotEqualNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_MULTIPLY: {
                    data[currentPC] = insert(BytecodePrimMultiplyNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_DIVIDE: {
                    data[currentPC] = insert(BytecodePrimDivideNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_MOD: {
                    data[currentPC] = insert(BytecodePrimModNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_MAKE_POINT: {
                    data[currentPC] = insert(BytecodePrimMakePointNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_SHIFT: {
                    data[currentPC] = insert(BytecodePrimBitShiftNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_DIV: {
                    data[currentPC] = insert(BytecodePrimDivNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_AND: {
                    data[currentPC] = insert(BytecodePrimBitAndNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_BIT_OR: {
                    data[currentPC] = insert(BytecodePrimBitOrNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_AT: {
                    data[currentPC] = insert(new SendBytecode1Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_AT_PUT: {
                    data[currentPC] = insert(new SendBytecode2Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_SIZE: {
                    data[currentPC] = insert(BytecodePrimSizeNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NEXT: {
                    data[currentPC] = insert(new SendBytecode0Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NEXT_PUT: {
                    data[currentPC] = insert(new SendBytecode1Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_AT_END: {
                    data[currentPC] = insert(new SendBytecode0Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_IDENTICAL: {
                    data[currentPC] = insert(BytecodePrimIdenticalSistaV1NodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_CLASS: {
                    data[currentPC] = insert(BytecodePrimClassNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NOT_IDENTICAL: {
                    data[currentPC] = insert(BytecodePrimNotIdenticalSistaV1NodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE: {
                    data[currentPC] = insert(new SendBytecode0Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_VALUE_WITH_ARG: {
                    data[currentPC] = insert(new SendBytecode1Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_DO: {
                    data[currentPC] = insert(new SendBytecode1Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NEW: {
                    data[currentPC] = insert(new SendBytecode0Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                    data[currentPC] = insert(new SendBytecode1Node(image.getSpecialSelector(b - 0x60)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_POINT_X: {
                    data[currentPC] = insert(BytecodePrimPointXNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.BYTECODE_PRIM_POINT_Y: {
                    data[currentPC] = insert(BytecodePrimPointYNodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                    BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(new SendBytecode0Node(selector));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                    BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(new SendBytecode1Node(selector));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.SEND_LIT_SEL2_0, BC.SEND_LIT_SEL2_1, BC.SEND_LIT_SEL2_2, BC.SEND_LIT_SEL2_3, BC.SEND_LIT_SEL2_4, BC.SEND_LIT_SEL2_5, BC.SEND_LIT_SEL2_6, BC.SEND_LIT_SEL2_7, //
                    BC.SEND_LIT_SEL2_8, BC.SEND_LIT_SEL2_9, BC.SEND_LIT_SEL2_A, BC.SEND_LIT_SEL2_B, BC.SEND_LIT_SEL2_C, BC.SEND_LIT_SEL2_D, BC.SEND_LIT_SEL2_E, BC.SEND_LIT_SEL2_F: {
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(b & 0xF);
                    data[currentPC] = insert(new SendBytecode2Node(selector));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    break;
                }
                case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                    final int offset = JumpBytecodes.calculateShortOffset(b);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsQuickNode.createForLoop(data, pc, 1, offset));
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
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL_VARIABLE: {
                    data[currentPC] = createLiteralNode(code.getAndResolveLiteral(getByteExtended(bc, pc, extA)));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_LITERAL, BC.EXT_PUSH_CHARACTER: {
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    pc++;
                    extA = 0;
                    break;
                }
                case BC.LONG_PUSH_TEMPORARY_VARIABLE, BC.PUSH_NEW_ARRAY, BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE, BC.LONG_STORE_TEMPORARY_VARIABLE: {
                    resolveProfiles[currentPC] = ConditionProfile.create();
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
                    data[currentPC] = insert(new SendBytecodeNaryNode(selector));
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_SEND_SUPER: {
                    final boolean isDirected = extB >= 64;
                    final int byte1 = getUnsignedInt(bc, pc++);
                    final int literalIndex = (byte1 >> 3) + (extA << 5);
                    final NativeObject selector = (NativeObject) code.getAndResolveLiteral(literalIndex);
                    if (isDirected) {
                        data[currentPC] = insert(new SendBytecodeSuperDirectedNode(selector));
                    } else {
                        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
                        data[currentPC] = insert(new SendBytecodeSuperNode(methodClass, selector));
                    }
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    extA = extB = 0;
                    break;
                }
                case BC.EXT_UNCONDITIONAL_JUMP: {
                    final int offset = JumpBytecodes.calculateLongExtendedOffset(getByte(bc, pc++), extB);
                    if (offset < 0) {
                        data[currentPC] = insert(CheckForInterruptsQuickNode.createForLoop(data, currentPC, 2, offset));
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
                    pc++;
                    final byte byteB = getByte(bc, pc++);
                    final boolean ignoreContext = (byteB & 0x40) != 0;
                    if (!ignoreContext) {
                        data[currentPC] = insert(GetOrCreateContextWithFrameNode.create());
                    }
                    extA = 0;
                    break;
                }
                case BC.EXT_PUSH_CLOSURE: {
                    final byte byteA = getByte(bc, pc++);
                    final int numArgs = (byteA & 7) + Math.floorMod(extA, 16) * 8;
                    final int blockSize = getByteExtended(bc, pc++, extB);
                    data[currentPC] = insert(new PushClosureNode(code, pc, numArgs));
                    pc += blockSize;
                    extA = extB = 0;
                    break;
                }
                case BC.PUSH_REMOTE_TEMP_LONG: {
                    data[currentPC] = insert(SqueakObjectAt0NodeGen.create());
                    resolveProfiles[currentPC] = ConditionProfile.create();
                    pc += 2;
                    break;
                }
                case BC.STORE_REMOTE_TEMP_LONG, BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                    data[currentPC] = insert(SqueakObjectAtPut0NodeGen.create());
                    pc += 2;
                    break;
                }
                default: {
                    throw CompilerDirectives.shouldNotReachHere();
                }
            }
        }
    }

    private static Node createLiteralNode(final Object literal) {
        if (literal instanceof final AbstractSqueakObjectWithClassAndHash l) {
            final String squeakClassName = l.getSqueakClassName();
            if (ArrayUtils.containsEqual(READONLY_CLASSES, squeakClassName)) {
                return new ReadLiteralVariableReadonlyNode(literal);
            } else {
                return new ReadLiteralVariableNode();
            }
        } else {
            throw SqueakException.create("Unexpected literal", literal);
        }
    }

    @Override
    @BytecodeInterpreterSwitch
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object execute(final VirtualFrame frame, final int startPC, final int startSP) {
        if (numArguments == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArguments = FrameAccess.getNumArguments(frame);
        }
        assert isBlock == FrameAccess.hasClosure(frame);

        final byte[] bc = uncheckedCast(code.getBytes(), byte[].class);

        int pc = startPC;
        int sp = startSP;
        int extA = 0;
        int extB = 0;

        final LoopCounter loopCounter = new LoopCounter();

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
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, FrameAccess.getReceiver(frame), b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_VAR_0, BC.PUSH_LIT_VAR_1, BC.PUSH_LIT_VAR_2, BC.PUSH_LIT_VAR_3, BC.PUSH_LIT_VAR_4, BC.PUSH_LIT_VAR_5, BC.PUSH_LIT_VAR_6, BC.PUSH_LIT_VAR_7, //
                        BC.PUSH_LIT_VAR_8, BC.PUSH_LIT_VAR_9, BC.PUSH_LIT_VAR_A, BC.PUSH_LIT_VAR_B, BC.PUSH_LIT_VAR_C, BC.PUSH_LIT_VAR_D, BC.PUSH_LIT_VAR_E, BC.PUSH_LIT_VAR_F: {
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], AbstractLiteralVariableNode.class).execute(this, code, b & 0xF));
                        break;
                    }
                    case BC.PUSH_LIT_CONST_00, BC.PUSH_LIT_CONST_01, BC.PUSH_LIT_CONST_02, BC.PUSH_LIT_CONST_03, BC.PUSH_LIT_CONST_04, BC.PUSH_LIT_CONST_05, BC.PUSH_LIT_CONST_06, BC.PUSH_LIT_CONST_07, //
                        BC.PUSH_LIT_CONST_08, BC.PUSH_LIT_CONST_09, BC.PUSH_LIT_CONST_0A, BC.PUSH_LIT_CONST_0B, BC.PUSH_LIT_CONST_0C, BC.PUSH_LIT_CONST_0D, BC.PUSH_LIT_CONST_0E, BC.PUSH_LIT_CONST_0F, //
                        BC.PUSH_LIT_CONST_10, BC.PUSH_LIT_CONST_11, BC.PUSH_LIT_CONST_12, BC.PUSH_LIT_CONST_13, BC.PUSH_LIT_CONST_14, BC.PUSH_LIT_CONST_15, BC.PUSH_LIT_CONST_16, BC.PUSH_LIT_CONST_17, //
                        BC.PUSH_LIT_CONST_18, BC.PUSH_LIT_CONST_19, BC.PUSH_LIT_CONST_1A, BC.PUSH_LIT_CONST_1B, BC.PUSH_LIT_CONST_1C, BC.PUSH_LIT_CONST_1D, BC.PUSH_LIT_CONST_1E, BC.PUSH_LIT_CONST_1F: {
                        push(frame, currentPC, sp++, code.getAndResolveLiteral(b & 0x1F));
                        break;
                    }
                    case BC.PUSH_TEMP_VAR_0, BC.PUSH_TEMP_VAR_1, BC.PUSH_TEMP_VAR_2, BC.PUSH_TEMP_VAR_3, BC.PUSH_TEMP_VAR_4, BC.PUSH_TEMP_VAR_5, BC.PUSH_TEMP_VAR_6, BC.PUSH_TEMP_VAR_7, //
                        BC.PUSH_TEMP_VAR_8, BC.PUSH_TEMP_VAR_9, BC.PUSH_TEMP_VAR_A, BC.PUSH_TEMP_VAR_B: {
                        push(frame, currentPC, sp++, getTemp(frame, b & 0xF));
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
                    case BC.PUSH_CONSTANT_ZERO: {
                        pushResolved(frame, sp++, 0L);
                        break;
                    }
                    case BC.PUSH_CONSTANT_ONE: {
                        pushResolved(frame, sp++, 1L);
                        break;
                    }
                    case BC.EXT_PUSH_PSEUDO_VARIABLE: {
                        if (extB == 0) {
                            pushResolved(frame, sp++, uncheckedCast(data[currentPC], GetOrCreateContextWithFrameNode.class).executeGet(frame));
                            break;
                        } else {
                            throw CompilerDirectives.shouldNotReachHere();
                        }
                    }
                    case BC.DUPLICATE_TOP: {
                        push(frame, currentPC, sp, top(frame, sp));
                        sp++;
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
                    case BC.RETURN_NIL_FROM_BLOCK: {
                        returnValue = uncheckedCast(data[currentPC], NormalReturnNode.class).execute(frame, NilObject.SINGLETON);
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.RETURN_TOP_FROM_BLOCK: {
                        returnValue = uncheckedCast(data[currentPC], NormalReturnNode.class).execute(frame, top(frame, sp));
                        pc = LOCAL_RETURN_PC;
                        break;
                    }
                    case BC.EXT_NOP: {
                        extA = extB = 0;
                        break;
                    }
                    /* bytecode prims with 0 args */
                    case BC.BYTECODE_PRIM_SIZE, BC.BYTECODE_PRIM_NEXT, BC.BYTECODE_PRIM_AT_END, BC.BYTECODE_PRIM_CLASS, BC.BYTECODE_PRIM_VALUE, BC.BYTECODE_PRIM_NEW, BC.BYTECODE_PRIM_POINT_X, BC.BYTECODE_PRIM_POINT_Y: {
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecode0.class).executeOrRewrite(frame, data, currentPC, receiver));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    /* bytecode prims with 1 arg */
                    case BC.BYTECODE_PRIM_ADD, BC.BYTECODE_PRIM_SUBTRACT, BC.BYTECODE_PRIM_LESS_THAN, BC.BYTECODE_PRIM_GREATER_THAN, BC.BYTECODE_PRIM_LESS_OR_EQUAL, BC.BYTECODE_PRIM_GREATER_OR_EQUAL, BC.BYTECODE_PRIM_EQUAL, //
                        BC.BYTECODE_PRIM_NOT_EQUAL, BC.BYTECODE_PRIM_MULTIPLY, BC.BYTECODE_PRIM_DIVIDE, BC.BYTECODE_PRIM_MOD, BC.BYTECODE_PRIM_MAKE_POINT, BC.BYTECODE_PRIM_BIT_SHIFT, BC.BYTECODE_PRIM_DIV, BC.BYTECODE_PRIM_BIT_AND, //
                        BC.BYTECODE_PRIM_BIT_OR, BC.BYTECODE_PRIM_AT, BC.BYTECODE_PRIM_NEXT_PUT, BC.BYTECODE_PRIM_IDENTICAL, BC.BYTECODE_PRIM_NOT_IDENTICAL, BC.BYTECODE_PRIM_VALUE_WITH_ARG, BC.BYTECODE_PRIM_DO, BC.BYTECODE_PRIM_NEW_WITH_ARG: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecode1.class).executeOrRewrite(frame, data, currentPC, receiver, arg));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.SEND_LIT_SEL0_0, BC.SEND_LIT_SEL0_1, BC.SEND_LIT_SEL0_2, BC.SEND_LIT_SEL0_3, BC.SEND_LIT_SEL0_4, BC.SEND_LIT_SEL0_5, BC.SEND_LIT_SEL0_6, BC.SEND_LIT_SEL0_7, //
                        BC.SEND_LIT_SEL0_8, BC.SEND_LIT_SEL0_9, BC.SEND_LIT_SEL0_A, BC.SEND_LIT_SEL0_B, BC.SEND_LIT_SEL0_C, BC.SEND_LIT_SEL0_D, BC.SEND_LIT_SEL0_E, BC.SEND_LIT_SEL0_F: {
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecode0Node.class).execute(frame, receiver));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.SEND_LIT_SEL1_0, BC.SEND_LIT_SEL1_1, BC.SEND_LIT_SEL1_2, BC.SEND_LIT_SEL1_3, BC.SEND_LIT_SEL1_4, BC.SEND_LIT_SEL1_5, BC.SEND_LIT_SEL1_6, BC.SEND_LIT_SEL1_7, //
                        BC.SEND_LIT_SEL1_8, BC.SEND_LIT_SEL1_9, BC.SEND_LIT_SEL1_A, BC.SEND_LIT_SEL1_B, BC.SEND_LIT_SEL1_C, BC.SEND_LIT_SEL1_D, BC.SEND_LIT_SEL1_E, BC.SEND_LIT_SEL1_F: {
                        final Object arg = pop(frame, --sp);
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecode1Node.class).execute(frame, receiver, arg));
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
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecode2Node.class).execute(frame, receiver, arg1, arg2));
                        pc = checkPCAfterSend(frame, pc);
                        break;
                    }
                    case BC.SHORT_UJUMP_0, BC.SHORT_UJUMP_1, BC.SHORT_UJUMP_2, BC.SHORT_UJUMP_3, BC.SHORT_UJUMP_4, BC.SHORT_UJUMP_5, BC.SHORT_UJUMP_6, BC.SHORT_UJUMP_7: {
                        final int offset = JumpBytecodes.calculateShortOffset(b);
                        pc += offset;
                        if (offset < 0) {
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
                                } else {
                                    FrameAccess.setInstructionPointer(frame, pc);
                                    uncheckedCast(data[currentPC], CheckForInterruptsQuickNode.class).execute(frame);
                                }
                                loopCounter.value = 0;
                            }
                        }
                        break;
                    }
                    case BC.SHORT_CJUMP_TRUE_0, BC.SHORT_CJUMP_TRUE_1, BC.SHORT_CJUMP_TRUE_2, BC.SHORT_CJUMP_TRUE_3, BC.SHORT_CJUMP_TRUE_4, BC.SHORT_CJUMP_TRUE_5, BC.SHORT_CJUMP_TRUE_6, BC.SHORT_CJUMP_TRUE_7: {
                        final Object stackValue = pop(frame, --sp);
                        if (stackValue instanceof final Boolean condition) {
                            if (uncheckedCast(data[currentPC], CountingConditionProfile.class).profile(condition)) {
                                pc += JumpBytecodes.calculateShortOffset(b);
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
                                pc += JumpBytecodes.calculateShortOffset(b);
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
                        assert extB != 0 : "should use numExtB?";
                        break;
                    }
                    case BC.EXT_PUSH_RECEIVER_VARIABLE: {
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SqueakObjectAt0Node.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL_VARIABLE: {
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], AbstractLiteralVariableNode.class).execute(this, code, getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_LITERAL: {
                        push(frame, currentPC, sp++, code.getAndResolveLiteral(getByteExtended(bc, pc++, extA)));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_PUSH_TEMPORARY_VARIABLE: {
                        push(frame, currentPC, sp++, getTemp(frame, getUnsignedInt(bc, pc++)));
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
                    case BC.EXT_PUSH_INTEGER: {
                        pushResolved(frame, sp++, (long) getByteExtended(bc, pc++, extB));
                        extB = 0;
                        break;
                    }
                    case BC.EXT_PUSH_CHARACTER: {
                        pushResolved(frame, sp++, CharacterObject.valueOf(getByteExtended(bc, pc++, extA)));
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
                        push(frame, currentPC, sp++, uncheckedCast(data[currentPC], SendBytecodeNaryNode.class).execute(frame, receiver, arguments));
                        pc = checkPCAfterSend(frame, pc);
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
                        final ClassObject lookupClass = isDirected ? uncheckedCast(pop(frame, --sp), ClassObject.class).getResolvedSuperclass() : null;
                        final Object[] arguments = popN(frame, sp, numArgs);
                        sp -= numArgs;
                        final Object receiver = popReceiver(frame, --sp);
                        externalizePCAndSP(frame, pc, sp);
                        final Object result;
                        if (isDirected) {
                            result = uncheckedCast(data[currentPC], SendBytecodeSuperDirectedNode.class).execute(frame, lookupClass, receiver, arguments);
                        } else {
                            result = uncheckedCast(data[currentPC], SendBytecodeSuperNode.class).execute(frame, receiver, arguments);
                        }
                        push(frame, currentPC, sp++, result);
                        pc = checkPCAfterSend(frame, pc);
                        extA = extB = 0;
                        break;
                    }
                    case BC.EXT_UNCONDITIONAL_JUMP: {
                        final int offset = JumpBytecodes.calculateLongExtendedOffset(getByte(bc, pc++), extB);
                        pc += offset;
                        if (offset < 0) {
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
                                } else {
                                    FrameAccess.setInstructionPointer(frame, pc);
                                    uncheckedCast(data[currentPC], CheckForInterruptsQuickNode.class).execute(frame);
                                }
                                loopCounter.value = 0;
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
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(bc, pc++, extA), pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_AND_POP_LITERAL_VARIABLE: {
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, code.getAndResolveLiteral(getByteExtended(bc, pc++, extA)), ASSOCIATION.VALUE, pop(frame, --sp));
                        extA = 0;
                        break;
                    }
                    case BC.LONG_STORE_AND_POP_TEMPORARY_VARIABLE: {
                        setStackValue(frame, getByte(bc, pc++), pop(frame, --sp));
                        break;
                    }
                    case BC.EXT_STORE_RECEIVER_VARIABLE: {
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, FrameAccess.getReceiver(frame), getByteExtended(bc, pc++, extA), top(frame, sp));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_STORE_LITERAL_VARIABLE: {
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, code.getAndResolveLiteral(getByteExtended(bc, pc++, extA)), ASSOCIATION.VALUE, top(frame, sp));
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
                        final CompiledCodeObject block = uncheckedCast(code.getAndResolveLiteral(literalIndex), CompiledCodeObject.class);
                        assert block.assertNotForwarded();
                        CompilerAsserts.partialEvaluationConstant(block);
                        final int blockInitialPC = block.getInitialPC();
                        final int blockNumArgs = block.getNumArgs();
                        final byte byteB = getByte(bc, pc++);
                        final int numCopied = Byte.toUnsignedInt(byteB) & 63;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        final boolean ignoreContext = (byteB & 0x40) != 0;
                        final boolean receiverOnStack = (byteB & 0x80) != 0;
                        final ContextObject outerContext = ignoreContext ? null : uncheckedCast(data[currentPC], GetOrCreateContextWithFrameNode.class).executeGet(frame);
                        final Object receiver = receiverOnStack ? pop(frame, --sp) : FrameAccess.getReceiver(frame);
                        final SqueakImageContext image = getContext();
                        pushResolved(frame, sp++, new BlockClosureObject(image, image.getFullBlockClosureClass(), block, blockInitialPC, blockNumArgs, copiedValues, receiver, outerContext));
                        extA = 0;
                        break;
                    }
                    case BC.EXT_PUSH_CLOSURE: {
                        final int byteA = getUnsignedInt(bc, pc++);
                        final int numCopied = (byteA >> 3 & 0x7) + Math.floorDiv(extA, 16) * 8;
                        final Object[] copiedValues = popN(frame, sp, numCopied);
                        sp -= numCopied;
                        pushResolved(frame, sp++, uncheckedCast(data[currentPC], PushClosureNode.class).execute(frame, copiedValues));
                        final int blockSize = getByteExtended(bc, pc++, extB);
                        pc += blockSize;
                        extA = extB = 0;
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
                    case BC.STORE_AND_POP_REMOTE_TEMP_LONG: {
                        final int remoteTempIndex = getUnsignedInt(bc, pc++);
                        final int tempVectorIndex = getUnsignedInt(bc, pc++);
                        uncheckedCast(data[currentPC], SqueakObjectAtPut0Node.class).execute(this, getTemp(frame, tempVectorIndex), remoteTempIndex, pop(frame, --sp));
                        break;
                    }
                    default: {
                        throw CompilerDirectives.shouldNotReachHere("Unknown bytecode");
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

    static final class NormalReturnNode extends AbstractNode {
        private final ConditionProfile hasModifiedSenderProfile = ConditionProfile.create();

        private Object execute(final VirtualFrame frame, final Object returnValue) {
            if (hasModifiedSenderProfile.profile(FrameAccess.hasModifiedSender(frame))) {
                throw new NonVirtualReturn(returnValue, FrameAccess.getSender(frame));
            } else {
                assert returnValue != null && !FrameAccess.hasModifiedSender(frame);
                FrameAccess.terminateFrame(frame);
                return returnValue;
            }
        }
    }

    static final class BlockReturnNode extends AbstractNode {
        @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode;
        @Child private Dispatch2Node sendAboutToReturnNode;

        private Object execute(final VirtualFrame frame, final int pc, final int sp, final Object returnValue) {
            externalizePCAndSP(frame, pc, sp);
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canBeReturnedTo()) {
                final ContextObject firstMarkedContext = ReturnFromClosureNode.firstUnwindMarkedOrThrowNLR(FrameAccess.getSender(frame), homeContext, returnValue);
                if (firstMarkedContext != null) {
                    getSendAboutToReturnNode().execute(frame, getGetOrCreateContextNode().executeGet(frame), returnValue, firstMarkedContext);
                }
            }
            LogUtils.SCHEDULING.info("ReturnFromClosureNode: sendCannotReturn");
            throw new CannotReturnToTarget(returnValue, getGetOrCreateContextNode().executeGet(frame));
        }

        private GetOrCreateContextWithFrameNode getGetOrCreateContextNode() {
            if (getOrCreateContextNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOrCreateContextNode = insert(GetOrCreateContextWithFrameNode.create());
            }
            return getOrCreateContextNode;
        }

        private Dispatch2Node getSendAboutToReturnNode() {
            if (sendAboutToReturnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sendAboutToReturnNode = insert(Dispatch2NodeGen.create(getContext().aboutToReturnSelector));
            }
            return sendAboutToReturnNode;
        }
    }

    abstract static class AbstractLiteralVariableNode extends AbstractNode {
        abstract Object execute(Node node, CompiledCodeObject code, int index);
    }

    static final class ReadLiteralVariableReadonlyNode extends AbstractLiteralVariableNode {
        private final Object value;

        ReadLiteralVariableReadonlyNode(final Object literal) {
            this.value = SqueakObjectAt0Node.executeUncached(literal, ASSOCIATION.VALUE);
        }

        Object execute(final Node node, final CompiledCodeObject code, final int index) {
            return value;
        }
    }

    static final class ReadLiteralVariableNode extends AbstractLiteralVariableNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0NodeGen.create();

        Object execute(final Node node, final CompiledCodeObject code, final int index) {
            return at0Node.execute(node, code.getAndResolveLiteral(index), ASSOCIATION.VALUE);
        }
    }

    static final class PushClosureNode extends AbstractNode {
        private final CompiledCodeObject shadowBlock;
        private final int numArgs;
        private final int closureStartPC;

        @Child private GetOrCreateContextWithFrameNode getOrCreateContextNode = GetOrCreateContextWithFrameNode.create();

        PushClosureNode(final CompiledCodeObject code, final int pc, final int numArgs) {
            this.closureStartPC = code.getInitialPC() + pc;
            shadowBlock = code.getOrCreateShadowBlock(closureStartPC);
            this.numArgs = numArgs;
        }

        BlockClosureObject execute(final VirtualFrame frame, final Object[] copiedValues) {
            final SqueakImageContext image = getContext();
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(image, image.blockClosureClass, shadowBlock, closureStartPC, numArgs, copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    public abstract static class AbstractSendBytecodeNode extends AbstractNode {
        @CompilationFinal private ConditionProfile returnExceptionProfile;
        @CompilationFinal private BranchProfile terminateProfile;

        protected final Object handleReturnException(final VirtualFrame frame, final AbstractStandardSendReturn returnException) {
            if (getReturnExceptionProfile().profile(returnException.targetIsFrame(frame))) {
                return returnException.getReturnValue();
            } else {
                if (returnException instanceof NonLocalReturn) {
                    terminateProfile.enter();
                    FrameAccess.terminateFrame(frame);
                }
                throw returnException;
            }
        }

        private ConditionProfile getReturnExceptionProfile() {
            if (returnExceptionProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                returnExceptionProfile = ConditionProfile.create();
                terminateProfile = BranchProfile.create();
            }
            return returnExceptionProfile;
        }
    }

    public static final class SendBytecode0Node extends AbstractSendBytecodeNode implements SendBytecode0 {
        @Child private Dispatch0Node dispatchNode;

        public SendBytecode0Node(final NativeObject selector) {
            dispatchNode = Dispatch0NodeGen.create(selector);
        }

        @Override
        public Object executeOrRewrite(final VirtualFrame frame, final Object[] data, final int currentPC, final Object receiver) {
            return execute(frame, receiver);
        }

        public Object execute(final VirtualFrame frame, final Object receiver) {
            try {
                return dispatchNode.execute(frame, receiver);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    public static final class SendBytecode1Node extends AbstractSendBytecodeNode implements SendBytecode1 {
        @Child private Dispatch1Node dispatchNode;

        public SendBytecode1Node(final NativeObject selector) {
            dispatchNode = Dispatch1NodeGen.create(selector);
        }

        @Override
        public Object executeOrRewrite(final VirtualFrame frame, final Object[] data, final int currentPC, final Object receiver, final Object arg) {
            return execute(frame, receiver, arg);
        }

        Object execute(final VirtualFrame frame, final Object receiver, final Object arg) {
            try {
                return dispatchNode.execute(frame, receiver, arg);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    static final class SendBytecode2Node extends AbstractSendBytecodeNode {
        @Child private Dispatch2Node dispatchNode;

        SendBytecode2Node(final NativeObject selector) {
            dispatchNode = Dispatch2NodeGen.create(selector);
        }

        Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
            try {
                return dispatchNode.execute(frame, receiver, arg1, arg2);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    static final class SendBytecodeNaryNode extends AbstractSendBytecodeNode {
        @Child private DispatchNaryNode dispatchNode;

        SendBytecodeNaryNode(final NativeObject selector) {
            dispatchNode = DispatchNaryNodeGen.create(selector);
        }

        Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            try {
                return dispatchNode.execute(frame, receiver, arguments);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    static final class SendBytecodeSuperDirectedNode extends AbstractSendBytecodeNode {
        @Child private DirectedSuperDispatchNaryNode dispatchNode;

        SendBytecodeSuperDirectedNode(final NativeObject selector) {
            dispatchNode = DirectedSuperDispatchNaryNodeGen.create(selector);
        }

        Object execute(final VirtualFrame frame, final ClassObject lookupClass, final Object receiver, final Object[] arguments) {
            try {
                return dispatchNode.execute(frame, lookupClass, receiver, arguments);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    static final class SendBytecodeSuperNode extends AbstractSendBytecodeNode {
        @Child private DispatchSuperNaryNode dispatchNode;

        SendBytecodeSuperNode(final ClassObject methodClass, final NativeObject selector) {
            dispatchNode = DispatchSuperNaryNodeGen.create(methodClass, selector);
        }

        Object execute(final VirtualFrame frame, final Object receiver, final Object[] arguments) {
            try {
                return dispatchNode.execute(frame, receiver, arguments);
            } catch (final AbstractStandardSendReturn r) {
                return handleReturnException(frame, r);
            }
        }
    }

    private static byte getByte(final byte[] bc, final int pc) {
        return UnsafeUtils.getByte(bc, pc);
    }

    private static int getUnsignedInt(final byte[] bc, final int pc) {
        return Byte.toUnsignedInt(getByte(bc, pc));
    }

    private static int getByteExtended(final byte[] bc, final int pc, final int extend) {
        return getUnsignedInt(bc, pc) + (extend << 8);
    }

    private Object handleReturn(final VirtualFrame frame, final int currentPC, final int pc, final int sp, final Object result) {
        if (isBlock) {
            return uncheckedCast(data[currentPC], BlockReturnNode.class).execute(frame, pc, sp, result);
        } else {
            return uncheckedCast(data[currentPC], NormalReturnNode.class).execute(frame, result);
        }
    }

    private void push(final VirtualFrame frame, final int currentPC, final int sp, final Object value) {
        setStackValue(frame, sp, resolve(currentPC, value));
    }

    private Object resolve(final int currentPC, final Object value) {
        if (resolveProfiles[currentPC].profile(value instanceof AbstractSqueakObjectWithClassAndHash)) {
            return ((AbstractSqueakObjectWithClassAndHash) value).resolveForwardingPointer();
        } else {
            return value;
        }
    }

    private void pushResolved(final VirtualFrame frame, final int sp, final Object value) {
        setStackValue(frame, sp, value);
    }

    private Object pop(final VirtualFrame frame, final int sp) {
        assert sp >= numArguments;
        final int slotIndex = FrameAccess.toStackSlotIndex(frame, sp);
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        if (slotIndex < numberOfSlots) {
            // FIXME: use frame.getObject()
            final Object result = frame.getValue(slotIndex);
            frame.setObject(slotIndex, NilObject.SINGLETON);
            return result;
        } else {
            CompilerDirectives.transferToInterpreter();
            final int auxSlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            final Object result = frame.getAuxiliarySlot(auxSlotIndex);
            frame.setAuxiliarySlot(auxSlotIndex, NilObject.SINGLETON);
            return result;
        }
    }

    private Object popReceiver(final VirtualFrame frame, final int sp) {
        return getStackValue(frame, sp);
    }

    private Object[] popN(final VirtualFrame frame, final int sp, final int numPop) {
        return numPop == 0 ? ArrayUtils.EMPTY_ARRAY : popNExploded(frame, sp, numPop);
    }

    @ExplodeLoop
    private Object[] popNExploded(final VirtualFrame frame, final int sp, final int numPop) {
        final Object[] stackValues = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            stackValues[numPop - 1 - i] = pop(frame, sp - 1 - i);
        }
        return stackValues;
    }

    private Object top(final VirtualFrame frame, final int sp) {
        return getStackValue(frame, sp - 1);
    }

    private Object getTemp(final VirtualFrame frame, final int sp) {
        if (sp < numArguments) {
            return frame.getArguments()[FrameAccess.getArgumentStartIndex() + sp];
        } else {
            return getSlotValue(frame, FrameAccess.toStackSlotIndex(frame, sp));
        }
    }

    private Object getStackValue(final VirtualFrame frame, final int sp) {
        assert sp >= numArguments;
        return getSlotValue(frame, FrameAccess.toStackSlotIndex(frame, sp));
    }

    private void setStackValue(final VirtualFrame frame, final int sp, final Object value) {
        assert sp >= numArguments;
        setSlotValue(frame, FrameAccess.toStackSlotIndex(frame, sp), value);
    }

    private static Object getSlotValue(final Frame frame, final int slotIndex) {
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        if (slotIndex < numberOfSlots) {
            // FIXME: use frame.getObject()
            return frame.getValue(slotIndex);
        } else {
            CompilerDirectives.transferToInterpreter();
            final int auxSlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            return frame.getAuxiliarySlot(auxSlotIndex);
        }
    }

    private static void setSlotValue(final Frame frame, final int slotIndex, final Object value) {
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        if (slotIndex < numberOfSlots) {
            frame.setObject(slotIndex, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            final int auxSlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            frame.setAuxiliarySlot(auxSlotIndex, value);
        }
    }

    private static void externalizePCAndSP(final VirtualFrame frame, final int pc, final int sp) {
        FrameAccess.setInstructionPointer(frame, pc);
        FrameAccess.setStackPointer(frame, sp);
    }

    private static int checkPCAfterSend(final VirtualFrame frame, final int pc) {
        final int framePC = FrameAccess.getInstructionPointer(frame);
        if (pc != framePC) {
            CompilerDirectives.transferToInterpreter();
            return framePC;
        } else {
            return pc;
        }
    }

    private Object getErrorObject() {
        final SqueakImageContext image = getContext();
        final int primFailCode = image.getPrimFailCode();
        final ArrayObject errorTable = image.primitiveErrorTable;
        if (primFailCode < errorTable.getObjectLength()) {
            return errorTable.getObject(primFailCode);
        } else {
            return (long) primFailCode;
        }
    }

    private void sendMustBeBooleanInInterpreter(final VirtualFrame frame, final int pc, final Object stackValue) {
        CompilerDirectives.transferToInterpreter();
        FrameAccess.setInstructionPointer(frame, pc);
        final SqueakImageContext image = getContext();
        image.mustBeBooleanSelector.executeAsSymbolSlow(image, frame, stackValue);
        throw SqueakException.create("Should not be reached");
    }

    /**
     * Smaller than int[1], does not kill int[] on write and doesn't need bounds checks.
     */
    private static final class LoopCounter {
        private static final int CHECK_LOOP_STRIDE = 1 << 14;
        private static final double CHECK_LOOP_PROBABILITY = 1.0D / CHECK_LOOP_STRIDE;
        private int value;
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

    @Override
    public CompiledCodeObject getCodeObject() {
        return code;
    }

    /*
     * Bytecode OSR support
     */

    @Override
    public Object executeOSR(final VirtualFrame osrFrame, final int targetPCAndSP, final Object interpreterState) {
        return execute(osrFrame, targetPCAndSP & 0xFFFF, targetPCAndSP >>> 16);
    }

    @Override
    public Object getOSRMetadata() {
        return osrMetadata;
    }

    @Override
    public void setOSRMetadata(final Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }

    @Override
    public Object[] storeParentFrameInArguments(final VirtualFrame parentFrame) {
        return FrameAccess.storeParentFrameInArguments(parentFrame);
    }

    @Override
    public Frame restoreParentFrameFromArguments(final Object[] arguments) {
        return FrameAccess.restoreParentFrameFromArguments(arguments);
    }

    /*
     * Node metadata
     */

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public boolean hasTag(final Class<? extends Tag> tag) {
        return StandardTags.RootTag.class == tag;
    }

    @Override
    public String getDescription() {
        return code.toString();
    }

    @Override
    public SourceSection getSourceSection() {
        final Source source = code.getSource();
        return source.createSection(1, 1, source.getLength());
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Override
    public Node copy() {
        return new BytecodeLoopNode(this);
    }

    @Override
    public Node deepCopy() {
        return new BytecodeLoopNode(this);
    }
}

/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import static de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode.ensureFinite;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.bytecode.BytecodeConfig;
import com.oracle.truffle.api.bytecode.BytecodeLabel;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.bytecode.BytecodeParser;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.ConstantOperand;
import com.oracle.truffle.api.bytecode.GenerateBytecode;
import com.oracle.truffle.api.bytecode.Operation;
import com.oracle.truffle.api.bytecode.StackValue;
import com.oracle.truffle.api.bytecode.Variadic;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.exceptions.Returns;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.interpreter.InterpreterSistaV1Node;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/**
 * Open questions: - How do I access pc/sp? - Can I use/preallocate any frame slots or are they
 * exclusively managed by Bytecode DSL? - How would I add support for unstructured backjumps? Would
 * Bytecode DSL need to reconstruct loops or is this purely a security check?
 */
@ImportStatic(SqueakGuards.class)
@GenerateBytecode(//
                languageClass = SqueakLanguage.class, //
                defaultLocalValue = "defaultLocalValue()", //
                enableUncachedInterpreter = true)
public abstract class SmalltalkSistaV1Interpreter extends RootNode implements BytecodeRootNode {

    protected SmalltalkSistaV1Interpreter(final SqueakLanguage language, final FrameDescriptor frameDescriptor) {
        super(language, frameDescriptor);
    }

    static Object defaultLocalValue() {
        CompilerAsserts.neverPartOfCompilation("Must be cached and not triggered during compilation.");
        return NilObject.SINGLETON;
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class StoreIntoReceiverVariable {
        @Specialization
        static void perform(final VirtualFrame frame,
                        final int variableIndex,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, FrameAccess.getReceiver(frame), variableIndex, value);
        }
    }

    @Operation
    @ConstantOperand(type = Object.class)
    @ConstantOperand(type = int.class)
    public static final class StoreIntoLiteralVariable {
        @Specialization
        static void perform(final Object literal,
                        final int variableIndex,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, literal, variableIndex, value);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class LoadRemoteTemp {
        @Specialization
        static Object perform(final int indexInArray,
                        final Object temp,
                        @Bind final Node inlineTarget,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(inlineTarget, temp, indexInArray);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class StoreIntoRemoteTemp {
        @Specialization
        static void perform(final int indexInArray,
                        final Object temp,
                        final Object value,
                        @Bind final Node inlineTarget,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(inlineTarget, temp, indexInArray, value);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class ReceiverVariable {
        @Specialization
        static Object perform(final VirtualFrame frame,
                        final int variableIndex,
                        @Bind final Node node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, FrameAccess.getReceiver(frame), variableIndex);
        }
    }

    @Operation
    @ConstantOperand(type = Object.class)
    public static final class LiteralVariable {
        @Specialization
        static Object perform(final Object literal,
                        @Bind final Node node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, literal, ObjectLayouts.ASSOCIATION.VALUE);
        }
    }

    @Operation(forceCached = true)
    public static final class ActiveContext {
        @Specialization
        static ContextObject perform(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached(inline = true) final GetOrCreateContextWithFrameNode contextNode) {
            return contextNode.executeGet(frame, node);
        }
    }

    @Operation
    @ConstantOperand(type = CompiledCodeObject.class)
    public static final class FullClosure {
        @Specialization
        static BlockClosureObject perform(final CompiledCodeObject block, final Object receiver, final ContextObject outerContext,
                        @Variadic final Object[] copiedValues) {
            return new BlockClosureObject(false, block, block.getNumArgs(), copiedValues, receiver, outerContext);
        }
    }

    @Operation(forceCached = true)
    @ConstantOperand(type = CompiledCodeObject.class)
    public static final class Closure {
        @Specialization
        static BlockClosureObject perform(final VirtualFrame frame, final CompiledCodeObject block, @Variadic final Object[] copiedValues,
                        @Bind final Node node,
                        @Cached(inline = true) final GetOrCreateContextWithFrameNode getOrCreateContextNode) {
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame, node);
            return new BlockClosureObject(true, block, block.getShadowBlockNumArgs(), copiedValues, FrameAccess.getReceiver(frame), outerContext);
        }
    }

    @Operation
    public static final class ReturnTopFromMethod {
        @Specialization
        static Object perform(final VirtualFrame frame, final Object returnValue,
                        @Bind final Node location,
                        @Cached final InlinedConditionProfile hasModifiedSenderProfile) {
            assert !FrameAccess.hasClosure(frame);
            if (false) { // (hasModifiedSenderProfile.profile(location, FrameAccess.hasModifiedSender(frame))) {
                final AbstractSqueakObject sender = FrameAccess.getSender(frame);
                if (sender instanceof final ContextObject context && !context.isDead()) {
                    throw new NonVirtualReturn(returnValue, sender);
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new CannotReturnToTarget(returnValue, GetOrCreateContextWithFrameNode.executeUncached(frame));
                }
            } else {
                // FIXME: FrameAccess.terminateFrame(frame);
                return returnValue;
            }
        }
    }

    @Operation
    public static final class ReturnTopFromClosure {
        @Specialization
        static Object perform(final VirtualFrame frame, final Object returnValue) {
            assert FrameAccess.hasClosure(frame);
            // Target is sender of closure's home context.
            final ContextObject homeContext = FrameAccess.getClosure(frame).getHomeContext();
            if (homeContext.canReturnToSender()) {
                throw new Returns.NonLocalReturn(returnValue, homeContext);
            } else {
                CompilerDirectives.transferToInterpreter();
                final ContextObject contextObject = GetOrCreateContextWithFrameNode.executeUncached(frame);
                throw new CannotReturnToTarget(returnValue, contextObject);
            }
        }
    }

    @Operation
    public static final class ReturnNilFromClosure {
        @Specialization
        static Object perform(final VirtualFrame frame) {
            return ReturnTopFromClosure.perform(frame, NilObject.SINGLETON);
        }
    }

    @Operation
    @ConstantOperand(type = int.class)
    public static final class NewArray {
        @Specialization
        static ArrayObject perform(final int arraySize,
                        @Bind final Node location) {
            final SqueakImageContext image = SqueakImageContext.get(location);
            return image.asArrayOfObjects(ArrayUtils.withAll(arraySize, NilObject.SINGLETON));
        }
    }

    @Operation
    public static final class NewArrayFromStack {
        @Specialization
        static ArrayObject perform(@Variadic final Object[] values,
                        @Bind final Node location) {
            return SqueakImageContext.get(location).asArrayOfObjects(values);
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendNilary {
        @Specialization
        @InliningCutoff
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver,
                        @Bind final Node location,
                        @Bind final BytecodeNode bytecode) {
            return null; // FIXME
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendUnary {
        @Specialization
        @InliningCutoff
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1,
                        @Bind final Node location,
                        @Bind final BytecodeNode bytecode) {
            return null; // FIXME
        }
    }

    @Operation
    @ConstantOperand(type = NativeObject.class)
    public static final class SelfSendBinary {
        @Specialization
        @InliningCutoff
        static Object perform(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Bind final Node location,
                        @Bind final BytecodeNode bytecode) {
            return null; // FIXME
        }
    }

    @Operation
    public static final class Not {
        @Specialization
        static boolean perform(final VirtualFrame frame, final boolean value) {
            return !value;
        }
    }

    public static SmalltalkSistaV1Interpreter build(final CompiledCodeObject code) {
        final var rootNodes = SmalltalkSistaV1InterpreterGen.create(SqueakLanguage.get(null), BytecodeConfig.DEFAULT, new SistaV1BytecodeParser(code));
        return rootNodes.getNode(0);
    }

    static class SistaV1BytecodeParser implements BytecodeParser<SmalltalkSistaV1InterpreterGen.Builder> {
        private final CompiledCodeObject code;
        private final BytecodeLocal[] temporarySlots;
        private final StackValue[] stackSlots;
        private final Map<Integer, Integer> jumpStackPointers = new HashMap<>();
        private Map<Integer, Integer> loopLocations;
        private EconomicMap<Integer, BytecodeLabel> jumpLocations;

        private int index;
        private int sp;

        SistaV1BytecodeParser(final CompiledCodeObject code) {
            this.code = code;
            temporarySlots = new BytecodeLocal[code.getNumTemps()];
            stackSlots = new StackValue[code.getNumTemps() + code.getMaxNumStackSlots()];
        }

        @Override
        public void parse(final SmalltalkSistaV1InterpreterGen.Builder b) {
            b.beginRoot();
            b.beginBlock();
            detectJumps(b);
            for (int i = 0; i < temporarySlots.length; i++) {
                temporarySlots[i] = b.createLocal();
            }

            index = code.getStartPCZeroBased();
            sp = code.getInitialSP();
            final int trailerPosition = code.getMaxPCZeroBased();

            while (index < trailerPosition) {
                final boolean isLoopStart = loopLocations.containsKey(index);
                if (isLoopStart) {
                    /*
                     * Keep the original Sista control flow inside an infinite structured loop.
                     * Forward conditional jumps leave the loop through their regular labels; the
                     * backward jump itself is represented by endWhile(). A Sista loop condition
                     * may span several bytecodes, so it must not be inferred from the first
                     * bytecode at the back-jump target.
                     */
                    b.beginWhile();
                    b.emitLoadConstant(true);
                    b.beginBlock();
                }
                final BytecodeLabel jumpLabel = jumpLocations.get(index);
                if (jumpLabel != null) {
                    assert !isLoopStart;
                    final Integer jumpStackPointer = jumpStackPointers.get(index);
                    assert jumpStackPointer != null;
                    sp = jumpStackPointer;
                    b.emitLabel(jumpLabel);
                }
                index += translateBytecode(b, 0, 0, 0, 0);
                final boolean isLoopEnd = loopLocations.containsValue(index);
                if (isLoopEnd) {
                    assert !isLoopStart;
                    b.endBlock();
                    b.endWhile();
                }
            }
            b.endBlock();
            b.endRoot();
        }

        private void detectJumps(final SmalltalkSistaV1InterpreterGen.Builder b) {
            if (jumpLocations != null) {
                return;
            }
            loopLocations = new HashMap<>();
            jumpLocations = EconomicMap.create();
            index = code.getStartPCZeroBased();
            final int trailerPosition = code.getMaxPCZeroBased();
            while (index < trailerPosition) {
                index += detectJumps(b, 0, 0, 0, 0);
            }
        }

        private byte getByte(final int index) {
            return code.getBytes()[index];
        }

        private int getUnsignedInt(final int index) {
            return Byte.toUnsignedInt(getByte(index));
        }

        private int detectJumps(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int numExtB) {
            final int indexWithExt = index + extBytes;
            final int op = getUnsignedInt(indexWithExt);
            switch (op) {
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> recordUnconditionalJump(b, 1 + InterpreterSistaV1Node.calculateShortOffset(op));
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF, /* JumpOnTrue */
                    0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 /* JumpOnFalse */
                    -> recordConditionalJump(b, 1 + InterpreterSistaV1Node.calculateShortOffset(op));

                case 0xE0 -> {
                    return detectJumps(b, extBytes + 2, (extA << 8) + getUnsignedInt(indexWithExt + 1), extB, numExtB);
                }
                case 0xE1 -> {
                    final int byteValue = getUnsignedInt(indexWithExt + 1);
                    return detectJumps(b, extBytes + 2, extA, numExtB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue, numExtB + 1);
                }

                case 0xED -> recordUnconditionalJump(b, 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB));
                case 0xEE /* JumpOnTrue */, 0xEF /* JumpOnFalse */ -> recordConditionalJump(b, 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB));
                case 0xFA -> {
                    final int blockSize = getUnsignedInt(indexWithExt + 2) + (extB << 8);
                    return 3 + extBytes + blockSize;
                }
                default -> {
                    /* not a jump */
                }
            }
            if (op <= 223) {
                return 1 + extBytes;
            } else if (op <= 247) {
                return 2 + extBytes;
            } else {
                return 3 + extBytes;
            }
        }

        private void recordUnconditionalJump(final SmalltalkSistaV1InterpreterGen.Builder b, final int offset) {
            assert offset != 0;
            final int jumpTarget = index + offset;
            if (jumpTarget > index) {
                if (!jumpLocations.containsKey(jumpTarget)) {
                    assert !loopLocations.containsKey(jumpTarget) && !loopLocations.containsValue(jumpTarget);
                    jumpLocations.put(jumpTarget, b.createLabel());
                }
            } else {
                assert !jumpLocations.containsKey(jumpTarget);
                loopLocations.put(jumpTarget, index);
            }
        }

        private void recordConditionalJump(final SmalltalkSistaV1InterpreterGen.Builder b, final int offset) {
            assert offset > 0;
            final int jumpTarget = index + offset;
            if (!jumpLocations.containsKey(jumpTarget)) {
                assert !loopLocations.containsKey(jumpTarget) && !loopLocations.containsValue(jumpTarget);
                jumpLocations.put(jumpTarget, b.createLabel());
            }
        }

        private void recordJumpStackPointer(final int jumpTarget) {
            final Integer previousStackPointer = jumpStackPointers.putIfAbsent(jumpTarget, sp);
            assert previousStackPointer == null || previousStackPointer == sp : "Inconsistent stack depth at jump target";
        }

        private int translateBytecode(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int numExtB) {
            final int indexWithExt = index + extBytes;
            final int op = getUnsignedInt(indexWithExt);
            switch (op) {
                case 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F //
                    -> emitPush(b, () -> b.emitReceiverVariable(op & 0xF));
                case 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F //
                    -> emitPush(b, () -> b.emitLiteralVariable(code.getAndResolveLiteral(op & 0xF)));
                case 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F //
                    -> emitPush(b, () -> b.emitLoadConstant(code.getAndResolveLiteral(op & 0x1F)));
                case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[op & 0x7]));
                case 0x48, 0x49, 0x4A, 0x4B -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[(op & 3) + 8]));
                case 0x4C -> emitPush(b, () -> emitLoadReceiver(b));
                case 0x4D -> emitPush(b, () -> b.emitLoadConstant(BooleanObject.TRUE));
                case 0x4E -> emitPush(b, () -> b.emitLoadConstant(BooleanObject.FALSE));
                case 0x4F -> emitPush(b, () -> b.emitLoadConstant(NilObject.SINGLETON));
                case 0x50 -> emitPush(b, () -> b.emitLoadConstant(0L));
                case 0x51 -> emitPush(b, () -> b.emitLoadConstant(1L));
                case 0x52 -> {
                    if (extB == 0) {
                        emitPush(b, b::emitActiveContext);
                    } else {
                        fail(op); // unused
                    }
                }
                case 0x53 -> emitPush(b, () -> emitTop(b));
                // unused
                case 0x58 -> emitReturn(b, () -> emitLoadReceiver(b));
                case 0x59 -> emitReturn(b, () -> b.emitLoadConstant(BooleanObject.TRUE));
                case 0x5A -> emitReturn(b, () -> b.emitLoadConstant(BooleanObject.FALSE));
                case 0x5B -> emitReturn(b, () -> b.emitLoadConstant(NilObject.SINGLETON));
                case 0x5C -> emitReturn(b, () -> {
                    b.beginReturnTopFromMethod();
                    emitTop(b);
                    b.endReturnTopFromMethod();
                });
                case 0x5D -> b.emitReturnNilFromClosure();
                case 0x5E -> {
                    if (extA == 0) {
                        b.beginReturnTopFromClosure();
                        emitTop(b);
                        b.endReturnTopFromClosure();
                    } else {
                        fail(op); // shouldBeImplemented, see #genExtReturnTopFromBlock
                    }
                }
                case 0x5F -> {
                    // nop
                }

                case 0x60 /* #+ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialAdd();
                    emitPopN(b, 2);
                    b.endSendSpecialAdd();
                });
                case 0x61 /* #- */ -> emitPush(b, 2, () -> { // #+
                    b.beginSendSpecialSubtract();
                    emitPopN(b, 2);
                    b.endSendSpecialSubtract();
                });
                case 0x62 /* #< */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialLessThan();
                    emitPopN(b, 2);
                    b.endSendSpecialLessThan();
                });
                case 0x63 /* #> */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialGreaterThan();
                    emitPopN(b, 2);
                    b.endSendSpecialGreaterThan();
                });
                case 0x64 /* #<= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialLessOrEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialLessOrEqual();
                });
                case 0x65 /* #>= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialGreaterOrEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialGreaterOrEqual();
                });
                case 0x66 /* #= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialEqual();
                });
                case 0x67 /* #~= */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialNotEqual();
                    emitPopN(b, 2);
                    b.endSendSpecialNotEqual();
                });
                case 0x68 /* #* */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialMultiply();
                    emitPopN(b, 2);
                    b.endSendSpecialMultiply();
                });
                case 0x69 /* #/ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialDivide();
                    emitPopN(b, 2);
                    b.endSendSpecialDivide();
                });
                case 0x6A /* #\\ */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialFloorMod();
                    emitPopN(b, 2);
                    b.endSendSpecialFloorMod();
                });
                case 0x6B /* #@ */
                    -> fail(op);
                case 0x6C /* #bitShift: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitShift();
                    emitPopN(b, 2);
                    b.endSendSpecialBitShift();
                });
                case 0x6D /* #// */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialFloorDivide();
                    emitPopN(b, 2);
                    b.endSendSpecialFloorDivide();
                });
                case 0x6E /* #bitAnd: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitAnd();
                    emitPopN(b, 2);
                    b.endSendSpecialBitAnd();
                });
                case 0x6F /* #bitOr: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialBitOr();
                    emitPopN(b, 2);
                    b.endSendSpecialBitOr();
                });
                case 0x70 /* #at: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialAt();
                    emitPopN(b, 2);
                    b.endSendSpecialAt();
                });
                case 0x71 /* #at:put: */ -> emitPush(b, 3, () -> {
                    b.beginSendSpecialAtPut();
                    emitPopN(b, 3);
                    b.endSendSpecialAtPut();
                });
                case 0x72 /* #size */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialSize();
                    emitPop(b);
                    b.endSendSpecialSize();
                });
                case 0x73 /* #next */ -> emitPush(b, 1, () -> {
                    fail(op);
                });
                case 0x74 /* #nextPut: */ -> emitPush(b, 2, () -> {
                    fail(op);
                });
                case 0x75 /* #atEnd */ -> emitPush(b, 1, () -> {
                    fail(op);
                });
                case 0x76 /* #== */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialIdentical();
                    emitPopN(b, 2);
                    b.endSendSpecialIdentical();
                });
                case 0x77 /* #class */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialClass();
                    emitPop(b);
                    b.endSendSpecialClass();
                });
                case 0x78 /* #~~ */ -> emitPush(b, 2, () -> {
                    fail(op);
                });
                case 0x79 /* #value */ -> emitPush(b, 1, () -> {
                    fail(op);
                });
                case 0x7A /* #value: */ -> emitPush(b, 2, () -> {
                    fail(op);
                });
                case 0x7B /* #do: */ -> emitPush(b, 2, () -> {
                    fail(op);
                });
                case 0x7C /* #new */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialNew();
                    emitPop(b);
                    b.endSendSpecialNew();
                });
                case 0x7D /* #new: */ -> emitPush(b, 2, () -> {
                    b.beginSendSpecialNewWithArgument();
                    emitPopN(b, 2);
                    b.endSendSpecialNewWithArgument();
                });
                case 0x7E /* #x */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialX();
                    emitPop(b);
                    b.endSendSpecialX();
                });
                case 0x7F /* #y */ -> emitPush(b, 1, () -> {
                    b.beginSendSpecialY();
                    emitPop(b);
                    b.endSendSpecialY();
                });
                case 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F //
                    -> emitPush(b, 1, () -> {
                        b.beginSelfSendNilary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPop(b);
                        b.endSelfSendNilary();
                    });
                case 0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F //
                    -> emitPush(b, 2, () -> {
                        b.beginSelfSendUnary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPopN(b, 2);
                        b.endSelfSendUnary();
                    });
                case 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF //
                    -> emitPush(b, 3, () -> {
                        b.beginSelfSendBinary((NativeObject) code.getAndResolveLiteral(op & 0xF));
                        emitPopN(b, 3);
                        b.endSelfSendBinary();
                    });
                case 0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7 -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    if (jumpTarget > index) {
                        recordJumpStackPointer(jumpTarget);
                        b.emitBranch(jumpLocations.get(jumpTarget));
                    }
                }
                case 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    b.beginIfThen();
                    emitPop(b);
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7 -> {
                    final int jumpTarget = index + 1 + InterpreterSistaV1Node.calculateShortOffset(op);
                    b.beginIfThen();
                    b.beginNot();
                    emitPop(b);
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF -> emitStoreIntoReceiverVariable(b, op & 7, true);
                case 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7 -> emitStoreIntoTemporaryLocation(b, op & 7, true);
                case 0xD8 -> emitPop(b);
                // unused
                case 0xE0 -> {
                    return translateBytecode(b, extBytes + 2, (extA << 8) + getUnsignedInt(indexWithExt + 1), extB, numExtB);
                }
                case 0xE1 -> {
                    final int byteValue = getUnsignedInt(indexWithExt + 1);
                    return translateBytecode(b, extBytes + 2, extA, numExtB == 0 && byteValue > 127 ? byteValue - 256 : (extB << 8) + byteValue, numExtB + 1);
                }
                case 0xE2 -> emitPush(b, () -> b.emitReceiverVariable(getUnsignedInt(indexWithExt + 1) + (extA << 8)));
                case 0xE3 -> emitPush(b, () -> b.emitLiteralVariable(code.getAndResolveLiteral(getUnsignedInt(indexWithExt + 1) + (extA << 8))));
                case 0xE4 -> emitPush(b, () -> b.emitLoadConstant(code.getAndResolveLiteral(getUnsignedInt(indexWithExt + 1) + (extA << 8))));
                case 0xE5 -> emitPush(b, () -> b.emitLoadLocal(temporarySlots[getUnsignedInt(indexWithExt + 1)]));
                // unused
                case 0xE7 -> {
                    final byte param = getByte(indexWithExt + 1);
                    final int arraySize = param & 127;
                    if (param < 0) {
                        final StackValue[] values = popStackValues(arraySize);
                        emitPush(b, () -> {
                            b.beginNewArrayFromStack();
                            emitStackValues(b, values);
                            b.endNewArrayFromStack();
                        });
                    } else {
                        emitPush(b, () -> b.emitNewArray(arraySize));
                    }
                }
                case 0xE8 -> {
                    final long smallIntegerValue = getUnsignedInt(indexWithExt + 1) + (extB << 8);
                    emitPush(b, () -> b.emitLoadConstant(smallIntegerValue));
                }
                case 0xE9 -> {
                    final Object characterValue = CharacterObject.valueOf(getUnsignedInt(indexWithExt + 1) + (extA << 8));
                    emitPush(b, () -> b.emitLoadConstant(characterValue));
                }
                // unused
                case 0xED -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    if (jumpTarget > index) {
                        recordJumpStackPointer(jumpTarget);
                        b.emitBranch(jumpLocations.get(jumpTarget));
                    }
                }
                case 0xEE -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    b.beginIfThen();
                    emitPop(b);
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xEF -> {
                    final int jumpTarget = index + 2 + extBytes + InterpreterSistaV1Node.calculateLongExtendedOffset(getByte(indexWithExt + 1), extB);
                    b.beginIfThen();
                    b.beginNot();
                    emitPop(b);
                    b.endNot();
                    recordJumpStackPointer(jumpTarget);
                    b.emitBranch(jumpLocations.get(jumpTarget));
                    b.endIfThen();
                }
                case 0xF0 -> emitStoreIntoReceiverVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF1 -> emitStoreIntoLiteralVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF2 -> emitStoreIntoTemporaryLocation(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), true);
                case 0xF3 -> emitStoreIntoReceiverVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                case 0xF4 -> emitStoreIntoLiteralVariable(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                case 0xF5 -> emitStoreIntoTemporaryLocation(b, getUnsignedInt(indexWithExt + 1) + (extA << 8), false);
                // unused
                case 0xF8 -> {
                    final int i = getUnsignedInt(indexWithExt + 1);
                    final int j = getByte(indexWithExt + 2) & 31;
                    final int primitiveIndex = i + (j << 8);
                    assert 1 <= primitiveIndex && primitiveIndex < 32767 : "primitiveIndex out of range";
                    if (primitiveIndex < 1000) {
                        // FIXME
                    } else {
                        // TODO
                        fail(op);
                    }
                }
                case 0xF9 -> emitFullClosure(b, extA, indexWithExt);
                case 0xFA -> {
                    return emitClosure(b, extBytes, extA, extB, indexWithExt);
                }
                case 0xFB -> emitPushRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2));
                case 0xFC -> emitStoreIntoRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2), false);
                case 0xFD -> emitStoreIntoRemoteTemporaryLocation(b, getUnsignedInt(indexWithExt + 1), getUnsignedInt(indexWithExt + 2), true);
                // unused
                default -> fail(op);
            }
            if (op <= 223) {
                return 1 + extBytes;
            } else if (op <= 247) {
                return 2 + extBytes;
            } else {
                return 3 + extBytes;
            }
        }

        private void emitPush(final SmalltalkSistaV1InterpreterGen.Builder b, final PushOperation pushOperation) {
            emitPush(b, 0, pushOperation);
        }

        private void emitPush(final SmalltalkSistaV1InterpreterGen.Builder b, final int numPopped, final PushOperation pushOperation) {
            final int stackIndex = sp - numPopped;
            b.beginBindStackValue();
            pushOperation.perform();
            sp++;
            stackSlots[stackIndex] = b.endBindStackValue();
        }

        @FunctionalInterface
        public interface PushOperation {
            void perform();
        }

        private static void emitLoadReceiver(final SmalltalkSistaV1InterpreterGen.Builder b) {
            b.emitLoadArgument(FrameAccess.getReceiverStartIndex());
        }

        private void emitPop(final SmalltalkSistaV1InterpreterGen.Builder b) {
            emitPopN(b, 1);
        }

        private void emitPopN(final SmalltalkSistaV1InterpreterGen.Builder b, final int numPopped) {
            emitStackValues(b, popStackValues(numPopped));
        }

        private void emitTop(final SmalltalkSistaV1InterpreterGen.Builder b) {
            b.emitLoadStackValue(stackSlots[sp - 1]);
        }

        private static void emitReturn(final SmalltalkSistaV1InterpreterGen.Builder b, final ReturnOperation returnOperation) {
            b.beginReturn();
            returnOperation.perform();
            b.endReturn();
        }

        @FunctionalInterface
        public interface ReturnOperation {
            void perform();
        }

        private StackValue[] popStackValues(final int size) {
            final StackValue[] values = new StackValue[size];
            sp -= size;
            System.arraycopy(stackSlots, sp, values, 0, size);
            return values;
        }

        private static void emitStackValues(final SmalltalkSistaV1InterpreterGen.Builder b, final StackValue[] values) {
            for (final StackValue value : values) {
                b.emitLoadStackValue(value);
            }
        }

        private void emitStoreIntoReceiverVariable(final SmalltalkSistaV1InterpreterGen.Builder b, final int variableIndex, final boolean shouldPop) {
            b.beginStoreIntoReceiverVariable(variableIndex);
            if (shouldPop) {
                emitPop(b);
            } else {
                emitTop(b);
            }
            b.endStoreIntoReceiverVariable();
        }

        private void emitStoreIntoLiteralVariable(final SmalltalkSistaV1InterpreterGen.Builder b, final int literalIndex, final boolean shouldPop) {
            b.beginStoreIntoLiteralVariable(code.getAndResolveLiteral(literalIndex), ObjectLayouts.ASSOCIATION.VALUE);
            if (shouldPop) {
                emitPop(b);
            } else {
                emitTop(b);
            }
            b.endStoreIntoLiteralVariable();
        }

        private void emitStoreIntoTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int tempIndex, final boolean shouldPop) {
            b.beginStoreLocal(temporarySlots[tempIndex]);
            if (shouldPop) {
                emitPop(b);
            } else {
                emitTop(b);
            }
            b.endStoreLocal();
        }

        private void emitPushRemoteTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int indexInArray, final int indexOfArray) {
            emitPush(b, () -> {
                b.beginLoadRemoteTemp(indexInArray);
                b.emitLoadLocal(temporarySlots[indexOfArray]);
                b.endLoadRemoteTemp();
            });
        }

        private void emitStoreIntoRemoteTemporaryLocation(final SmalltalkSistaV1InterpreterGen.Builder b, final int indexInArray, final int indexOfArray, final boolean shouldPop) {
            b.beginStoreIntoRemoteTemp(indexInArray);
            b.emitLoadLocal(temporarySlots[indexOfArray]);
            if (shouldPop) {
                emitPop(b);
            } else {
                emitTop(b);
            }
            b.endStoreIntoRemoteTemp();
        }

        private void emitFullClosure(final SmalltalkSistaV1InterpreterGen.Builder b, final int extA, final int indexWithExt) {
            final byte byteA = getByte(indexWithExt + 1);
            final byte byteB = getByte(indexWithExt + 2);
            final int literalIndex = Byte.toUnsignedInt(byteA) + (extA << 8);
            final CompiledCodeObject block = (CompiledCodeObject) code.getLiteral(literalIndex);
            final int numCopied = Byte.toUnsignedInt(byteB) & 63;
            final boolean ignoreOuterContext = (byteB >> 6 & 1) == 1;
            final boolean receiverOnStack = (byteB >> 7 & 1) == 1;
            final StackValue[] copiedValues = popStackValues(numCopied);
            emitPush(b, () -> {
                b.beginFullClosure(block);
                if (receiverOnStack) {
                    emitPop(b);
                } else {
                    emitLoadReceiver(b);
                }
                if (ignoreOuterContext) {
                    b.emitLoadNull();
                } else {
                    b.emitActiveContext();
                }
                emitStackValues(b, copiedValues);
                b.endFullClosure();
            });
        }

        private int emitClosure(final SmalltalkSistaV1InterpreterGen.Builder b, final int extBytes, final int extA, final int extB, final int indexWithExt) {
            final byte byteA = getByte(indexWithExt + 1);
            final byte byteB = getByte(indexWithExt + 2);
            final int numArgs = (byteA & 7) + Math.floorMod(extA, 16) * 8;
            final int numCopied = (Byte.toUnsignedInt(byteA) >> 3 & 0x7) + Math.floorDiv(extA, 16) * 8;
            final int successorPC = code.getInitialPC() + index + 3 + extBytes;
            final int blockSize = Byte.toUnsignedInt(byteB) + (extB << 8);
            final CompiledCodeObject block = code.createShadowBlock(successorPC, numArgs, numCopied, blockSize);
            final StackValue[] copiedValues = popStackValues(numCopied);
            emitPush(b, () -> {
                b.beginClosure(block);
                emitStackValues(b, copiedValues);
                b.endClosure();
            });
            return 3 + extBytes + blockSize;
        }

        private static void fail(final int opcode) {
            throw SqueakException.create("Unknown bytecode:", opcode);
        }
    }

    @Operation
    public static final class SendSpecialAdd {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.addExact(lhs, rhs);
        }

//        @Specialization(replaces = "doLong")
//        static Object doLongWithOverflow(final long lhs, final long rhs,
//                        @Bind final SqueakImageContext image) {
//            return LargeIntegerObject.add(image, lhs, rhs);
//        }
//
//        @Specialization
//        static Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return rhs.add(lhs);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()")
        static double doLongDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) {
            return lhs + rhs;
        }

//        @Specialization
//        static Object doLongFloat(final long lhs, final FloatObject rhs,
//                        @Bind final Node node,
//                        @Cached final FloatObjectNodes.AsFloatObjectIfNessaryNode boxNode) {
//            return boxNode.execute(node, lhs + rhs.getValue());
//        }
    }

    @Operation
    public static final class SendSpecialSubtract {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.subtractExact(lhs, rhs);
        }

//        @Specialization(replaces = "doLong")
//        static Object doLongWithOverflow(final long lhs, final long rhs,
//                        @Bind final SqueakImageContext image) {
//            return LargeIntegerObject.subtract(image, lhs, rhs);
//        }
//
//        @Specialization
//        static Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return LargeIntegerObject.subtract(lhs, rhs);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()")
        static double doLongDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) {
            return lhs - rhs;
        }

//        @Specialization
//        static Object doLongFloat(final long lhs, final FloatObject rhs,
//                        @Bind final Node node,
//                        @Cached final FloatObjectNodes.AsFloatObjectIfNessaryNode boxNode) {
//            return boxNode.execute(node, lhs - rhs.getValue());
//        }
    }

    @Operation
    public static final class SendSpecialLessThan {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs < rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) >= 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs < rhs);
            }
        }
    }

    @Operation
    public static final class SendSpecialGreaterThan {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs > rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) <= 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs > rhs);
            }
        }
    }

    @Operation
    public static final class SendSpecialLessOrEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs <= rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) > 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs <= rhs);
            }
        }
    }

    @Operation
    public static final class SendSpecialGreaterOrEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs >= rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) < 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.wrap(lhs >= rhs);
            }
        }
    }

    @Operation
    public static final class SendSpecialEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs == rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) == 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.FALSE;
            }
        }

        /** Quick return `false` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(image, rhs)", "!isPointersObject(rhs)"})
        static boolean doQuickFalse(final long lhs, final AbstractSqueakObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.FALSE;
        }
    }

    @Operation
    public static final class SendSpecialNotEqual {
        @Specialization
        static boolean doLong(final long lhs, final long rhs) {
            return BooleanObject.wrap(lhs != rhs);
        }

//        @Specialization
//        static boolean doLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return BooleanObject.wrap(rhs.compareTo(lhs) != 0);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixComparison()")
        static boolean doDouble(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isExactProfile) {
            if (isExactProfile.profile(node, lhs == rhs)) {
                return doLong(lhs, (long) rhs);
            } else {
                return BooleanObject.TRUE;
            }
        }

        /** Quick return `true` if b is not a Number or Complex. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isFloatObject(rhs)", "!isLargeIntegerObject(image, rhs)", "!isPointersObject(rhs)"})
        static boolean doQuickTrue(final long lhs, final AbstractSqueakObject rhs,
                        @Bind final SqueakImageContext image) {
            return BooleanObject.TRUE;
        }
    }

    @Operation
    public static final class SendSpecialMultiply {
        @Specialization(rewriteOn = ArithmeticException.class)
        static long doLong(final long lhs, final long rhs) {
            return Math.multiplyExact(lhs, rhs);
        }

//        @Specialization(replaces = "doLong")
//        static Object doLongWithOverflow(final long lhs, final long rhs,
//                        @Bind final SqueakImageContext image) {
//            return LargeIntegerObject.multiply(image, lhs, rhs);
//        }
//
//        @Specialization
//        static Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return rhs.multiply(lhs);
//        }

        @Specialization(guards = "image.flags.numericPrimsMixArithmetic()", rewriteOn = RespecializeException.class)
        static double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) throws RespecializeException {
            return ensureFinite(lhs * rhs);
        }

//        @Specialization(guards = "image.flags.isPrimitiveDoMixedArithmetic()", replaces = "doLongDoubleFinite")
//        static Object doLongDouble(final long lhs, final double rhs,
//                        @Bind final SqueakImageContext image,
//                        @Bind final Node node,
//                        @Cached final FloatObjectNodes.AsFloatObjectIfNessaryNode boxNode) {
//            return boxNode.execute(node, lhs * rhs);
//        }
    }

    @Operation
    public static final class SendSpecialDivide {
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        static long doLong(final long lhs, final long rhs) {
            return lhs / rhs;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"}, replaces = "doLong")
        static Object doLongFraction(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile fractionProfile,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectWriteNode writeNode) {
            if (fractionProfile.profile(node, SqueakGuards.isIntegralWhenDividedBy(lhs, rhs))) {
                return lhs / rhs;
            } else {
                return image.asFraction(lhs, rhs, writeNode);
            }
        }

//        @SuppressWarnings("unused")
//        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
//        static LargeIntegerObject doLongOverflow(final long lhs, final long rhs,
//                        @Bind final SqueakImageContext image) {
//            return LargeIntegerObject.createLongMinOverflowResult(image);
//        }

        @Specialization(guards = {"image.flags.numericPrimsMixArithmetic()", "!isZero(rhs)"}, rewriteOn = RespecializeException.class)
        static double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final SqueakImageContext image) throws RespecializeException {
            return ensureFinite(lhs / rhs);
        }

//        @Specialization(guards = {"image.flags.isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, replaces = "doLongDoubleFinite")
//        static Object doLongDouble(final long lhs, final double rhs,
//                        @Bind final SqueakImageContext image,
//                        @Bind final Node node,
//                        @Cached final FloatObjectNodes.AsFloatObjectIfNessaryNode boxNode) {
//            return boxNode.execute(node, lhs / rhs);
//        }
    }

    @Operation
    public static final class SendSpecialFloorMod {
        /** Profiled version of {@link Math#floorMod(long, long)}. */
        @Specialization(guards = "rhs != 0")
        static long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long r = lhs % rhs;
            // if the signs are different and modulo not zero, adjust result
            if (profile.profile(node, (lhs ^ rhs) < 0 && r != 0)) {
                return r + rhs;
            } else {
                return r;
            }
        }

//        @Specialization(guards = "!rhs.isZero()")
//        static Object doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return rhs.floorModReverseOrder(lhs);
//        }
    }

    @Operation
    public static final class SendSpecialBitShift {
        @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
        static long doLongPositive(final long receiver, final long arg) {
            return receiver << arg;
        }

//        @Specialization(guards = {"arg >= 0", "isLShiftLongOverflow(receiver, arg)"})
//        static Object doLongPositiveOverflow(final long receiver, final long arg,
//                        @Bind final SqueakImageContext image) {
//            /*
//             * -1 in check needed, because we do not want to shift a positive long into negative
//             * long (most significant bit indicates positive/negative).
//             */
//            return LargeIntegerObject.shiftLeftPositive(image, receiver, (int) arg);
//        }

        @Specialization(guards = {"arg < 0", "inLongSizeRange(arg)"})
        static long doLongNegativeInLongSizeRange(final long receiver, final long arg) {
            /*
             * The result of a right shift can only become smaller than the receiver and 0 or -1 at
             * minimum, so no BigInteger needed here.
             */
            return receiver >> -arg;
        }

        @Specialization(guards = {"arg < 0", "!inLongSizeRange(arg)"})
        static long doLongNegative(final long receiver, @SuppressWarnings("unused") final long arg) {
            return receiver >= 0 ? 0L : -1L;
        }

        static boolean isLShiftLongOverflow(final long receiver, final long arg) {
            return Long.numberOfLeadingZeros(receiver) - 1 < arg;
        }

        static boolean inLongSizeRange(final long arg) {
            return -Long.SIZE < arg;
        }
    }

    @Operation
    public static final class SendSpecialFloorDivide {
        /** Profiled version of {@link Math#floorDiv(long, long)}. */
        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        static long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile profile) {
            final long q = lhs / rhs;
            // if the signs are different and modulo not zero, round down
            if (profile.profile(node, (lhs ^ rhs) < 0 && (q * rhs != lhs))) {
                return q - 1;
            } else {
                return q;
            }
        }

//        @SuppressWarnings("unused")
//        @Specialization(guards = {"isOverflowDivision(lhs, rhs)"})
//        static LargeIntegerObject doLongOverflowDivision(final long lhs, final long rhs,
//                        @Bind final SqueakImageContext image) {
//            return LargeIntegerObject.createLongMinOverflowResult(image);
//        }
//
//        @Specialization(guards = {"!rhs.isZero()"})
//        static long doLongLargeInteger(final long lhs, final LargeIntegerObject rhs) {
//            return LargeIntegerObject.floorDivide(lhs, rhs);
//        }
    }

    @Operation
    public static final class SendSpecialBitAnd {
        @Specialization
        static long doLong(final long receiver, final long arg) {
            return receiver & arg;
        }

//        @Specialization(rewriteOn = ArithmeticException.class)
//        static long doLongLargeQuick(final long receiver, final LargeIntegerObject arg,
//                        @Bind final Node node,
//                        @Cached final InlinedConditionProfile positiveProfile) {
//            return receiver & (positiveProfile.profile(node, receiver >= 0) ? arg.longValue() : arg.longValueExact());
//        }
//
//        @Specialization(replaces = "doLongLargeQuick")
//        static Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
//            return arg.and(receiver);
//        }
    }

    @Operation
    public static final class SendSpecialBitOr {
        @Specialization
        static long doLong(final long receiver, final long arg) {
            return receiver | arg;
        }

//        @Specialization(rewriteOn = ArithmeticException.class)
//        static long doLongLargeQuick(final long receiver, final LargeIntegerObject arg) {
//            return receiver | arg.longValueExact();
//        }
//
//        @Specialization(replaces = "doLongLargeQuick")
//        static Object doLongLarge(final long receiver, final LargeIntegerObject arg) {
//            return arg.or(receiver);
//        }
    }

    @Operation
    public static final class SendSpecialMakePoint {
        @Specialization
        static PointersObject doPoint(final Object xPos, final Object yPos,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, xPos, yPos);
        }
    }

    @Operation
    public static final class SendSpecialAt {
        @Specialization
        static Object perform(final Object receiver, final long index,
                        @Bind final Node node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, receiver, index);
        }
    }

    @Operation
    public static final class SendSpecialAtPut {
        @Specialization
        static void perform(final Object receiver, final long index, final Object value,
                        @Bind final Node node,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, receiver, index, value);
        }
    }

    @Operation
    public static final class SendSpecialSize {
        @Specialization
        static long perform(final Object receiver,
                        @Bind final Node node,
                        @Cached final SqueakObjectSizeNode sizeNode) {
            return sizeNode.execute(node, receiver);
        }
    }

    @Operation(forceCached = true)
    public static final class SendSpecialIdentical {
        @Specialization
        static boolean perform(final Object left, final Object right,
                        @Bind final Node node,
                        @Cached final SqueakObjectIdentityNode identityNode) {
            return identityNode.execute(node, left, right);
        }
    }

    @Operation
    public static final class SendSpecialClass {
        @Specialization
        static Object performASO(final AbstractSqueakObject receiver,
                        @Bind final Node node,
                        @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }

        @Specialization(replaces = "performASO")
        static Object perform(final Object receiver,
                        @Bind final Node node,
                        @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
            return classNode.executeLookup(node, receiver);
        }
    }

    @Operation
    public static final class SendSpecialNew {
        @Specialization
        static Object perform(final ClassObject receiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("classNode") @Cached final SqueakObjectNewNode newNode) {
            return newNode.execute(node, receiver);
        }
    }

    @Operation
    public static final class SendSpecialNewWithArgument {
        @Specialization
        static Object perform(final ClassObject receiver, final long extraSize,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Shared("classNode") @Cached final SqueakObjectNewNode newNode) {
            return newNode.execute(node, receiver, /* FIXME: */ Math.toIntExact(extraSize));
        }
    }

    @Operation
    @ImportStatic(SqueakImageContext.class)
    public static final class SendSpecialX {
        @Specialization(guards = "receiver.getSqueakClass() == get(node).pointClass")
        static Object performAPO(final AbstractPointersObject receiver,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(receiver, ObjectLayouts.POINT.X);
        }

        @Fallback
        static Object perform(final Object receiver,
                        @Bind final Node node) {
            return null; // FIXME: Send #x
        }
    }

    @Operation
    @ImportStatic(SqueakImageContext.class)
    public static final class SendSpecialY {
        @Specialization(guards = "receiver.getSqueakClass() == get(node).pointClass")
        static Object performAPO(final AbstractPointersObject receiver,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(receiver, ObjectLayouts.POINT.Y);
        }

        @Fallback
        static Object perform(final Object receiver,
                        @Bind final Node node) {
            return null; // FIXME: Send #y
        }
    }
}

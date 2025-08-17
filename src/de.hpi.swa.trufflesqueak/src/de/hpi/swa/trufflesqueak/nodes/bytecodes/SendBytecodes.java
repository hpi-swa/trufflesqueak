/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.AsFloatObjectIfNessaryNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
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
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers.PrimDigitBitAndNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers.PrimDigitBitOrNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimAddLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimAddNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitAndNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitOrNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitShiftNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimDivideLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimDivideNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimEqualLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorDivideNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorModLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorModNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterOrEqualLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterThanLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessOrEqualLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessThanLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimMakePointNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimMultiplyLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimMultiplyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimNotEqualLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimNotEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatAddNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatGreaterThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessOrEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatLessThanNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatMultiplyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatNotEqualNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatSubtractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSubtractLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSubtractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimExitToDebuggerNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class SendBytecodes {
    public abstract static class AbstractSendNode extends AbstractInstrumentableBytecodeNode {
        private final int stackPointer;
        private final ConditionProfile nlrProfile = ConditionProfile.create();
        private final ConditionProfile nvrProfile = ConditionProfile.create();

        @Child protected DispatchSelectorNode dispatchNode;
        @Child private FrameStackPushNode pushNode;

        private AbstractSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final int numArgs, final int numAdditional) {
            super(code, index, numBytecodes);
            stackPointer = FrameAccess.getStackPointer(frame) - 1 - numArgs - numAdditional;
            assert stackPointer >= 0 : "Bad stack pointer";
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            FrameAccess.setStackPointer(frame, stackPointer);
            Object result;
            try {
                result = dispatchNode.execute(frame);
            } catch (final NonLocalReturn nlr) {
                if (nlrProfile.profile(nlr.getTargetContextOrMarker() == FrameAccess.getMarker(frame) || nlr.getTargetContextOrMarker() == FrameAccess.getContext(frame))) {
                    result = nlr.getReturnValue();
                } else {
                    throw nlr;
                }
            } catch (final NonVirtualReturn nvr) {
                if (nvrProfile.profile(nvr.getTargetContext() == FrameAccess.getContext(frame))) {
                    result = nvr.getReturnValue();
                } else {
                    throw nvr;
                }
            }
            assert result != null : "Result of a message send should not be null";
            getPushNode().execute(frame, result);
        }

        private FrameStackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(FrameStackPushNode.create());
            }
            return pushNode;
        }

        private NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            if (tag == StandardTags.CallTag.class) {
                return true;
            }
            if (tag == DebuggerTags.AlwaysHalt.class) {
                return PrimExitToDebuggerNode.SELECTOR_NAME.equals(getSelector().asStringUnsafe());
            }
            return super.hasTag(tag);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return getBytecodeName() + ": " + getSelector().asStringUnsafe();
        }

        protected abstract String getBytecodeName();
    }

    public static final class SelfSendNode extends AbstractSendNode {
        public SelfSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final NativeObject selector, final int numArgs) {
            super(frame, code, index, numBytecodes, numArgs, 0);
            dispatchNode = DispatchSelectorNode.create(frame, selector, numArgs);
        }

        @Override
        protected String getBytecodeName() {
            return "send";
        }
    }

    public static final class SuperSendNode extends AbstractSendNode {
        public SuperSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(frame, code, index, numBytecodes, numArgs, 0);
            final NativeObject selector = (NativeObject) code.getLiteral(literalIndex);
            dispatchNode = DispatchSelectorNode.createSuper(frame, code, selector, numArgs);
        }

        @Override
        protected String getBytecodeName() {
            return "sendSuper";
        }
    }

    public static final class DirectedSuperSendNode extends AbstractSendNode {
        public DirectedSuperSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final int selectorLiteralIndex, final int numArgs) {
            super(frame, code, index, numBytecodes, numArgs, 1 /* directed class */);
            assert 0 <= selectorLiteralIndex && selectorLiteralIndex < 65535 : "selectorLiteralIndex out of range";
            assert 0 <= numArgs && numArgs <= 31 : "numArgs out of range";
            final NativeObject selector = (NativeObject) code.getLiteral(selectorLiteralIndex);
            dispatchNode = DispatchSelectorNode.createDirectedSuper(frame, selector, numArgs);
        }

        @Override
        protected String getBytecodeName() {
            return "directedSuperSend";
        }
    }

    public abstract static class SendSpecialNode extends AbstractInstrumentableBytecodeNode {
        private SendSpecialNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
        }

        public static AbstractBytecodeNode create(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int selectorIndex) {
            return switch (selectorIndex) {
                case 0 /* #+ */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimAddNodeGen.create());
                case 1 /* #- */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimSubtractNodeGen.create());
                case 2 /* #< */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimLessThanNodeGen.create());
                case 3 /* #> */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimGreaterThanNodeGen.create());
                case 4 /* #<= */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimLessOrEqualNodeGen.create());
                case 5 /* #>= */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimGreaterOrEqualNodeGen.create());
                case 6 /* #= */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimEqualNodeGen.create());
                case 7 /* #~= */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimNotEqualNodeGen.create());
                case 8 /* #* */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimMultiplyNodeGen.create());
                case 9 /* #/ */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimDivideNodeGen.create());
                case 10 /* #\\ */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimModNodeGen.create());
                case 11 /* #@ */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimMakePointNodeGen.create());
                case 12 /* #bitShift: */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimBitShiftNodeGen.create());
                case 13 /* #// */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimDivNodeGen.create());
                case 14 /* #bitAnd: */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimBitAndNodeGen.create());
                case 15 /* #bitOr: */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimBitOrNodeGen.create());
                // case 16 /* #at: */ -> fallthrough;
                // case 17 /* #at:put: */ -> fallthrough;
                case 18 /* #size */ -> new SendSpecial0Node(frame, code, index, selectorIndex, BytecodePrimSizeNodeGen.create());
                // case 19 /* #next */ -> fallthrough;
                // case 20 /* #nextPut: */ -> fallthrough;
                // case 21 /* #atEnd */ -> fallthrough;
                case 22 /* #== */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimIdenticalSistaV1NodeGen.create());
                case 23 /* #class */ -> new SendSpecial0Node(frame, code, index, selectorIndex, BytecodePrimClassNodeGen.create());
                case 24 /* #~~ */ -> new SendSpecial1Node(frame, code, index, selectorIndex, BytecodePrimNotIdenticalSistaV1NodeGen.create());
                // case 25 /* #value */ -> should go through SelfSendNode;
                // case 26 /* #value: */ -> should go through SelfSendNode;
                // case 27 /* #do: */ -> fallthrough;
                // case 28 /* #new */ -> fallthrough;
                // case 29 /* #new: */ -> fallthrough;
                case 30 /* #x: */ -> new SendSpecial0Node(frame, code, index, selectorIndex, BytecodePrimPointXNodeGen.create());
                case 31 /* #y: */ -> new SendSpecial0Node(frame, code, index, selectorIndex, BytecodePrimPointYNodeGen.create());
                default -> {
                    final SqueakImageContext image = SqueakImageContext.getSlow();
                    final NativeObject specialSelector = image.getSpecialSelector(selectorIndex);
                    final int numArguments = image.getSpecialSelectorNumArgs(selectorIndex);
                    yield new SelfSendNode(frame, code, index, 1, specialSelector, numArguments);
                }
            };
        }

        protected final void rewriteToSend(final VirtualFrame frame, final DispatchBytecodePrimNode dispatchNode) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // Replace with normal send (pc needs to be written first)
            FrameAccess.setInstructionPointer(frame, getSuccessorIndex());
            // Lookup specialSelector and replace with normal send
            final SqueakImageContext image = getContext();
            final int selectorIndex = dispatchNode.getSelectorIndex();
            final NativeObject specialSelector = image.getSpecialSelector(selectorIndex);
            final int numArguments = image.getSpecialSelectorNumArgs(selectorIndex);
            final CompiledCodeObject code = FrameAccess.getCodeObject(frame);
            final int numBytescodes = 1;
            final int index = getSuccessorIndex() - code.getInitialPC() - numBytescodes;
            replace(new SelfSendNode(frame, code, index, numBytescodes, specialSelector, numArguments)).executeVoid(frame);
        }

        protected abstract static class DispatchBytecodePrimNode extends AbstractNode {
            protected final NativeObject getSpecialSelector() {
                return getContext().getSpecialSelector(getSelectorIndex());
            }

            abstract int getSelectorIndex();
        }

        protected static final class SendSpecial0Node extends SendSpecialNode {
            @Child private FrameStackReadNode receiverNode;
            @Child private FrameStackWriteNode writeResultNode;
            @Child private DispatchBytecodePrim0Node dispatchNode;

            SendSpecial0Node(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int selectorIndex, final DispatchBytecodePrim0Node dispatchNode) {
                super(code, index, 1);
                final int stackPointer = FrameAccess.getStackPointer(frame);
                receiverNode = FrameStackReadNode.create(frame, stackPointer - 1, false); // overwritten
                writeResultNode = FrameStackWriteNode.create(frame, stackPointer - 1);
                assert selectorIndex == dispatchNode.getSelectorIndex();
                this.dispatchNode = dispatchNode;
            }

            @Override
            public void executeVoid(final VirtualFrame frame) {
                try {
                    writeResultNode.executeWrite(frame, dispatchNode.execute(frame, receiverNode.executeRead(frame)));
                } catch (final UnsupportedSpecializationException | PrimitiveFailed use) {
                    rewriteToSend(frame, dispatchNode);
                }
            }

            @Override
            public String toString() {
                CompilerAsserts.neverPartOfCompilation();
                return "send: " + dispatchNode.getSpecialSelector().asStringUnsafe();
            }

            abstract static class DispatchBytecodePrim0Node extends DispatchBytecodePrimNode {
                abstract Object execute(VirtualFrame frame, Object receiver);
            }

            /**
             * Subset of {@link de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode} for
             * classes that do not override #size. Returns long values. OpenSmalltalkVM always
             * performs sends.
             */
            @GenerateInline(false)
            abstract static class BytecodePrimSizeNode extends DispatchBytecodePrim0Node {
                @Override
                protected final int getSelectorIndex() {
                    return 18;
                }

                @Specialization
                protected static final long doNilObject(final NilObject receiver) {
                    return receiver.size();
                }

                /*
                 * Cannot use specialization for NativeObject as Cuis has lots of conflicting
                 * overrides, such as Float64Array#size.
                 */

                @Specialization
                protected static final long doArrayObject(final ArrayObject receiver,
                                @Bind final Node node,
                                @Cached final ArrayObjectSizeNode sizeNode) {
                    return sizeNode.execute(node, receiver);
                }

                @Specialization
                protected static final long doClass(final ClassObject obj) {
                    return obj.size();
                }

                @Specialization
                protected static final long doContext(final ContextObject obj) {
                    return obj.size();
                }

                /*
                 * Cannot use specialization for BlockClosureObject due to FullBlockClosure#size
                 * override.
                 */

                @Specialization
                protected static final long doCode(final CompiledCodeObject obj) {
                    return obj.size();
                }

                @Specialization
                protected static final long doFloat(final FloatObject obj) {
                    return obj.size();
                }

                @Specialization
                protected static final long doCharacterObject(final CharacterObject obj) {
                    return obj.size();
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimClassNode extends DispatchBytecodePrim0Node {
                @Override
                protected final int getSelectorIndex() {
                    return 23;
                }

                @Specialization
                protected static final ClassObject doGeneric(final Object receiver,
                                @Bind final Node node,
                                @Cached final SqueakObjectClassNode classNode) {
                    return classNode.executeLookup(node, receiver);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimPointXNode extends DispatchBytecodePrim0Node {
                @Override
                protected final int getSelectorIndex() {
                    return 30;
                }

                @Specialization(guards = "getContext(node).isPoint(receiver)")
                protected static final Object doX(final AbstractPointersObject receiver,
                                @Bind final Node node,
                                @Cached final AbstractPointersObjectReadNode readNode) {
                    return readNode.execute(node, receiver, POINT.X);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimPointYNode extends DispatchBytecodePrim0Node {
                @Override
                protected final int getSelectorIndex() {
                    return 31;
                }

                @Specialization(guards = "getContext(node).isPoint(receiver)")
                protected static final Object doY(final AbstractPointersObject receiver,
                                @Bind final Node node,
                                @Cached final AbstractPointersObjectReadNode readNode) {
                    return readNode.execute(node, receiver, POINT.Y);
                }
            }
        }

        protected static final class SendSpecial1Node extends SendSpecialNode {
            private final int newStackPointer;
            @Child private FrameStackReadNode receiverNode;
            @Child private FrameStackReadNode arg1Node;
            @Child private FrameStackWriteNode writeResultNode;
            @Child private DispatchBytecodePrim1Node dispatchNode;

            SendSpecial1Node(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int selectorIndex, final DispatchBytecodePrim1Node dispatchNode) {
                super(code, index, 1);
                final int stackPointer = FrameAccess.getStackPointer(frame);
                newStackPointer = stackPointer - 1;
                receiverNode = FrameStackReadNode.create(frame, stackPointer - 2, false); // overwritten
                arg1Node = FrameStackReadNode.create(frame, stackPointer - 1, false);
                writeResultNode = FrameStackWriteNode.create(frame, stackPointer - 2);
                assert selectorIndex == dispatchNode.getSelectorIndex();
                this.dispatchNode = dispatchNode;
            }

            @Override
            public void executeVoid(final VirtualFrame frame) {
                try {
                    final Object result = dispatchNode.execute(frame, receiverNode.executeRead(frame), arg1Node.executeRead(frame));
                    FrameAccess.setStackPointer(frame, newStackPointer);
                    writeResultNode.executeWrite(frame, result);
                } catch (final UnsupportedSpecializationException | PrimitiveFailed use) {
                    rewriteToSend(frame, dispatchNode);
                }
            }

            @Override
            public String toString() {
                CompilerAsserts.neverPartOfCompilation();
                return "send: " + dispatchNode.getSpecialSelector().asStringUnsafe();
            }

            abstract static class DispatchBytecodePrim1Node extends DispatchBytecodePrimNode {
                abstract Object execute(VirtualFrame frame, Object receiver, Object arg1);
            }

            @GenerateInline(false)
            abstract static class BytecodePrimAddNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 0;
                }

                @Specialization(rewriteOn = ArithmeticException.class)
                protected static final long doLong(final long lhs, final long rhs) {
                    return PrimAddNode.doLong(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimAddNode.doLongWithOverflow(lhs, rhs, image);
                }

                @Specialization
                protected static final double doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatAddNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final double doLongDouble(final long lhs, final double rhs) {
                    return PrimAddNode.doLongDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final double doDoubleLong(final double lhs, final long rhs) {
                    return PrimSmallFloatAddNode.doLong(lhs, rhs);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimAddLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimAddLargeIntegersNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimAddNode.doLongLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimSubtractNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 1;
                }

                @Specialization(rewriteOn = ArithmeticException.class)
                protected static final long doLong(final long lhs, final long rhs) {
                    return PrimSubtractNode.doLong(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimSubtractNode.doLongWithOverflow(lhs, rhs, image);
                }

                @Specialization
                protected static final double doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatSubtractNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final double doLongDouble(final long lhs, final double rhs) {
                    return PrimSubtractNode.doLongDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final double doDoubleLong(final double lhs, final long rhs) {
                    return PrimSmallFloatSubtractNode.doLong(lhs, rhs);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimSubtractLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimSubtractLargeIntegersNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimSubtractNode.doLongLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimLessThanNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 2;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimLessThanNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatLessThanNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimLessThanNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatLessThanNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimLessThanLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimLessThanLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimLessThanNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimGreaterThanNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 3;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimGreaterThanNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatGreaterThanNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimGreaterThanNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatGreaterThanNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimGreaterThanLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimGreaterThanLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimGreaterThanNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimLessOrEqualNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 4;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimLessOrEqualNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatLessOrEqualNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimLessOrEqualNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatLessOrEqualNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimLessOrEqualLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimLessOrEqualLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimLessOrEqualNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimGreaterOrEqualNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 5;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimGreaterOrEqualNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatGreaterOrEqualNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimGreaterOrEqualNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatGreaterOrEqualNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimGreaterOrEqualLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimGreaterOrEqualLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimGreaterOrEqualNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimEqualNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 6;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimEqualNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatEqualNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimEqualNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatEqualNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimEqualLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimEqualLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimEqualNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimNotEqualNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 7;
                }

                @Specialization
                protected static final boolean doLong(final long lhs, final long rhs) {
                    return PrimNotEqualNode.doLong(lhs, rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return PrimSmallFloatNotEqualNode.doDouble(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimNotEqualNode.doDouble(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
                protected static final boolean doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("isExactProfile") @Cached final InlinedConditionProfile isExactProfile) {
                    return PrimSmallFloatNotEqualNode.doLong(lhs, rhs, node, isExactProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final boolean doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimNotEqualLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final boolean doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
                    return PrimNotEqualLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, sameSignProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final boolean doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimNotEqualNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimMultiplyNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 8;
                }

                @Specialization(rewriteOn = ArithmeticException.class)
                protected static final long doLong(final long lhs, final long rhs) {
                    return PrimMultiplyNode.doLong(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimMultiplyNode.doLongWithOverflow(lhs, rhs, image);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
                protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
                    return PrimMultiplyNode.doLongDoubleFinite(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongDoubleFinite")
                protected static final Object doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return PrimMultiplyNode.doLongDouble(lhs, rhs, node, boxNode);
                }

                @Specialization(rewriteOn = RespecializeException.class)
                protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
                    return PrimSmallFloatMultiplyNode.doDoubleFinite(lhs, rhs);
                }

                @Specialization(replaces = "doDoubleFinite")
                protected static final Object doDouble(final double lhs, final double rhs,
                                @Bind final Node node,
                                @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return PrimSmallFloatMultiplyNode.doDouble(lhs, rhs, node, boxNode);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
                protected static final double doDoubleLongFinite(final double lhs, final long rhs) throws RespecializeException {
                    return PrimSmallFloatMultiplyNode.doLongFinite(lhs, rhs);
                }

                @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doDoubleLongFinite")
                protected static final Object doDoubleLong(final double lhs, final long rhs,
                                @Bind final Node node,
                                @Shared("boxNode") @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return PrimSmallFloatMultiplyNode.doLong(lhs, rhs, node, boxNode);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimMultiplyLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimMultiplyLargeIntegersNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimMultiplyNode.doLongLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimDivideNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 9;
                }

                @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
                protected static final long doLong(final long lhs, final long rhs) {
                    return PrimDivideNode.doLong(lhs, rhs);
                }

                @Specialization(guards = {"rhs != 0"}, replaces = "doLong")
                protected static final Object doLongFraction(final long lhs, final long rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile isOverflowProfile,
                                @Exclusive @Cached final InlinedConditionProfile isIntegralProfile,
                                @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return PrimDivideNode.doLongFraction(lhs, rhs, image, node, isOverflowProfile, isIntegralProfile, writeNode);
                }

                @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, rewriteOn = RespecializeException.class)
                protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
                    return PrimDivideNode.doLongDoubleFinite(lhs, rhs);
                }

                @Specialization(guards = {"isPrimitiveDoMixedArithmetic()", "!isZero(rhs)"}, replaces = "doLongDoubleFinite")
                protected static final Object doLongDouble(final long lhs, final double rhs,
                                @Bind final Node node,
                                @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return PrimDivideNode.doLongDouble(lhs, rhs, node, boxNode);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "rhs != 0"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile successProfile) {
                    return PrimDivideLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image, node, successProfile);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)", "!isZero(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile successProfile) {
                    return PrimDivideLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, successProfile);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimModNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 10;
                }

                @Specialization(guards = "rhs != 0")
                protected static final long doLong(final long lhs, final long rhs,
                                @Bind final Node node,
                                @Cached final InlinedConditionProfile profile) {
                    return PrimFloorModNode.doLong(lhs, rhs, node, profile);
                }

                @Specialization(guards = "image.isLargeInteger(lhs)")
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimFloorModLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimFloorModLargeIntegersNode.doLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimMakePointNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 11;
                }

                @Specialization
                protected static final PointersObject doLong(final long xPos, final Object yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return PrimMakePointNode.doPoint(xPos, yPos, node, writeNode);
                }

                @Specialization
                protected static final PointersObject doDouble(final double xPos, final Object yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return PrimMakePointNode.doPoint(xPos, yPos, node, writeNode);
                }

                @Specialization(guards = "getContext(node).isLargeInteger(xPos)")
                protected static final PointersObject doLargeInteger(final NativeObject xPos, final Object yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return PrimMakePointNode.doPoint(xPos, yPos, node, writeNode);
                }

                @Specialization
                protected static final PointersObject doFloat(final FloatObject xPos, final Object yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return PrimMakePointNode.doPoint(xPos, yPos, node, writeNode);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimBitShiftNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 12;
                }

                @Specialization(guards = {"arg >= 0"})
                protected static final Object doLongPositive(final long receiver, final long arg,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile isOverflowProfile) {
                    return PrimBitShiftNode.doLongPositive(receiver, arg, node, isOverflowProfile);
                }

                @Specialization(guards = {"arg < 0"})
                protected static final long doLongNegativeInLongSizeRange(final long receiver, final long arg,
                                @Bind final Node node,
                                @Exclusive @Cached final InlinedConditionProfile inLongSizeRangeProfile) {
                    return PrimBitShiftNode.doLongNegativeInLongSizeRange(receiver, arg, node, inLongSizeRangeProfile);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimDivNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 13;
                }

                @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
                protected static final long doLong(final long lhs, final long rhs,
                                @Bind final Node node,
                                @Cached final InlinedConditionProfile profile) {
                    return PrimFloorDivideNode.doLong(lhs, rhs, node, profile);
                }

                @Specialization(guards = {"!isZero(rhs)", "image.isLargeInteger(rhs)"})
                protected static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimFloorDivideNode.doLongLargeInteger(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimBitAndNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 14;
                }

                @Specialization
                protected static final long doLong(final long receiver, final long arg) {
                    return PrimBitAndNode.doLong(receiver, arg);
                }

                @Specialization(guards = {"image.isLargeInteger(arg)"}, rewriteOn = ArithmeticException.class)
                public static final long doLongLargeQuick(final long receiver, final NativeObject arg,
                                @Bind final SqueakImageContext image) {
                    return PrimBitAndNode.doLongLargeQuick(receiver, arg, image);
                }

                @Specialization(guards = {"image.isLargeInteger(arg)"}, replaces = "doLongLargeQuick")
                public static final Object doLongLarge(final long receiver, final NativeObject arg,
                                @Bind final SqueakImageContext image) {
                    return PrimBitAndNode.doLongLarge(receiver, arg, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitAndNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitAndNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitAndNode.doLong(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimBitOrNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 15;
                }

                @Specialization
                protected static final long doLong(final long receiver, final long arg) {
                    return PrimBitOrNode.doLong(receiver, arg);
                }

                @Specialization(guards = {"image.isLargeInteger(arg)"}, rewriteOn = ArithmeticException.class)
                public static final long doLongLargeQuick(final long receiver, final NativeObject arg,
                                @Bind final SqueakImageContext image) {
                    return PrimBitOrNode.doLongLargeQuick(receiver, arg, image);
                }

                @Specialization(guards = {"image.isLargeInteger(arg)"}, replaces = "doLongLargeQuick")
                public static final Object doLongLarge(final long receiver, final NativeObject arg,
                                @Bind final SqueakImageContext image) {
                    return PrimBitOrNode.doLongLarge(receiver, arg, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)"})
                protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitOrNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
                protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitOrNode.doLargeInteger(lhs, rhs, image);
                }

                @Specialization(guards = {"image.isLargeInteger(rhs)"})
                protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                                @Bind final SqueakImageContext image) {
                    return PrimDigitBitOrNode.doLong(lhs, rhs, image);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimIdenticalSistaV1Node extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 22;
                }

                @Specialization
                protected static final boolean doGeneric(final Object receiver, final Object arg1,
                                @Bind final Node node,
                                @Cached final SqueakObjectIdentityNode identityNode) {
                    return identityNode.execute(node, receiver, arg1);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimNotIdenticalSistaV1Node extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 24;
                }

                @Specialization
                protected static final boolean doGeneric(final Object receiver, final Object arg1,
                                @Bind final Node node,
                                @Cached final SqueakObjectIdentityNode identityNode) {
                    return !identityNode.execute(node, receiver, arg1);
                }
            }
        }
    }
}

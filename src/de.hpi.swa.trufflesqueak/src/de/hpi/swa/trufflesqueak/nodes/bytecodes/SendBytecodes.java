/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import static de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode.ensureFinite;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
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

import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
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
                    final SqueakImageContext image = code.getSqueakClass().getImage();
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
                } catch (final UnsupportedSpecializationException use) {
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

            @GenerateInline(false)
            abstract static class BytecodePrimSizeNode extends DispatchBytecodePrim0Node {
                @Override
                protected final int getSelectorIndex() {
                    return 18;
                }

                @Specialization(guards = "receiver.isByteType()")
                protected static final long doNativeObjectByte(final NativeObject receiver) {
                    return receiver.getByteLength();
                }

                @Specialization
                protected static final long doArray(final ArrayObject receiver,
                                @Bind final Node node,
                                @Cached final ArrayObjectSizeNode sizeNode) {
                    return sizeNode.execute(node, receiver);
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

                @Specialization(guards = "getContext(node).isPointClass(receiver.getSqueakClass())")
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

                @Specialization(guards = "getContext(node).isPointClass(receiver.getSqueakClass())")
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
                } catch (final UnsupportedSpecializationException use) {
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
                    return Math.addExact(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
                    return LargeIntegerObject.add(image, lhs, rhs);
                }

                @Specialization
                protected static final double doDouble(final double lhs, final double rhs) {
                    return lhs + rhs;
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
                    return Math.subtractExact(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
                    return LargeIntegerObject.subtract(image, lhs, rhs);
                }

                @Specialization
                protected static final double doDouble(final double lhs, final double rhs) {
                    return lhs - rhs;
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
                    return BooleanObject.wrap(lhs < rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs < rhs);
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
                    return BooleanObject.wrap(lhs > rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs > rhs);
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
                    return BooleanObject.wrap(lhs <= rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs <= rhs);
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
                    return BooleanObject.wrap(lhs >= rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs >= rhs);
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
                    return BooleanObject.wrap(lhs == rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs == rhs);
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
                    return BooleanObject.wrap(lhs != rhs);
                }

                @Specialization
                protected static final boolean doDouble(final double lhs, final double rhs) {
                    return BooleanObject.wrap(lhs != rhs);
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
                    return Math.multiplyExact(lhs, rhs);
                }

                @Specialization(replaces = "doLong")
                protected static final Object doLongWithOverflow(final long lhs, final long rhs, @Bind final SqueakImageContext image) {
                    return LargeIntegerObject.multiply(image, lhs, rhs);
                }

                @Specialization(rewriteOn = RespecializeException.class)
                protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
                    return ensureFinite(lhs * rhs);
                }

                @Specialization(replaces = "doDoubleFinite")
                protected static final Object doDouble(final double lhs, final double rhs,
                                @Bind final Node node,
                                @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return boxNode.execute(node, lhs * rhs);
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
                    return lhs / rhs;
                }

                @Specialization(guards = "!isZero(rhs)", rewriteOn = RespecializeException.class)
                protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
                    return ensureFinite(lhs / rhs);
                }

                @Specialization(guards = "!isZero(rhs)", replaces = "doDoubleFinite")
                protected static final Object doDouble(final double lhs, final double rhs,
                                @Bind final Node node,
                                @Cached final AsFloatObjectIfNessaryNode boxNode) {
                    return boxNode.execute(node, lhs / rhs);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimModNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 10;
                }

                /** Profiled version of {@link Math#floorMod(long, long)}. */
                @Specialization(guards = "rhs != 0")
                protected static final long doLong(final long lhs, final long rhs,
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
            }

            @GenerateInline(false)
            abstract static class BytecodePrimMakePointNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 11;
                }

                @Specialization
                protected static final PointersObject doLong(final long xPos, final long yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return getContext(node).asPoint(writeNode, node, xPos, yPos);
                }

                @Specialization
                protected static final PointersObject doDouble(final double xPos, final double yPos,
                                @Bind final Node node,
                                @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
                    return getContext(node).asPoint(writeNode, node, xPos, yPos);
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimBitShiftNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 12;
                }

                @Specialization(guards = {"arg >= 0", "!isLShiftLongOverflow(receiver, arg)"})
                protected static final long doLongPositive(final long receiver, final long arg) {
                    return receiver << arg;
                }
            }

            @GenerateInline(false)
            abstract static class BytecodePrimDivNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 13;
                }

                /** Profiled version of {@link Math#floorDiv(long, long)}. */
                @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
                protected static final long doLong(final long lhs, final long rhs,
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
            }

            @GenerateInline(false)
            abstract static class BytecodePrimBitAndNode extends DispatchBytecodePrim1Node {
                @Override
                protected final int getSelectorIndex() {
                    return 14;
                }

                @Specialization
                protected static final long doLong(final long receiver, final long arg) {
                    return receiver & arg;
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
                    return receiver | arg;
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

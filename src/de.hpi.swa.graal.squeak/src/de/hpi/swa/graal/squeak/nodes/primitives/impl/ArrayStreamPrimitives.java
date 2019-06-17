package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeAcceptsValueNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveWithSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractBasicAtOrAtPutNode extends AbstractPrimitiveWithSizeNode {
        @Child protected SqueakObjectInstSizeNode instSizeNode;
        @Child private SqueakObjectSizeNode sizeNode;

        protected AbstractBasicAtOrAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final boolean inBoundsOfSqueakObject(final long index, final AbstractSqueakObject target) {
            return SqueakGuards.inBounds1(index + getInstSizeNode().execute(target), getSizeNode().execute(target));
        }

        protected final SqueakObjectInstSizeNode getInstSizeNode() {
            if (instSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instSizeNode = insert(SqueakObjectInstSizeNode.create());
            }
            return instSizeNode;
        }

        protected final SqueakObjectSizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sizeNode = insert(SqueakObjectSizeNode.create());
            }
            return sizeNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 60)
    protected abstract static class PrimBasicAtNode extends AbstractBasicAtOrAtPutNode implements TernaryPrimitive {
        protected PrimBasicAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected static final Object doNative(final NativeObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectReadNode") @Cached final NativeObjectReadNode nativeObjectReadNode) {
            return nativeObjectReadNode.execute(receiver, index - 1);
        }

        @Specialization(guards = "inBounds1(index, receiver.size())")
        protected static final long doLargeInteger(final LargeIntegerObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.getNativeAt0(index - 1);
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected static final Object doArray(final ArrayObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("arrayObjectReadNode") @Cached final ArrayObjectReadNode arrayObjectReadNode) {
            return arrayObjectReadNode.execute(receiver, index - 1);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)",
                        "!isNativeObject(receiver)", "!isLargeIntegerObject(receiver)", "!isFloatObject(receiver)", "!isArrayObject(receiver)"})
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver));
        }

        /*
         * Context>>#object:basicAt:
         */

        @Specialization(guards = "inBounds(index, target)")
        protected static final Object doNative(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index,
                        @Shared("nativeObjectReadNode") @Cached final NativeObjectReadNode nativeObjectReadNode) {
            return nativeObjectReadNode.execute(target, index - 1);
        }

        @Specialization(guards = "inBounds1(index, target.size())")
        protected static final long doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index) {
            return target.getNativeAt0(index - 1);
        }

        @Specialization(guards = "inBounds(index, target)")
        protected static final Object doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target, final long index,
                        @Shared("arrayObjectReadNode") @Cached final ArrayObjectReadNode arrayObjectReadNode) {
            return arrayObjectReadNode.execute(target, index - 1);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)",
                        "!isNativeObject(target)", "!isLargeIntegerObject(target)", "!isFloatObject(target)", "!isArrayObject(target)"})
        protected final Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject target, final long index,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(target, index - 1 + getInstSizeNode().execute(target));
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractBasicAtOrAtPutNode implements QuaternaryPrimitive {
        @Child private NativeAcceptsValueNode nativeAcceptsValueNode;

        protected PrimBasicAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getNativeAcceptsValueNode().execute(receiver, value)"})
        protected static final char doNativeChar(final NativeObject receiver, final long index, final char value, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getNativeAcceptsValueNode().execute(receiver, value)"})
        protected static final Object doNativeChar(final NativeObject receiver, final long index, final CharacterObject value, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getNativeAcceptsValueNode().execute(receiver, value)"})
        protected static final long doNativeLong(final NativeObject receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getNativeAcceptsValueNode().execute(receiver, value)"})
        protected static final Object doNativeLargeInteger(final NativeObject receiver, final long index, final LargeIntegerObject value, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(index, receiver.size())")
        protected static final char doLargeIntegerChar(final LargeIntegerObject receiver, final long index, final char value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(index, receiver.size())")
        protected static final long doLargeIntegerLong(final LargeIntegerObject receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(index, receiver.size())")
        protected static final Object doLargeInteger(final LargeIntegerObject receiver, final long index, final LargeIntegerObject value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value.longValueExact());
            return value;
        }

        @Specialization// (guards = "inBounds(index, receiver)")
        protected static final Object doArray(final ArrayObject receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("arrayObjectWriteNode") @Cached final ArrayObjectWriteNode arrayObjectWriteNode) {
            arrayObjectWriteNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)",
                        "!isNativeObject(receiver)", "!isEmptyObject(receiver)", "!isArrayObject(receiver)"})
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, final Object value,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node) {
            atput0Node.execute(receiver, index - 1 + getInstSizeNode().execute(receiver), value);
            return value;
        }

        /*
         * Context>>#object:basicAt:put:
         */

        @Specialization(guards = {"inBounds(index, target)", "getNativeAcceptsValueNode().execute(target, value)"})
        protected char doNativeChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final char value,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getNativeAcceptsValueNode().execute(target, value)"})
        protected CharacterObject doNativeChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final CharacterObject value,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getNativeAcceptsValueNode().execute(target, value)"})
        protected long doNativeLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final long value,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getNativeAcceptsValueNode().execute(target, value)"})
        protected Object doNativeLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final LargeIntegerObject value,
                        @Shared("nativeObjectWriteNode") @Cached final NativeObjectWriteNode nativeObjectWriteNode) {
            nativeObjectWriteNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(value, BYTE_MAX)")
        protected char doLargeIntegerChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final char value) {
            target.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(value, BYTE_MAX)")
        protected long doLargeIntegerLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final long value) {
            target.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = {"value.fitsIntoLong()", "inBounds1(value.longValueExact(), BYTE_MAX)"})
        protected Object doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final LargeIntegerObject value) {
            target.setNativeAt0(index - 1, value.longValueExact());
            return value;
        }

        @Specialization(guards = "inBounds(index, target)")
        protected static final Object doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target, final long index, final Object value,
                        @Shared("arrayObjectWriteNode") @Cached final ArrayObjectWriteNode arrayObjectWriteNode) {
            arrayObjectWriteNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)",
                        "!isNativeObject(target)", "!isEmptyObject(target)", "!isArrayObject(target)"})
        protected Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject target, final long index,
                        final Object value,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node) {
            atput0Node.execute(target, index - 1 + getInstSizeNode().execute(target), value);
            return value;
        }

        protected final NativeAcceptsValueNode getNativeAcceptsValueNode() {
            /**
             * Could use @Shared for this node as its used in guards, the annotator did not like it.
             */
            if (nativeAcceptsValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nativeAcceptsValueNode = insert(NativeAcceptsValueNode.create());
            }
            return nativeAcceptsValueNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSizeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "receiver.getSqueakClass().isVariable()")
        protected static final long doAbstractPointers(final AbstractPointersObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doArrayObject(final ArrayObject receiver, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("arrayObjectSizeNode") @Cached final ArrayObjectSizeNode arrayObjectSizeNode) {
            return arrayObjectSizeNode.execute(receiver);
        }

        @Specialization
        protected static final long doClosure(final BlockClosureObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doCompiledMethod(final CompiledBlockObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doCompiledBlock(final CompiledMethodObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doContext(final ContextObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doFloatObject(final FloatObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.size() - receiver.instsize();
        }

        @Specialization
        protected static final long doNativeObject(final NativeObject receiver, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeObjectSizeNode") @Cached final NativeObjectSizeNode nativeObjectSizeNode) {
            return nativeObjectSizeNode.execute(receiver);
        }

        /*
         * Context>>#objectSize:
         */

        @Specialization(guards = "target.getSqueakClass().isVariable()")
        protected static final long doAbstractPointers(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractPointersObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doArrayObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target,
                        @Shared("arrayObjectSizeNode") @Cached final ArrayObjectSizeNode arrayObjectSizeNode) {
            return arrayObjectSizeNode.execute(target);
        }

        @Specialization
        protected static final long doClosure(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final BlockClosureObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doCompiledMethod(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final CompiledBlockObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doCompiledBlock(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final CompiledMethodObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doContext(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ContextObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doFloatObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final FloatObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target) {
            return target.size() - target.instsize();
        }

        @Specialization
        protected static final long doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target,
                        @Shared("nativeObjectSizeNode") @Cached final NativeObjectSizeNode nativeObjectSizeNode) {
            return nativeObjectSizeNode.execute(target);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveWithSizeNode implements BinaryPrimitive {

        protected PrimStringAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, obj)"})
        protected static final Object doNativeObject(final NativeObject obj, final long index,
                        @Cached final NativeObjectReadNode readNode,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return CharacterObject.valueOf((int) (long) readNode.execute(obj, index - 1), isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveWithSizeNode implements TernaryPrimitive {
        @Child protected NativeAcceptsValueNode acceptsValueNode = NativeAcceptsValueNode.create();
        @Child private NativeObjectWriteNode writeNode = NativeObjectWriteNode.create();

        protected PrimStringAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final char doNativeObject(final NativeObject obj, final long index, final char value) {
            writeNode.execute(obj, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final CharacterObject doNativeObject(final NativeObject obj, final long index, final CharacterObject value) {
            writeNode.execute(obj, index - 1, value.getValue());
            return value;
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final Object doNativeObject(final NativeObject obj, final long index, final long value) {
            writeNode.execute(obj, index - 1, value);
            return CharacterObject.valueOf((int) value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 143)
    protected abstract static class PrimShortAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimShortAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds0(largerOffset(index), receiver.getByteLength())"})
        protected static final long doNativeBytes(final NativeObject receiver, final long index) {
            final int offset = minusOneAndDouble(index);
            final byte[] bytes = receiver.getByteStorage();
            final int byte0 = (byte) Byte.toUnsignedLong(bytes[offset]);
            int byte1 = (int) Byte.toUnsignedLong(bytes[offset + 1]) << 8;
            if ((byte1 & 0x8000) != 0) {
                byte1 = 0xffff0000 | byte1;
            }
            return byte1 | byte0;
        }

        @Specialization(guards = {"receiver.isShortType()", "inBounds1(index, receiver.getShortLength())"})
        protected static final long doNativeShorts(final NativeObject receiver, final long index) {
            return Short.toUnsignedLong(receiver.getShortStorage()[(int) index]);
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds0(minusOneAndCutInHalf(index), receiver.getIntLength())"})
        protected static final long doNativeInts(final NativeObject receiver, final long index) {
            final int word = receiver.getIntStorage()[minusOneAndCutInHalf(index)];
            int shortValue;
            if ((index - 1) % 2 == 0) {
                shortValue = word & 0xffff;
            } else {
                shortValue = word >> 16 & 0xffff;
            }
            if ((shortValue & 0x8000) != 0) {
                shortValue = 0xffff0000 | shortValue;
            }
            return shortValue;
        }

        protected static final int minusOneAndDouble(final long index) {
            return (int) ((index - 1) * 2);
        }

        protected static final int largerOffset(final long index) {
            return minusOneAndDouble(index) + 1;
        }

        protected static final int minusOneAndCutInHalf(final long index) {
            return ((int) index - 1) / 2;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "receiver.isLongType()")
        protected static final long doNativeLongs(final NativeObject receiver, final long index) {
            throw SqueakException.create("Not yet implemented: shortAtPut0"); // TODO: implement
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 144)
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimShortAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isByteType()"})
        protected static final long doNativeBytes(final NativeObject receiver, final long index, final long value) {
            final int offset = (int) ((index - 1) * 2);
            final byte[] bytes = receiver.getByteStorage();
            bytes[offset] = (byte) value;
            bytes[offset + 1] = (byte) (value >> 8);
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isShortType()"})
        protected static final long doNativeShorts(final NativeObject receiver, final long index, final long value) {
            receiver.getShortStorage()[(int) index] = (short) value;
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isIntType()", "isEven(index)"})
        protected static final long doNativeIntsEven(final NativeObject receiver, final long index, final long value) {
            final int wordIndex = (int) ((index - 1) / 2);
            final int[] ints = receiver.getIntStorage();
            ints[wordIndex] = ints[wordIndex] & 0xffff0000 | (int) value & 0xffff;
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isIntType()", "!isEven(index)"})
        protected static final long doNativeIntsOdd(final NativeObject receiver, final long index, final long value) {
            final int wordIndex = (int) ((index - 1) / 2);
            final int[] ints = receiver.getIntStorage();
            ints[wordIndex] = (int) value << 16 | ints[wordIndex] & 0xffff;
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"inShortRange(value)", "receiver.isLongType()"})
        protected static final long doNativeLongs(final NativeObject receiver, final long index, final long value) {
            throw SqueakException.create("Not yet implemented: shortAtPut0"); // TODO: implement
        }

        protected static final boolean inShortRange(final long value) {
            return -0x8000 <= value && value <= 0x8000;
        }

        protected static final boolean isEven(final long index) {
            return (index - 1) % 2 == 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 165)
    protected abstract static class PrimIntegerAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimIntegerAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds1(index, receiver.getByteLength())"})
        protected static final long doNativeByte(final NativeObject receiver, final long index) {
            return receiver.getByteStorage()[(int) index - 1];
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())"})
        protected static final long doNativeInt(final NativeObject receiver, final long index) {
            return receiver.getIntStorage()[(int) index - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 166)
    protected abstract static class PrimIntegerAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimIntegerAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds1(index, receiver.getByteLength())", "fitsIntoByte(value)"})
        protected static final long doNativeByte(final NativeObject receiver, final long index, final long value) {
            receiver.getByteStorage()[(int) index - 1] = (byte) value;
            return value;
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())", "fitsIntoInt(value)"})
        protected static final long doNativeInt(final NativeObject receiver, final long index, final long value) {
            receiver.getIntStorage()[(int) index - 1] = (int) value;
            return value;
        }
    }
}

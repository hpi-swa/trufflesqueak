package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeAcceptsValueNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractBasicAtOrAtPutNode extends AbstractPrimitiveNode {
        @Child protected SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        protected AbstractBasicAtOrAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final boolean inBoundsOfSqueakObject(final long index, final Object target) {
            return SqueakGuards.inBounds1(index + instSizeNode.execute(target), sizeNode.execute(target));
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 60)
    protected abstract static class PrimBasicAtNode extends AbstractBasicAtOrAtPutNode implements TernaryPrimitive {
        protected PrimBasicAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)"})
        protected final Object doSqueakObject(final Object receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver));
        }

        /* Context>>#object:basicAt: */
        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)"})
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final Object target, final long index,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(target, index - 1 + instSizeNode.execute(target));
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractBasicAtOrAtPutNode implements QuaternaryPrimitive {
        @Child private NativeAcceptsValueNode nativeAcceptsNode;

        protected PrimBasicAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)", "getNativeAcceptsNode().execute(receiver, value)"})
        protected static final Object doNativeObject(final NativeObject receiver, final long index, final Object value,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("nativeWriteNode") @Cached final NativeObjectWriteNode writeNode) {
            writeNode.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)", "!isUsedJavaPrimitive(receiver)", "!isNativeObject(receiver)"})
        protected final Object doSqueakObject(final Object receiver, final long index, final Object value,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node) {
            atput0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver), value);
            return value;
        }

        /* Context>>#object:basicAt:put: */

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)", "getNativeAcceptsNode().execute(target, value)"})
        protected static final Object doNativeObject(@SuppressWarnings("unused") final Object receiver, final NativeObject target, final long index, final Object value,
                        @Shared("nativeWriteNode") @Cached final NativeObjectWriteNode writeNode) {
            writeNode.execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)", "!isUsedJavaPrimitive(target)", "!isNativeObject(target)"})
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index,
                        final Object value,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node) {
            atput0Node.execute(target, index - 1 + instSizeNode.execute(target), value);
            return value;
        }

        /* TODO: Use @Shared as soon as https://github.com/oracle/graal/issues/1207 is fixed. */
        protected final NativeAcceptsValueNode getNativeAcceptsNode() {
            if (nativeAcceptsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nativeAcceptsNode = insert(NativeAcceptsValueNode.create());
            }
            return nativeAcceptsNode;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSizeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isUsedJavaPrimitive(receiver)"})
        protected static final long doSqueakObject(final Object receiver, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("instSizeNode") @Cached final SqueakObjectInstSizeNode instSizeNode) {
            return sizeNode.execute(receiver) - instSizeNode.execute(receiver);
        }

        /* Context>>#objectSize: */
        @Specialization(guards = {"!isUsedJavaPrimitive(target)", "!isNotProvided(target)"})
        protected static final long doSqueakObject(@SuppressWarnings("unused") final Object receiver, final Object target,
                        @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("instSizeNode") @Cached final SqueakObjectInstSizeNode instSizeNode) {
            return sizeNode.execute(target) - instSizeNode.execute(target);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimStringAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"obj.isByteType()", "inBounds1(index, obj.getByteLength())"})
        protected static final char doNativeObjectBytes(final NativeObject obj, final long index) {
            return (char) (obj.getByteStorage()[(int) index - 1] & 0xFF);
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final Object doNativeObjectInts(final NativeObject obj, final long index,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return CharacterObject.valueOf(obj.getIntStorage()[(int) index - 1], isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimStringAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"obj.isByteType()", "inBounds1(index, obj.getByteLength())", "inByteRange(value)"})
        protected static final char doNativeObjectBytes(final NativeObject obj, final long index, final char value) {
            obj.getByteStorage()[(int) index - 1] = (byte) value;
            return value;
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final char doNativeObjectInts(final NativeObject obj, final long index, final char value) {
            obj.getIntStorage()[(int) index - 1] = value;
            return value;
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final CharacterObject doNativeObjectInts(final NativeObject obj, final long index, final CharacterObject value) {
            obj.getIntStorage()[(int) index - 1] = (int) value.getValue();
            return value;
        }

        protected static final boolean inByteRange(final char value) {
            return value <= NativeObject.BYTE_MAX;
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

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())", "fitsIntoInt(value)"})
        protected static final long doNativeInt(final NativeObject receiver, final long index, final long value) {
            receiver.getIntStorage()[(int) index - 1] = (int) value;
            return value;
        }
    }
}

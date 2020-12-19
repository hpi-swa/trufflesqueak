/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractBasicAtOrAtPutNode extends AbstractPrimitiveNode {
        @Child protected SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        protected final boolean inBoundsOfSqueakObject(final Object target, final int instSize, final long index) {
            return SqueakGuards.inBounds1(index + instSize, sizeNode.execute(target));
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 60)
    protected abstract static class PrimBasicAt2Node extends AbstractBasicAtOrAtPutNode implements BinaryPrimitive {
        @Specialization
        protected final Object doSqueakObject(final Object receiver, final long index,
                        @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            final int instSize = instSizeNode.execute(receiver);
            if (inBoundsOfSqueakObject(receiver, instSize, index)) {
                return at0Node.execute(receiver, index - 1 + instSize);
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 60)
    protected abstract static class PrimBasicAt3Node extends AbstractBasicAtOrAtPutNode implements TernaryPrimitive {
        @Specialization
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final Object target, final long index,
                        @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            final int instSize = instSizeNode.execute(target);
            if (inBoundsOfSqueakObject(target, instSize, index)) {
                return at0Node.execute(target, index - 1 + instSize);
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPut3Node extends AbstractBasicAtOrAtPutNode implements TernaryPrimitive {
        @Specialization
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, final Object value,
                        @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            final int instSize = instSizeNode.execute(receiver);
            if (inBoundsOfSqueakObject(receiver, instSize, index)) {
                atput0Node.execute(receiver, index - 1 + instSize, value);
                return value;
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPut4Node extends AbstractBasicAtOrAtPutNode implements QuaternaryPrimitive {
        @Specialization
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            final int instSize = instSizeNode.execute(target);
            if (inBoundsOfSqueakObject(target, instSize, index)) {
                atput0Node.execute(target, index - 1 + instSize, value);
                return value;
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSize1Node extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Specialization
        protected static final long doSqueakObject(final AbstractSqueakObject receiver,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectInstSizeNode instSizeNode) {
            return sizeNode.execute(receiver) - instSizeNode.execute(receiver);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSize2Node extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final long doSqueakObject(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectInstSizeNode instSizeNode) {
            return sizeNode.execute(target) - instSizeNode.execute(target);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"obj.isByteType()", "inBounds1(index, obj.getByteLength())"})
        protected static final char doNativeObjectBytes(final NativeObject obj, final long index) {
            return (char) (obj.getByte(index - 1) & 0xFF);
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final Object doNativeObjectInts(final NativeObject obj, final long index,
                        @Cached final ConditionProfile isImmediateProfile) {
            return CharacterObject.valueOf(Integer.toUnsignedLong(obj.getInt(index - 1)), isImmediateProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"obj.isByteType()", "inBounds1(index, obj.getByteLength())", "inByteRange(value)"})
        protected static final char doNativeObjectBytes(final NativeObject obj, final long index, final char value) {
            obj.setByte(index - 1, (byte) value);
            return value;
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final char doNativeObjectInts(final NativeObject obj, final long index, final char value) {
            obj.setInt(index - 1, value);
            return value;
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final CharacterObject doNativeObjectInts(final NativeObject obj, final long index, final CharacterObject value) {
            obj.setInt(index - 1, (int) value.getValue());
            return value;
        }

        protected static final boolean inByteRange(final char value) {
            return value <= NativeObject.BYTE_MAX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 143)
    protected abstract static class PrimShortAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength(), 2)"})
        protected static final long doNativeInts(final NativeObject receiver, final long index) {
            return UnsafeUtils.getShort(receiver.getIntStorage(), index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 144)
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength(), 2)", "inShortRange(value)"})
        protected static final long doNativeInts(final NativeObject receiver, final long index, final long value) {
            UnsafeUtils.putShort(receiver.getIntStorage(), index - 1, (short) value);
            return value;
        }

        protected static final boolean inShortRange(final long value) {
            return -0x8000 <= value && value <= 0x8000;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 165)
    protected abstract static class PrimIntegerAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())"})
        protected static final long doNativeInt(final NativeObject receiver, final long index) {
            return receiver.getInt(index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 166)
    protected abstract static class PrimIntegerAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())", "fitsIntoInt(value)"})
        protected static final long doNativeInt(final NativeObject receiver, final long index, final long value) {
            receiver.setInt(index - 1, (int) value);
            return value;
        }
    }
}

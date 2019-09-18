/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
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
import de.hpi.swa.graal.squeak.util.NotProvided;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

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

        protected final boolean inBoundsOfSqueakObject(final Object target, final long index) {
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

        @Specialization
        protected final Object doSqueakObject(final Object receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            if (inBoundsOfSqueakObject(receiver, index)) {
                return at0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver));
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }

        /* Context>>#object:basicAt: */
        @Specialization
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final Object target, final long index,
                        @Shared("at0Node") @Cached final SqueakObjectAt0Node at0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            return doSqueakObject(target, index, NotProvided.SINGLETON, at0Node, outOfBounceProfile);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractBasicAtOrAtPutNode implements QuaternaryPrimitive {
        protected PrimBasicAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, final Object value,
                        @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            if (inBoundsOfSqueakObject(receiver, index)) {
                atput0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver), value);
                return value;
            } else {
                outOfBounceProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
        }

        /* Context>>#object:basicAt:put: */
        @Specialization
        protected final Object doSqueakObject(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @Shared("atput0Node") @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final BranchProfile outOfBounceProfile) {
            return doSqueakObject(target, index, value, NotProvided.SINGLETON, atput0Node, outOfBounceProfile);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSizeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doSqueakObject(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final NotProvided notProvided,
                        @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("instSizeNode") @Cached final SqueakObjectInstSizeNode instSizeNode) {
            return sizeNode.execute(receiver) - instSizeNode.execute(receiver);
        }

        /* Context>>#objectSize: */
        @Specialization
        protected static final long doSqueakObject(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target,
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
            return (char) (obj.getByte(index - 1) & 0xFF);
        }

        @Specialization(guards = {"obj.isIntType()", "inBounds1(index, obj.getIntLength())"})
        protected static final Object doNativeObjectInts(final NativeObject obj, final long index,
                        @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return CharacterObject.valueOf(obj.getInt(index - 1), isFiniteProfile);
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

        protected PrimShortAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength(), 2)"})
        protected static final long doNativeInts(final NativeObject receiver, final long index) {
            return Short.toUnsignedLong(UnsafeUtils.getShort(receiver.getIntStorage(), index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 144)
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimShortAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

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

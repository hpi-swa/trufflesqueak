/*
 * Copyright (c) 2020-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class Float64ArrayPlugin extends AbstractPrimitiveFactoryHolder {
    private static final VectorSpecies<Long> LONG_VECTOR_SPECIES = LongVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Double> DOUBLE_VECTOR_SPECIES = DoubleVector.SPECIES_PREFERRED;

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return Float64ArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddFloat64Array")
    public abstract static class PrimAddFloat64ArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            final int length = longs1.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                vector1.add(vector2).reinterpretAsLongs().intoArray(longs1, i);
            }
            for (; i < length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) + Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddScalar")
    public abstract static class PrimAddScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                vector.add(scalarValue).reinterpretAsLongs().intoArray(longs, i);
            }
            for (; i < length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) + scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAt")
    public abstract static class PrimFloat64ArrayAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected static final double doAt(final NativeObject receiver, final long index) {
            return Double.longBitsToDouble(receiver.getLong(index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAtPut")
    public abstract static class PrimFloat64ArrayAtPutNode extends AbstractPrimitiveNode implements Primitive2 {
        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected static final double doDouble(final NativeObject receiver, final long index, final double value) {
            receiver.setLong(index - 1, Double.doubleToRawLongBits(value));
            return value;
        }

        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected final FloatObject doFloat(final NativeObject receiver, final long index, final FloatObject value) {
            return FloatObject.valueOf(getContext(), doDouble(receiver, index, value.getValue()));
        }

        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected static final double doFloat(final NativeObject receiver, final long index, final long value) {
            return doDouble(receiver, index, value);
        }

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()", "isFraction(value, node)"})
        protected static final double doFraction(final NativeObject receiver, final long index, final PointersObject value,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectNodes.AbstractPointersObjectReadNode readNode) {
            return doDouble(receiver, index, SqueakImageContext.fromFraction(value, readNode, node));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isFallback(node, receiver, index, value)")
        protected static final Object doFail(final NativeObject receiver, final long index, final Object value,
                        @Bind final Node node) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        protected static final boolean isFallback(final Node node, final NativeObject receiver, final long index, final Object value) {
            return !(receiver.isIntType() && index <= receiver.getIntLength() && (value instanceof Double || value instanceof FloatObject || value instanceof Long ||
                            (value instanceof PointersObject pointersObject && SqueakGuards.isFraction(pointersObject, node))));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivFloat64Array")
    public abstract static class PrimDivFloat64ArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            final int length = longs1.length;
            /* "Check if any of the argument's values is zero". */
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                if (vector2.compare(VectorOperators.EQ, 0).anyTrue()) {
                    throw PrimitiveFailed.transferToInterpreterAndBadArgument();
                }
            }
            for (; i < length; i++) {
                if (Double.longBitsToDouble(longs2[i]) == 0) {
                    throw PrimitiveFailed.transferToInterpreterAndBadArgument();
                }
            }
            i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                vector1.div(vector2).reinterpretAsLongs().intoArray(longs1, i);
            }
            for (; i < length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) / Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivScalar")
    public abstract static class PrimDivScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final double scalarValue) {
            if (scalarValue == 0) {
                throw PrimitiveFailed.transferToInterpreterAndBadArgument();
            }
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                vector.div(scalarValue).reinterpretAsLongs().intoArray(longs, i);
            }
            for (; i < length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) / scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDotProduct")
    public abstract static class PrimDotProductNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "aFloatVector.isLongType()", "receiver.getLongLength() == aFloatVector.getLongLength()"})
        protected static final double doDot64bit(final NativeObject receiver, final NativeObject aFloatVector) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = aFloatVector.getLongStorage();
            final int length = longs1.length;
            int i = 0;
            DoubleVector acc = DoubleVector.zero(DOUBLE_VECTOR_SPECIES);
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                acc = acc.add(vector1.mul(vector2));
            }
            double result = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                result += Double.longBitsToDouble(longs1[i]) * Double.longBitsToDouble(longs2[i]);
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEqual")
    public abstract static class PrimFloat64ArrayEqualNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "other.isLongType()"})
        protected static final boolean doEqual(final NativeObject receiver, final NativeObject other,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameLengthProfile) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = other.getLongStorage();
            final int length = longs1.length;
            if (sameLengthProfile.profile(node, length != longs2.length)) {
                return BooleanObject.FALSE;
            } else {
                int i = 0;
                for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                    final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                    final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                    if (!vector1.compare(VectorOperators.EQ, vector2).allTrue()) {
                        return BooleanObject.FALSE;
                    }
                }
                return BooleanObject.wrap(Arrays.equals(longs1, i, length, longs2, i, length));
            }
        }

        /*
         * Specialization for quick nil checks.
         */
        @SuppressWarnings("unused")
        @Specialization
        protected static final boolean doNilCase(final NativeObject receiver, final NilObject other) {
            return BooleanObject.FALSE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFromFloatArray")
    public abstract static class PrimFromFloatArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "other.isIntType()", "receiver.getLongLength() == other.getIntLength()"})
        protected static final NativeObject doFromFloatArray(final NativeObject receiver, final NativeObject other) {
            final long[] longs = receiver.getLongStorage();
            final int[] ints = other.getIntStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = Double.doubleToRawLongBits(Float.intBitsToFloat(ints[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHashArray")
    public abstract static class PrimHashArrayNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isLongType()")
        protected static final long doHash(final NativeObject receiver) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            LongVector acc = LongVector.zero(LONG_VECTOR_SPECIES);
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final LongVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i);
                acc = acc.add(vector);
            }
            long hash = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                hash += longs[i];
            }
            return hash & 0x1fffffff;
        }
    }

    // primitiveLength: no Implementation because it is not used in Squeak.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulFloat64Array")
    public abstract static class PrimMulFloat64ArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()",
                        "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doMul(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            final int length = longs1.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                vector1.mul(vector2).reinterpretAsLongs().intoArray(longs1, i);
            }
            for (; i < length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) * Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulScalar")
    public abstract static class PrimMulScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doMul(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                vector.mul(scalarValue).reinterpretAsLongs().intoArray(longs, i);
            }
            for (; i < length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) * scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNormalize")
    public abstract static class PrimFloat64ArrayNormalizeNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doNormalize(final NativeObject receiver) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            DoubleVector acc = DoubleVector.zero(DOUBLE_VECTOR_SPECIES);
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                acc = acc.add(vector.mul(vector));
            }
            double len = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                final double value = Double.longBitsToDouble(longs[i]);
                len += value * value;
            }
            if (len <= 0.0D) {
                throw PrimitiveFailed.BAD_RECEIVER;
            }
            final double sqrtLen = Math.sqrt(len);
            i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                vector.div(sqrtLen).reinterpretAsLongs().intoArray(longs, i);
            }
            for (; i < length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) / sqrtLen);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubFloat64Array")
    public abstract static class PrimSubFloat64ArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doSub(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            final int length = longs1.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector1 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs1, i).reinterpretAsDoubles();
                final DoubleVector vector2 = LongVector.fromArray(LONG_VECTOR_SPECIES, longs2, i).reinterpretAsDoubles();
                vector1.sub(vector2).reinterpretAsLongs().intoArray(longs1, i);
            }
            for (; i < length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) - Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubScalar")
    public abstract static class PrimSubScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doSub(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                vector.sub(scalarValue).reinterpretAsLongs().intoArray(longs, i);
            }
            for (; i < length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) - scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSum")
    public abstract static class PrimFloat64ArraySumNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isLongType()"})
        protected static final double doSum(final NativeObject receiver) {
            final long[] longs = receiver.getLongStorage();
            final int length = longs.length;
            int i = 0;
            DoubleVector acc = DoubleVector.zero(DOUBLE_VECTOR_SPECIES);
            for (; i < LONG_VECTOR_SPECIES.loopBound(length); i += LONG_VECTOR_SPECIES.length()) {
                final DoubleVector vector = LongVector.fromArray(LONG_VECTOR_SPECIES, longs, i).reinterpretAsDoubles();
                acc = acc.add(vector);
            }
            double sum = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                sum += Double.longBitsToDouble(longs[i]);
            }
            return sum;
        }
    }
}

/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
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
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class FloatArrayPlugin extends AbstractPrimitiveFactoryHolder {
    private static final VectorSpecies<Integer> INT_VECTOR_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Float> FLOAT_VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddFloatArray")
    public abstract static class PrimAddFloatArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()", "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            final int length = ints1.length;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                vector1.add(vector2).reinterpretAsInts().intoArray(ints1, i);
            }
            for (; i < length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) + Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddScalar")
    public abstract static class PrimAddScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final double doubleScalarValue) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            final float scalarValue = (float) doubleScalarValue;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                vector.add(scalarValue).reinterpretAsInts().intoArray(ints, i);
            }
            for (; i < length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) + scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAt")
    public abstract static class PrimFloatArrayAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final double doAt(final NativeObject receiver, final long index) {
            return Float.intBitsToFloat(receiver.getInt(index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAtPut")
    public abstract static class PrimFloatArrayAtPutNode extends AbstractPrimitiveNode implements Primitive2 {
        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final double doDouble(final NativeObject receiver, final long index, final double value) {
            receiver.setInt(index - 1, Float.floatToRawIntBits((float) value));
            return value;
        }

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected final FloatObject doFloat(final NativeObject receiver, final long index, final FloatObject value) {
            return FloatObject.valueOf(getContext(), doDouble(receiver, index, value.getValue()));
        }

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
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
    @SqueakPrimitive(names = "primitiveDivFloatArray")
    public abstract static class PrimDivFloatArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()", "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            final int length = ints1.length;
            /* "Check if any of the argument's values is zero". */
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                if (vector2.compare(VectorOperators.EQ, 0).anyTrue()) {
                    throw PrimitiveFailed.transferToInterpreterAndBadArgument();
                }
            }
            for (; i < length; i++) {
                if (Float.intBitsToFloat(ints2[i]) == 0) {
                    throw PrimitiveFailed.transferToInterpreterAndBadArgument();
                }
            }
            i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                vector1.div(vector2).reinterpretAsInts().intoArray(ints1, i);
            }
            for (; i < length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) / Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivScalar")
    public abstract static class PrimDivScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final double doubleScalarValue) {
            if (doubleScalarValue == 0) {
                throw PrimitiveFailed.transferToInterpreterAndBadArgument();
            }
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            final float scalarValue = (float) doubleScalarValue;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                vector.div(scalarValue).reinterpretAsInts().intoArray(ints, i);
            }
            for (; i < length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) / scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDotProduct")
    public abstract static class PrimDotProductNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "aFloatVector.isIntType()", "receiver.getIntLength() == aFloatVector.getIntLength()"})
        protected static final double doDot64bit(final NativeObject receiver, final NativeObject aFloatVector) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = aFloatVector.getIntStorage();
            final int length = ints1.length;
            int i = 0;
            FloatVector acc = FloatVector.zero(FLOAT_VECTOR_SPECIES);
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                acc = acc.add(vector1.mul(vector2));
            }
            float result = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                result += Float.intBitsToFloat(ints1[i]) * Float.intBitsToFloat(ints2[i]);
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEqual")
    public abstract static class PrimFloatArrayEqualNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "other.isIntType()"})
        protected static final boolean doEqual(final NativeObject receiver, final NativeObject other,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile sameLengthProfile) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = other.getIntStorage();
            final int length = ints1.length;
            if (sameLengthProfile.profile(node, length != ints2.length)) {
                return BooleanObject.FALSE;
            } else {
                int i = 0;
                for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                    final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                    final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                    if (!vector1.compare(VectorOperators.EQ, vector2).allTrue()) {
                        return BooleanObject.FALSE;
                    }
                }
                return BooleanObject.wrap(Arrays.equals(ints1, i, length, ints2, i, length));
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
    @SqueakPrimitive(names = "primitiveFromFloat64Array")
    public abstract static class PrimFromFloat64ArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "other.isLongType()", "receiver.getIntLength() == other.getLongLength()"})
        protected static final NativeObject doFromFloat64Array(final NativeObject receiver, final NativeObject other) {
            final int[] ints = receiver.getIntStorage();
            final long[] longs = other.getLongStorage();
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Float.floatToRawIntBits((float) Double.longBitsToDouble(longs[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHashArray")
    public abstract static class PrimHashArrayNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isIntType()")
        protected static final long doHash(final NativeObject receiver) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            int i = 0;
            IntVector acc = IntVector.zero(INT_VECTOR_SPECIES);
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final IntVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i);
                acc = acc.add(vector);
            }
            int hash = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                hash += ints[i];
            }
            return hash & 0x1fffffff;
        }
    }

    // primitiveLength: no Implementation because it is not used in Squeak.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulFloatArray")
    public abstract static class PrimMulFloatArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()", "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doMul(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            final int length = ints1.length;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                vector1.mul(vector2).reinterpretAsInts().intoArray(ints1, i);
            }
            for (; i < length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) * Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulScalar")
    public abstract static class PrimMulScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doMul(final NativeObject receiver, final double doubleScalarValue) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            final float scalarValue = (float) doubleScalarValue;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                vector.mul(scalarValue).reinterpretAsInts().intoArray(ints, i);
            }
            for (; i < length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) * scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNormalize")
    public abstract static class PrimFloatArrayNormalizeNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doNormalize(final NativeObject receiver) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            int i = 0;
            FloatVector acc = FloatVector.zero(FLOAT_VECTOR_SPECIES);
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                acc = acc.add(vector.mul(vector));
            }
            float len = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                final float value = Float.intBitsToFloat(ints[i]);
                len += value * value;
            }
            if (len <= 0.0F) {
                throw PrimitiveFailed.BAD_RECEIVER;
            }
            final float sqrtLen = (float) Math.sqrt(len);
            i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                vector.div(sqrtLen).reinterpretAsInts().intoArray(ints, i);
            }
            for (; i < length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) / sqrtLen);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubFloatArray")
    public abstract static class PrimSubFloatArrayNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()", "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doSub(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            final int length = ints1.length;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector1 = IntVector.fromArray(INT_VECTOR_SPECIES, ints1, i).reinterpretAsFloats();
                final FloatVector vector2 = IntVector.fromArray(INT_VECTOR_SPECIES, ints2, i).reinterpretAsFloats();
                vector1.sub(vector2).reinterpretAsInts().intoArray(ints1, i);
            }
            for (; i < length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) - Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubScalar")
    public abstract static class PrimSubScalarNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doSub(final NativeObject receiver, final double doubleScalarValue) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            final float scalarValue = (float) doubleScalarValue;
            int i = 0;
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                vector.sub(scalarValue).reinterpretAsInts().intoArray(ints, i);
            }
            for (; i < length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) - scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSum")
    public abstract static class PrimFloatArraySumNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isIntType()"})
        protected static final double doSum(final NativeObject receiver) {
            final int[] ints = receiver.getIntStorage();
            final int length = ints.length;
            int i = 0;
            FloatVector acc = FloatVector.zero(FLOAT_VECTOR_SPECIES);
            for (; i < INT_VECTOR_SPECIES.loopBound(length); i += INT_VECTOR_SPECIES.length()) {
                final FloatVector vector = IntVector.fromArray(INT_VECTOR_SPECIES, ints, i).reinterpretAsFloats();
                acc = acc.add(vector);
            }
            float sum = acc.reduceLanes(VectorOperators.ADD);
            for (; i < length; i++) {
                sum += Float.intBitsToFloat(ints[i]);
            }
            return sum;
        }
    }
}

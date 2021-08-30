/*
 * Copyright (c) 2020-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class Float64ArrayPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddFloat64Array")
    public abstract static class PrimAddFloat64ArrayNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            for (int i = 0; i < longs1.length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) + Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddScalar")
    public abstract static class PrimAddScalarNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) + (float) scalarValue);
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAt")
    public abstract static class PrimFloat64ArrayAtNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected static final double doAt(final NativeObject receiver, final long index) {
            return Double.longBitsToDouble(receiver.getLong((int) index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAtPut")
    public abstract static class PrimFloat64ArrayAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "index <= receiver.getLongLength()"})
        protected static final double doDouble(final NativeObject receiver, final long index, final double value) {
            receiver.getLongStorage()[(int) index - 1] = Double.doubleToRawLongBits((float) value);
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivFloat64Array")
    public abstract static class PrimDivFloat64ArrayNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();
            /* "Check if any of the argument's values is zero". */
            for (final long value : longs2) {
                if (Double.longBitsToDouble(value) == 0) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
            }
            for (int i = 0; i < longs1.length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) / Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivScalar")
    public abstract static class PrimDivScalarNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = Double.doubleToRawLongBits((float) (Double.longBitsToDouble(longs[i]) / scalarValue));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDotProduct")
    public abstract static class PrimDotProductNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "aFloatVector.isLongType()", "receiver.getLongLength() == aFloatVector.getLongLength()"})
        protected static final double doDot64bit(final NativeObject receiver, final NativeObject aFloatVector) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = aFloatVector.getLongStorage();
            float result = 0;
            for (int i = 0; i < longs1.length; i++) {
                result += Double.longBitsToDouble(longs1[i]) * Double.longBitsToDouble(longs2[i]);
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEqual")
    public abstract static class PrimFloat64ArrayEqualNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "other.isLongType()"})
        protected static final boolean doEqual(final NativeObject receiver, final NativeObject other) {
            return BooleanObject.wrap(Arrays.equals(receiver.getLongStorage(), other.getLongStorage()));
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
    public abstract static class PrimFromFloatArrayNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "other.isIntType()", "receiver.getLongLength() == other.getIntLength()"})
        protected static final NativeObject doFromFloatArray(final NativeObject receiver, final NativeObject other) {
            final long[] longs = receiver.getLongStorage();
            final int[] ints = other.getIntStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = ints[i];
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHashArray")
    public abstract static class PrimHashArrayNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization(guards = "receiver.isLongType()")
        protected static final long doHash(final NativeObject receiver) {
            final long[] words = receiver.getLongStorage();
            long hash = 0;
            for (final long word : words) {
                hash += word;
            }
            return hash & 0x1fffffff;
        }
    }

    // primitiveLength: no Implementation because it is not used in Squeak.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulFloat64Array")
    public abstract static class PrimMulFloat64ArrayNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()",
                        "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doMul(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();

            for (int i = 0; i < longs1.length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) * Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulScalar")
    public abstract static class PrimMulScalarNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doMul(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) * (float) scalarValue);
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNormalize") // TODO: implement primitive
    public abstract static class PrimFloat64ArrayNormalizeNode extends AbstractPrimitiveNode {

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubFloat64Array")
    public abstract static class PrimSubFloat64ArrayNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()", "floatArray.isLongType()", "receiver.getLongLength() == floatArray.getLongLength()"})
        protected static final NativeObject doSub(final NativeObject receiver, final NativeObject floatArray) {
            final long[] longs1 = receiver.getLongStorage();
            final long[] longs2 = floatArray.getLongStorage();

            for (int i = 0; i < longs1.length; i++) {
                longs1[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs1[i]) - Double.longBitsToDouble(longs2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubScalar")
    public abstract static class PrimSubScalarNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()"})
        protected static final NativeObject doSub(final NativeObject receiver, final double scalarValue) {
            final long[] longs = receiver.getLongStorage();
            for (int i = 0; i < longs.length; i++) {
                longs[i] = Double.doubleToRawLongBits(Double.longBitsToDouble(longs[i]) - (float) scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSum")
    public abstract static class PrimFloat64ArraySumNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization(guards = {"receiver.isLongType()"})
        protected static final double doSum(final NativeObject receiver) {
            final long[] words = receiver.getLongStorage();
            double sum = 0;
            for (final long word : words) {
                sum += Double.longBitsToDouble(word);
            }
            return sum;
        }
    }
}

/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class FloatArrayPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddFloatArray")
    public abstract static class PrimAddFloatArrayNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()",
                        "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            for (int i = 0; i < ints1.length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) + Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddScalar")
    public abstract static class PrimAddScalarNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doAdd(final NativeObject receiver, final double scalarValue) {
            final int[] ints = receiver.getIntStorage();
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) + (float) scalarValue);
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAt")
    public abstract static class PrimFloatArrayAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final double doAt(final NativeObject receiver, final long index) {
            return Float.intBitsToFloat(receiver.getInt((int) index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAtPut")
    public abstract static class PrimFloatArrayAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final double doDouble(final NativeObject receiver, final long index, final double value) {
            receiver.getIntStorage()[(int) index - 1] = Float.floatToRawIntBits((float) value);
            return value;
        }

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final FloatObject doFloat(final NativeObject receiver, final long index, final FloatObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return FloatObject.valueOf(image, doDouble(receiver, index, value.getValue()));
        }

        @Specialization(guards = {"receiver.isIntType()", "index <= receiver.getIntLength()"})
        protected static final double doFloat(final NativeObject receiver, final long index, final long value) {
            return doDouble(receiver, index, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivFloatArray")
    public abstract static class PrimDivFloatArrayNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()",
                        "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();
            /* "Check if any of the argument's values is zero". */
            for (final int value : ints2) {
                if (Float.intBitsToFloat(value) == 0) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
            }
            for (int i = 0; i < ints1.length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) / Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDivScalar")
    public abstract static class PrimDivScalarNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doDiv(final NativeObject receiver, final double scalarValue) {
            final int[] ints = receiver.getIntStorage();
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(ints[i]) / scalarValue));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDotProduct")
    public abstract static class PrimDotProductNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "aFloatVector.isIntType()", "receiver.getIntLength() == aFloatVector.getIntLength()"})
        protected static final double doDot64bit(final NativeObject receiver, final NativeObject aFloatVector) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = aFloatVector.getIntStorage();
            float result = 0;
            for (int i = 0; i < ints1.length; i++) {
                result += Float.intBitsToFloat(ints1[i]) * Float.intBitsToFloat(ints2[i]);
            }
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEqual")
    public abstract static class PrimFloatArrayEqualNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "other.isIntType()"})
        protected static final boolean doEqual(final NativeObject receiver, final NativeObject other) {
            return BooleanObject.wrap(Arrays.equals(receiver.getIntStorage(), other.getIntStorage()));
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
    @SqueakPrimitive(names = "primitiveHashArray")
    public abstract static class PrimHashArrayNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @Specialization(guards = "receiver.isIntType()")
        protected static final long doHash(final NativeObject receiver) {
            final int[] words = receiver.getIntStorage();
            long hash = 0;
            for (final int word : words) {
                hash += word;
            }
            return hash & 0x1fffffff;
        }
    }

    // primitiveLength: no Implementation because it is not used in Squeak.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulFloatArray")
    public abstract static class PrimMulFloatArrayNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()",
                        "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doMul(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();

            for (int i = 0; i < ints1.length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) * Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMulScalar")
    public abstract static class PrimMulScalarNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doMul(final NativeObject receiver, final double scalarValue) {
            final int[] ints = receiver.getIntStorage();
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) * (float) scalarValue);
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNormalize") // TODO: implement primitive
    public abstract static class PrimFloatArrayNormalizeNode extends AbstractPrimitiveNode {

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubFloatArray")
    public abstract static class PrimSubFloatArrayNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()", "floatArray.isIntType()",
                        "receiver.getIntLength() == floatArray.getIntLength()"})
        protected static final NativeObject doSub(final NativeObject receiver, final NativeObject floatArray) {
            final int[] ints1 = receiver.getIntStorage();
            final int[] ints2 = floatArray.getIntStorage();

            for (int i = 0; i < ints1.length; i++) {
                ints1[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints1[i]) - Float.intBitsToFloat(ints2[i]));
            }
            return receiver;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSubScalar")
    public abstract static class PrimSubScalarNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()"})
        protected static final NativeObject doSub(final NativeObject receiver, final double scalarValue) {
            final int[] ints = receiver.getIntStorage();
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Float.floatToRawIntBits(Float.intBitsToFloat(ints[i]) - (float) scalarValue);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSum")
    public abstract static class PrimFloatArraySumNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @Specialization(guards = {"receiver.isIntType()"})
        protected static final double doSum(final NativeObject receiver) {
            final int[] words = receiver.getIntStorage();
            double sum = 0;
            for (final int word : words) {
                sum += Float.intBitsToFloat(word);
            }
            return sum;
        }
    }
}

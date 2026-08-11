/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class SqueakFFIPrims extends AbstractPrimitiveFactoryHolder {

    // TODO: implement primitiveCalloutWithArgs

    // TODO: implement primitiveLoadSymbolFromModule

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIDoubleAt")
    protected abstract static class PrimFFIDoubleAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong) {
            return VarHandleUtils.getDoubleFromBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIDoubleAtPut")
    protected abstract static class PrimFFIDoubleAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong, final double value) {
            VarHandleUtils.putDoubleIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIFloatAt")
    protected abstract static class PrimFFIFloatAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 4)"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong) {
            return VarHandleUtils.getFloatFromBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIFloatAtPut")
    protected abstract static class PrimFFIFloatAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 4)"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong, final double value) {
            VarHandleUtils.putFloatIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, (float) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIIntegerAt")
    protected abstract static class PrimFFIIntegerAtNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 1", "isSigned"})
        protected static final long doAt1Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return byteArray.getByte(byteOffsetLong - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 1", "!isSigned"})
        protected static final long doAt1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return byteArray.getByteUnsigned(byteOffsetLong - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 2", "isSigned"})
        protected static final long doAt2Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt16AtNode.signedInt16At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 2", "!isSigned"})
        protected static final long doAt2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt16AtNode.doAt(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "isSigned"})
        protected static final long doAt4Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt32AtNode.signedInt32At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "!isSigned"})
        protected static final long doAt4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt32AtNode.doAt(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "isSigned"})
        protected static final long doAt8Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt64AtNode.signedInt64At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "!isSigned"})
        protected static final Object doAt8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile positiveProfile) {
            return PrimUnsignedInt64AtNode.unsignedInt64At(image, byteArray, byteOffsetLong, positiveProfile, node);
        }
    }

    protected static final class FFIGuards {
        protected static final long MAX_VALUE_SIGNED_1 = 1L << 8 * 1 - 1;
        protected static final long MAX_VALUE_SIGNED_2 = 1L << 8 * 2 - 1;
        protected static final long MAX_VALUE_SIGNED_4 = 1L << 8 * 4 - 1;
        protected static final BigInteger MAX_VALUE_SIGNED_8 = BigInteger.ONE.shiftLeft(8 * 8 - 1);
        protected static final long MAX_VALUE_UNSIGNED_1 = 1L << 8 * 1;
        protected static final long MAX_VALUE_UNSIGNED_2 = 1L << 8 * 2;
        protected static final long MAX_VALUE_UNSIGNED_4 = 1L << 8 * 4;

        protected static boolean inSignedBounds(final long value, final long max) {
            return value >= -max && value < max;
        }

        protected static boolean inUnsignedBounds(final long value, final long max) {
            return 0 <= value && value < max;
        }

        protected static boolean inByteBounds(final long byteOffset, final int byteLength, final long byteSize) {
            return byteOffset > 0 && byteSize > 0 && byteOffset <= byteLength - byteSize + 1;
        }

        @TruffleBoundary
        protected static boolean inSignedBounds(final SqueakImageContext image, final NativeObject value, final BigInteger max) {
            final BigInteger bigInteger = LargeIntegers.toBigInteger(image, value);
            return bigInteger.compareTo(BigInteger.ZERO.subtract(max)) >= 0 && bigInteger.compareTo(max) < 0;
        }

        @TruffleBoundary
        protected static boolean inUnsignedBounds(final SqueakImageContext image, final NativeObject value) {
            return LargeIntegers.isZeroOrPositive(image, value) && LargeIntegers.lessThanOneShiftedBy64(value);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIIntegerAtPut")
    protected abstract static class PrimFFIIntegerAtPutNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 1", "isSigned",
                        "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final long doAtPut1Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut1Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 1", "!isSigned",
                        "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final long doAtPut1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt8AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 2", "isSigned",
                        "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final long doAtPut2Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut2Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 2", "!isSigned",
                        "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final long doAtPut2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt16AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "isSigned",
                        "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final long doAtPut4Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut4Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "!isSigned",
                        "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final long doAtPut4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "isSigned", "image.isLargeInteger(value)",
                        "fitsIntoLong(value)",
                        "inSignedBounds(longValueExact(value), MAX_VALUE_SIGNED_4)"})
        protected static final NativeObject doAtPut4SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value, final long byteSize,
                        final boolean isSigned,
                        @Bind final SqueakImageContext image) {
            return doAtPut4UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned, image);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 4", "!isSigned", "image.isLargeInteger(value)",
                        "fitsIntoLong(value)",
                        "inUnsignedBounds(longValueExact(value), MAX_VALUE_UNSIGNED_4)"})
        protected static final NativeObject doAtPut4UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value, final long byteSize,
                        final boolean isSigned,
                        @Bind final SqueakImageContext image) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "isSigned"})
        protected static final Object doAtPut8Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut8Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "!isSigned", "value >= 0"})
        protected static final Object doAtPut8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt64AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "isSigned", "image.isLargeInteger(value)",
                        "inSignedBounds(image, value, MAX_VALUE_SIGNED_8)"})
        protected static final Object doAtPut8SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value, final long byteSize, final boolean isSigned,
                        @Bind final SqueakImageContext image) {
            return doAtPut8UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned, image);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), byteSize)", "byteSize == 8", "!isSigned", "image.isLargeInteger(value)",
                        "inUnsignedBounds(image, value)"})
        protected static final Object doAtPut8UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value, final long byteSize, final boolean isSigned,
                        @Bind final SqueakImageContext image) {
            return PrimUnsignedInt64AtPutNode.doAtPut(byteArray, byteOffsetLong, value, image);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt8At")
    protected abstract static class PrimSignedInt8AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 1)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return byteArray.getByte(byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt8AtPut")
    protected abstract static class PrimSignedInt8AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 1)", "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt8AtPutNode.doAtPut(byteArray, byteOffset, value);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt16At")
    protected abstract static class PrimSignedInt16AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 2)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt16At(byteArray, byteOffset);
        }

        private static short signedInt16At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getShortFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt16AtPut")
    protected abstract static class PrimSignedInt16AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 2)", "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt16AtPutNode.doAtPut(byteArray, byteOffset, value);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt32At")
    protected abstract static class PrimSignedInt32AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 4)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt32At(byteArray, byteOffset);
        }

        private static int signedInt32At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getIntFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt32AtPut")
    protected abstract static class PrimSignedInt32AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 4)", "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffset, value);
        }

        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 4)", "fitsIntoLong(value)",
                        "inSignedBounds(longValueExact(value), MAX_VALUE_SIGNED_4)"})
        protected static final NativeObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt64At")
    protected abstract static class PrimSignedInt64AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 8)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt64At(byteArray, byteOffset);
        }

        private static long signedInt64At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getLongFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = {"primitiveSignedInt64AtPut", //
                    "primitiveSignedInt648At" /* typo in ByteArray>>#long64At:put: */})
    protected abstract static class PrimSignedInt64AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffsetLong, final long value) {
            VarHandleUtils.putLongIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)", "image.isLargeInteger(value)",
                        "inSignedBounds(image, value, MAX_VALUE_SIGNED_8)"})
        protected static final NativeObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value,
                        @Bind final SqueakImageContext image) {
            atPutNativeLarge(byteArray, byteOffsetLong, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt8At")
    protected abstract static class PrimUnsignedInt8AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 1)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return byteArray.getByteUnsigned(byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt8AtPut")
    protected abstract static class PrimUnsignedInt8AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 1)", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            byteArray.setByte(byteOffset - 1, (byte) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt16At")
    protected abstract static class PrimUnsignedInt16AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 2)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return Short.toUnsignedLong(PrimSignedInt16AtNode.signedInt16At(byteArray, byteOffset));
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt16AtPut")
    protected abstract static class PrimUnsignedInt16AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 2)", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            VarHandleUtils.putShortIntoBytes(byteArray.getByteStorage(), (int) byteOffset - 1, (short) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt32At")
    protected abstract static class PrimUnsignedInt32AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 4)"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return Integer.toUnsignedLong(PrimSignedInt32AtNode.signedInt32At(byteArray, byteOffset));
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt32AtPut")
    protected abstract static class PrimUnsignedInt32AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 4)", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            VarHandleUtils.putIntIntoBytes(byteArray.getByteStorage(), (int) byteOffset - 1, (int) value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 4)", "fitsIntoLong(value)",
                        "inUnsignedBounds(longValueExact(value), MAX_VALUE_UNSIGNED_4)"})
        @ExplodeLoop
        protected static final NativeObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getByteStorage();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 4; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt64At")
    protected abstract static class PrimUnsignedInt64AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffset, byteArray.getByteLength(), 8)"})
        protected static final Object doAt(final NativeObject byteArray, final long byteOffset,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile positiveProfile) {
            return unsignedInt64At(image, byteArray, byteOffset, positiveProfile, node);
        }

        private static Object unsignedInt64At(final SqueakImageContext image, final NativeObject byteArray, final long byteOffset, final InlinedConditionProfile positiveProfile, final Node node) {
            final long signedLong = PrimSignedInt64AtNode.signedInt64At(byteArray, byteOffset);
            if (positiveProfile.profile(node, signedLong >= 0)) {
                return signedLong;
            } else {
                return LargeIntegers.toUnsigned(image, signedLong);
            }
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt64AtPut")
    protected abstract static class PrimUnsignedInt64AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)", "value >= 0"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffsetLong, final long value) {
            return PrimSignedInt64AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "inByteBounds(byteOffsetLong, byteArray.getByteLength(), 8)", "image.isLargeInteger(value)", "inUnsignedBounds(image, value)"})
        protected static final NativeObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value,
                        @Bind final SqueakImageContext image) {
            atPutNativeLarge(byteArray, byteOffsetLong, value);
            return value;
        }
    }

    @ExplodeLoop
    private static void atPutNativeLarge(final NativeObject byteArray, final long byteOffsetLong, final NativeObject value) {
        final int byteOffset = (int) byteOffsetLong - 1;
        final byte[] targetBytes = byteArray.getByteStorage();
        final byte[] sourceBytes = value.getByteStorage();
        final int numSourceBytes = sourceBytes.length;
        for (int i = 0; i < 8; i++) {
            targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakFFIPrimsFactory.getFactories();
    }
}

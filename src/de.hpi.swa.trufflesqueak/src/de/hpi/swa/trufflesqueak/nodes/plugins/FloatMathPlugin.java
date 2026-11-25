/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

/**
 * The FloatMathPlugin provides bit-identical floating-point results across platforms. The OpenSmalltalk
 * VM implements it with fdlibm, and so does {@link StrictMath}. The inverse hyperbolic functions are
 * not (yet) available in {@link StrictMath} and are therefore ported from fdlibm 5.3. As in the
 * OpenSmalltalk VM, primitives fail if the result is NaN.
 */
public final class FloatMathPlugin extends AbstractPrimitiveFactoryHolder {
    private static final double LN2 = 6.93147180559945286227e-01; /* 0x3FE62E42, 0xFEFA39EF */
    private static final double HUGE = 1.0e300;

    private static Object wrap(final double result) {
        if (Double.isFinite(result)) {
            return result;
        } else if (Double.isNaN(result)) {
            throw PrimitiveFailed.GENERIC_ERROR;
        } else {
            return FloatObject.valueOf(result);
        }
    }

    private static int highWord(final double x) {
        return (int) (Double.doubleToRawLongBits(x) >>> 32);
    }

    /* fdlibm: e_acosh.c */
    private static double acosh(final double x) {
        final int hx = highWord(x);
        if (hx < 0x3ff00000) { /* x < 1 */
            return Double.NaN;
        } else if (hx >= 0x41b00000) { /* x > 2**28 */
            if (hx >= 0x7ff00000) { /* x is inf or NaN */
                return x + x;
            } else {
                return StrictMath.log(x) + LN2; /* acosh(huge) = log(2x) */
            }
        } else if (x == 1.0) {
            return 0.0; /* acosh(1) = 0 */
        } else if (hx > 0x40000000) { /* 2**28 > x > 2 */
            final double t = x * x;
            return StrictMath.log(2.0 * x - 1.0 / (x + StrictMath.sqrt(t - 1.0)));
        } else { /* 1 < x < 2 */
            final double t = x - 1.0;
            return StrictMath.log1p(t + StrictMath.sqrt(2.0 * t + t * t));
        }
    }

    /* fdlibm: s_asinh.c */
    private static double asinh(final double x) {
        final int hx = highWord(x);
        final int ix = hx & 0x7fffffff;
        if (ix >= 0x7ff00000) { /* x is inf or NaN */
            return x + x;
        }
        if (ix < 0x3e300000) { /* |x| < 2**-28 */
            if (HUGE + x > 1.0) {
                return x; /* return x inexact except 0 */
            }
        }
        final double w;
        if (ix > 0x41b00000) { /* |x| > 2**28 */
            w = StrictMath.log(Math.abs(x)) + LN2;
        } else if (ix > 0x40000000) { /* 2**28 > |x| > 2.0 */
            final double t = Math.abs(x);
            w = StrictMath.log(2.0 * t + 1.0 / (StrictMath.sqrt(x * x + 1.0) + t));
        } else { /* 2.0 > |x| > 2**-28 */
            final double t = x * x;
            w = StrictMath.log1p(Math.abs(x) + t / (1.0 + StrictMath.sqrt(1.0 + t)));
        }
        return hx > 0 ? w : -w;
    }

    /* fdlibm: e_atanh.c */
    private static double atanh(final double x) {
        final int hx = highWord(x);
        final int ix = hx & 0x7fffffff;
        final double absX = Math.abs(x);
        if (absX > 1.0 || Double.isNaN(x)) { /* |x| > 1 */
            return Double.NaN;
        }
        if (ix == 0x3ff00000) {
            return x / 0.0;
        }
        if (ix < 0x3e300000 && HUGE + x > 0.0) {
            return x; /* x < 2**-28 */
        }
        double t;
        if (ix < 0x3fe00000) { /* x < 0.5 */
            t = absX + absX;
            t = 0.5 * StrictMath.log1p(t + t * absX / (1.0 - absX));
        } else {
            t = 0.5 * StrictMath.log1p((absX + absX) / (1.0 - absX));
        }
        return hx >= 0 ? t : -t;
    }

    /* fdlibm: s_modf.c (fractional part only) */
    private static double fractionalPart(final double x) {
        if (Double.isInfinite(x)) {
            return Math.copySign(0.0, x);
        } else {
            final double integralPart = x < 0 ? Math.ceil(x) : Math.floor(x);
            return Math.copySign(x - integralPart, x);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcCos")
    protected abstract static class PrimArcCosNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.acos(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcCosH")
    protected abstract static class PrimArcCosHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(acosh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcSin")
    protected abstract static class PrimArcSinNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.asin(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcSinH")
    protected abstract static class PrimArcSinHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(asinh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcTan")
    protected abstract static class PrimArcTanNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.atan(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcTanH")
    protected abstract static class PrimArcTanHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(atanh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCos")
    protected abstract static class PrimCosNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.cos(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCosH")
    protected abstract static class PrimCosHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.cosh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExp")
    protected abstract static class PrimExpNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.exp(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFractionalPart")
    protected abstract static class PrimFractionalPartNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(fractionalPart(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLog10")
    protected abstract static class PrimLog10Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.log10(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLogN")
    protected abstract static class PrimLogNNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.log(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSin")
    protected abstract static class PrimSinNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.sin(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSinH")
    protected abstract static class PrimSinHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.sinh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSqrt")
    protected abstract static class PrimSqrtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.sqrt(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTan")
    protected abstract static class PrimTanNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.tan(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTanH")
    protected abstract static class PrimTanHNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value) {
            return wrap(StrictMath.tanh(value));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value) {
            return doDouble(receiver, value.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArcTan2")
    protected abstract static class PrimArcTan2Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double a, final double b) {
            return wrap(StrictMath.atan2(a, b));
        }

        @Specialization
        protected static final Object doFloatDouble(final Object receiver, final FloatObject a, final double b) {
            return doDouble(receiver, a.getValue(), b);
        }

        @Specialization
        protected static final Object doDoubleFloat(final Object receiver, final double a, final FloatObject b) {
            return doDouble(receiver, a, b.getValue());
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject a, final FloatObject b) {
            return doDouble(receiver, a.getValue(), b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHypot")
    protected abstract static class PrimHypotNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double a, final double b) {
            return wrap(StrictMath.hypot(a, b));
        }

        @Specialization
        protected static final Object doFloatDouble(final Object receiver, final FloatObject a, final double b) {
            return doDouble(receiver, a.getValue(), b);
        }

        @Specialization
        protected static final Object doDoubleFloat(final Object receiver, final double a, final FloatObject b) {
            return doDouble(receiver, a, b.getValue());
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject a, final FloatObject b) {
            return doDouble(receiver, a.getValue(), b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRaisedToPower")
    protected abstract static class PrimRaisedToPowerNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double a, final double b) {
            return wrap(StrictMath.pow(a, b));
        }

        @Specialization
        protected static final Object doFloatDouble(final Object receiver, final FloatObject a, final double b) {
            return doDouble(receiver, a.getValue(), b);
        }

        @Specialization
        protected static final Object doDoubleFloat(final Object receiver, final double a, final FloatObject b) {
            return doDouble(receiver, a, b.getValue());
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject a, final FloatObject b) {
            return doDouble(receiver, a.getValue(), b.getValue());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTimesTwoPower")
    protected abstract static class PrimTimesTwoPowerNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final Object doDouble(@SuppressWarnings("unused") final Object receiver, final double value, final long exponent) {
            return wrap(Math.scalb(value, (int) MiscUtils.clamp(exponent, Integer.MIN_VALUE, Integer.MAX_VALUE)));
        }

        @Specialization
        protected static final Object doFloat(final Object receiver, final FloatObject value, final long exponent) {
            return doDouble(receiver, value.getValue(), exponent);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatMathPluginFactory.getFactories();
    }
}

package de.hpi.swa.graal.squeak.nodes;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class SqueakGuards {

    public static boolean between(final long value, final int minIncluded, final int maxIncluded) {
        return minIncluded <= value && value <= maxIncluded;
    }

    public static boolean fitsIntoByte(final long value) {
        return Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE;
    }

    public static boolean fitsIntoInt(final long value) {
        return Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE;
    }

    public static boolean inBounds0(final long index, final int size) {
        return 0 <= index && index < size;
    }

    public static boolean inBounds1(final long index, final int size) {
        return 0 < index && index <= size;
    }

    public static boolean isAbstractSqueakObject(final Object object) {
        return object instanceof AbstractSqueakObject;
    }

    public static boolean isArrayObject(final Object object) {
        return object instanceof ArrayObject;
    }

    public static boolean isBlockClosureObject(final Object object) {
        return object instanceof BlockClosureObject;
    }

    public static boolean isBoolean(final Object value) {
        return value instanceof Boolean;
    }

    public static boolean isCharacter(final Object object) {
        return object instanceof Character;
    }

    public static boolean isClassObject(final Object object) {
        return object instanceof ClassObject;
    }

    public static boolean isCompiledMethodObject(final Object object) {
        return object instanceof CompiledMethodObject;
    }

    public static boolean isContextObject(final Object object) {
        return object instanceof ContextObject;
    }

    public static boolean isDouble(final Object value) {
        return value instanceof Double;
    }

    public static boolean isEmptyObject(final Object object) {
        return object instanceof EmptyObject;
    }

    public static boolean isFloatObject(final Object object) {
        return object instanceof FloatObject;
    }

    public static boolean isFrameMarker(final Object object) {
        return object instanceof FrameMarker;
    }

    public static boolean isIntegralWhenDividedBy(final long a, final long b) {
        return a % b == 0;
    }

    public static boolean isLargeIntegerObject(final Object object) {
        return object instanceof LargeIntegerObject;
    }

    public static boolean isLong(final Object value) {
        return value instanceof Long;
    }

    public static boolean isMinValueDividedByMinusOne(final long a, final long b) {
        return a == Long.MIN_VALUE && b == -1;
    }

    public static boolean isNativeObject(final Object object) {
        return object instanceof NativeObject;
    }

    public static boolean isNilObject(final Object object) {
        return object instanceof NilObject;
    }

    public static boolean isNotProvided(final Object obj) {
        return NotProvided.isInstance(obj);
    }

    public static boolean isPointersObject(final Object obj) {
        return obj instanceof PointersObject;
    }

    public static boolean isSmallInteger(final SqueakImageContext image, final long value) {
        if (image.flags.is64bit()) {
            return isSmallInteger64bit(value);
        } else {
            return isSmallInteger32bit(value);
        }
    }

    public static boolean isSmallInteger32bit(final long value) {
        return LargeIntegerObject.SMALLINTEGER32_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER32_MAX;
    }

    public static boolean isSmallInteger64bit(final long value) {
        return LargeIntegerObject.SMALLINTEGER64_MIN <= value && value <= LargeIntegerObject.SMALLINTEGER64_MAX;
    }

    public static boolean isZero(final double value) {
        return value == 0;
    }

    private SqueakGuards() {
    }
}

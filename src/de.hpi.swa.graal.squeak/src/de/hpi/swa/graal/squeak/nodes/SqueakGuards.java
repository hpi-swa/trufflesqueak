/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.TruffleOptions;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.util.NotProvided;
import de.hpi.swa.graal.squeak.util.OSDetector;

public final class SqueakGuards {

    private SqueakGuards() {
    }

    public static boolean between(final long value, final int minIncluded, final int maxIncluded) {
        return minIncluded <= value && value <= maxIncluded;
    }

    public static boolean fitsIntoByte(final long value) {
        return Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE;
    }

    public static boolean fitsIntoInt(final long value) {
        return Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE;
    }

    public static boolean inBounds1(final long index, final int size) {
        return 0 < index && index <= size;
    }

    public static boolean inBounds1(final long index, final int size, final int scale) {
        return 0 < index && (index - 1) / scale < size;
    }

    public static boolean isAbstractPointersObject(final Object object) {
        return object instanceof AbstractPointersObject;
    }

    public static boolean isAbstractSqueakObject(final Object object) {
        return object instanceof AbstractSqueakObject;
    }

    public static boolean isAOT() {
        return TruffleOptions.AOT;
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

    public static boolean isCharacterObject(final Object object) {
        return object instanceof CharacterObject;
    }

    public static boolean isClassObject(final Object object) {
        return object instanceof ClassObject;
    }

    public static boolean isCompiledBlockObject(final Object object) {
        return object instanceof CompiledBlockObject;
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

    public static boolean isInfinite(final double value) {
        return Double.isInfinite(value);
    }

    public static boolean isIntegralWhenDividedBy(final long a, final long b) {
        return a % b == 0;
    }

    public static boolean isLargeIntegerObject(final Object object) {
        return object instanceof LargeIntegerObject;
    }

    public static boolean isLinux() {
        return OSDetector.SINGLETON.isLinux();
    }

    public static boolean isLong(final Object value) {
        return value instanceof Long;
    }

    public static boolean isLongMinValue(final long value) {
        return value == Long.MIN_VALUE;
    }

    public static boolean isMacOS() {
        return OSDetector.SINGLETON.isMacOS();
    }

    public static boolean isNativeObject(final Object object) {
        return object instanceof NativeObject;
    }

    public static boolean isNil(final Object object) {
        return object == NilObject.SINGLETON;
    }

    public static boolean isNotProvided(final Object obj) {
        return obj == NotProvided.SINGLETON;
    }

    public static boolean isOverflowDivision(final long a, final long b) {
        return a == Long.MIN_VALUE && b == -1;
    }

    public static boolean isPointersObject(final Object object) {
        return object instanceof PointersObject;
    }

    public static boolean isPowerOfTwo(final long value) {
        return value > 1 && (value & value - 1) == 0;
    }

    public static boolean isUsedJavaPrimitive(final Object value) {
        final Class<? extends Object> clazz = value.getClass();
        return clazz == Boolean.class || clazz == Long.class || clazz == Double.class || clazz == Character.class;
    }

    public static boolean isZero(final double value) {
        return value == 0;
    }

    public static boolean isZeroOrGreater(final double value) {
        return value >= 0;
    }

    public static long to0(final long value) {
        return value - 1;
    }
}

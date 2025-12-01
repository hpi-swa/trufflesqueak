/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.util.OS;

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

    public static boolean isCompiledCodeObject(final Object object) {
        return object instanceof CompiledCodeObject;
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

    public static boolean isForeignObject(final Object object) {
        return !(isAbstractSqueakObject(object) || isUsedJavaPrimitive(object));
    }

    public static boolean isFraction(final PointersObject object, final Node node) {
        CompilerAsserts.compilationConstant(node);
        return object.getSqueakClass() == SqueakImageContext.get(node).getFractionClass();
    }

    public static boolean isGreaterThanZero(final double value) {
        return value > 0;
    }

    public static boolean isInfinite(final double value) {
        return Double.isInfinite(value);
    }

    public static boolean isIntegralWhenDividedBy(final long a, final long b) {
        return a % b == 0;
    }

    public static boolean isLargeIntegerObject(final SqueakImageContext image, final Object object) {
        return object instanceof final NativeObject o && image.isLargeInteger(o);
    }

    public static boolean isLessThanZero(final double value) {
        return value < 0;
    }

    @Idempotent
    public static boolean isLinux() {
        return OS.isLinux();
    }

    public static boolean isLong(final Object value) {
        return value instanceof Long;
    }

    public static boolean isLongMinValue(final long value) {
        return value == Long.MIN_VALUE;
    }

    public static boolean isLShiftLongOverflow(final long receiver, final long arg) {
        return Long.numberOfLeadingZeros(receiver) - 1 < arg;
    }

    @Idempotent
    public static boolean isMacOS() {
        return OS.isMacOS();
    }

    public static boolean isNativeObject(final Object object) {
        return object instanceof NativeObject;
    }

    public static boolean isNil(final Object object) {
        return object == NilObject.SINGLETON;
    }

    public static boolean isOverflowDivision(final long a, final long b) {
        return a == Long.MIN_VALUE && b == -1;
    }

    public static boolean isPointersObject(final Object object) {
        return object instanceof PointersObject;
    }

    public static boolean isUsedJavaPrimitive(final Object value) {
        return value instanceof Boolean || value instanceof Long || value instanceof Double || value instanceof Character;
    }

    public static boolean isZero(final double value) {
        return value == 0;
    }

    public static boolean isZeroOrGreater(final double value) {
        return value >= 0;
    }

    public static long minus2(final long value) {
        return value - 2;
    }

    public static long to0(final long value) {
        return value - 1;
    }
}

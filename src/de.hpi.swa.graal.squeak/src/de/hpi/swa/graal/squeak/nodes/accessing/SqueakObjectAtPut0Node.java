package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;

@ImportStatic({NativeObject.class, SqueakGuards.class})
public abstract class SqueakObjectAtPut0Node extends Node {

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(Object obj, long index, Object value);

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()"})
    protected static final void doEmptyArray(final ArrayObject obj, final long index, final NilObject value) {
        // Nothing to do
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()"})
    protected static final void doEmptyArray(final ArrayObject obj, final long index, final AbstractSqueakObject value) {
        obj.transitionFromEmptyToAbstractSqueakObjects();
        doArrayOfSqueakObjects(obj, index, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()"})
    protected static final void doEmptyArrayToLong(final ArrayObject obj, final long index, final long value) {
        obj.transitionFromEmptyToLongs();
        doArrayOfLongs(obj, index, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()"})
    protected static final void doEmptyArrayToDouble(final ArrayObject obj, final long index, final double value) {
        obj.transitionFromEmptyToDoubles();
        doArrayOfDoubles(obj, index, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()", "!isAbstractSqueakObject(value)", "!isLong(value)", "!isDouble(value)"})
    protected static final void doEmptyArrayToObject(final ArrayObject obj, final long index, final Object value) {
        obj.transitionFromEmptyToObjects();
        doArrayOfObjects(obj, index, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index >= obj.getEmptyStorage()"})
    protected static final void doEmptyArrayOutOfBounds(final ArrayObject obj, final long index, final Object value) {
        throw new IndexOutOfBoundsException();
    }

    @Specialization(guards = "obj.isAbstractSqueakObjectType()")
    protected static final void doArrayOfSqueakObjects(final ArrayObject obj, final long index, final AbstractSqueakObject value) {
        obj.atput0SqueakObject(index, value);
    }

    @Specialization(guards = {"obj.isAbstractSqueakObjectType()", "!isAbstractSqueakObject(value)"})
    protected static final void doArrayOfSqueakObjects(final ArrayObject obj, final long index, final Object value) {
        obj.transitionFromAbstractSqueakObjectsToObjects();
        doArrayOfObjects(obj, index, value);
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final void doArrayOfLongs(final ArrayObject obj, final long index, final long value) {
        obj.atput0Long(index, value);
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final void doArrayOfLongs(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
        obj.atputNil0Long(index);
    }

    @Specialization(guards = {"obj.isLongType()", "!isLong(value)", "!isNilObject(value)"})
    protected static final void doArrayOfLongs(final ArrayObject obj, final long index, final Object value) {
        obj.transitionFromLongsToObjects();
        doArrayOfObjects(obj, index, value);
    }

    @Specialization(guards = "obj.isDoubleType()")
    protected static final void doArrayOfDoubles(final ArrayObject obj, final long index, final double value) {
        obj.atput0Double(index, value);
    }

    @Specialization(guards = "obj.isDoubleType()")
    protected static final void doArrayOfDoubles(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
        obj.atputNil0Double(index);
    }

    @Specialization(guards = {"obj.isDoubleType()", "!isDouble(value)", "!isNilObject(value)"})
    protected static final void doArrayOfDoubles(final ArrayObject obj, final long index, final Object value) {
        obj.transitionFromDoublesToObjects();
        doArrayOfObjects(obj, index, value);
    }

    @Specialization(guards = "obj.isObjectType()")
    protected static final void doArrayOfObjects(final ArrayObject obj, final long index, final Object value) {
        obj.atput0Object(index, value);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClass(final ClassObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization(guards = {"obj.isByteType()", "value >= 0", "value <= BYTE_MAX"})
    protected static final void doNativeBytes(final NativeObject obj, final long index, final long value) {
        obj.getByteStorage()[(int) index] = (byte) value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isByteType()", "value < 0 || value > BYTE_MAX"})
    protected static final void doNativeBytesIllegal(final NativeObject obj, final long index, final long value) {
        throw new IllegalArgumentException("Illegal value for byte array: " + value);
    }

    @Specialization(guards = {"obj.isShortType()", "value >= 0", "value <= SHORT_MAX"})
    protected static final void doNativeShorts(final NativeObject obj, final long index, final long value) {
        obj.getShortStorage()[(int) index] = (short) value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isShortType()", "value < 0 || value > SHORT_MAX"})
    protected static final void doNativeShortsIllegal(final NativeObject obj, final long index, final long value) {
        throw new IllegalArgumentException("Illegal value for short array: " + value);
    }

    @Specialization(guards = {"obj.isIntType()", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doNativeInts(final NativeObject obj, final long index, final long value) {
        obj.getIntStorage()[(int) index] = (int) value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isIntType()", "value < 0 || value > INTEGER_MAX"})
    protected static final void doNativeIntsIllegal(final NativeObject obj, final long index, final long value) {
        throw new IllegalArgumentException("Illegal value for int array: " + value);
    }

    @Specialization(guards = {"obj.isLongType()", "value >= 0"})
    protected static final void doNativeLongs(final NativeObject obj, final long index, final long value) {
        obj.getLongStorage()[(int) index] = value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isLongType()", "value < 0"})
    protected static final void doNativeLongsIllegal(final NativeObject obj, final long index, final long value) {
        throw new IllegalArgumentException("Illegal value for long array: " + value);
    }

    protected static final boolean inByteRange(final char value) {
        return value <= NativeObject.BYTE_MAX;
    }

    @Specialization(guards = {"obj.isByteType()", "inByteRange(value)"})
    protected static final void doNativeBytesChar(final NativeObject obj, final long index, final char value) {
        doNativeBytes(obj, index, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isByteType()", "!inByteRange(value)"})
    protected static final void doNativeBytesCharIllegal(final NativeObject obj, final long index, final char value) {
        throw new IllegalArgumentException("Illegal value for byte array: " + value);
    }

    @Specialization(guards = "obj.isShortType()") // char values fit into short
    protected static final void doNativeShortsChar(final NativeObject obj, final long index, final char value) {
        doNativeShorts(obj, index, value);
    }

    @Specialization(guards = "obj.isIntType()")
    protected static final void doNativeIntsChar(final NativeObject obj, final long index, final char value) {
        doNativeInts(obj, index, value);
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final void doNativeLongsChar(final NativeObject obj, final long index, final char value) {
        doNativeLongs(obj, index, value);
    }

    @Specialization(guards = {"obj.isByteType()", "value.inRange(0, BYTE_MAX)"})
    protected static final void doNativeBytesLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeBytes(obj, index, value.longValueExact());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isByteType()", "!value.inRange(0, BYTE_MAX)"})
    protected static final void doNativeBytesLargeIntegerIllegal(final NativeObject obj, final long index, final LargeIntegerObject value) {
        throw new IllegalArgumentException("Illegal value for byte array: " + value);
    }

    @Specialization(guards = {"obj.isShortType()", "value.inRange(0, SHORT_MAX)"})
    protected static final void doNativeShortsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeShorts(obj, index, value.longValueExact());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isShortType()", "!value.inRange(0, SHORT_MAX)"})
    protected static final void doNativeShortsLargeIntegerIllegal(final NativeObject obj, final long index, final LargeIntegerObject value) {
        throw new IllegalArgumentException("Illegal value for short array: " + value);
    }

    @Specialization(guards = {"obj.isIntType()", "value.inRange(0, INTEGER_MAX)"})
    protected static final void doNativeIntsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeInts(obj, index, value.longValueExact());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isIntType()", "!value.inRange(0, INTEGER_MAX)"})
    protected static final void doNativeIntsLargeIntegerIllegal(final NativeObject obj, final long index, final LargeIntegerObject value) {
        throw new IllegalArgumentException("Illegal value for int array: " + value);
    }

    @Specialization(guards = {"obj.isLongType()", "value.isZeroOrPositive()"})
    protected static final void doNativeLongsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeLongs(obj, index, value.longValueExact());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isLongType()", "!value.isZeroOrPositive()"})
    protected static final void doNativeLongsLargeIntegerIllegal(final NativeObject obj, final long index, final LargeIntegerObject value) {
        throw new IllegalArgumentException("Illegal value for long array: " + value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.longValueExact());
    }

    @Specialization(guards = {"index == 0 || index == 1", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doFloat(final FloatObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization(guards = {"index == 0 || index == 1", "!value.inRange(0, INTEGER_MAX)"})
    protected static final void doFloat(final FloatObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.longValueExact());
    }

    @Specialization
    protected static final void doCode(final CompiledCodeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClosure(final BlockClosureObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doEmpty(final EmptyObject obj, final long index, final Object value) {
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doNil(final NilObject obj, final long index, final Object value) {
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFallback(final Object obj, final long index, final Object value) {
        throw new SqueakException("Object does not support atput0:", obj);
    }
}

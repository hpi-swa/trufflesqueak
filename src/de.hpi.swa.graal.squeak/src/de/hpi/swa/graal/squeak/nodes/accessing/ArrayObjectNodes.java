package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectSizeNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodesFactory.GetObjectArrayNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodesFactory.ReadArrayObjectNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodesFactory.WriteArrayObjectNodeGen;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class ArrayObjectNodes {

    public abstract static class ReadArrayObjectNode extends Node {

        public static ReadArrayObjectNode create() {
            return ReadArrayObjectNodeGen.create();
        }

        public abstract Object execute(ArrayObject obj, long index);

        @Specialization(guards = {"obj.isEmptyType()", "index >= 0", "index < obj.getEmptyStorage()"})
        protected static final NilObject doEmptyArray(final ArrayObject obj, @SuppressWarnings("unused") final long index) {
            return obj.getNil();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"obj.isEmptyType()", "index < 0 || index >= obj.getEmptyStorage()"})
        protected static final long doEmptyArrayOutOfBounds(final ArrayObject obj, final long index) {
            throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
        }

        @Specialization(guards = "obj.isAbstractSqueakObjectType()")
        protected static final AbstractSqueakObject doArrayOfSqueakObjects(final ArrayObject obj, final long index) {
            return obj.at0SqueakObject(index);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final Object doArrayOfBooleans(final ArrayObject obj, final long index) {
            return obj.at0Boolean(index);
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final Object doArrayOfChars(final ArrayObject obj, final long index) {
            return obj.at0Char(index);
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object doArrayOfLongs(final ArrayObject obj, final long index) {
            return obj.at0Long(index);
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object doArrayOfDoubles(final ArrayObject obj, final long index) {
            return obj.at0Double(index);
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final Object doArrayOfObjects(final ArrayObject obj, final long index) {
            return obj.at0Object(index);
        }

        @Fallback
        protected static final Object doFail(final ArrayObject obj, final long index) {
            throw new SqueakException("Unexpected values:", obj, index);
        }
    }

    @ImportStatic({ArrayObject.class, SqueakGuards.class})
    public abstract static class WriteArrayObjectNode extends Node {

        public static WriteArrayObjectNode create() {
            return WriteArrayObjectNodeGen.create();
        }

        public abstract void execute(ArrayObject obj, long index, Object value);

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
        protected static final void doEmptyArrayToBoolean(final ArrayObject obj, final long index, final boolean value) {
            obj.transitionFromEmptyToBooleans();
            doArrayOfBooleans(obj, index, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"obj.isEmptyType()", "index < obj.getEmptyStorage()"})
        protected static final void doEmptyArrayToChar(final ArrayObject obj, final long index, final char value) {
            obj.transitionFromEmptyToChars();
            doArrayOfChars(obj, index, value);
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
            throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
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

        @Specialization(guards = "obj.isBooleanType()")
        protected static final void doArrayOfBooleans(final ArrayObject obj, final long index, final boolean value) {
            obj.atput0Boolean(index, value);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final void doArrayOfBooleans(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.atputNil0Boolean(index);
        }

        @Specialization(guards = {"obj.isBooleanType()", "!isBoolean(value)", "!isNilObject(value)"})
        protected static final void doArrayOfBooleans(final ArrayObject obj, final long index, final Object value) {
            obj.transitionFromBooleansToObjects();
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final void doArrayOfChars(final ArrayObject obj, final long index, final char value) {
            obj.atput0Char(index, value);
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final void doArrayOfChars(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.atputNil0Char(index);
        }

        @Specialization(guards = {"obj.isCharType()", "!isCharacter(value)", "!isNilObject(value)"})
        protected static final void doArrayOfChars(final ArrayObject obj, final long index, final Object value) {
            obj.transitionFromCharsToObjects();
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = {"obj.isLongType()", "!isLongNilTag(value)"})
        protected static final void doArrayOfLongs(final ArrayObject obj, final long index, final long value) {
            obj.atput0Long(index, value);
        }

        @Specialization(guards = {"obj.isLongType()", "isLongNilTag(value)"})
        protected static final void doArrayOfLongsNilTagClash(final ArrayObject obj, final long index, final long value) {
            // `value` happens to be long nil tag, need to despecialize to be able store it.
            obj.transitionFromLongsToObjects();
            obj.atput0Object(index, value);
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

        @Specialization(guards = {"obj.isDoubleType()", "!isDoubleNilTag(value)"})
        protected static final void doArrayOfDoubles(final ArrayObject obj, final long index, final double value) {
            obj.atput0Double(index, value);
        }

        @Specialization(guards = {"obj.isDoubleType()", "isDoubleNilTag(value)"})
        protected static final void doArrayOfDoublesNilTagClash(final ArrayObject obj, final long index, final double value) {
            // `value` happens to be double nil tag, need to despecialize to be able store it.
            obj.transitionFromLongsToObjects();
            obj.atput0Object(index, value);
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

        @Fallback
        protected static final void doFail(final ArrayObject obj, final long index, final Object value) {
            throw new SqueakException("Unexpected values:", obj, index, value);
        }
    }

    public abstract static class ArrayObjectSizeNode extends Node {

        public static ArrayObjectSizeNode create() {
            return ArrayObjectSizeNodeGen.create();
        }

        public abstract int execute(ArrayObject obj);

        @Specialization(guards = "obj.isEmptyType()")
        protected static final int doEmptyArrayObject(final ArrayObject obj) {
            return obj.getEmptyStorage();
        }

        @Specialization(guards = "obj.isAbstractSqueakObjectType()")
        protected static final int doArrayObjectOfSqueakObjects(final ArrayObject obj) {
            return obj.getAbstractSqueakObjectLength();
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final int doArrayObjectOfBooleans(final ArrayObject obj) {
            return obj.getBooleanLength();
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final int doArrayObjectOfChars(final ArrayObject obj) {
            return obj.getCharLength();
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final int doArrayObjectOfLongs(final ArrayObject obj) {
            return obj.getLongLength();
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final int doArrayObjectOfDoubles(final ArrayObject obj) {
            return obj.getDoubleLength();
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final int doArrayObjectOfObjects(final ArrayObject obj) {
            return obj.getObjectLength();
        }

        @Fallback
        protected static final int doFail(final ArrayObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }

    public abstract static class GetObjectArrayNode extends Node {

        public static GetObjectArrayNode create() {
            return GetObjectArrayNodeGen.create();
        }

        public abstract Object[] execute(ArrayObject obj);

        @Specialization(guards = "obj.isEmptyType()")
        protected static final Object[] doEmptyArray(final ArrayObject obj) {
            return ArrayUtils.withAll(obj.getEmptyStorage(), obj.getNil());
        }

        @Specialization(guards = "obj.isAbstractSqueakObjectType()")
        protected static final Object[] doArrayOfSqueakObjects(final ArrayObject obj) {
            return obj.getAbstractSqueakObjectStorage();
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final Object[] doArrayOfBooleans(final ArrayObject obj) {
            final byte[] booleans = obj.getBooleanStorage();
            final int length = booleans.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                final byte value = booleans[i];
                if (value == ArrayObject.BOOLEAN_FALSE_TAG) {
                    objects[i] = obj.image.sqFalse;
                } else if (value == ArrayObject.BOOLEAN_TRUE_TAG) {
                    objects[i] = obj.image.sqTrue;
                } else {
                    assert value == ArrayObject.BOOLEAN_NIL_TAG;
                    objects[i] = obj.image.nil;
                }
            }
            return objects;
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final Object[] doArrayOfChars(final ArrayObject obj) {
            final char[] chars = obj.getCharStorage();
            final int length = chars.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                final long value = chars[i];
                if (value == ArrayObject.CHAR_NIL_TAG) {
                    objects[i] = obj.image.nil;
                } else {
                    objects[i] = value;
                }
            }
            return objects;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object[] doArrayOfLongs(final ArrayObject obj) {
            final long[] longs = obj.getLongStorage();
            final int length = longs.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                final long value = longs[i];
                if (value == ArrayObject.LONG_NIL_TAG) {
                    objects[i] = obj.image.nil;
                } else {
                    objects[i] = value;
                }
            }
            return objects;
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object[] doArrayOfDoubles(final ArrayObject obj) {
            final double[] doubles = obj.getDoubleStorage();
            final int length = doubles.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                final double value = doubles[i];
                if (value == ArrayObject.DOUBLE_NIL_TAG) {
                    objects[i] = obj.image.nil;
                } else {
                    objects[i] = value;
                }
            }
            return objects;
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final Object[] doArrayOfObjects(final ArrayObject obj) {
            return obj.getObjectStorage();
        }

        @Fallback
        protected static final Object[] doFail(final ArrayObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }
}

package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodesFactory.GetObjectArrayNodeGen;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class ArrayObjectNodes {
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

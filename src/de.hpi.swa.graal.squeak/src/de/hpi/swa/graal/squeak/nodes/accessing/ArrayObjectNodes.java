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

        @Specialization(guards = "obj.isLongType()")
        protected static final Object[] doArrayOfLongs(final ArrayObject obj) {
            final long[] longs = obj.getLongStorage();
            final int length = longs.length;
            final Long[] boxedLongs = new Long[length];
            for (int i = 0; i < length; i++) {
                boxedLongs[i] = longs[i];
            }
            return boxedLongs;
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object[] doArrayOfDoubles(final ArrayObject obj) {
            final double[] doubles = obj.getDoubleStorage();
            final int length = doubles.length;
            final Double[] boxedDoubles = new Double[length];
            for (int i = 0; i < length; i++) {
                boxedDoubles[i] = doubles[i];
            }
            return boxedDoubles;
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

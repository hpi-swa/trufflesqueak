/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectCopyIntoObjectArrayNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectReadNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectToObjectArrayCopyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectWriteNodeGen;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ArrayObjectNodes {

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class ArrayObjectReadNode extends AbstractNode {

        public static ArrayObjectReadNode getUncached() {
            return ArrayObjectReadNodeGen.getUncached();
        }

        public abstract Object execute(Node node, ArrayObject obj, long index);

        public static final Object executeUncached(final ArrayObject obj, final long index) {
            return getUncached().execute(null, obj, index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final NilObject doEmptyArray(final ArrayObject obj, final long index) {
            assert 0 <= index && index < obj.getEmptyLength() : "Unexpected index: " + index;
            return NilObject.SINGLETON;
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final Object doArrayOfBooleans(final Node node, final ArrayObject obj, final long index,
                        @Exclusive @Cached final InlinedConditionProfile falseProfile,
                        @Exclusive @Cached final InlinedConditionProfile trueProfile) {
            final byte value = obj.getByte(index);
            if (falseProfile.profile(node, value == ArrayObject.BOOLEAN_FALSE_TAG)) {
                return BooleanObject.FALSE;
            } else if (trueProfile.profile(node, value == ArrayObject.BOOLEAN_TRUE_TAG)) {
                return BooleanObject.TRUE;
            } else {
                assert value == ArrayObject.BOOLEAN_NIL_TAG;
                return NilObject.SINGLETON;
            }
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final Object doArrayOfChars(final Node node, final ArrayObject obj, final long index,
                        @Shared("nilProfile") @Cached final InlinedConditionProfile nilProfile) {
            final char value = obj.getChar(index);
            return nilProfile.profile(node, value == ArrayObject.CHAR_NIL_TAG) ? NilObject.SINGLETON : value;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object doArrayOfLongs(final Node node, final ArrayObject obj, final long index,
                        @Shared("nilProfile") @Cached final InlinedConditionProfile nilProfile) {
            final long value = obj.getLong(index);
            return nilProfile.profile(node, value == ArrayObject.LONG_NIL_TAG) ? NilObject.SINGLETON : value;
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object doArrayOfDoubles(final Node node, final ArrayObject obj, final long index,
                        @Shared("nilProfile") @Cached final InlinedConditionProfile nilProfile) {
            final double value = obj.getDouble(index);
            return nilProfile.profile(node, Double.doubleToRawLongBits(value) == ArrayObject.DOUBLE_NIL_TAG_LONG) ? NilObject.SINGLETON : value;
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final Object doArrayOfObjects(final ArrayObject obj, final long index) {
            assert obj.getObject(index) != null : "Unexpected `null` value";
            return obj.getObject(index);
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class ArrayObjectShallowCopyNode extends AbstractNode {

        public abstract ArrayObject execute(Node node, ArrayObject obj);

        @Specialization(guards = "obj.isEmptyType()")
        protected static final ArrayObject doEmptyArray(final ArrayObject obj) {
            return obj.shallowCopy(obj.getEmptyStorage());
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final ArrayObject doArrayOfBooleans(final ArrayObject obj) {
            return obj.shallowCopy(obj.getBooleanStorage().clone());
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final ArrayObject doArrayOfChars(final ArrayObject obj) {
            return obj.shallowCopy(obj.getCharStorage().clone());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final ArrayObject doArrayOfLongs(final ArrayObject obj) {
            return obj.shallowCopy(obj.getLongStorage().clone());
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final ArrayObject doArrayOfDoubles(final ArrayObject obj) {
            return obj.shallowCopy(obj.getDoubleStorage().clone());
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final ArrayObject doArrayOfObjects(final ArrayObject obj) {
            return obj.shallowCopy(obj.getObjectStorage().clone());
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class ArrayObjectSizeNode extends AbstractNode {

        public abstract int execute(Node node, ArrayObject obj);

        public static final int executeUncached(final ArrayObject obj) {
            return ArrayObjectSizeNodeGen.getUncached().execute(null, obj);
        }

        @Specialization(guards = "obj.isEmptyType()")
        protected static final int doEmptyArrayObject(final ArrayObject obj) {
            return obj.getEmptyStorage();
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
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class ArrayObjectToObjectArrayCopyNode extends AbstractNode {

        public abstract Object[] execute(Node node, ArrayObject obj);

        public static final Object[] executeUncached(final ArrayObject obj) {
            return ArrayObjectToObjectArrayCopyNodeGen.getUncached().execute(null, obj);
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final Object[] doArrayOfObjects(final ArrayObject obj) {
            return obj.getObjectStorage();
        }

        @Specialization(guards = "obj.isEmptyType()")
        protected static final Object[] doEmptyArray(final ArrayObject obj) {
            return ArrayUtils.withAll(obj.getEmptyStorage(), NilObject.SINGLETON);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final Object[] doArrayOfBooleans(final Node node, final ArrayObject obj,
                        @Cached final InlinedBranchProfile isNilTagProfile) {
            final byte[] booleans = obj.getBooleanStorage();
            final int length = booleans.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                objects[i] = ArrayObject.toObjectFromBoolean(booleans[i], isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final Object[] doArrayOfChars(final Node node, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final char[] chars = obj.getCharStorage();
            final int length = chars.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                final char value = chars[i];
                objects[i] = ArrayObject.toObjectFromChar(value, isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object[] doArrayOfLongs(final Node node, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final long[] longs = obj.getLongStorage();
            final int length = longs.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                objects[i] = ArrayObject.toObjectFromLong(longs[i], isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object[] doArrayOfDoubles(final Node node, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final double[] doubles = obj.getDoubleStorage();
            final int length = doubles.length;
            final Object[] objects = new Object[length];
            for (int i = 0; i < length; i++) {
                objects[i] = ArrayObject.toObjectFromDouble(doubles[i], isNilTagProfile, node);
            }
            return objects;
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class ArrayObjectToObjectArrayWithFirstNode extends AbstractNode {

        public abstract Object[] execute(Node node, Object first, ArrayObject obj);

        @Specialization(guards = "obj.isObjectType()")
        protected static final Object[] doArrayOfObjects(final Object first, final ArrayObject obj) {
            final Object[] result = new Object[1 + obj.getObjectLength()];
            result[0] = first;
            ArrayUtils.arraycopy(obj.getObjectStorage(), 0, result, 1, obj.getObjectLength());
            return result;
        }

        @Specialization(guards = "obj.isEmptyType()")
        protected static final Object[] doEmptyArray(final Object first, final ArrayObject obj) {
            final Object[] result = ArrayUtils.withAll(1 + obj.getEmptyLength(), NilObject.SINGLETON);
            result[0] = first;
            return result;
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final Object[] doArrayOfBooleans(final Node node, final Object first, final ArrayObject obj,
                        @Cached final InlinedBranchProfile isNilTagProfile) {
            final byte[] booleans = obj.getBooleanStorage();
            final int length = booleans.length;
            final Object[] objects = new Object[1 + length];
            objects[0] = first;
            for (int i = 0; i < length; i++) {
                objects[1 + i] = ArrayObject.toObjectFromBoolean(booleans[i], isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final Object[] doArrayOfChars(final Node node, final Object first, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final char[] chars = obj.getCharStorage();
            final int length = chars.length;
            final Object[] objects = new Object[1 + length];
            objects[0] = first;
            for (int i = 0; i < length; i++) {
                final char value = chars[i];
                objects[1 + i] = ArrayObject.toObjectFromChar(value, isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object[] doArrayOfLongs(final Node node, final Object first, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final long[] longs = obj.getLongStorage();
            final int length = longs.length;
            final Object[] objects = new Object[1 + length];
            objects[0] = first;
            for (int i = 0; i < length; i++) {
                objects[1 + i] = ArrayObject.toObjectFromLong(longs[i], isNilTagProfile, node);
            }
            return objects;
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final Object[] doArrayOfDoubles(final Node node, final Object first, final ArrayObject obj,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final double[] doubles = obj.getDoubleStorage();
            final int length = doubles.length;
            final Object[] objects = new Object[1 + length];
            objects[0] = first;
            for (int i = 0; i < length; i++) {
                objects[1 + i] = ArrayObject.toObjectFromDouble(doubles[i], isNilTagProfile, node);
            }
            return objects;
        }
    }

    public abstract static class ArrayObjectCopyIntoObjectArrayNode extends AbstractNode {
        private final int offset;

        public ArrayObjectCopyIntoObjectArrayNode(final int offset) {
            this.offset = offset;
        }

        @NeverDefault
        public static ArrayObjectCopyIntoObjectArrayNode create(final int offset) {
            return ArrayObjectCopyIntoObjectArrayNodeGen.create(offset);
        }

        @NeverDefault
        public static ArrayObjectCopyIntoObjectArrayNode createForFrameArguments() {
            return create(FrameAccess.getArgumentStartIndex());
        }

        public abstract void execute(Object[] target, ArrayObject obj);

        @Specialization(guards = "obj.isObjectType()")
        protected final void doArrayOfObjects(final Object[] target, final ArrayObject obj) {
            ArrayUtils.arraycopy(obj.getObjectStorage(), 0, target, offset, obj.getObjectLength());
        }

        @Specialization(guards = "obj.isEmptyType()")
        protected final void doEmptyArray(final Object[] target, final ArrayObject obj) {
            ArrayUtils.fill(target, offset, offset + obj.getEmptyLength(), NilObject.SINGLETON);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected final void doArrayOfBoolean(final Object[] target, final ArrayObject obj,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile isNilTagProfile) {
            final byte[] booleans = obj.getBooleanStorage();
            for (int i = 0; i < booleans.length; i++) {
                target[offset + i] = ArrayObject.toObjectFromBoolean(booleans[i], isNilTagProfile, node);
            }
        }

        @Specialization(guards = "obj.isCharType()")
        protected final void doArrayOfChars(final Object[] target, final ArrayObject obj,
                        @Bind final Node node,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final char[] chars = obj.getCharStorage();
            for (int i = 0; i < chars.length; i++) {
                target[offset + i] = ArrayObject.toObjectFromChar(chars[i], isNilTagProfile, node);
            }
        }

        @Specialization(guards = "obj.isLongType()")
        protected final void doArrayOfLongs(final Object[] target, final ArrayObject obj,
                        @Bind final Node node,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final long[] longs = obj.getLongStorage();
            for (int i = 0; i < longs.length; i++) {
                target[offset + i] = ArrayObject.toObjectFromLong(longs[i], isNilTagProfile, node);
            }
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected final void doArrayOfDoubles(final Object[] target, final ArrayObject obj,
                        @Bind final Node node,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            final double[] doubles = obj.getDoubleStorage();
            for (int i = 0; i < doubles.length; i++) {
                target[offset + i] = ArrayObject.toObjectFromDouble(doubles[i], isNilTagProfile, node);
            }
        }
    }

    @GenerateInline
    @GenerateUncached
    @ImportStatic(ArrayObject.class)
    public abstract static class ArrayObjectWriteNode extends AbstractNode {

        @NeverDefault
        public static ArrayObjectWriteNode create() {
            return ArrayObjectWriteNodeGen.create();
        }

        public abstract void execute(Node node, ArrayObject obj, long index, Object value);

        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final void doEmptyArray(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            assert index < obj.getEmptyLength();
            // Nothing to do.
        }

        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final void doEmptyArrayToBoolean(final ArrayObject obj, final long index, final boolean value) {
            obj.transitionFromEmptyToBooleans();
            doArrayOfBooleans(obj, index, value);
        }

        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final void doEmptyArrayToChar(final Node node, final ArrayObject obj, final long index, final char value,
                        @Shared("nilTagProfile") @Cached final InlinedBranchProfile nilTagProfile) {
            if (ArrayObject.isCharNilTag(value)) {
                nilTagProfile.enter(node);
                doEmptyArrayToObject(obj, index, value);
            } else {
                obj.transitionFromEmptyToChars();
                doArrayOfChars(obj, index, value);
            }
        }

        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final void doEmptyArrayToLong(final Node node, final ArrayObject obj, final long index, final long value,
                        @Shared("nilTagProfile") @Cached final InlinedBranchProfile nilTagProfile) {
            if (ArrayObject.isLongNilTag(value)) {
                nilTagProfile.enter(node);
                doEmptyArrayToObject(obj, index, value);
            } else {
                obj.transitionFromEmptyToLongs();
                doArrayOfLongs(obj, index, value);
            }
        }

        @Specialization(guards = {"obj.isEmptyType()"})
        protected static final void doEmptyArrayToDouble(final Node node, final ArrayObject obj, final long index, final double value,
                        @Shared("isNilTagBranchProfile") @Cached final InlinedBranchProfile isNilTagBranchProfile) {
            if (ArrayObject.isDoubleNilTag(value)) {
                isNilTagBranchProfile.enter(node);
                doEmptyArrayToObject(obj, index, value);
            } else {
                obj.transitionFromEmptyToDoubles();
                doArrayOfDoubles(obj, index, value);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"obj.isEmptyType()"}, replaces = {"doEmptyArrayToBoolean", "doEmptyArrayToChar", "doEmptyArrayToLong", "doEmptyArrayToDouble"})
        protected static final void doEmptyArrayToObject(final ArrayObject obj, final long index, final Object value) {
            obj.transitionFromEmptyToObjects();
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final void doArrayOfBooleans(final ArrayObject obj, final long index, final boolean value) {
            obj.setByte(index, value ? ArrayObject.BOOLEAN_TRUE_TAG : ArrayObject.BOOLEAN_FALSE_TAG);
        }

        @Specialization(guards = "obj.isBooleanType()")
        protected static final void doArrayOfBooleansNil(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.setByte(index, ArrayObject.BOOLEAN_NIL_TAG);
        }

        @Specialization(guards = {"obj.isBooleanType()"}, replaces = {"doArrayOfBooleans", "doArrayOfBooleansNil"})
        protected static final void doArrayOfBooleansGeneric(final Node node, final ArrayObject obj, final long index, final Object value,
                        @Shared("isNilTagBranchProfile") @Cached final InlinedBranchProfile isNilTagBranchProfile) {
            obj.transitionFromBooleansToObjects(isNilTagBranchProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = {"obj.isCharType()", "!isCharNilTag(value)"})
        protected static final void doArrayOfChars(final ArrayObject obj, final long index, final char value) {
            obj.setChar(index, value);
        }

        @Specialization(guards = {"obj.isCharType()", "isCharNilTag(value)"})
        protected static final void doArrayOfCharsNilTagClash(final Node node, final ArrayObject obj, final long index, final char value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            /** `value` happens to be char nil tag, need to despecialize to be able to store it. */
            obj.transitionFromCharsToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isCharType()")
        protected static final void doArrayOfCharsNil(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.setChar(index, ArrayObject.CHAR_NIL_TAG);
        }

        @Specialization(guards = {"obj.isCharType()"}, replaces = {"doArrayOfChars", "doArrayOfCharsNilTagClash", "doArrayOfCharsNil"})
        protected static final void doArrayOfCharsGeneric(final Node node, final ArrayObject obj, final long index, final Object value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            obj.transitionFromCharsToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = {"obj.isLongType()", "!isLongNilTag(value)"})
        protected static final void doArrayOfLongs(final ArrayObject obj, final long index, final long value) {
            obj.setLong(index, value);
        }

        @Specialization(guards = {"obj.isLongType()", "isLongNilTag(value)"})
        protected static final void doArrayOfLongsNilTagClash(final Node node, final ArrayObject obj, final long index, final long value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            /** `value` happens to be long nil tag, need to despecialize to be able to store it. */
            obj.transitionFromLongsToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final void doArrayOfLongsNil(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.setLong(index, ArrayObject.LONG_NIL_TAG);
        }

        @Specialization(guards = {"obj.isLongType()"}, replaces = {"doArrayOfLongs", "doArrayOfLongsNilTagClash", "doArrayOfLongsNil"})
        protected static final void doArrayOfLongsGeneric(final Node node, final ArrayObject obj, final long index, final Object value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            obj.transitionFromLongsToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = {"obj.isDoubleType()", "!isDoubleNilTag(value)"})
        protected static final void doArrayOfDoubles(final ArrayObject obj, final long index, final double value) {
            obj.setDouble(index, value);
        }

        @Specialization(guards = {"obj.isDoubleType()", "isDoubleNilTag(value)"})
        protected static final void doArrayOfDoublesNilTagClash(final Node node, final ArrayObject obj, final long index, final double value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            // `value` happens to be double nil tag, need to despecialize to be able to store it.
            obj.transitionFromDoublesToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isDoubleType()")
        protected static final void doArrayOfDoublesNil(final ArrayObject obj, final long index, @SuppressWarnings("unused") final NilObject value) {
            obj.setDouble(index, ArrayObject.DOUBLE_NIL_TAG);
        }

        @Specialization(guards = {"obj.isDoubleType()"}, replaces = {"doArrayOfDoubles", "doArrayOfDoublesNilTagClash", "doArrayOfDoublesNil"})
        protected static final void doArrayOfDoublesGeneric(final Node node, final ArrayObject obj, final long index, final Object value,
                        @Shared("isNilTagProfile") @Cached final InlinedConditionProfile isNilTagProfile) {
            obj.transitionFromDoublesToObjects(isNilTagProfile, node);
            doArrayOfObjects(obj, index, value);
        }

        @Specialization(guards = "obj.isObjectType()")
        protected static final void doArrayOfObjects(final ArrayObject obj, final long index, final Object value) {
            obj.setObject(index, value);
        }
    }
}

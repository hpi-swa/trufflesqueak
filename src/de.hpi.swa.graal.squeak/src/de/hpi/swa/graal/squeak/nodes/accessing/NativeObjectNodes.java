/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeGetBytesNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeObjectReadNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeObjectSizeNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeObjectWriteNodeGen;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

public final class NativeObjectNodes {
    @GenerateUncached
    public abstract static class NativeObjectReadNode extends AbstractNode {

        public static NativeObjectReadNode create() {
            return NativeObjectReadNodeGen.create();
        }

        public abstract Object execute(NativeObject obj, long index);

        @Specialization(guards = "obj.isByteType()")
        protected static final long doNativeBytes(final NativeObject obj, final long index) {
            return Byte.toUnsignedLong(obj.getByte(index));
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final long doNativeShorts(final NativeObject obj, final long index) {
            return Short.toUnsignedLong(obj.getShort(index));
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final long doNativeInts(final NativeObject obj, final long index) {
            return Integer.toUnsignedLong(obj.getInt(index));
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object doNativeLongs(final NativeObject obj, final long index,
                        @Cached("createBinaryProfile()") final ConditionProfile positiveValueProfile) {
            final long value = obj.getLong(index);
            if (positiveValueProfile.profile(value >= 0)) {
                return value;
            } else {
                return LargeIntegerObject.toUnsigned(obj.image, value);
            }
        }
    }

    @GenerateUncached
    @ImportStatic(NativeObject.class)
    public abstract static class NativeObjectWriteNode extends AbstractNode {

        public static NativeObjectWriteNode create() {
            return NativeObjectWriteNodeGen.create();
        }

        public abstract void execute(NativeObject obj, long index, Object value);

        @Specialization(guards = {"obj.isByteType()", "value >= 0", "value <= BYTE_MAX"})
        protected static final void doNativeBytes(final NativeObject obj, final long index, final long value) {
            obj.setByte(index, (byte) value);
        }

        @Specialization(guards = {"obj.isShortType()", "value >= 0", "value <= SHORT_MAX"})
        protected static final void doNativeShorts(final NativeObject obj, final long index, final long value) {
            obj.setShort(index, (short) value);
        }

        @Specialization(guards = {"obj.isIntType()", "value >= 0", "value <= INTEGER_MAX"})
        protected static final void doNativeInts(final NativeObject obj, final long index, final long value) {
            obj.setInt(index, (int) value);
        }

        @Specialization(guards = {"obj.isLongType()", "value >= 0"})
        protected static final void doNativeLongs(final NativeObject obj, final long index, final long value) {
            obj.setLong(index, value);
        }

        protected static final boolean inByteRange(final char value) {
            return value <= NativeObject.BYTE_MAX;
        }

        @Specialization(guards = {"obj.isByteType()", "inByteRange(value)"})
        protected static final void doNativeBytesChar(final NativeObject obj, final long index, final char value) {
            doNativeBytes(obj, index, value);
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

        /*
         * CharacterObject hold values > Character.MAX_VALUE, which cannot fit into byte/short type.
         */

        @Specialization(guards = "obj.isIntType()")
        protected static final void doNativeIntsChar(final NativeObject obj, final long index, final CharacterObject value) {
            doNativeInts(obj, index, value.getValue());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final void doNativeLongsChar(final NativeObject obj, final long index, final CharacterObject value) {
            doNativeLongs(obj, index, value.getValue());
        }

        @Specialization(guards = {"obj.isByteType()", "value.inRange(0, BYTE_MAX)"})
        protected static final void doNativeBytesLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeBytes(obj, index, value.longValue());
        }

        @Specialization(guards = {"obj.isShortType()", "value.inRange(0, SHORT_MAX)"})
        protected static final void doNativeShortsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeShorts(obj, index, value.longValue());
        }

        @Specialization(guards = {"obj.isIntType()", "value.inRange(0, INTEGER_MAX)"})
        protected static final void doNativeIntsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeInts(obj, index, value.longValue());
        }

        @Specialization(guards = {"obj.isLongType()", "value.isZeroOrPositive()", "value.fitsIntoLong()"})
        protected static final void doNativeLongsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeLongs(obj, index, value.longValue());
        }

        @Specialization(guards = {"obj.isLongType()", "value.isZeroOrPositive()", "!value.fitsIntoLong()", "value.lessThanOneShiftedBy64()"})
        protected static final void doNativeLongsLargeIntegerSigned(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeLongs(obj, index, value.toSignedLong());
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final void doFail(final NativeObject obj, final long index, final Object value) {
            /*
             * Throw primitive failed (instead of UnsupportedSpecializationException) here for
             * PrimBasicAtPutNode.
             */
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateUncached
    public abstract static class NativeObjectSizeNode extends AbstractNode {

        public static NativeObjectSizeNode create() {
            return NativeObjectSizeNodeGen.create();
        }

        public static NativeObjectSizeNode getUncached() {
            return NativeObjectSizeNodeGen.getUncached();
        }

        public abstract int execute(NativeObject obj);

        @Specialization(guards = "obj.isByteType()")
        protected static final int doNativeBytes(final NativeObject obj) {
            return obj.getByteLength();
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final int doNativeShorts(final NativeObject obj) {
            return obj.getShortLength();
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final int doNativeInts(final NativeObject obj) {
            return obj.getIntLength();
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final int doNativeLongs(final NativeObject obj) {
            return obj.getLongLength();
        }
    }

    @GenerateUncached
    public abstract static class NativeGetBytesNode extends AbstractNode {

        public static NativeGetBytesNode create() {
            return NativeGetBytesNodeGen.create();
        }

        public abstract byte[] execute(NativeObject obj);

        @Specialization(guards = "obj.isByteType()")
        protected static final byte[] doNativeBytes(final NativeObject obj) {
            return obj.getByteStorage();
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final byte[] doNativeShorts(final NativeObject obj) {
            return ArrayConversionUtils.bytesFromShorts(obj.getShortStorage());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final byte[] doNativeInts(final NativeObject obj) {
            return ArrayConversionUtils.bytesFromInts(obj.getIntStorage());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final byte[] doNativeLongs(final NativeObject obj) {
            return ArrayConversionUtils.bytesFromLongs(obj.getLongStorage());
        }
    }

    @GenerateUncached
    public abstract static class NativeObjectShallowCopyNode extends AbstractNode {

        public abstract NativeObject execute(NativeObject obj);

        @Specialization(guards = "obj.isByteType()")
        protected static final NativeObject doNativeBytes(final NativeObject obj) {
            return obj.shallowCopy(obj.getByteStorage().clone());
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final NativeObject doNativeShorts(final NativeObject obj) {
            return obj.shallowCopy(obj.getShortStorage().clone());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final NativeObject doNativeInts(final NativeObject obj) {
            return obj.shallowCopy(obj.getIntStorage().clone());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final NativeObject doNativeLongs(final NativeObject obj) {
            return obj.shallowCopy(obj.getLongStorage().clone());
        }
    }
}

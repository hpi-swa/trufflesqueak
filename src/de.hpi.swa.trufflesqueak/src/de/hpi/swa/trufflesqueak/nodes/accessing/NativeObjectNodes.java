/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodesFactory.NativeObjectSizeNodeGen;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class NativeObjectNodes {
    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeObjectReadNode extends AbstractNode {

        public abstract Object execute(Node node, NativeObject obj, long index);

        @Specialization(guards = "obj.isShortType()")
        protected static final long doNativeShorts(final NativeObject obj, final long index) {
            return Short.toUnsignedLong(obj.getShort(index));
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final long doNativeInts(final NativeObject obj, final long index) {
            return Integer.toUnsignedLong(obj.getInt(index));
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final Object doNativeLongs(final Node node, final NativeObject obj, final long index,
                        @Cached final InlinedConditionProfile positiveValueProfile) {
            final long value = obj.getLong(index);
            if (positiveValueProfile.profile(node, value >= 0)) {
                return value;
            } else {
                return LargeIntegerObject.toUnsigned(getContext(node), value);
            }
        }

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final long doNativeTruffleString(final Node node, final NativeObject obj, final long index, @Cached final TruffleString.ReadByteNode readByteNode) {
            return Integer.toUnsignedLong(obj.readByteTruffleString((int) index, readByteNode));
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(NativeObject.class)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeObjectWriteNode extends AbstractNode {

        public abstract void execute(Node node, NativeObject obj, long index, Object value);

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

        @Specialization(guards = {"obj.isTruffleStringType()", "value >= 0", "value <= BYTE_MAX"})
        protected static final void doNativeByteString(NativeObject obj, long index, final long value, @Cached.Shared("truffleString") @Cached final MutableTruffleString.WriteByteNode writeByteNode) {
            obj.writeByteTruffleString((int) index, (byte) value, writeByteNode);
        }

        protected static final boolean inByteRange(final char value) {
            return value <= NativeObject.BYTE_MAX;
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

        @Specialization(guards = {"obj.isTruffleStringType()", "inByteRange(value)"})
        protected static final void doNativeByteStringChar(NativeObject obj, long index, final char value, @Cached.Shared("truffleString") @Cached final MutableTruffleString.WriteByteNode writeByteNode) {
            doNativeByteString(obj, index, value, writeByteNode);
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

        @Specialization(guards = {"obj.isTruffleStringType()", "value.inRange(0, BYTE_MAX)"})
        protected static final void doNativeByteStringLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value, @Cached.Shared("truffleString") @Cached final MutableTruffleString.WriteByteNode writeByteNode) {
            doNativeByteString(obj, index, value.longValue(), writeByteNode);
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

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class NativeObjectSizeNode extends AbstractNode {

        public abstract int execute(Node node, NativeObject obj);

        public static final int executeUncached(final NativeObject obj) {
            return NativeObjectSizeNodeGen.getUncached().execute(null, obj);
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

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final int doNativeTruffleString(final NativeObject obj) {
            return obj.getTruffleStringByteLength();
        }
    }

    @GenerateInline
    @GenerateCached(false)
    public abstract static class NativeObjectByteSizeNode extends AbstractNode {

        public abstract int execute(Node node, NativeObject obj);

        @Specialization(guards = "obj.isShortType()")
        protected static final int doNativeShorts(final NativeObject obj) {
            return obj.getShortLength() * Short.BYTES;
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final int doNativeInts(final NativeObject obj) {
            return obj.getIntLength() * Integer.BYTES;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final int doNativeLongs(final NativeObject obj) {
            return obj.getLongLength() * Long.BYTES;
        }

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final int doNativeTruffleString(final NativeObject obj) {
            return obj.getTruffleStringByteLength();
        }
    }

    @GenerateInline
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeGetBytesNode extends AbstractNode {

        public abstract byte[] execute(Node node, NativeObject obj);

        @Specialization(guards = "obj.isShortType()")
        protected static final byte[] doNativeShorts(final NativeObject obj) {
            return UnsafeUtils.toBytes(obj.getShortStorage());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final byte[] doNativeInts(final NativeObject obj) {
            return UnsafeUtils.toBytes(obj.getIntStorage());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final byte[] doNativeLongs(final NativeObject obj) {
            return UnsafeUtils.toBytes(obj.getLongStorage());
        }

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final byte[] toTruffleString(final NativeObject obj, @Cached final TruffleString.CopyToByteArrayNode node) {
            return obj.getTruffleStringAsBytesCopy(node);
        }
    }

    @GenerateInline
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeGetShortsNode extends AbstractNode {

        public abstract short[] execute(Node node, NativeObject obj);

        @Specialization(guards = "obj.isShortType()")
        protected static final short[] doNativeShorts(final NativeObject obj) {
            return obj.getShortStorage();
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final short[] doNativeInts(final NativeObject obj) {
            return UnsafeUtils.toShorts(obj.getIntStorage());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final short[] doNativeLongs(final NativeObject obj) {
            return UnsafeUtils.toShorts(obj.getLongStorage());
        }

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final short[] toTruffleString(final NativeObject obj, @Cached final TruffleString.CopyToByteArrayNode node) {
            return UnsafeUtils.toShorts(obj.getTruffleStringAsBytesCopy(node));
        }
    }

    @GenerateInline
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeGetIntsNode extends AbstractNode {

        public abstract int[] execute(Node node, NativeObject obj);

        @Specialization(guards = "obj.isShortType()")
        protected static final int[] doNativeShorts(final NativeObject obj) {
            return UnsafeUtils.toInts(obj.getShortStorage());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final int[] doNativeInts(final NativeObject obj) {
            return obj.getIntStorage();
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final int[] doNativeLongs(final NativeObject obj) {
            return UnsafeUtils.toInts(obj.getLongStorage());
        }

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final int[] toTruffleString(final NativeObject obj, @Cached final TruffleString.CopyToByteArrayNode node) {
            return UnsafeUtils.toInts(obj.getTruffleStringAsBytesCopy(node));
        }
    }

    @GenerateInline
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeGetLongsNode extends AbstractNode {

        public abstract long[] execute(Node node, NativeObject obj);

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final long[] doNativeBytes(final NativeObject obj, @Cached final TruffleString.GetInternalByteArrayNode objGetInternalByteArrayNode) {
            return UnsafeUtils.toLongs(obj.getTruffleStringAsReadonlyBytes(objGetInternalByteArrayNode));
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final long[] doNativeShorts(final NativeObject obj) {
            return UnsafeUtils.toLongs(obj.getShortStorage());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final long[] doNativeInts(final NativeObject obj) {
            return UnsafeUtils.toLongs(obj.getIntStorage());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final long[] doNativeLongs(final NativeObject obj) {
            return obj.getLongStorage();
        }
    }

    @GenerateInline
    @GenerateCached(false)
    @SuppressWarnings("truffle-inlining") // inline = false is default for @Cached
    public abstract static class NativeObjectShallowCopyNode extends AbstractNode {

        public abstract NativeObject execute(Node node, NativeObject obj);

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

        @Specialization(guards = "obj.isTruffleStringType()")
        protected static final NativeObject doNativeTruffleString(final NativeObject obj, @Cached final TruffleString.CopyToByteArrayNode copyToByteArrayNode, @Cached final MutableTruffleString.FromByteArrayNode fromByteArrayNode) {
            return obj.shallowCopyTruffleString(copyToByteArrayNode, fromByteArrayNode);
        }
    }
}

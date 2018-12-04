package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeAcceptsValueNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeGetBytesNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeObjectSizeNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.ReadNativeObjectNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.WriteNativeObjectNodeGen;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

public final class NativeObjectNodes {

    public abstract static class NativeAcceptsValueNode extends Node {

        public static NativeAcceptsValueNode create() {
            return NativeAcceptsValueNodeGen.create();
        }

        public abstract boolean execute(NativeObject obj, Object value);

        @Specialization(guards = "obj.isByteType()")
        protected static final boolean doNativeBytes(@SuppressWarnings("unused") final NativeObject obj, final char value) {
            return value <= NativeObject.BYTE_MAX;
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final boolean doNativeShorts(@SuppressWarnings("unused") final NativeObject obj, final char value) {
            return value <= NativeObject.SHORT_MAX;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "obj.isIntType() || obj.isLongType()")
        protected static final boolean doNativeInts(final NativeObject obj, final char value) {
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "obj.isByteType()")
        protected static final boolean doNativeBytes(final NativeObject obj, final CharacterObject value) {
            return false; // CharacterObject never fits into byte.
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final boolean doNativeShorts(@SuppressWarnings("unused") final NativeObject obj, final CharacterObject value) {
            return value.getValue() <= NativeObject.SHORT_MAX;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "obj.isIntType() || obj.isLongType()")
        protected static final boolean doNativeInts(final NativeObject obj, final CharacterObject value) {
            return true;
        }

        @Specialization(guards = "obj.isByteType()")
        protected static final boolean doNativeBytes(@SuppressWarnings("unused") final NativeObject obj, final long value) {
            return 0 <= value && value <= NativeObject.BYTE_MAX;
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final boolean doNativeShorts(@SuppressWarnings("unused") final NativeObject obj, final long value) {
            return 0 <= value && value <= NativeObject.SHORT_MAX;
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final boolean doNativeInts(@SuppressWarnings("unused") final NativeObject obj, final long value) {
            return 0 <= value && value <= NativeObject.INTEGER_MAX;
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final boolean doNativeLongs(@SuppressWarnings("unused") final NativeObject obj, final long value) {
            return 0 <= value;
        }

        @Specialization(guards = {"obj.isByteType()"})
        protected static final boolean doNativeBytesLargeInteger(@SuppressWarnings("unused") final NativeObject obj, final LargeIntegerObject value) {
            return value.inRange(0, NativeObject.BYTE_MAX);
        }

        @Specialization(guards = {"obj.isShortType()"})
        protected static final boolean doNativeShortsLargeInteger(@SuppressWarnings("unused") final NativeObject obj, final LargeIntegerObject value) {
            return value.inRange(0, NativeObject.SHORT_MAX);
        }

        @Specialization(guards = {"obj.isIntType()"})
        protected static final boolean doNativeIntsLargeInteger(@SuppressWarnings("unused") final NativeObject obj, final LargeIntegerObject value) {
            return value.inRange(0, NativeObject.INTEGER_MAX);
        }

        @Specialization(guards = {"obj.isLongType()"})
        protected static final boolean doNativeLongsLargeInteger(@SuppressWarnings("unused") final NativeObject obj, final LargeIntegerObject value) {
            return value.isZeroOrPositive();
        }

        @Fallback
        protected static final boolean doFail(final NativeObject object, final Object value) {
            throw new SqueakException("Unexpected values:", object, value);
        }
    }

    public abstract static class ReadNativeObjectNode extends Node {

        public static ReadNativeObjectNode create() {
            return ReadNativeObjectNodeGen.create();
        }

        public abstract long execute(NativeObject obj, long index);

        @Specialization(guards = "obj.isByteType()")
        protected static final long doNativeBytes(final NativeObject obj, final long index) {
            return Byte.toUnsignedLong(obj.getByteStorage()[(int) index]);
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final long doNativeShorts(final NativeObject obj, final long index) {
            return Short.toUnsignedLong(obj.getShortStorage()[(int) index]);
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final long doNativeInts(final NativeObject obj, final long index) {
            return Integer.toUnsignedLong(obj.getIntStorage()[(int) index]);
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final long doNativeLongs(final NativeObject obj, final long index) {
            return obj.getLongStorage()[(int) index];
        }

        @Fallback
        protected static final long doFail(final NativeObject obj, final long index) {
            throw new SqueakException("Unexpected values:", obj, index);
        }
    }

    @ImportStatic(NativeObject.class)
    public abstract static class WriteNativeObjectNode extends Node {

        public static WriteNativeObjectNode create() {
            return WriteNativeObjectNodeGen.create();
        }

        public abstract void execute(NativeObject obj, long index, Object value);

        @Specialization(guards = {"obj.isByteType()", "value >= 0", "value <= BYTE_MAX"})
        protected static final void doNativeBytes(final NativeObject obj, final long index, final long value) {
            obj.getByteStorage()[(int) index] = (byte) value;
        }

        @Specialization(guards = {"obj.isShortType()", "value >= 0", "value <= SHORT_MAX"})
        protected static final void doNativeShorts(final NativeObject obj, final long index, final long value) {
            obj.getShortStorage()[(int) index] = (short) value;
        }

        @Specialization(guards = {"obj.isIntType()", "value >= 0", "value <= INTEGER_MAX"})
        protected static final void doNativeInts(final NativeObject obj, final long index, final long value) {
            obj.getIntStorage()[(int) index] = (int) value;
        }

        @Specialization(guards = {"obj.isLongType()", "value >= 0"})
        protected static final void doNativeLongs(final NativeObject obj, final long index, final long value) {
            obj.getLongStorage()[(int) index] = value;
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

        @Specialization(guards = "obj.isIntType()")
        protected static final void doNativeIntsChar(final NativeObject obj, final long index, final CharacterObject value) {
            doNativeInts(obj, index, value.getValue());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final void doNativeLongsChar(final NativeObject obj, final long index, final char value) {
            doNativeLongs(obj, index, value);
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final void doNativeLongsChar(final NativeObject obj, final long index, final CharacterObject value) {
            doNativeLongs(obj, index, value.getValue());
        }

        @Specialization(guards = {"obj.isByteType()", "value.inRange(0, BYTE_MAX)"})
        protected static final void doNativeBytesLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeBytes(obj, index, value.longValueExact());
        }

        @Specialization(guards = {"obj.isShortType()", "value.inRange(0, SHORT_MAX)"})
        protected static final void doNativeShortsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeShorts(obj, index, value.longValueExact());
        }

        @Specialization(guards = {"obj.isIntType()", "value.inRange(0, INTEGER_MAX)"})
        protected static final void doNativeIntsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeInts(obj, index, value.longValueExact());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"obj.isIntType()", "!value.inRange(0, INTEGER_MAX)"})
        protected static final void doNativeIntsLargeIntegerIllegal(final NativeObject obj, final long index, final LargeIntegerObject value) {
            throw new SqueakException("Illegal value for int array: " + value);
        }

        @Specialization(guards = {"obj.isLongType()", "value.isZeroOrPositive()"})
        protected static final void doNativeLongsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
            doNativeLongs(obj, index, value.longValueExact());
        }

        @Fallback
        protected static final void doFail(final NativeObject obj, final long index, final Object value) {
            throw new SqueakException("Unexpected values:", obj, index, value);
        }
    }

    public abstract static class NativeObjectSizeNode extends Node {

        public static NativeObjectSizeNode create() {
            return NativeObjectSizeNodeGen.create();
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

        @Fallback
        protected static final int doFail(final NativeObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }

    public abstract static class NativeGetBytesNode extends Node {

        public static NativeGetBytesNode create() {
            return NativeGetBytesNodeGen.create();
        }

        @TruffleBoundary
        public final String executeAsString(final NativeObject obj) {
            return new String(execute(obj));
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

        @Fallback
        protected static final byte[] doFail(final NativeObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }
}

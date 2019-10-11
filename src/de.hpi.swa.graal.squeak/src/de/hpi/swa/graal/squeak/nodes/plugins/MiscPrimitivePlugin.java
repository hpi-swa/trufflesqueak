/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class MiscPrimitivePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscPrimitivePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCompareString")
    public abstract static class PrimCompareStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitiveWithoutFallback {

        public PrimCompareStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"string1Value.isByteType()", "string2Value.isByteType()", "orderValue.isByteType()", "orderValue.getByteLength() >= 256"})
        protected static final long doCompare(@SuppressWarnings("unused") final Object receiver, final NativeObject string1Value, final NativeObject string2Value,
                        final NativeObject orderValue) {
            final byte[] string1 = string1Value.getByteStorage();
            final byte[] string2 = string2Value.getByteStorage();
            final byte[] order = orderValue.getByteStorage();
            final int len1 = string1.length;
            final int len2 = string2.length;
            final int min = Math.min(len1, len2);
            for (int i = 0; i < min; i++) {
                final byte c1 = UnsafeUtils.getByte(order, UnsafeUtils.getByte(string1, i) & 0xff);
                final byte c2 = UnsafeUtils.getByte(order, UnsafeUtils.getByte(string2, i) & 0xff);
                if (c1 != c2) {
                    return (c1 & 0xff) < (c2 & 0xff) ? 1L : 3L;
                }
            }
            return len1 == len2 ? 2L : len1 < len2 ? 1L : 3L;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final long doFail(final Object receiver, final Object string1, final Object string2, final Object order) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCompressToByteArray")
    public abstract static class PrimCompressToByteArrayNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        public PrimCompressToByteArrayNode(final CompiledMethodObject method) {
            super(method);
        }

        private static int encodeBytesOf(final int anInt, final byte[] ba, final int i) {
            ba[i - 1] = (byte) (anInt >> (24 & 0xff));
            ba[i + 0] = (byte) (anInt >> (16 & 0xff));
            ba[i + 1] = (byte) (anInt >> (8 & 0xff));
            ba[i + 2] = (byte) (anInt >> (0 & 0xff));
            return i + 4;
        }

        // expects i to be a 1-based (Squeak) index
        private static int encodeInt(final int anInt, final byte[] ba, final int i) {
            if (anInt <= 223) {
                ba[i - 1] = (byte) anInt;
                return i + 1;
            }
            if (anInt <= 7935) {
                ba[i - 1] = (byte) (anInt / 256 + 224);
                ba[i] = (byte) (anInt % 256);
                return i + 2;
            }
            ba[i - 1] = (byte) 255;
            return encodeBytesOf(anInt, ba, i + 1);
        }

        @Specialization(guards = {"bm.isIntType()", "ba.isByteType()"})
        protected static final long doCompress(@SuppressWarnings("unused") final Object receiver, final NativeObject bm, final NativeObject ba) {
            // "Store a run-coded compression of the receiver into the byteArray ba,
            // and return the last index stored into. ba is assumed to be large enough.
            // The encoding is as follows...
            // S {N D}*.
            // S is the size of the original bitmap, followed by run-coded pairs.
            // N is a run-length * 4 + data code.
            // D, the data, depends on the data code...
            // 0 skip N words, D is absent
            // 1 N words with all 4 bytes = D (1 byte)
            // 2 N words all = D (4 bytes)
            // 3 N words follow in D (4N bytes)
            // S and N are encoded as follows...
            // 0-223 0-223
            // 224-254 (0-30)*256 + next byte (0-7935)
            // 255 next 4 bytes"
            final byte[] baBytes = ba.getByteStorage();
            final int[] bmInts = bm.getIntStorage();
            final int size = bmInts.length;
            int i = encodeInt(size, baBytes, 1);
            int k = 0;
            while (k < size) {
                final int word = bmInts[k];
                final int lowByte = word & 0xFF;
                final boolean eqBytes = (word >> 8 & 0xFF) == lowByte &&
                                (word >> 16 & 0xFF) == lowByte && (word >> 24 & 0xFF) == lowByte;

                int j = k;
                // scan for equal words...
                while (j + 1 < size && word == bmInts[j + 1]) {
                    j++;
                }
                if (j > k) {
                    // We have two or more equal words, ending at j
                    if (eqBytes) {
                        // Actually words of equal bytes
                        i = encodeInt((j - k + 1) * 4 + 1, baBytes, i);
                        baBytes[i - 1] = (byte) lowByte;
                        i++;
                    } else {
                        i = encodeInt((j - k + 1) * 4 + 2, baBytes, i);
                        i = encodeBytesOf(word, baBytes, i);
                    }
                    k = j + 1;
                } else {
                    // Check for word of 4 == bytes
                    if (eqBytes) {
                        // Note 1 word of 4 == bytes
                        i = encodeInt(1 * 4 + 1, baBytes, i);
                        baBytes[i - 1] = (byte) lowByte;
                        i++;
                        k++;
                    } else {
                        // Finally, check for junk
                        // scan for unequal words...
                        while (j + 1 < size && bmInts[j] != bmInts[j + 1]) {
                            j++;
                        }
                        if (j + 1 == size) {
                            j++;
                        }
                        // We have one or more unmatching words, ending at j-1
                        i = encodeInt((j - k) * 4 + 3, baBytes, i);
                        for (int m = k; m < j; m++) {
                            i = encodeBytesOf(bmInts[m], baBytes, i);
                        }
                        k = j;
                    }
                }
            }
            return i - 1;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!ba.isByteType()"})
        protected static final long doFailBadArgument(final Object receiver, final Object bm, final NativeObject ba) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveConvert8BitSigned")
    public abstract static class PrimConvert8BitSignedNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        public PrimConvert8BitSignedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"aByteArray.isByteType()", "aSoundBuffer.isIntType()", "aByteArray.getByteLength() > aSoundBuffer.getIntLength()"})
        protected static final Object doConvert(final Object receiver, final NativeObject aByteArray, final NativeObject aSoundBuffer) {
            final byte[] bytes = aByteArray.getByteStorage();
            final int[] ints = aSoundBuffer.getIntStorage();
            for (int i = 0; i < bytes.length; i++) {
                final int wordIndex = i / 2;
                final long value = (bytes[i] & 0xff) << 8;
                if (i % 2 == 0) {
                    ints[wordIndex] = ints[wordIndex] & 0xffff0000 | (int) value & 0xffff;
                } else {
                    ints[wordIndex] = (int) value << 16 | ints[wordIndex] & 0xffff;
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecompressFromByteArray")
    public abstract static class PrimDecompressFromByteArrayNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        public PrimDecompressFromByteArrayNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"bm.isIntType()", "ba.isByteType()"})
        protected static final Object doDecompress(final Object receiver, final NativeObject bm, final NativeObject ba, final long index) {
            /**
             * <pre>
                 Decompress the body of a byteArray encoded by compressToByteArray (qv)...
                 The format is simply a sequence of run-coded pairs, {N D}*.
                     N is a run-length * 4 + data code.
                     D, the data, depends on the data code...
                         0   skip N words, D is absent
                             (could be used to skip from one raster line to the next)
                         1   N words with all 4 bytes = D (1 byte)
                         2   N words all = D (4 bytes)
                         3   N words follow in D (4N bytes)
                     S and N are encoded as follows (see decodeIntFrom:)...
                         0-223   0-223
                         224-254 (0-30)*256 + next byte (0-7935)
                         255     next 4 bytes
                 NOTE:  If fed with garbage, this routine could read past the end of ba, but it should fail before writing past the ned of bm.
             * </pre>
             */

            final byte[] baBytes = ba.getByteStorage();
            final int[] bmInts = bm.getIntStorage();
            int i = (int) index - 1;
            final int end = baBytes.length;
            int k = 0;
            final int pastEnd = bmInts.length + 1;
            while (i < end) {
                // Decode next run start N
                int anInt = baBytes[i++] & 0xff;
                if (anInt > 223) {
                    if (anInt <= 254) {
                        anInt = (anInt - 224) * 256 + (baBytes[i++] & 0xff);
                    } else {
                        anInt = (baBytes[i++] & 0xff) << 24 | (baBytes[i++] & 0xff) << 16 | (baBytes[i++] & 0xff) << 8 | baBytes[i++] & 0xff;
                    }
                }
                final long n = anInt >> 2;
                if (k + n > pastEnd) {
                    CompilerDirectives.transferToInterpreter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                switch (anInt & 3) {
                    case 0: // skip
                        break;
                    case 1: { // n consecutive words of 4 bytes = the following byte
                        final int data = (baBytes[i] & 0xff) << 24 | (baBytes[i] & 0xff) << 16 | (baBytes[i] & 0xff) << 8 | baBytes[i++] & 0xff;
                        for (int j = 0; j < n; j++) {
                            bmInts[k++] = data;
                        }
                        break;
                    }
                    case 2: { // n consecutive words = 4 following bytes
                        final int data = (baBytes[i++] & 0xff) << 24 | (baBytes[i++] & 0xff) << 16 | (baBytes[i++] & 0xff) << 8 | baBytes[i++] & 0xff;
                        for (int j = 0; j < n; j++) {
                            bmInts[k++] = data;
                        }
                        break;
                    }

                    case 3: { // n consecutive words from the data
                        for (int m = 0; m < n; m++) {
                            bmInts[k++] = (baBytes[i++] & 0xff) << 24 | (baBytes[i++] & 0xff) << 16 | (baBytes[i++] & 0xff) << 8 | baBytes[i++] & 0xff;
                        }
                        break;
                    }
                    default:
                        break; // cannot happen
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFindFirstInString")
    public abstract static class PrimFindFirstInStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        public PrimFindFirstInStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "inclusionMap.isByteType()", "inclusionMap.getByteLength() == 256"})
        protected static final long doFind(@SuppressWarnings("unused") final Object receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            final byte[] stringBytes = string.getByteStorage();
            final byte[] inclusionMapBytes = inclusionMap.getByteStorage();
            final int stringSize = stringBytes.length;
            long index = start - 1;
            while (index < stringSize && UnsafeUtils.getByte(inclusionMapBytes, UnsafeUtils.getByte(stringBytes, index) & 0xff) == 0) {
                index++;
            }
            return index >= stringSize ? 0L : index + 1;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"start >= 1", "string.isByteType()", "inclusionMap.isByteType()", "inclusionMap.getByteLength() != 256"})
        protected static final long doFindNot256(final Object receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"start >= 1", "!string.isByteType() || !inclusionMap.isByteType()"})
        protected static final long doFailBadArgument(final Object receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"start < 1"})
        protected static final long doFailBadIndex(final Object receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFindSubstring")
    public abstract static class PrimFindSubstringNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        public PrimFindSubstringNode(final CompiledMethodObject method) {
            super(method);
        }

        public abstract Object executeFindSubstring(VirtualFrame frame);

        @SuppressWarnings("unused")
        @Specialization(guards = {"key.isByteType()", "key.getByteLength() == 0"})
        protected static final long doFindQuick(@SuppressWarnings("unused") final Object receiver, final NativeObject key, final NativeObject body, final long start,
                        final NativeObject matchTable) {
            return 0L;
        }

        @Specialization(guards = {"key.isByteType()", "key.getByteLength() > 0", "body.isByteType()", "matchTable.isByteType()", "matchTable.getByteLength() >= 256"})
        protected static final long doFind(@SuppressWarnings("unused") final Object receiver, final NativeObject key, final NativeObject body, final long start,
                        final NativeObject matchTable) {
            final byte[] keyBytes = key.getByteStorage();
            final int keyBytesLength = keyBytes.length;
            assert keyBytesLength != 0;
            final byte[] bodyBytes = body.getByteStorage();
            final int bodyBytesLength = bodyBytes.length;
            final byte[] matchTableBytes = matchTable.getByteStorage();
            for (int startIndex = Math.max((int) start - 1, 0); startIndex <= bodyBytesLength - keyBytesLength; startIndex++) {
                int index = 0;
                while (matchTableBytes[bodyBytes[startIndex + index] & 0xff] == matchTableBytes[keyBytes[index] & 0xff]) {
                    if (index == keyBytesLength - 1) {
                        return startIndex + 1;
                    } else {
                        index++;
                    }
                }
            }
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!key.isByteType() || (!body.isByteType() || !matchTable.isByteType())")
        protected static final long doInvalidKey(final Object receiver, final NativeObject key, final NativeObject body, final long start, final NativeObject matchTable) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIndexOfAsciiInString")
    public abstract static class PrimIndexOfAsciiInStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        public PrimIndexOfAsciiInStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start >= 0", "string.isByteType()"})
        protected static final long doNativeObject(@SuppressWarnings("unused") final Object receiver, final long value, final NativeObject string, final long start) {
            final byte[] bytes = string.getByteStorage();
            for (int i = (int) (start - 1); i < bytes.length; i++) {
                if ((bytes[i] & 0xff) == value) {
                    return i + 1;
                }
            }
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringHash")
    public abstract static class PrimStringHashNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        private static final int TWO_LEFT_SHIFTED_BY_28 = 2 << 28;

        public PrimStringHashNode(final CompiledMethodObject method) {
            super(method);
        }

        /* Byte(Array|String|Symbol)>>#hashWithInitialHash: */

        @Specialization(guards = {"string.isByteType()"})
        protected static final long doNativeObject(final NativeObject string, final long initialHash, @SuppressWarnings("unused") final NotProvided notProvided) {
            return calculateHash(initialHash, string.getByteStorage());
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject largeInteger, final long initialHash, @SuppressWarnings("unused") final NotProvided notProvided) {
            return calculateHash(initialHash, largeInteger.getBytes());
        }

        @Specialization(guards = {"isLongMinValue(value)"})
        protected static final long doLongMinValue(@SuppressWarnings("unused") final long value, final long initialHash, @SuppressWarnings("unused") final NotProvided notProvided) {
            return calculateHash(initialHash, LargeIntegerObject.getLongMinOverflowResultBytes());
        }

        /* (Byte(Array|String|Symbol) class|MiscPrimitivePluginTest)>>#hashBytes:startingWith: */

        @Specialization(guards = {"string.isByteType()"})
        protected static final long doNativeObject(@SuppressWarnings("unused") final Object receiver, final NativeObject string, final long initialHash) {
            return calculateHash(initialHash, string.getByteStorage());
        }

        @Specialization
        protected static final long doLargeInteger(@SuppressWarnings("unused") final Object receiver, final LargeIntegerObject largeInteger, final long initialHash) {
            return calculateHash(initialHash, largeInteger.getBytes());
        }

        @Specialization(guards = {"isLongMinValue(value)"})
        protected static final long doLongMinValue(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final long value, final long initialHash) {
            return calculateHash(initialHash, LargeIntegerObject.getLongMinOverflowResultBytes());
        }

        private static long calculateHash(final long initialHash, final byte[] bytes) {
            int hash = (int) (initialHash & 0x0fffffff);
            final int length = bytes.length;
            for (int i = 0; i < length; i++) {
                hash = (hash + (UnsafeUtils.getByte(bytes, i) & 0xff)) * 0x19660D % TWO_LEFT_SHIFTED_BY_28;
            }
            return hash & 0x0fffffffL;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTranslateStringWithTable")
    public abstract static class PrimTranslateStringWithTableNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        public PrimTranslateStringWithTableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table.isByteType()", "table.getByteLength() >= 256"})
        protected static final Object doNativeObject(final Object receiver, final NativeObject string, final long start, final long stop, final NativeObject table) {
            final byte[] stringBytes = string.getByteStorage();
            final byte[] tableBytes = table.getByteStorage();
            for (int i = (int) start - 1; i < stop; i++) {
                stringBytes[i] = UnsafeUtils.getByte(tableBytes, UnsafeUtils.getByte(stringBytes, i) & 0xff);
            }
            return receiver;
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table.isIntType()", "table.getIntLength() >= 256"})
        protected static final Object doNativeObjectIntTable(final Object receiver, final NativeObject string, final long start, final long stop,
                        final NativeObject table) {
            final byte[] stringBytes = string.getByteStorage();
            final int[] tableBytes = table.getIntStorage();
            for (int i = (int) start - 1; i < stop; i++) {
                stringBytes[i] = (byte) UnsafeUtils.getInt(tableBytes, UnsafeUtils.getByte(stringBytes, i) & 0xff);
            }
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"start >= 1", "!string.isByteType()"})
        protected static final AbstractSqueakObject doFailBadArguments(final Object receiver, final NativeObject string, final long start, final long stop, final NativeObject table) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"hasBadIndex(string, start, stop)"})
        protected static final AbstractSqueakObject doFailBadIndex(final Object receiver, final NativeObject string, final long start, final long stop, final NativeObject table) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        protected static final boolean hasBadIndex(final NativeObject string, final long start, final long stop) {
            return start < 1 || string.isByteType() && stop > string.getByteLength();
        }
    }
}

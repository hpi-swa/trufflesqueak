/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuaternaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimHashMultiplyNode;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class MiscPrimitivePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscPrimitivePluginFactory.getFactories();
    }

    public abstract static class AbstractPrimCompareStringNode extends AbstractPrimitiveNode {
        private final LoopConditionProfile loopProfile = LoopConditionProfile.create();

        protected static final NativeObject asciiOrderOrNull(final NativeObject orderValue) {
            if (orderValue.isByteType() && orderValue.getByteLength() == 256) {
                final byte[] bytes = orderValue.getByteStorage();
                /* AsciiOrder is the identity function. */
                for (int i = 0; i < bytes.length; i++) {
                    if ((bytes[i] & 0xff) != i) {
                        return null;
                    }
                }
                return orderValue;
            }
            return null;
        }

        protected static final NativeObject validOrderOrNull(final NativeObject orderValue) {
            return orderValue.isByteType() && orderValue.getByteLength() >= 256 ? orderValue : null;
        }

        protected final long compareAsciiOrder(final NativeObject string1, final NativeObject string2) {
            final int len1 = string1.getByteLength();
            final int len2 = string2.getByteLength();
            final int min = Math.min(len1, len2);
            int i = 0;
            try {
                for (; loopProfile.inject(i < min); i++) {
                    final byte c1 = string1.getByte(i);
                    final byte c2 = string2.getByte(i);
                    if (c1 != c2) {
                        return (c1 & 0xff) < (c2 & 0xff) ? -1L : 1L;
                    }
                }
            } finally {
                profileAndReportLoopCount(loopProfile, i);
            }
            return len1 == len2 ? 0L : len1 < len2 ? -1L : 1L;
        }

        protected final long compare(final NativeObject string1, final NativeObject string2, final NativeObject orderValue) {
            final int len1 = string1.getByteLength();
            final int len2 = string2.getByteLength();
            final int min = Math.min(len1, len2);
            int i = 0;
            try {
                for (; loopProfile.inject(i < min); i++) {
                    final byte c1 = orderValue.getByte(string1.getByteUnsigned(i));
                    final byte c2 = orderValue.getByte(string2.getByteUnsigned(i));
                    if (c1 != c2) {
                        return (c1 & 0xff) < (c2 & 0xff) ? -1L : 1L;
                    }
                }
            } finally {
                profileAndReportLoopCount(loopProfile, i);
            }
            return len1 == len2 ? 0L : len1 < len2 ? -1L : 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCompareString")
    public abstract static class PrimCompareStringNode extends AbstractPrimCompareStringNode {

        @Specialization(guards = {"string1.isByteType()", "string2.isByteType()", "orderValue == cachedAsciiOrder"}, limit = "1")
        protected final long doCompareAsciiOrder(@SuppressWarnings("unused") final Object receiver, final NativeObject string1, final NativeObject string2,
                        @SuppressWarnings("unused") final NativeObject orderValue,
                        @SuppressWarnings("unused") @Cached("asciiOrderOrNull(orderValue)") final NativeObject cachedAsciiOrder) {
            return compareAsciiOrder(string1, string2) + 2L;
        }

        @Specialization(guards = {"string1.isByteType()", "string2.isByteType()", "orderValue == cachedOrder"}, limit = "1")
        protected final long doCompareCached(@SuppressWarnings("unused") final Object receiver, final NativeObject string1, final NativeObject string2,
                        @SuppressWarnings("unused") final NativeObject orderValue,
                        @Cached("validOrderOrNull(orderValue)") final NativeObject cachedOrder) {
            return compare(string1, string2, cachedOrder) + 2L;
        }

        @Specialization(guards = {"string1.isByteType()", "string2.isByteType()", "orderValue.isByteType()", "orderValue.getByteLength() >= 256"}, //
                        replaces = {"doCompareAsciiOrder", "doCompareCached"})
        protected final long doCompare(@SuppressWarnings("unused") final Object receiver, final NativeObject string1, final NativeObject string2,
                        final NativeObject orderValue) {
            return compare(string1, string2, orderValue) + 2L;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final long doFail(final Object receiver, final Object string1, final Object string2, final Object order) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCompressToByteArray")
    public abstract static class PrimCompressToByteArrayNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        private static int encodeBytesOf(final int anInt, final NativeObject ba, final int i) {
            ba.setByte(i - 1, (byte) (anInt >> 24));
            ba.setByte(i + 0, (byte) (anInt >> 16));
            ba.setByte(i + 1, (byte) (anInt >> 8));
            ba.setByte(i + 2, (byte) anInt);
            return i + 4;
        }

        // expects i to be a 1-based (Squeak) index
        private static int encodeInt(final int anInt, final NativeObject ba, final int i) {
            if (anInt <= 223) {
                ba.setByte(i - 1, anInt);
                return i + 1;
            }
            if (anInt <= 7935) {
                ba.setByte(i - 1, anInt / 256 + 224);
                ba.setByte(i, anInt % 256);
                return i + 2;
            }
            ba.setByte(i - 1, 255);
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
            final int size = bm.getIntLength();
            int i = encodeInt(size, ba, 1);
            int k = 0;
            while (k < size) {
                final int word = bm.getInt(k);
                final int lowByte = word & 0xFF;
                final boolean eqBytes = (word >> 8 & 0xFF) == lowByte &&
                                (word >> 16 & 0xFF) == lowByte && (word >> 24 & 0xFF) == lowByte;

                int j = k;
                // scan for equal words...
                while (j + 1 < size && word == bm.getInt(j + 1)) {
                    j++;
                }
                if (j > k) {
                    // We have two or more equal words, ending at j
                    if (eqBytes) {
                        // Actually words of equal bytes
                        i = encodeInt((j - k + 1) * 4 + 1, ba, i);
                        ba.setByte(i - 1, lowByte);
                        i++;
                    } else {
                        i = encodeInt((j - k + 1) * 4 + 2, ba, i);
                        i = encodeBytesOf(word, ba, i);
                    }
                    k = j + 1;
                } else {
                    // Check for word of 4 == bytes
                    if (eqBytes) {
                        // Note 1 word of 4 == bytes
                        i = encodeInt(1 * 4 + 1, ba, i);
                        ba.setByte(i - 1, lowByte);
                        i++;
                        k++;
                    } else {
                        // Finally, check for junk
                        // scan for unequal words...
                        while (j + 1 < size && bm.getInt(j) != bm.getInt(j + 1)) {
                            j++;
                        }
                        if (j + 1 == size) {
                            j++;
                        }
                        // We have one or more unmatching words, ending at j-1
                        i = encodeInt((j - k) * 4 + 3, ba, i);
                        for (int m = k; m < j; m++) {
                            i = encodeBytesOf(bm.getInt(m), ba, i);
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
    public abstract static class PrimConvert8BitSignedNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = {"aByteArray.isByteType()", "aSoundBuffer.isIntType()", "aByteArrayLength > aSoundBuffer.getIntLength()"})
        protected static final Object doConvert(final Object receiver, final NativeObject aByteArray, final NativeObject aSoundBuffer,
                        @Bind("aByteArray.getByteLength()") final int aByteArrayLength) {
            for (int i = 0; i < aByteArrayLength; i++) {
                final int wordIndex = i / 2;
                final long value = aByteArray.getByteUnsigned(i) << 8;
                if (i % 2 == 0) {
                    aSoundBuffer.setInt(wordIndex, aSoundBuffer.getInt(wordIndex) & 0xffff0000 | (int) value & 0xffff);
                } else {
                    aSoundBuffer.setInt(wordIndex, (int) value << 16 | aSoundBuffer.getInt(wordIndex) & 0xffff);
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecompressFromByteArray")
    public abstract static class PrimDecompressFromByteArrayNode extends AbstractPrimitiveNode implements QuaternaryPrimitiveFallback {
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

            int i = (int) index - 1;
            final int end = ba.getByteLength();
            int k = 0;
            final int pastEnd = bm.getIntLength() + 1;
            while (i < end) {
                // Decode next run start N
                int anInt = ba.getByteUnsigned(i++);
                if (anInt > 223) {
                    if (anInt <= 254) {
                        anInt = (anInt - 224) * 256 + ba.getByteUnsigned(i++);
                    } else {
                        anInt = ba.getByteUnsigned(i++) << 24 | ba.getByteUnsigned(i++) << 16 | ba.getByteUnsigned(i++) << 8 | ba.getByteUnsigned(i++);
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
                        final int data = ba.getByteUnsigned(i) << 24 | ba.getByteUnsigned(i) << 16 | ba.getByteUnsigned(i) << 8 | ba.getByteUnsigned(i++);
                        for (int j = 0; j < n; j++) {
                            bm.setInt(k++, data);
                        }
                        break;
                    }
                    case 2: { // n consecutive words = 4 following bytes
                        final int data = ba.getByteUnsigned(i++) << 24 | ba.getByteUnsigned(i++) << 16 | ba.getByteUnsigned(i++) << 8 | ba.getByteUnsigned(i++);
                        for (int j = 0; j < n; j++) {
                            bm.setInt(k++, data);
                        }
                        break;
                    }

                    case 3: { // n consecutive words from the data
                        for (int m = 0; m < n; m++) {
                            bm.setInt(k++, ba.getByteUnsigned(i++) << 24 | ba.getByteUnsigned(i++) << 16 | ba.getByteUnsigned(i++) << 8 | ba.getByteUnsigned(i++));
                        }
                        break;
                    }
                    default:
                        CompilerDirectives.transferToInterpreter();
                        break; // cannot happen
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFindFirstInString")
    public abstract static class PrimFindFirstInStringNode extends AbstractPrimitiveNode {

        @Specialization(guards = {"start > 0", "string.isByteType()", "inclusionMap == cachedInclusionMap"}, limit = "1")
        protected final long doFindCached(@SuppressWarnings("unused") final Object receiver, final NativeObject string, @SuppressWarnings("unused") final NativeObject inclusionMap,
                        final long start,
                        @Cached("validInclusionMapOrNull(inclusionMap)") final NativeObject cachedInclusionMap,
                        @Cached final ConditionProfile notFoundProfile,
                        @Cached final LoopConditionProfile loopProfile) {
            return doFind(receiver, string, cachedInclusionMap, start, notFoundProfile, loopProfile);
        }

        protected static final NativeObject validInclusionMapOrNull(final NativeObject inclusionMap) {
            return inclusionMap.isByteType() && inclusionMap.getByteLength() == 256 ? inclusionMap : null;
        }

        @Specialization(guards = {"start > 0", "string.isByteType()", "inclusionMap.isByteType()", "inclusionMap.getByteLength() == 256"}, replaces = "doFindCached")
        protected final long doFind(@SuppressWarnings("unused") final Object receiver, final NativeObject string, final NativeObject inclusionMap, final long start,
                        @Cached final ConditionProfile notFoundProfile,
                        @Cached final LoopConditionProfile loopProfile) {
            final int stringSize = string.getByteLength();
            long index = start - 1;
            try {
                while (loopProfile.inject(index < stringSize && inclusionMap.getByte(string.getByteUnsigned(index)) == 0)) {
                    index++;
                }
            } finally {
                profileAndReportLoopCount(loopProfile, index);
            }
            return notFoundProfile.profile(index >= stringSize) ? 0L : index + 1;
        }

        @Fallback
        @SuppressWarnings("unused")
        protected static final long doFail(final Object receiver, final Object string, final Object inclusionMap, final Object start) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFindSubstring")
    public abstract static class PrimFindSubstringNode extends AbstractPrimitiveNode implements QuinaryPrimitiveFallback {
        @Specialization
        protected final long doFind(@SuppressWarnings("unused") final Object receiver, final NativeObject key, final NativeObject body, final long start,
                        final NativeObject matchTable,
                        @Cached final ConditionProfile quickReturnProfile,
                        @Cached final BranchProfile foundProfile,
                        @Cached final BranchProfile notFoundProfile,
                        @Cached final LoopConditionProfile outerLoopProfile,
                        @Cached final LoopConditionProfile innerLoopProfile) {
            if (!key.isByteType() || !body.isByteType() || !matchTable.isByteType() || matchTable.getByteLength() < 256) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final int keyLength = key.getByteLength();
            if (quickReturnProfile.profile(keyLength == 0)) {
                return 0L;
            } else {
                final int bodyLength = body.getByteLength();
                long startIndex = Math.max(start - 1, 0);
                try {
                    for (; outerLoopProfile.inject(startIndex <= bodyLength - keyLength); startIndex++) {
                        int index = 0;
                        try {
                            while (innerLoopProfile.inject(matchTable.getByte(body.getByteUnsigned(startIndex + index)) == matchTable.getByte(key.getByteUnsigned(index)))) {
                                if (index == keyLength - 1) {
                                    foundProfile.enter();
                                    return startIndex + 1;
                                } else {
                                    index++;
                                }
                            }
                        } finally {
                            profileAndReportLoopCount(innerLoopProfile, startIndex);
                        }
                    }
                } finally {
                    profileAndReportLoopCount(outerLoopProfile, startIndex);
                }
                notFoundProfile.enter();
                return 0L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIndexOfAsciiInString")
    public abstract static class PrimIndexOfAsciiInStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitiveFallback {

        @Specialization(guards = {"start >= 0", "string.isByteType()"})
        protected final long doNativeObject(@SuppressWarnings("unused") final Object receiver, final long value, final NativeObject string, final long start,
                        @Cached final BranchProfile foundProfile,
                        @Cached final BranchProfile notFoundProfile,
                        @Cached final LoopConditionProfile loopProfile) {
            final byte valueByte = (byte) value;
            long i = start - 1;
            try {
                for (; loopProfile.inject(i < string.getByteLength()); i++) {
                    if (string.getByte(i) == valueByte) {
                        foundProfile.enter();
                        return i + 1;
                    }
                }
            } finally {
                profileAndReportLoopCount(loopProfile, i);
            }
            notFoundProfile.enter();
            return 0L;
        }
    }

    private abstract static class AbstractPrimStringHashNode extends AbstractPrimitiveNode {
        private final LoopConditionProfile loopProfile = LoopConditionProfile.create();

        protected final long calculateHash(final long initialHash, final byte[] bytes) {
            long hash = initialHash & PrimHashMultiplyNode.HASH_MULTIPLY_MASK;
            final int length = bytes.length;
            try {
                for (int i = 0; loopProfile.inject(i < length); i++) {
                    hash = (hash + (UnsafeUtils.getByte(bytes, i) & 0xff)) * PrimHashMultiplyNode.HASH_MULTIPLY_CONSTANT & PrimHashMultiplyNode.HASH_MULTIPLY_MASK;
                }
            } finally {
                profileAndReportLoopCount(loopProfile, length);
            }
            return hash;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringHash")
    /* Byte(Array|String|Symbol)>>#hashWithInitialHash: */
    public abstract static class PrimStringHash2Node extends AbstractPrimStringHashNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"string.isByteType()"})
        protected final long doNativeObject(final NativeObject string, final long initialHash) {
            return calculateHash(initialHash, string.getByteStorage());
        }

        @Specialization
        protected final long doLargeInteger(final LargeIntegerObject largeInteger, final long initialHash) {
            return calculateHash(initialHash, largeInteger.getBytes());
        }

        @Specialization(guards = {"isLongMinValue(value)"})
        protected final long doLongMinValue(@SuppressWarnings("unused") final long value, final long initialHash) {
            return calculateHash(initialHash, LargeIntegerObject.getLongMinOverflowResultBytes());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringHash")
    /* (Byte(Array|String|Symbol) class|MiscPrimitivePluginTest)>>#hashBytes:startingWith: */
    public abstract static class PrimStringHash3Node extends AbstractPrimStringHashNode implements TernaryPrimitiveFallback {
        @Specialization(guards = {"string.isByteType()"})
        protected final long doNativeObject(@SuppressWarnings("unused") final Object receiver, final NativeObject string, final long initialHash) {
            return calculateHash(initialHash, string.getByteStorage());
        }

        @Specialization
        protected final long doLargeInteger(@SuppressWarnings("unused") final Object receiver, final LargeIntegerObject largeInteger, final long initialHash) {
            return calculateHash(initialHash, largeInteger.getBytes());
        }

        @Specialization(guards = {"isLongMinValue(value)"})
        protected final long doLongMinValue(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final long value, final long initialHash) {
            return calculateHash(initialHash, LargeIntegerObject.getLongMinOverflowResultBytes());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTranslateStringWithTable")
    public abstract static class PrimTranslateStringWithTableNode extends AbstractPrimitiveNode implements QuinaryPrimitiveFallback {

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table == cachedTable"}, limit = "1")
        protected final Object doNativeObjectCachedTable(final Object receiver, final NativeObject string, final long start, final long stop,
                        @SuppressWarnings("unused") final NativeObject table,
                        @Cached("byteTableOrNull(table)") final NativeObject cachedTable,
                        @Cached final LoopConditionProfile loopProfile) {
            return doNativeObject(receiver, string, start, stop, cachedTable, loopProfile);
        }

        protected static final NativeObject byteTableOrNull(final NativeObject table) {
            return table.isByteType() && table.getByteLength() >= 256 ? table : null;
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table.isByteType()", "table.getByteLength() >= 256"}, replaces = "doNativeObjectCachedTable")
        protected final Object doNativeObject(final Object receiver, final NativeObject string, final long start, final long stop, final NativeObject table,
                        @Cached final LoopConditionProfile loopProfile) {
            long i = start - 1;
            try {
                for (; loopProfile.inject(i < stop); i++) {
                    string.setByte(i, table.getByte(string.getByteUnsigned(i)));
                }
            } finally {
                profileAndReportLoopCount(loopProfile, i);
            }
            return receiver;
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table == cachedTable"}, limit = "1")
        protected final Object doNativeObjectIntTableCached(final Object receiver, final NativeObject string, final long start, final long stop,
                        @SuppressWarnings("unused") final NativeObject table,
                        @Cached("intTableOrNull(table)") final NativeObject cachedTable,
                        @Cached final LoopConditionProfile loopProfile) {
            return doNativeObjectIntTable(receiver, string, start, stop, cachedTable, loopProfile);
        }

        protected static final NativeObject intTableOrNull(final NativeObject table) {
            return table.isIntType() && table.getIntLength() >= 256 ? table : null;
        }

        @Specialization(guards = {"start >= 1", "string.isByteType()", "stop <= string.getByteLength()", "table.isIntType()", "table.getIntLength() >= 256"}, replaces = "doNativeObjectIntTableCached")
        protected final Object doNativeObjectIntTable(final Object receiver, final NativeObject string, final long start, final long stop,
                        final NativeObject table,
                        @Cached final LoopConditionProfile loopProfile) {
            long i = start - 1;
            try {
                for (; loopProfile.inject(i < stop); i++) {
                    string.setByte(i, table.getInt(string.getByteUnsigned(i)));
                }
            } finally {
                profileAndReportLoopCount(loopProfile, i);
            }
            return receiver;
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

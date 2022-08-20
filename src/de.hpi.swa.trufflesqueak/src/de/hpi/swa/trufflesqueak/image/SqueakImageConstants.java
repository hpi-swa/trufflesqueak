/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class SqueakImageConstants {

    /** General. */
    public static final int WORD_SIZE = Long.BYTES;
    public static final int MULTIPLE_BYTECODE_SETS_BITMASK = 0x200;
    public static final int IMAGE_HEADER_SIZE = WORD_SIZE * 16;
    public static final int IMAGE_HEADER_MEMORY_SIZE_POSITION = WORD_SIZE;
    public static final int IMAGE_HEADER_FIRST_FRAGMENT_SIZE_POSITION = 9 * WORD_SIZE;
    public static final int IMAGE_BRIDGE_SIZE = 2 * WORD_SIZE; /* bridge and nextSegmentSize. */
    public static final int[] SUPPORTED_IMAGE_FORMATS = new int[]{68021, 68021 | MULTIPLE_BYTECODE_SETS_BITMASK};

    /** Object Header. */
    public static final long OVERFLOW_SLOTS = 255;
    public static final long SLOTS_MASK = 0xFFL << 56;

    /** Object Header Tag Bits. */
    public static final int NUM_TAG_BITS = 3;
    public static final int OBJECT_TAG = 0;
    public static final int SMALL_INTEGER_TAG = 1;
    public static final int CHARACTER_TAG = 2;
    public static final int SMALL_FLOAT_TAG = 4;

    /** SmallInteger. */
    public static final long SMALL_INTEGER_MAX_VAL = 0xFFFFFFFFFFFFFFFL;
    public static final long SMALL_INTEGER_MIN_VAL = -0x1000000000000000L;

    /** SmallFloat. */
    /* SmallFloat64's have the same mantissa as IEEE double-precision floating point. */
    public static final long SMALL_FLOAT_MANTISSA_BITS = 52L;
    /*
     * 896 is 1023 - 127, where 1023 is the mid-point of the 11-bit double precision exponent range,
     * and 127 is the mid-point of the 8-bit SmallDouble exponent range.
     */
    public static final long SMALL_FLOAT_EXPONENT_OFFSET = 896L;
    public static final long SMALL_FLOAT_TAG_BITS_MASK = SMALL_FLOAT_EXPONENT_OFFSET << SMALL_FLOAT_MANTISSA_BITS + 1;
    public static final int SMALL_FLOAT_TAGGED_EXPONENT_SIZE = 8;

    /** Class Index Puns. */
    public static final int FREE_OBJECT_CLASS_INDEX_PUN = 0;
    public static final int ARRAY_CLASS_INDEX_PUN = 16;
    public static final int WORD_SIZE_CLASS_INDEX_PUN = 19;
    public static final int LAST_CLASS_INDEX_PUN = 31;

    /** Class Table. */
    /* 22-bit class mask => ~ 4M classes */
    public static final int CLASS_INDEX_FIELD_WIDTH = 22;
    public static final int CLASS_INDEX_MASK = (1 << CLASS_INDEX_FIELD_WIDTH) - 1;
    /* 1024 entries per page (2^10); 22 bit classIndex implies 2^12 pages. */
    public static final int CLASS_TABLE_MAJOR_INDEX_SHIFT = 10;
    public static final int CLASS_TABLE_MINOR_INDEX_MASK = (1 << CLASS_TABLE_MAJOR_INDEX_SHIFT) - 1;
    public static final int CLASS_TABLE_PAGE_SIZE = 1 << CLASS_TABLE_MAJOR_INDEX_SHIFT;
    /* Answer the number of slots for class table pages in the hidden root object. */
    public static final int CLASS_TABLE_ROOT_SLOTS = 1 << CLASS_INDEX_FIELD_WIDTH - CLASS_TABLE_MAJOR_INDEX_SHIFT;

    /** CompactClassIndices. */
    public static final int CLASS_FULL_BLOCK_CLOSURE_COMPACT_INDEX = 38;

    /** Hidden Objects. */
    /* Answer the number of extra root slots in the root of the hidden root object. */
    public static final int HIDDEN_ROOT_SLOTS = 8;
    public static final int NUM_FREE_LISTS = 64;
    public static final int OBJ_STACK_PAGE_SLOTS = 4092;

    public static int majorClassIndexOf(final int classIndex) {
        return classIndex >> CLASS_TABLE_MAJOR_INDEX_SHIFT;
    }

    public static int minorClassIndexOf(final int classIndex) {
        return classIndex & CLASS_TABLE_MINOR_INDEX_MASK;
    }

    public static int classTableIndexFor(final int majorIndex, final int minorIndex) {
        return majorIndex << CLASS_TABLE_MAJOR_INDEX_SHIFT | minorIndex & CLASS_TABLE_MINOR_INDEX_MASK;
    }

    /**
     * Object Header Specification (see SpurMemoryManager).
     *
     * <pre>
     *  MSB:  | 8: numSlots       | (on a byte boundary)
     *        | 2 bits            |   (msb,lsb = {isMarked,?})
     *        | 22: identityHash  | (on a word boundary)
     *        | 3 bits            |   (msb <-> lsb = {isGrey,isPinned,isRemembered}
     *        | 5: format         | (on a byte boundary)
     *        | 2 bits            |   (msb,lsb = {isImmutable,?})
     *        | 22: classIndex    | (on a word boundary) : LSB
     * </pre>
     */
    public static final class ObjectHeader {
        private static final int NUM_SLOTS_SIZE = 1 << 8;
        public static final int HASH_AND_CLASS_INDEX_SIZE = 1 << 22;
        private static final int FORMAT_SIZE = 1 << 5;
        private static final int PINNED_BIT_SHIFT = 30;

        public static int getClassIndex(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 0, HASH_AND_CLASS_INDEX_SIZE);
        }

        public static int getFormat(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 24, FORMAT_SIZE);
        }

        public static int getHash(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 32, HASH_AND_CLASS_INDEX_SIZE);
        }

        public static int getNumSlots(final long headerWord) {
            return MiscUtils.bitSplit(headerWord, 56, NUM_SLOTS_SIZE);
        }

        public static boolean isPinned(final long headerWord) {
            return (headerWord >> PINNED_BIT_SHIFT & 1) == 1;
        }

        public static long getHeader(final long numSlots, final long identityHash, final long format, final long classIndex) {
            assert numSlots < NUM_SLOTS_SIZE && identityHash < HASH_AND_CLASS_INDEX_SIZE && format < FORMAT_SIZE && classIndex < HASH_AND_CLASS_INDEX_SIZE;
            return numSlots << 56 | identityHash << 32 | format << 24 | classIndex;
        }

        public static long getHeader(final long numSlots, final long identityHash, final long format, final long classIndex, final boolean isPinned) {
            return getHeader(numSlots, identityHash, format, classIndex) | (isPinned ? 1 << 30 : 0);
        }
    }
}

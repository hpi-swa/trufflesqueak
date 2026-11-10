/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Objects;

/** A sequential reader with a long byte position. The caller owns the segment's lifetime. */
public final class MemorySegmentReader {
    private final MemorySegment segment;
    private final ValueLayout.OfShort shortLayout;
    private final ValueLayout.OfInt intLayout;
    private final ValueLayout.OfLong longLayout;
    private long position;

    public MemorySegmentReader(final MemorySegment segment, final ByteOrder byteOrder) {
        this.segment = Objects.requireNonNull(segment);
        shortLayout = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(byteOrder);
        intLayout = ValueLayout.JAVA_INT_UNALIGNED.withOrder(byteOrder);
        longLayout = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(byteOrder);
    }

    public long position() {
        return position;
    }

    public void position(final long newPosition) {
        if (newPosition < 0 || newPosition > segment.byteSize()) {
            throw new IllegalArgumentException("Position outside segment: " + newPosition);
        }
        position = newPosition;
    }

    public void skip(final long byteCount) {
        position(Math.addExact(position, byteCount));
    }

    public byte[] getBytes(final int numBytes) {
        final byte[] result = new byte[numBytes];
        MemorySegment.copy(segment, position, MemorySegment.ofArray(result), 0, numBytes);
        position += numBytes;
        return result;
    }

    public short getShort() {
        final short value = segment.get(shortLayout, position);
        position += Short.BYTES;
        return value;
    }

    public int getInt() {
        final int value = segment.get(intLayout, position);
        position += Integer.BYTES;
        return value;
    }

    public long getLong() {
        final long value = segment.get(longLayout, position);
        position += Long.BYTES;
        return value;
    }
}

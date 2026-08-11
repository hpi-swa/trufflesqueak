/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interpreterproxy;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import de.hpi.swa.trufflesqueak.model.NativeObject;

abstract class NativeObjectWrapper {
    final MemorySegment segment;

    NativeObjectWrapper(final MemorySegment segment) {
        this.segment = segment;
    }

    static NativeObjectWrapper from(final NativeObject nativeObject, final Arena arena) {
        if (nativeObject.isByteType()) {
            return new NativeObjectByteWrapper(nativeObject, arena);
        } else if (nativeObject.isIntType()) {
            return new NativeObjectIntWrapper(nativeObject, arena);
        } else if (nativeObject.isLongType()) {
            return new NativeObjectLongWrapper(nativeObject, arena);
        } else if (nativeObject.isShortType()) {
            return new NativeObjectShortWrapper(nativeObject, arena);
        } else {
            assert false;
            return null;
        }
    }

    abstract void copyFromSegmentToStorage();

    abstract int byteSizeOf();

    private static final class NativeObjectByteWrapper extends NativeObjectWrapper {
        private final byte[] storage;

        NativeObjectByteWrapper(final NativeObject nativeObject, final Arena arena) {
            super(arena.allocateFrom(ValueLayout.JAVA_BYTE, nativeObject.getByteStorage()));
            storage = nativeObject.getByteStorage();
        }

        @Override
        public int byteSizeOf() {
            return storage.length * Byte.BYTES;
        }

        @Override
        public void copyFromSegmentToStorage() {
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, storage, 0, storage.length);
        }
    }

    private static final class NativeObjectIntWrapper extends NativeObjectWrapper {
        private final int[] storage;

        NativeObjectIntWrapper(final NativeObject nativeObject, final Arena arena) {
            super(arena.allocateFrom(ValueLayout.JAVA_INT, nativeObject.getIntStorage()));
            storage = nativeObject.getIntStorage();
        }

        @Override
        public int byteSizeOf() {
            return storage.length * Integer.BYTES;
        }

        @Override
        public void copyFromSegmentToStorage() {
            MemorySegment.copy(segment, ValueLayout.JAVA_INT, 0, storage, 0, storage.length);
        }
    }

    private static final class NativeObjectLongWrapper extends NativeObjectWrapper {
        private final long[] storage;

        NativeObjectLongWrapper(final NativeObject nativeObject, final Arena arena) {
            super(arena.allocateFrom(ValueLayout.JAVA_LONG, nativeObject.getLongStorage()));
            storage = nativeObject.getLongStorage();
        }

        @Override
        public int byteSizeOf() {
            return storage.length * Long.BYTES;
        }

        @Override
        public void copyFromSegmentToStorage() {
            MemorySegment.copy(segment, ValueLayout.JAVA_LONG, 0, storage, 0, storage.length);
        }
    }

    private static final class NativeObjectShortWrapper extends NativeObjectWrapper {
        private final short[] storage;

        NativeObjectShortWrapper(final NativeObject nativeObject, final Arena arena) {
            super(arena.allocateFrom(ValueLayout.JAVA_SHORT, nativeObject.getShortStorage()));
            storage = nativeObject.getShortStorage();
        }

        @Override
        public int byteSizeOf() {
            return storage.length * Short.BYTES;
        }

        @Override
        public void copyFromSegmentToStorage() {
            MemorySegment.copy(segment, ValueLayout.JAVA_SHORT, 0, storage, 0, storage.length);
        }
    }
}

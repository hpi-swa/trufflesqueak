/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

final class IntStorage extends NativeObjectStorage {
    private final int[] storage;

    IntStorage(final int[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Integer.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeInts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeIntsBackAndFree(nativeAddress, storage);
    }
}

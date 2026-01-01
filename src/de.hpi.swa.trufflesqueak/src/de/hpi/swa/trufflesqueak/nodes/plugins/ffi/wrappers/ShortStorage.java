/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

final class ShortStorage extends NativeObjectStorage {
    private final short[] storage;

    ShortStorage(final short[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Short.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeShorts(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeShortsBackAndFree(nativeAddress, storage);
    }
}

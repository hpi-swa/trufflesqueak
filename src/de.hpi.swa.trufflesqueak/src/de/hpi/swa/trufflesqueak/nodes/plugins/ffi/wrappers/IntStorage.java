/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class IntStorage extends NativeObjectStorage {
    final int[] storage;

    public IntStorage(final int[] storage) {
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
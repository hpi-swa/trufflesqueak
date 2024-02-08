/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class ByteStorage extends NativeObjectStorage {
    final byte[] storage;

    public ByteStorage(final byte[] storage) {
        this.storage = storage;
    }

    @Override
    public int byteSizeOf() {
        return storage.length * Byte.BYTES;
    }

    @Override
    protected long allocate() {
        return UnsafeUtils.allocateNativeBytes(storage);
    }

    @Override
    public void cleanup() {
        UnsafeUtils.copyNativeBytesBackAndFree(nativeAddress, storage);
    }
}
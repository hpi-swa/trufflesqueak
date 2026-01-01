/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

final class ByteStorage extends NativeObjectStorage {
    private final byte[] storage;

    ByteStorage(final byte[] storage) {
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

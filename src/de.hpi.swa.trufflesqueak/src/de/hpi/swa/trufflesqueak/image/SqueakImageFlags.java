/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakImageFlags {
    @CompilationFinal private long oldBaseAddress = -1;
    @CompilationFinal private long headerFlags;
    @CompilationFinal private long snapshotScreenSize;
    @CompilationFinal private int maxExternalSemaphoreTableSize;

    public void initialize(final long oldBaseAddressValue, final long flags, final long screenSize, final int lastMaxExternalSemaphoreTableSize) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        oldBaseAddress = oldBaseAddressValue;
        headerFlags = flags;
        snapshotScreenSize = screenSize;
        maxExternalSemaphoreTableSize = lastMaxExternalSemaphoreTableSize;
    }

    public long getOldBaseAddress() {
        assert oldBaseAddress > 0;
        return oldBaseAddress;
    }

    public long getHeaderFlags() {
        return headerFlags;
    }

    // For some reason, header flags appear to be shifted by 2.
    public long getHeaderFlagsDecoded() {
        return headerFlags >> 2;
    }

    public long getSnapshotScreenSize() {
        return snapshotScreenSize;
    }

    public int getSnapshotScreenWidth() {
        return (int) snapshotScreenSize >> 16 & 0xffff;
    }

    public int getSnapshotScreenHeight() {
        return (int) snapshotScreenSize & 0xffff;
    }

    public int getMaxExternalSemaphoreTableSize() {
        return maxExternalSemaphoreTableSize;
    }
}

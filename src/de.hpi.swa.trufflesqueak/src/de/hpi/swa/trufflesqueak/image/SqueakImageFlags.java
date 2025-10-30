/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Idempotent;

public final class SqueakImageFlags {
    private static final int PRIMITIVE_DO_MIXED_ARITHMETIC = 0x100;
    private static final int PREEMPTION_DOES_NOT_YIELD = 0x010;

    @CompilationFinal private long oldBaseAddress = -1;
    @CompilationFinal private long headerFlags;
    @CompilationFinal private long snapshotScreenSize;
    @CompilationFinal private int maxExternalSemaphoreTableSize;
    @CompilationFinal private boolean isPrimitiveDoMixedArithmetic;
    @CompilationFinal private boolean preemptionYields;

    public void initialize(final long oldBaseAddressValue, final long flags, final long screenSize, final int lastMaxExternalSemaphoreTableSize) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        oldBaseAddress = oldBaseAddressValue;
        headerFlags = flags;
        snapshotScreenSize = screenSize;
        maxExternalSemaphoreTableSize = lastMaxExternalSemaphoreTableSize;
        isPrimitiveDoMixedArithmetic = (headerFlags & PRIMITIVE_DO_MIXED_ARITHMETIC) == 0;
        preemptionYields = (headerFlags & PREEMPTION_DOES_NOT_YIELD) == 0;
    }

    public long getOldBaseAddress() {
        assert oldBaseAddress > 0;
        return oldBaseAddress;
    }

    public long getHeaderFlags() {
        return headerFlags;
    }

    // For some reason, header flags appear to be shifted by 2 (see #getImageHeaderFlagsParameter).
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

    @Idempotent
    public boolean isPrimitiveDoMixedArithmetic() {
        return isPrimitiveDoMixedArithmetic;
    }

    @Idempotent
    public boolean preemptionYields() {
        return preemptionYields;
    }
}

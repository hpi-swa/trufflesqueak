/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Idempotent;

public final class SqueakImageFlags {
    private static final int NUMERIC_PRIMS_MIX_ARITHMETIC = 0x100;
    private static final int NUMERIC_PRIMS_MIX_COMPARISON = 0x800;
    private static final int PREEMPTION_DOES_NOT_YIELD = 0x010;

    @CompilationFinal private long oldBaseAddress = -1;
    @CompilationFinal private long headerFlags;
    @CompilationFinal private long snapshotScreenSize;
    @CompilationFinal private int maxExternalSemaphoreTableSize;
    @CompilationFinal private boolean numericPrimsMixArithmetic;
    @CompilationFinal private boolean numericPrimsMixComparison;
    @CompilationFinal private boolean preemptionYields;

    public void initialize(final long oldBaseAddressValue, final long flags, final long screenSize, final int lastMaxExternalSemaphoreTableSize) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        oldBaseAddress = oldBaseAddressValue;
        setHeaderFlags(flags);
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

    private void setHeaderFlags(final long headerFlags) {
        final long oldHeaderFlags = this.headerFlags;
        this.headerFlags = headerFlags;
        if (oldHeaderFlags != headerFlags) {
            /*
             * This is a trick to work around an incompatible change in OSVM: Squeak does not update
             * the header flags on startup, so initialization uses the old behavior. Cuis 7.3
             * updates the header once via #doMixedArithmetic: and since the two lower bits change,
             * the old behavior is used twice. Cuis 7.5 updates the header twice via
             * #doMixedArithmetic: and #doMixedArithmetic:, and since the two lower bits no longer
             * change after the second update, the new behavior is used.
             */
            numericPrimsUpdateOldBehavior();
        } else {
            numericPrimsUpdateNewBehavior();
        }
        preemptionYields = (headerFlags & PREEMPTION_DOES_NOT_YIELD) == 0;
    }

    // For some reason, header flags appear to be shifted by 2 (see #getImageHeaderFlagsParameter).
    public long getHeaderFlagsDecoded() {
        return headerFlags >> 2;
    }

    public void setHeaderFlagsEncoded(final long headerFlags) {
        setHeaderFlags(headerFlags << 2);
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

    private void numericPrimsUpdateOldBehavior() {
        numericPrimsMixArithmetic = (headerFlags & NUMERIC_PRIMS_MIX_ARITHMETIC) == 0;
        numericPrimsMixComparison = numericPrimsMixArithmetic;
    }

    private void numericPrimsUpdateNewBehavior() {
        numericPrimsMixArithmetic = (headerFlags & NUMERIC_PRIMS_MIX_ARITHMETIC) != 0;
        numericPrimsMixComparison = (headerFlags & NUMERIC_PRIMS_MIX_COMPARISON) != 0;
    }

    @Idempotent
    public boolean numericPrimsMixArithmetic() {
        return numericPrimsMixArithmetic;
    }

    @Idempotent
    public boolean numericPrimsMixComparison() {
        return numericPrimsMixComparison;
    }

    @Idempotent
    public boolean preemptionYields() {
        return preemptionYields;
    }
}

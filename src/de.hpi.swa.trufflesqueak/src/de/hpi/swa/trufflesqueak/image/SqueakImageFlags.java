/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Idempotent;

public final class SqueakImageFlags {
    private static final int NUMERIC_PRIMS_MIX_ARITHMETIC = 0x100;
    private static final int NUMERIC_PRIMS_MIX_COMPARISON = 0x800;
    private static final int PREEMPTION_DOES_NOT_YIELD = 0x010;

    @CompilationFinal private Assumption headerFlagsAssumption = Assumption.create("constant headerFlags");

    @CompilationFinal private long oldBaseAddress = -1;
    private long screenSize;

    @CompilationFinal private long headerFlags;
    @CompilationFinal private int maxExternalSemaphoreTableSize;
    @CompilationFinal private boolean numericPrimsMixArithmetic;
    @CompilationFinal private boolean numericPrimsMixComparison;
    @CompilationFinal private boolean preemptionYields;

    public void initialize(final long oldBaseAddressValue, final long flags, final long snapshotScreenSize, final int lastMaxExternalSemaphoreTableSize) {
        CompilerAsserts.neverPartOfCompilation();
        oldBaseAddress = oldBaseAddressValue;
        setHeaderFlags(flags);
        screenSize = snapshotScreenSize;
        maxExternalSemaphoreTableSize = lastMaxExternalSemaphoreTableSize;
    }

    public long getOldBaseAddress() {
        assert oldBaseAddress > 0;
        return oldBaseAddress;
    }

    public long getHeaderFlags() {
        if (!headerFlagsAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return headerFlags;
    }

    @TruffleBoundary
    private void setHeaderFlags(final long headerFlags) {
        final long oldHeaderFlags = this.headerFlags;
        this.headerFlags = headerFlags;

        final Assumption oldAssumption = this.headerFlagsAssumption;
        this.headerFlagsAssumption = Assumption.create("constant headerFlags");
        oldAssumption.invalidate();

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
        return getHeaderFlags() >> 2;
    }

    public void setHeaderFlagsEncoded(final long headerFlags) {
        setHeaderFlags(headerFlags << 2);
    }

    public long getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(final int width, final int height) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        screenSize = ((long) width << 16) | (height & 0xFFFFL);
    }

    public int getScreenWidth() {
        return (int) screenSize >> 16 & 0xffff;
    }

    public int getScreenHeight() {
        return (int) screenSize & 0xffff;
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
        if (!headerFlagsAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return numericPrimsMixArithmetic;
    }

    @Idempotent
    public boolean numericPrimsMixComparison() {
        if (!headerFlagsAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return numericPrimsMixComparison;
    }

    @Idempotent
    public boolean preemptionYields() {
        if (!headerFlagsAssumption.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return preemptionYields;
    }
}

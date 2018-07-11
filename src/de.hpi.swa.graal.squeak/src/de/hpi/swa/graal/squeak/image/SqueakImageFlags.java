package de.hpi.swa.graal.squeak.image;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakImageFlags {
    @CompilationFinal private int fullScreenFlag = 0;
    @CompilationFinal private int imageFloatsBigEndian;
    @CompilationFinal private boolean flagInterpretedMethods;
    @CompilationFinal private boolean preemptionYields;
    @CompilationFinal private boolean newFinalization;

    public void initialize(final int headerFlags) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        fullScreenFlag = headerFlags & 1;
        imageFloatsBigEndian = (headerFlags & 2) == 0 ? 1 : 0;
        flagInterpretedMethods = (headerFlags & 8) != 0;
        preemptionYields = (headerFlags & 16) == 0;
        newFinalization = (headerFlags & 64) != 0;
    }

    public int getFullScreenFlag() {
        return fullScreenFlag;
    }

    public int getImageFloatsBigEndian() {
        return imageFloatsBigEndian;
    }

    public boolean isFlagInterpretedMethods() {
        return flagInterpretedMethods;
    }

    public boolean isPreemptionYields() {
        return preemptionYields;
    }

    public boolean isNewFinalization() {
        return newFinalization;
    }
}

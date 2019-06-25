package de.hpi.swa.graal.squeak.image;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;

public final class SqueakImageFlags {
    @CompilationFinal private boolean is64bit;
    @CompilationFinal private int wordSize;
    @CompilationFinal private int fullScreenFlag = 0;
    @CompilationFinal private int imageFloatsBigEndian;
    @CompilationFinal private boolean flagInterpretedMethods;
    @CompilationFinal private boolean preemptionYields;
    @CompilationFinal private boolean newFinalization;
    @CompilationFinal private DisplayPoint lastWindowSize;
    @CompilationFinal private int maxExternalSemaphoreTableSize;

    public void initialize(final boolean is64bitImage, final int headerFlags, final int lastWindowSizeWord, final int lastMaxExternalSemaphoreTableSize) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        is64bit = is64bitImage;
        wordSize = is64bitImage ? ArrayConversionUtils.LONG_BYTE_SIZE : ArrayConversionUtils.INTEGER_BYTE_SIZE;
        fullScreenFlag = headerFlags & 1;
        imageFloatsBigEndian = (headerFlags & 2) == 0 ? 1 : 0;
        flagInterpretedMethods = (headerFlags & 8) != 0;
        preemptionYields = (headerFlags & 16) == 0;
        newFinalization = (headerFlags & 64) != 0;
        lastWindowSize = new DisplayPoint(lastWindowSizeWord >> 16 & 0xffff, lastWindowSizeWord & 0xffff);
        maxExternalSemaphoreTableSize = lastMaxExternalSemaphoreTableSize;
    }

    public boolean is64bit() {
        return is64bit;
    }

    public int wordSize() {
        return wordSize;
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

    public DisplayPoint getLastWindowSize() {
        return lastWindowSize;
    }

    public int getMaxExternalSemaphoreTableSize() {
        return maxExternalSemaphoreTableSize;
    }
}

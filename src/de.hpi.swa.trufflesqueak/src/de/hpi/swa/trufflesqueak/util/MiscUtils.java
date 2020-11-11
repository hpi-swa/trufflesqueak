/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.nodes.plugins.JPEGReadWriter2Plugin;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

public final class MiscUtils {
    /**
     * {@link ColorModel#getRGBdefault()} with alpha = 1.0. Transparency not needed at this point.
     * More importantly for the {@link JPEGReadWriter2Plugin}, {@link BufferedImage}s without alpha
     * channel can be exported as JPEG.
     */
    private static final DirectColorModel COLOR_MODEL_32BIT = new DirectColorModel(
                    32,
                    0x00ff0000,  // Red
                    0x0000ff00,  // Green
                    0x000000ff   // Blue
    );

    // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
    public static final long EPOCH_DELTA_SECONDS = (69L * 365 + 17) * 24 * 3600;
    public static final long EPOCH_DELTA_MICROSECONDS = EPOCH_DELTA_SECONDS * 1000 * 1000;
    public static final long TIME_ZONE_OFFSET_MICROSECONDS = (Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)) * 1000L;
    public static final long TIME_ZONE_OFFSET_SECONDS = TIME_ZONE_OFFSET_MICROSECONDS / 1000 / 1000;

    @CompilationFinal private static SecureRandom random;

    private MiscUtils() {
    }

    public static int bitSplit(final long value, final int offset, final int size) {
        return (int) (value >> offset & size - 1);
    }

    @TruffleBoundary
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @TruffleBoundary
    public static String format(final String format, final Object... args) {
        return String.format(format, args);
    }

    @TruffleBoundary
    public static long getCollectionCount() {
        long totalCollectionCount = 0;
        for (final GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalCollectionCount += Math.max(gcBean.getCollectionCount(), 0);
        }
        return totalCollectionCount;
    }

    @TruffleBoundary
    public static long getCollectionTime() {
        long totalCollectionTime = 0;
        for (final GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalCollectionTime += Math.max(gcBean.getCollectionTime(), 0);
        }
        return totalCollectionTime;
    }

    @TruffleBoundary
    public static String getGraalVMInformation() {
        final String graalVMVersion = System.getProperty("graalvm.version", "");
        if (graalVMVersion.isEmpty()) {
            return ""; // No information available; not running on GraalVM.
        }
        final String graalVMHome = System.getProperty("graalvm.home", "n/a");
        return String.format("GRAAL_VERSION=%s\nGRAAL_HOME=%s", graalVMVersion, graalVMHome);
    }

    @TruffleBoundary
    public static long getHeapMemoryMax() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
    }

    @TruffleBoundary
    public static long getHeapMemoryUsed() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    @TruffleBoundary
    public static String getJavaClassPath() {
        return System.getProperty("java.class.path");
    }

    @TruffleBoundary
    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    @TruffleBoundary
    public static long getObjectPendingFinalizationCount() {
        return ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount();
    }

    public static SecureRandom getSecureRandom() {
        /* SecureRandom must be initialized at (native image) runtime. */
        if (random == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            random = new SecureRandom();
        }
        return random;
    }

    @TruffleBoundary
    public static long getStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    @TruffleBoundary
    public static String getSystemProperties() {
        final Properties properties = System.getProperties();
        final StringBuilder sb = new StringBuilder(256);
        sb.append("\n\n== System Properties =================================>\n");
        final Object[] keys = properties.keySet().toArray();
        Arrays.sort(keys);
        for (final Object systemKey : keys) {
            final String key = (String) systemKey;
            sb.append(String.format("%s = %s\n", key, System.getProperty(key, "n/a")));
        }
        sb.append("<= System Properties ===================================\n\n");
        return sb.toString();
    }

    @TruffleBoundary
    public static long getTotalCompilationTime() {
        final CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
        if (compilationBean.isCompilationTimeMonitoringSupported()) {
            return compilationBean.getTotalCompilationTime();
        } else {
            return -1L;
        }
    }

    @TruffleBoundary
    public static long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    @TruffleBoundary
    public static String getVMInformation() {
        final String jre;
        if (System.getProperty("java.version").startsWith("1.8")) {
            jre = "jre" + File.separator;
        } else {
            jre = "";
        }
        final File releaseFile = new File(System.getProperty("java.home") + jre + File.separator + "languages" + File.separator + SqueakLanguageConfig.ID + File.separator + "release");
        if (releaseFile.canRead()) {
            final Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(releaseFile));
            } catch (final IOException e) {
                e.printStackTrace();
                throw SqueakException.create("Could not read release file.", e);
            }
            final String source = properties.getProperty("SOURCE", "unknown source").replaceAll("\"", "");
            final String graalVMVersion = properties.getProperty("GRAALVM_VERSION", "unknown GraalVM version").replaceAll("\"", "");
            final String javaVersion = properties.getProperty("JAVA_VERSION", "unknown Java version").replaceAll("\"", "");
            final String osName = properties.getProperty("OS_NAME", "unknown os name").replaceAll("\"", "");
            final String osArch = properties.getProperty("OS_ARCH", "unknown os arch").replaceAll("\"", "");
            final String commitInfo = properties.getProperty("COMMIT_INFO", "unknown commit").replaceAll("\"", "");
            return String.format("%s\nbuilt for GraalVM %s (Java %s, %s, %s)\n%s", source, graalVMVersion, javaVersion, osName, osArch, commitInfo);
        } else {
            return String.format("\n%s (%s; %s)\n", System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vm.info"));
        }
    }

    @TruffleBoundary
    public static String getVMPath() {
        final String binaryName = OSDetector.SINGLETON.isWindows() ? "java.exe" : "java";
        return System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + binaryName;
    }

    public static boolean isBlank(final String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /* Wraps bitmap in a BufferedImage for efficient drawing. */
    @TruffleBoundary
    public static BufferedImage new32BitBufferedImage(final int[] words, final int width, final int height) {
        final SampleModel sm = COLOR_MODEL_32BIT.createCompatibleSampleModel(width, height);
        final DataBufferInt db = new DataBufferInt(words, words.length);
        final WritableRaster raster = Raster.createWritableRaster(sm, db, null);
        return new BufferedImage(COLOR_MODEL_32BIT, raster, true, null);
    }

    @TruffleBoundary
    public static long runtimeFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    @TruffleBoundary
    public static long runtimeMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    @TruffleBoundary
    public static long runtimeTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    @TruffleBoundary
    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @TruffleBoundary
    public static byte[] stringToBytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @TruffleBoundary
    public static int[] stringToCodePointsArray(final String value) {
        return value.codePoints().toArray();
    }

    @TruffleBoundary
    public static String stringValueOf(final char value) {
        return String.valueOf(value);
    }

    @TruffleBoundary
    public static void systemGC() {
        System.gc();
    }

    @TruffleBoundary
    public static byte[] toBytes(final String value) {
        return value.getBytes();
    }

    /** Similar to {@link Math#toIntExact(long)}, but uses an assertion. */
    public static int toIntExact(final long value) {
        assert (int) value == value;
        return (int) value;
    }

    public static long toJavaMicrosecondsUTC(final long microseconds) {
        return microseconds - EPOCH_DELTA_MICROSECONDS;
    }

    public static long toSqueakMicrosecondsLocal(final long microseconds) {
        return toSqueakMicrosecondsUTC(microseconds) + TIME_ZONE_OFFSET_MICROSECONDS;
    }

    public static long toSqueakMicrosecondsUTC(final long microseconds) {
        return microseconds + EPOCH_DELTA_MICROSECONDS;
    }

    public static long toSqueakSecondsLocal(final long seconds) {
        return seconds + EPOCH_DELTA_SECONDS + TIME_ZONE_OFFSET_SECONDS;
    }

    @TruffleBoundary
    public static String toString(final Object value) {
        return value.toString();
    }
}

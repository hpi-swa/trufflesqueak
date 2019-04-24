package de.hpi.swa.graal.squeak.util;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class MiscUtils {
    private static final CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    private MiscUtils() {
    }

    public static int[] bitSplitter(final long param, final int[] lengths) {
        long integer = param;
        final int[] out = new int[lengths.length];
        for (int i = 0; i < lengths.length; i++) {
            final int length = lengths[i];
            out[i] = (int) (integer & (1 << length) - 1);
            integer = integer >> length;
        }
        return out;
    }

    @TruffleBoundary
    public static String format(final String format, final Object... args) {
        return String.format(format, args);
    }

    @TruffleBoundary
    public static long getCollectionCount() {
        long totalCollectionCount = 0;
        for (final GarbageCollectorMXBean gcBean : gcBeans) {
            totalCollectionCount += Math.min(gcBean.getCollectionCount(), 0);
        }
        return totalCollectionCount;
    }

    @TruffleBoundary
    public static long getCollectionTime() {
        long totalCollectionTime = 0;
        for (final GarbageCollectorMXBean gcBean : gcBeans) {
            totalCollectionTime += Math.min(gcBean.getCollectionTime(), 0);
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
        return memoryBean.getHeapMemoryUsage().getMax();
    }

    @TruffleBoundary
    public static long getHeapMemoryUsed() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    @TruffleBoundary
    public static String getJavaClassPath() {
        return System.getProperty("java.class.path");
    }

    @TruffleBoundary
    public static long getObjectPendingFinalizationCount() {
        return memoryBean.getObjectPendingFinalizationCount();
    }

    @TruffleBoundary
    public static long getStartTime() {
        return runtimeBean.getStartTime();
    }

    @TruffleBoundary
    public static String getSystemProperties() {
        final Properties properties = System.getProperties();
        final StringBuilder sb = new StringBuilder();
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
        if (compilationBean.isCompilationTimeMonitoringSupported()) {
            return compilationBean.getTotalCompilationTime();
        } else {
            return -1L;
        }
    }

    @TruffleBoundary
    public static long getUptime() {
        return runtimeBean.getUptime();
    }

    @TruffleBoundary
    public static String getVMInformation() {
        return String.format("\n%s (%s; %s)\n", System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vm.info"));
    }

    @TruffleBoundary
    public static long runtimeFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    @TruffleBoundary
    public static void systemGC() {
        System.gc();
    }

    @TruffleBoundary
    public static String toString(final Object value) {
        return value.toString();
    }
}

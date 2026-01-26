/*
 * Copyright (c) 2026-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.time.Instant;
import java.util.Calendar;

public final class TimeUtils {

    // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
    private static final long EPOCH_DELTA_SECONDS = (69L * 365 + 17) * 24 * 3600;
    private static final long TIME_ZONE_OFFSET_MICROSECONDS = (Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)) * 1000L;
    private static final long TIME_ZONE_OFFSET_SECONDS = TIME_ZONE_OFFSET_MICROSECONDS / 1000 / 1000;

    private static final Instant STARTUP_INSTANT = Instant.now().plusSeconds(EPOCH_DELTA_SECONDS);
    private static final long START_UP_MILLIS = System.currentTimeMillis();
    private static final long START_UP_NANOS = System.nanoTime();

    private TimeUtils() {
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long toSqueakMicrosecondsLocal(final long microsecondsUTC) {
        return microsecondsUTC + TIME_ZONE_OFFSET_MICROSECONDS;
    }

    public static long toSqueakSecondsLocal(final long seconds) {
        return seconds + EPOCH_DELTA_SECONDS + TIME_ZONE_OFFSET_SECONDS;
    }

    public static long startUpMicrosecondsUTC() {
        return toMicroseconds(STARTUP_INSTANT);
    }

    public static long currentMicrosecondsUTC() {
        return toMicroseconds(STARTUP_INSTANT.plusNanos(elapsedNanos()));
    }

    public static long elapsedMillis() {
        return currentTimeMillis() - START_UP_MILLIS;
    }

    private static long elapsedNanos() {
        return System.nanoTime() - START_UP_NANOS;
    }

    private static long toMicroseconds(final Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000;
    }
}

/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerAsserts;

/*
 * Counts how often a primitive has failed in a certain time window and indicates whether this
 * node should continue to send the primitive eagerly or not. This is useful to avoid
 * rewriting primitives that set up the image and then are retried in their fallback code
 * (e.g. primitiveCopyBits).
 */
public final class PrimitiveFailedCounter {
    private static final int MAX_NUM_FAILURES = 3;
    private static final int TIME_WINDOW_MILLIS = 1_000;
    private long lastCheckMillis = System.currentTimeMillis();
    private int count;

    public static PrimitiveFailedCounter create() {
        return new PrimitiveFailedCounter();
    }

    public boolean shouldNoLongerSendEagerly() {
        CompilerAsserts.neverPartOfCompilation();
        if (System.currentTimeMillis() - lastCheckMillis > TIME_WINDOW_MILLIS) {
            count = 0; // reset
        }
        lastCheckMillis = System.currentTimeMillis();
        return ++count > MAX_NUM_FAILURES;
    }
}

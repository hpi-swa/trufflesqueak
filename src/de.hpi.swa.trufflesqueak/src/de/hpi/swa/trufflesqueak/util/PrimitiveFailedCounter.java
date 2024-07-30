/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.Node;

/*
 * Counts how often a primitive has failed in a certain time window and indicates whether this
 * node should continue to send the primitive eagerly or not. This is useful to avoid
 * rewriting primitives that set up the image and then are retried in their fallback code
 * (e.g. primitiveCopyBits).
 */
public final class PrimitiveFailedCounter {
    private static final int MAX_NUM_FAILURES = 3;
    private static final int TIME_WINDOW_MILLIS = 1_000;

    // Use an assumption to avoid invalidating primitive code in rare failures on slow path.
    private final Assumption assumption;

    private long lastCheckMillis = System.currentTimeMillis();
    private int count;

    public PrimitiveFailedCounter(final Node originNode) {
        assumption = Truffle.getRuntime().createAssumption(originNode.getClass().getSimpleName());
    }

    public static PrimitiveFailedCounter create(final Node originNode) {
        return originNode != null ? new PrimitiveFailedCounter(originNode) : null;
    }

    public boolean shouldRewriteToCall() {
        CompilerAsserts.neverPartOfCompilation();
        assert assumption.isValid();
        if (System.currentTimeMillis() - lastCheckMillis > TIME_WINDOW_MILLIS) {
            count = 0; // reset
        }
        lastCheckMillis = System.currentTimeMillis();
        if (++count > MAX_NUM_FAILURES) {
            assumption.invalidate("failed too often");
            return true;
        } else {
            return false;
        }
    }

    public Assumption getAssumption() {
        return assumption;
    }
}

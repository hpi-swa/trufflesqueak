/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

/*
 * Counts how often a primitive has failed and indicates whether this node should continue to
 * send the primitive eagerly or not. This is useful to avoid rewriting primitives that set up
 * the image and then are retried in their fallback code (e.g. primitiveCopyBits).
 */
public final class PrimitiveFailedCounter {
    private static final int PRIMITIVE_FAILED_THRESHOLD = 3;
    private int count;

    public static PrimitiveFailedCounter create() {
        return new PrimitiveFailedCounter();
    }

    public boolean shouldNoLongerSendEagerly() {
        return ++count > PRIMITIVE_FAILED_THRESHOLD;
    }
}

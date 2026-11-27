/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.junit.BeforeClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;

/**
 * Runs several contexts, one after another and at the same time, each with its own engine or with
 * a shared engine. State of one context must neither leak into nor break another one.
 */
public final class SqueakMultiContextTest {
    private static final String ID = SqueakLanguageConfig.ID;
    private static final int TIMEOUT_SECONDS = 300;
    private static String imagePath;

    @BeforeClass
    public static void findTestImage() {
        imagePath = AbstractSqueakTestCaseWithImage.getPathToTestImage();
    }

    @Test
    public void testSequentialContexts() throws Exception {
        for (int i = 0; i < 2; i++) {
            try (Context context = newContext(null)) {
                assertIndependentContext(context, "sequential" + i, null);
            }
        }
    }

    @Test
    public void testSequentialContextsWithSharedEngine() throws Exception {
        try (Engine engine = Engine.create(ID)) {
            for (int i = 0; i < 2; i++) {
                try (Context context = newContext(engine)) {
                    assertIndependentContext(context, "sequential" + i, null);
                }
            }
        }
    }

    @Test
    public void testConcurrentContexts() throws Exception {
        runConcurrently(null);
    }

    @Test
    public void testConcurrentContextsWithSharedEngine() throws Exception {
        try (Engine engine = Engine.create(ID)) {
            runConcurrently(engine);
        }
    }

    private static void runConcurrently(final Engine engine) throws Exception {
        final int numContexts = 2;
        final CyclicBarrier barrier = new CyclicBarrier(numContexts);
        final ExecutorService executor = Executors.newFixedThreadPool(numContexts);
        try {
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numContexts; i++) {
                final String key = "concurrent" + i;
                futures.add(executor.submit(() -> {
                    try (Context context = newContext(engine)) {
                        assertIndependentContext(context, key, barrier);
                    }
                    return null;
                }));
            }
            for (final Future<?> future : futures) {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static Context newContext(final Engine engine) {
        final Context.Builder builder = Context.newBuilder(ID).allowAllAccess(true);
        builder.option(ID + "." + SqueakLanguageOptions.IMAGE_PATH, imagePath);
        builder.option(ID + "." + SqueakLanguageOptions.HEADLESS, "true");
        builder.option(ID + "." + SqueakLanguageOptions.TESTING, "true");
        if (engine != null) {
            builder.engine(engine);
        }
        return builder.build();
    }

    /* Defines a global with a context-specific value and checks that it stays unaffected. */
    private static void assertIndependentContext(final Context context, final String key, final CyclicBarrier barrier) throws Exception {
        assertFalse(context.eval(ID, "Smalltalk includesKey: #TsMultiContextProbe").asBoolean());
        context.eval(ID, "Smalltalk at: #TsMultiContextProbe put: '" + key + "'");
        if (barrier != null) {
            barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS); // All contexts have defined their global.
        }
        assertEquals(5050, context.eval(ID, "(1 to: 100) inject: 0 into: [:a :b | a + b]").asInt());
        assertEquals(key, context.eval(ID, "Smalltalk at: #TsMultiContextProbe").asString());
    }
}

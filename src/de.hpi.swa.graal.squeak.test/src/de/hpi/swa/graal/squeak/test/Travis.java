/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.LongSupplier;

public final class Travis {

    private static final Travis INSTANCE = new Travis();

    private final PrintStream out;

    private final Deque<Fold> folds = new ConcurrentLinkedDeque<>();

    private Travis() {
        out = enabled() ? System.out : new NullPrintStream();
    }

    private static boolean enabled() {
        final String env = System.getenv("TRAVIS");
        return env != null && !env.isEmpty();
    }

    private static class NullPrintStream extends PrintStream {

        NullPrintStream() {
            super(new OutputStream() {

                @Override
                public void write(final int b) throws IOException {
                    // Do nothing.
                }
            });
        }

    }

    protected static Travis get() {
        return INSTANCE;
    }

    protected void begin(final String stage, final String title) {
        final Fold fold = Fold.of(stage);
        printBegin(title, fold);
        folds.push(fold);
    }

    private void printBegin(final String title, final Fold fold) {
        out.print("travis_fold:start:" + fold.stage + AnsiCodes.CR + AnsiCodes.CLEAR);
        out.print("travis_time:start:" + fold.timerId + AnsiCodes.CR + AnsiCodes.CLEAR);
        out.println(AnsiCodes.BOLD + AnsiCodes.BLUE + title + AnsiCodes.RESET);
    }

    protected void end() {
        printEnd(folds.pop().completed());
    }

    private void printEnd(final Fold fold) {
        out.printf("\ntravis_time:end:%s:start=%d,finish=%d,duration=%d" + AnsiCodes.CR + AnsiCodes.CLEAR,
                        fold.timerId, fold.created, fold.finished, fold.duration());
        out.print("travis_fold:end:" + fold.stage + AnsiCodes.CR + AnsiCodes.CLEAR);
    }

    private static String newTimerId() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    protected static final class Fold {

        private static final LongSupplier CLOCK = System::nanoTime;

        private final String stage;
        private final String timerId;
        private final long created;
        private final long finished;

        private Fold(final String stage, final String timerId, final long created, final long finished) {
            this.stage = stage;
            this.timerId = timerId;
            this.created = created;
            this.finished = finished;
        }

        private static Fold of(final String stage) {
            return new Fold(stage, newTimerId(), CLOCK.getAsLong(), -1);
        }

        private Fold completed() {
            return new Fold(stage, timerId, created, CLOCK.getAsLong());
        }

        private long duration() {
            return finished - created;
        }
    }

    protected static class AnsiCodes {
        protected static final String BOLD = "\033[1m";
        protected static final String RED = "\033[31;1m";
        protected static final String GREEN = "\033[32;1m";
        protected static final String BLUE = "\033[34m";
        protected static final String YELLOW = "\033[33;1m";
        protected static final String RESET = "\033[0m";
        protected static final String CLEAR = "\033[0K";
        protected static final String CR = "\r";
    }
}

package de.hpi.swa.graal.squeak.test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.LongSupplier;

final class Travis {

    private static final Travis INSTANCE = new Travis();

    private final PrintStream out;

    private Travis() {
        this.out = enabled() ? System.out : nullPrintStream();
    }

    private static boolean enabled() {
        final String env = System.getenv("TRAVIS");
        return env != null && !env.isEmpty();
    }

    private static PrintStream nullPrintStream() {
        try {
            // waits for OutputStream#nullOutputStream() (introduced in Java 11)
            return new PrintStream("/dev/null");
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Travis get() {
        return INSTANCE;
    }

    private final Deque<Fold> folds = new ConcurrentLinkedDeque<>();

    void begin(final String stage, final String title) {
        final Fold fold = Fold.of(stage);
        printBegin(title, fold);
        folds.push(fold);
    }

    private void printBegin(final String title, final Fold fold) {
        out.print("travis_fold:start:" + fold.stage + AnsiCodes.CR + AnsiCodes.CLEAR);
        out.print("travis_time:start:" + fold.timerId + AnsiCodes.CR + AnsiCodes.CLEAR);
        out.println(AnsiCodes.BOLD + AnsiCodes.BLUE + title + AnsiCodes.RESET);
    }

    void end() {
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

    static final class Fold {

        private static final LongSupplier CLOCK = System::nanoTime;

        final String stage;
        final String timerId;
        final long created;
        final long finished;

        private Fold(final String stage, final String timerId, final long created,
                        final long finished) {

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

        long duration() {
            return finished - created;
        }
    }

    static class AnsiCodes {
        static final String BOLD = "\033[1m";
        static final String RED = "\033[31;1m";
        static final String GREEN = "\033[32;1m";
        static final String BLUE = "\033[34m";
        static final String YELLOW = "\033[33;1m";
        static final String RESET = "\033[0m";
        static final String CLEAR = "\033[0K";
        static final String CR = "\r";
    }
}

package de.hpi.swa.graal.squeak.util;

import java.io.PrintWriter;

public class StopWatch {
    long startTime = 0;
    long stopTime = 0;
    public final String name;

    StopWatch() {
        this.name = "stopwatch";
    }

    StopWatch(final String name) {
        this.name = name;
    }

    public static StopWatch start(final String name) {
        final StopWatch watch = new StopWatch(name);
        watch.start();
        return watch;
    }

    public void start() {
        startTime = System.nanoTime();
    }

    public long stop() {
        stopTime = System.nanoTime();
        return stopTime - startTime;
    }

    long delta() {
        return stopTime - startTime;
    }

    public void printTime() {
        final double deltaf = (delta() / 1000_000) / 1000.0;
        final PrintWriter output = new PrintWriter(System.out, true);
        output.println(name + ":\t" + deltaf + "s");
    }

    public void stopAndPrint() {
        stop();
        printTime();
    }
}

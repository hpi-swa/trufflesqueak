package de.hpi.swa.graal.squeak.util;

import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class StopWatch {
    long startTime = 0;
    long stopTime = 0;
    public final String name;

    public StopWatch() {
        this.name = "stopwatch";
    }

    public StopWatch(final String name) {
        this.name = name;
    }

    public static StopWatch start(final String name) {
        final StopWatch watch = new StopWatch(name);
        watch.start();
        return watch;
    }

    @TruffleBoundary
    public void start() {
        startTime = System.nanoTime();
    }

    @TruffleBoundary
    public long stop() {
        stopTime = System.nanoTime();
        return stopTime - startTime;
    }

    long delta() {
        return stopTime - startTime;
    }

    @TruffleBoundary
    public void printTime() {
        final double deltaf = (delta() / 1000_000.0) / 1000.0;
        print(name + ":\t" + deltaf + "s");
    }

    @TruffleBoundary
    public void printTimeMS() {
        final double deltaf = delta() / 1000_000.0;
        print(name + ":\t" + deltaf + "ms");
    }

    public void printTimeNS() {
        print(name + ":\t" + delta() + "ns");
    }

    public void stopAndPrint() {
        stop();
        printTime();
    }

    public void stopAndPrintMS() {
        stop();
        printTimeMS();
    }

    public void stopAndPrintNS() {
        stop();
        printTimeNS();
    }

    @TruffleBoundary
    private static void print(final String str) {
        final PrintWriter output = new PrintWriter(System.out, true);
        output.println(str);
    }
}

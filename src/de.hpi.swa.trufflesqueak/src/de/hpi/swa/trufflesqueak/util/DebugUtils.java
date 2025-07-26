/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

/**
 * Helper functions for debugging purposes.
 */
public final class DebugUtils {
    public static final boolean UNDER_DEBUG = isDebugging(ManagementFactory.getRuntimeMXBean().getInputArguments());

    private static boolean isDebugging(final List<String> arguments) {
        for (final String argument : arguments) {
            if ("-Xdebug".equals(argument)) {
                return true;
            } else if (argument.startsWith("-agentlib:jdwp")) {
                return true;
            }
        }
        return false;
    }

    public static void dumpState() {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        MiscUtils.systemGC();
        final StringBuilder sb = new StringBuilder("Thread dump");
        dumpThreads(sb);
        println(sb.toString());
        println(currentState());
    }

    public static void dumpThreads(final StringBuilder sb) {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        sb.append("\r\n\r\n\r\nTotal number of threads started: ").append(ManagementFactory.getThreadMXBean().getTotalStartedThreadCount()).append("\r\n\r\n");

        final Runtime r = Runtime.getRuntime();
        sb.append("Total Memory : ").append(r.totalMemory()).append("\r\nMax Memory   : ").append(r.maxMemory()).append("\r\nFree Memory  : ").append(r.freeMemory()).append("\r\n\r\n");

        final ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        for (final ThreadInfo info : threads) {
            sb.append('"').append(info.getThreadName()).append("\" Id=").append(info.getThreadId()).append(' ').append(info.getThreadState());
            if (info.getLockName() != null) {
                sb.append(" on ").append(info.getLockName());
            }
            if (info.getLockOwnerName() != null) {
                sb.append(" owned by \"").append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
            }
            if (info.isSuspended()) {
                sb.append(" (suspended)");
            }
            if (info.isInNative()) {
                sb.append(" (in native)");
            }
            sb.append("\r\n");
            int i = 0;
            for (; i < info.getStackTrace().length; i++) {
                final StackTraceElement ste = info.getStackTrace()[i];
                sb.append("\tat ").append(ste.toString());
                sb.append("\r\n");
                if (i == 0 && info.getLockInfo() != null) {
                    final Thread.State ts = info.getThreadState();
                    switch (ts) {
                        case BLOCKED -> sb.append("\t-  blocked on ").append(info.getLockInfo()).append("\r\n");
                        case WAITING, TIMED_WAITING -> sb.append("\t-  waiting on ").append(info.getLockInfo()).append("\r\n");
                        default -> {
                        }
                    }
                }

                for (final MonitorInfo mi : info.getLockedMonitors()) {
                    if (mi.getLockedStackDepth() == i) {
                        sb.append("\t-  locked ").append(mi).append("\r\n");
                    }
                }
            }
            if (i < info.getStackTrace().length) {
                sb.append("\t...\r\n");
            }

            final LockInfo[] locks = info.getLockedSynchronizers();
            if (locks.length > 0) {
                sb.append("\r\n\tNumber of locked synchronizers = ").append(locks.length).append("\r\n");
                for (final LockInfo li : locks) {
                    sb.append("\t- ").append(li).append("\r\n");
                }
            }

            sb.append("\r\n\r\n");
        }
    }

    public static String currentState() {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        final SqueakImageContext image = SqueakImageContext.getSlow();
        final StringBuilder b = new StringBuilder(64);
        b.append("\nImage processes state\n");
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final long activePriority = (long) activeProcess.instVarAt0Slow(PROCESS.PRIORITY);
        b.append("*Active process @").append(Integer.toHexString(activeProcess.hashCode())).append(" priority ").append(activePriority).append('\n');
        final Object interruptSema = image.getSpecialObject(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        printSemaphoreOrNil(b, "*Interrupt semaphore @", interruptSema, true);
        final Object timerSema = image.getSpecialObject(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE);
        printSemaphoreOrNil(b, "*Timer semaphore @", timerSema, true);
        final Object finalizationSema = image.getSpecialObject(SPECIAL_OBJECT.THE_FINALIZATION_SEMAPHORE);
        printSemaphoreOrNil(b, "*Finalization semaphore @", finalizationSema, true);
        final Object lowSpaceSema = image.getSpecialObject(SPECIAL_OBJECT.THE_LOW_SPACE_SEMAPHORE);
        printSemaphoreOrNil(b, "*Low space semaphore @", lowSpaceSema, true);
        final ArrayObject externalObjects = (ArrayObject) image.getSpecialObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY);
        if (!externalObjects.isEmptyType()) {
            final Object[] semaphores = externalObjects.getObjectStorage();
            for (int i = 0; i < semaphores.length; i++) {
                printSemaphoreOrNil(b, "*External semaphore at index " + (i + 1) + " @", semaphores[i], false);
            }
        }
        final Object[] lists = ((ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS)).getObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            printLinkedList(b, "*Quiescent processes list at priority " + (i + 1), (PointersObject) lists[i]);
        }
        return b.toString();
    }

    public static void printSqStackTrace() {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        final boolean isCIBuild = System.getenv().containsKey("GITHUB_ACTIONS");
        final int[] depth = new int[1];
        final Object[] lastSender = {null};
        final TruffleLogger log = LogUtils.DEBUG;
        log.info("== Truffle stack trace ===========================================================");
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            if (depth[0]++ > 50 && isCIBuild) {
                return null;
            }
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!FrameAccess.isTruffleSqueakFrame(current)) {
                return null;
            }
            final CompiledCodeObject code = FrameAccess.getCodeObject(current);
            lastSender[0] = FrameAccess.getSender(current);
            final Object marker = FrameAccess.getMarker(current);
            final Object context = FrameAccess.getContext(current);
            final String prefix = FrameAccess.hasClosure(current) ? "[] in " : "";
            final String argumentsString = ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(current));
            log.info(MiscUtils.format("%s%s #(%s) [marker: %s, context: %s, sender: %s]", prefix, code, argumentsString, marker, context, lastSender[0]));
            return null;
        });
        if (lastSender[0] instanceof final ContextObject c) {
            log.info("== Squeak frames ================================================================");
            printSqStackTrace(c);
        }
    }

    public static void printSqStackTrace(final ContextObject context) {
        LogUtils.DEBUG.info(() -> getSqStackTrace(context));
    }

    public static String getSqStackTrace(final ContextObject context) {
        CompilerAsserts.neverPartOfCompilation("For debugging purposes only");
        final StringBuilder b = new StringBuilder();
        printSqMaterializedStackTraceOn(b, context);
        return b.toString();
    }

    private static void printSemaphoreOrNil(final StringBuilder b, final String label, final Object semaphoreOrNil, final boolean printIfNil) {
        if (semaphoreOrNil instanceof final PointersObject semaphore) {
            b.append(label).append(Integer.toHexString(semaphore.hashCode())).append(" with ").append(semaphore.instVarAt0Slow(SEMAPHORE.EXCESS_SIGNALS)).append(" excess signals");
            if (!printLinkedList(b, "", semaphore)) {
                b.append(" and no processes\n");
            }
        } else {
            if (printIfNil) {
                b.append(label).append(" is nil\n");
            }
        }
    }

    private static boolean printLinkedList(final StringBuilder b, final String label, final PointersObject linkedList) {
        Object temp = linkedList.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
        if (temp instanceof final PointersObject obj) {
            b.append(label).append(" and process");
            if (obj != linkedList.instVarAt0Slow(LINKED_LIST.LAST_LINK)) {
                b.append("es:\n");
            } else {
                b.append(":\n");
            }
            while (temp instanceof final PointersObject aProcess) {
                final Object aContext = aProcess.instVarAt0Slow(PROCESS.SUSPENDED_CONTEXT);
                if (aContext instanceof final ContextObject c) {
                    b.append("\tprocess @").append(Integer.toHexString(aProcess.hashCode())).append(" with suspended context ").append(aContext).append(" and stack trace:\n");
                    printSqMaterializedStackTraceOn(b, c);
                } else {
                    b.append("\tprocess @").append(Integer.toHexString(aProcess.hashCode())).append(" with suspended context nil\n");
                }
                temp = aProcess.instVarAt0Slow(PROCESS.NEXT_LINK);
            }
            return true;
        } else {
            return false;
        }
    }

    private static void printSqMaterializedStackTraceOn(final StringBuilder b, final ContextObject context) {
        ContextObject current = context;
        while (current != null && current.hasTruffleFrame()) {
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments();
            b.append(MiscUtils.format("%s #(%s) [%s]", current, ArrayUtils.toJoinedString(", ", rcvrAndArgs), current.getFrameMarker())).append('\n');
            final Object sender = current.getFrameSender();
            if (sender == NilObject.SINGLETON) {
                break;
            } else if (sender instanceof FrameMarker) {
                b.append(sender).append('\n');
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }

    private static void println(final Object object) {
        // Checkstyle: stop
        System.out.println(object);
        // Checkstyle: resume
    }
}

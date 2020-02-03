package de.hpi.swa.graal.squeak.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.sun.management.DiagnosticCommandMBean;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import sun.management.ManagementFactoryHelper;

public class DebugUtils {
    private static final AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
    /*
     * Helper functions for debugging purposes.
     */

    public static void printSqMaterializedStackTraceOn(final StringBuilder b, final ContextObject context) {
        ContextObject current = context;
        while (current != null && current.hasTruffleFrame()) {
            final Object[] rcvrAndArgs = current.getReceiverAndNArguments(current.getBlockOrMethod().getNumArgsAndCopied());
            b.append(MiscUtils.format("%s #(%s) [%s]", current, ArrayUtils.toJoinedString(", ", rcvrAndArgs), current.getFrameMarker()));
            b.append('\n');
            final Object sender = current.getFrameSender();
            if (sender == NilObject.SINGLETON) {
                break;
            } else if (sender instanceof FrameMarker) {
                b.append(sender);
                b.append('\n');
                break;
            } else {
                current = (ContextObject) sender;
            }
        }
    }

    public static void dumpState(final SqueakImageContext image) {
        MiscUtils.gc();
        final StringBuilder sb = new StringBuilder("Thread dump");
        dumpThreads(sb);
        System.err.println(sb.toString());
        if (image != null) {
            try {
                System.err.println(currentState(image));
            } catch (final RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public static void dumpHeap() {
        try {
            ManagementFactoryHelper.getDiagnosticMXBean().dumpHeap(".." + FileSystems.getDefault().getSeparator() + System.currentTimeMillis() + ".hprof", true);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @TruffleBoundary
    public static void dumpThreads(final StringBuilder sb) {
        sb.append("\r\n\r\n\r\n");
        sb.append("Total number of threads started: ");
        sb.append(ManagementFactory.getThreadMXBean().getTotalStartedThreadCount());
        sb.append("\r\n\r\n");

        final Runtime r = Runtime.getRuntime();
        sb.append("Total Memory : ");
        sb.append(r.totalMemory());
        sb.append("\r\n");
        sb.append("Max Memory   : ");
        sb.append(r.maxMemory());
        sb.append("\r\n");
        sb.append("Free Memory  : ");
        sb.append(r.freeMemory());
        sb.append("\r\n\r\n");

        final ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        for (final ThreadInfo info : threads) {
            sb.append("\"" + info.getThreadName() + "\" Id=" + info.getThreadId() + " " + info.getThreadState());
            if (info.getLockName() != null) {
                sb.append(" on " + info.getLockName());
            }
            if (info.getLockOwnerName() != null) {
                sb.append(" owned by \"" + info.getLockOwnerName() + "\" Id=" + info.getLockOwnerId());
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
                sb.append("\tat " + ste.toString());
                sb.append("\r\n");
                if (i == 0 && info.getLockInfo() != null) {
                    final Thread.State ts = info.getThreadState();
                    switch (ts) {
                        case BLOCKED:
                            sb.append("\t-  blocked on " + info.getLockInfo());
                            sb.append("\r\n");
                            break;
                        case WAITING:
                            sb.append("\t-  waiting on " + info.getLockInfo());
                            sb.append("\r\n");
                            break;
                        case TIMED_WAITING:
                            sb.append("\t-  waiting on " + info.getLockInfo());
                            sb.append("\r\n");
                            break;
                        default:
                    }
                }

                for (final MonitorInfo mi : info.getLockedMonitors()) {
                    if (mi.getLockedStackDepth() == i) {
                        sb.append("\t-  locked " + mi);
                        sb.append("\r\n");
                    }
                }
            }
            if (i < info.getStackTrace().length) {
                sb.append("\t...");
                sb.append("\r\n");
            }

            final LockInfo[] locks = info.getLockedSynchronizers();
            if (locks.length > 0) {
                sb.append("\r\n\tNumber of locked synchronizers = " + locks.length);
                sb.append("\r\n");
                for (final LockInfo li : locks) {
                    sb.append("\t- " + li);
                    sb.append("\r\n");
                }
            }

            sb.append("\r\n\r\n");
        }
    }

    @TruffleBoundary
    public static void printSqStackTrace(final ContextObject context) {
        final StringBuilder b = new StringBuilder();
        printSqMaterializedStackTraceOn(b, context);
        context.image.getOutput().println(b.toString());
    }

    public static String currentState(final SqueakImageContext image) {
        final StringBuilder b = new StringBuilder();
        b.append("\nImage processes state\n");
        final PointersObject activeProcess = image.getActiveProcess(pointersReadNode);
        final long activePriority = pointersReadNode.executeLong(activeProcess, PROCESS.PRIORITY);
        b.append("*Active process @");
        b.append(Integer.toHexString(activeProcess.hashCode()));
        b.append(" priority ");
        b.append(activePriority);
        b.append('\n');
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
        final Object[] lists = pointersReadNode.executeArray(image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS).getObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            printLinkedList(b, "*Quiescent processes list at priority " + (i + 1), (PointersObject) lists[i]);
        }
        return b.toString();
    }

    private static boolean printLinkedList(final StringBuilder b, final String label, final PointersObject linkedList) {
        Object temp = pointersReadNode.execute(linkedList, LINKED_LIST.FIRST_LINK);
        if (temp instanceof PointersObject) {
            b.append(label);
            b.append(" and process");
            if (temp != pointersReadNode.execute(linkedList, LINKED_LIST.LAST_LINK)) {
                b.append("es:\n");
            } else {
                b.append(":\n");
            }
            while (temp instanceof PointersObject) {
                final PointersObject aProcess = (PointersObject) temp;
                final Object aContext = pointersReadNode.execute(aProcess, PROCESS.SUSPENDED_CONTEXT);
                if (aContext instanceof ContextObject) {
                    assert ((ContextObject) aContext).getProcess() == null || ((ContextObject) aContext).getProcess() == aProcess;
                    b.append("\tprocess @");
                    b.append(Integer.toHexString(aProcess.hashCode()));
                    b.append(" with suspended context ");
                    b.append(aContext);
                    b.append(" and stack trace:\n");
                    DebugUtils.printSqMaterializedStackTraceOn(b, (ContextObject) aContext);
                } else {
                    b.append("\tprocess @");
                    b.append(Integer.toHexString(aProcess.hashCode()));
                    b.append(" with suspended context nil\n");
                }
                temp = pointersReadNode.execute(aProcess, PROCESS.NEXT_LINK);
            }
            return true;
        } else {
            return false;
        }
    }

    private static void printSemaphoreOrNil(final StringBuilder b, final String label, final Object semaphoreOrNil, final boolean printIfNil) {
        if (semaphoreOrNil instanceof PointersObject) {
            b.append(label);
            b.append(Integer.toHexString(semaphoreOrNil.hashCode()));
            b.append(" with ");
            b.append(pointersReadNode.executeLong((AbstractPointersObject) semaphoreOrNil, SEMAPHORE.EXCESS_SIGNALS));
            b.append(" excess signals");
            if (!printLinkedList(b, "", (PointersObject) semaphoreOrNil)) {
                b.append(" and no processes\n");
            }
        } else {
            if (printIfNil) {
                b.append(label);
                b.append(" is nil\n");
            }
        }
    }

    @TruffleBoundary
    public static void printSqStackTrace() {
        CompilerDirectives.transferToInterpreter();
        final boolean isTravisBuild = System.getenv().containsKey("TRAVIS");
        final int[] depth = new int[1];
        final boolean[] truffleFrames = new boolean[1];

        new FramesAndContextsIterator(
                        (frame, code) -> {
                            if (depth[0]++ > 50 && isTravisBuild) {
                                return;
                            }
                            final PrintWriter err = FrameAccess.getMethod(frame).image.getError();
                            if (!truffleFrames[0]) {
                                truffleFrames[0] = true;
                                err.println("== Truffle stack trace ===========================================================");
                            }
                            final Object sender = FrameAccess.getSender(frame);
                            final Object marker = FrameAccess.getMarker(frame, code);
                            final Object context = FrameAccess.getContext(frame, code);
                            final String argumentsString = ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame));
                            err.println(MiscUtils.format("%s #(%s) [marker: %s, sender: %s]", context, argumentsString, marker, sender));
                        },
                        (context) -> {
                            if (depth[0]++ > 50 && isTravisBuild) {
                                return;
                            }
                            final PrintWriter err = context.image.getError();
                            if (truffleFrames[0]) {
                                truffleFrames[0] = false;
                                err.println("== Squeak frames ================================================================");
                            }
                            final Object[] rcvrAndArgs = context.getReceiverAndNArguments(context.getBlockOrMethod().getNumArgsAndCopied());
                            err.println(MiscUtils.format("%s #(%s) [%s]", context, ArrayUtils.toJoinedString(", ", rcvrAndArgs), context.getFrameMarker()));
                        }).scanFor((FrameMarker) null, NilObject.SINGLETON, NilObject.SINGLETON);
    }

    public static String logSwitch(final PointersObject newProcess, final int newPriority, final PointersObject currentProcess, final ContextObject thisContext, final ContextObject newContext) {
        final StringBuilder b = new StringBuilder();
        b.append("Switching from process @");
        b.append(Integer.toHexString(currentProcess.hashCode()));
        b.append(" with priority ");
        b.append(pointersReadNode.executeLong(currentProcess, PROCESS.PRIORITY));
        b.append(" and stack\n");
        printSqMaterializedStackTraceOn(b, thisContext);
        b.append("\n...to process @");
        b.append(Integer.toHexString(newProcess.hashCode()));
        b.append(" with priority ");
        b.append(newPriority);
        b.append(" and stack\n");
        printSqMaterializedStackTraceOn(b, newContext);
        return b.toString();
    }

    public static String logNoSwitch(final PointersObject newProcess) {
        final StringBuilder b = new StringBuilder();
        b.append("\nCannot resume process @");
        b.append(Integer.toHexString(newProcess.hashCode()));
        b.append(" with priority ");
        b.append(pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY));
        final AbstractSqueakObject newContext = (AbstractSqueakObject) pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        if (newContext == NilObject.SINGLETON) {
            b.append(" and nil suspendedContext\n");
        } else {
            b.append(" and stack\n");
            printSqMaterializedStackTraceOn(b, (ContextObject) newContext);
        }
        b.append("\n...because it hs a lower priority than the currently active process @");
        final PointersObject currentProcess = newProcess.image.getActiveProcess(pointersReadNode);
        b.append(Integer.toHexString(currentProcess.hashCode()));
        b.append(" with priority ");
        b.append(pointersReadNode.executeLong(currentProcess, PROCESS.PRIORITY));
        return b.toString();
    }

    public static String stackFor(final VirtualFrame frame, final CompiledCodeObject code) {
        final Object[] frameArguments = frame.getArguments();
        final Object receiver = frameArguments[3];
        final StringBuilder b = new StringBuilder("\n\t\t- Receiver:                         ");
        b.append(receiver);
        if (receiver instanceof ContextObject) {
            final ContextObject context = (ContextObject) receiver;
            if (context.hasTruffleFrame()) {
                final MaterializedFrame receiverFrame = context.getTruffleFrame();
                final Object[] receiverFrameArguments = receiverFrame.getArguments();
                b.append("\n\t\t\t\t- Receiver:                         ");
                b.append(receiverFrameArguments[3]);
                final CompiledCodeObject receiverCode = receiverFrameArguments[2] != null ? ((BlockClosureObject) receiverFrameArguments[2]).getCompiledBlock()
                                : (CompiledMethodObject) receiverFrameArguments[0];
                if (receiverCode != null) {
                    b.append("\n\t\t\t\t- Stack (including args and temps)\n");
                    final int zeroBasedStackp = FrameAccess.getStackPointer(receiverFrame, receiverCode) - 1;
                    final int numArgs = receiverCode.getNumArgs();
                    for (int i = 0; i < numArgs; i++) {
                        final Object value = receiverFrameArguments[i + 4];
                        b.append(zeroBasedStackp == i ? "\t\t\t\t\t\t-> a" : "\t\t\t\t\t\t\ta");
                        b.append(i);
                        b.append("\t");
                        b.append(value);
                        b.append("\n");
                    }
                    final FrameSlot[] stackSlots = receiverCode.getStackSlotsUnsafe();
                    boolean addedSeparator = false;
                    final FrameDescriptor frameDescriptor = receiverCode.getFrameDescriptor();
                    final int initialStackp;
                    if (receiverCode instanceof CompiledBlockObject) {
                        assert ((BlockClosureObject) receiverFrameArguments[2]).getCopied().length == receiverCode.getNumArgsAndCopied() - receiverCode.getNumArgs();
                        initialStackp = receiverCode.getNumArgsAndCopied();
                        for (int i = numArgs; i < initialStackp; i++) {
                            final Object value = receiverFrameArguments[i + 4];
                            b.append(zeroBasedStackp == i ? "\t\t\t\t\t\t-> c" : "\t\t\t\t\t\t\tc");
                            b.append(i);
                            b.append("\t");
                            b.append(value);
                            b.append("\n");
                        }
                    } else {
                        initialStackp = receiverCode.getNumTemps();
                        for (int i = numArgs; i < initialStackp; i++) {
                            final FrameSlot slot = stackSlots[i];
                            Object value = null;
                            if (slot != null && (value = receiverFrame.getValue(slot)) != null) {
                                b.append(zeroBasedStackp == i ? "\t\t\t\t\t\t-> t" : "\t\t\t\t\t\t\tt");
                                b.append(i);
                                b.append("\t");
                                b.append(value);
                                b.append("\n");
                            }
                        }
                    }
                    int j = initialStackp;
                    for (int i = initialStackp; i < stackSlots.length; i++) {
                        final FrameSlot slot = stackSlots[i];
                        Object value = null;
                        if (slot != null && frameDescriptor.getFrameSlotKind(slot) != FrameSlotKind.Illegal && (value = receiverFrame.getValue(slot)) != null) {
                            if (!addedSeparator) {
                                addedSeparator = true;
                                b.append("\t\t\t\t\t\t\t------------------------------------------------\n");
                            }
                            b.append(zeroBasedStackp == i ? "\t\t\t\t\t\t\t->\t" : "\t\t\t\t\t\t\t\t\t");
                            b.append(value);
                            b.append("\n");
                        } else {
                            j = i;
                            if (zeroBasedStackp == i) {
                                if (!addedSeparator) {
                                    addedSeparator = true;
                                    b.append("\t\t\t\t\t\t\t------------------------------------------------\n");
                                }
                                b.append("\t\t\t\t\t\t\t->\tnull\n");
                            }
                            break; // This and all following slots are not in use.
                        }
                    }
                    if (j == 0 && !addedSeparator) {
                        b.deleteCharAt(b.length() - 1);
                        b.append(" is empty\n");
                    } else if (!addedSeparator) {
                        b.append("\t\t\t\t\t\t\t------------------------------------------------\n");
                    }
                }
            }
        }
        b.append("\n\t\t- Stack (including args and temps)\n");
        final int zeroBasedStackp = FrameAccess.getStackPointer(frame, code) - 1;
        final int numArgs = code.getNumArgs();
        for (int i = 0; i < numArgs; i++) {
            final Object value = frameArguments[i + 4];
            b.append(zeroBasedStackp == i ? "\t\t\t\t-> a" : "\t\t\t\t\ta");
            b.append(i);
            b.append("\t");
            b.append(value);
            b.append("\n");
        }
        final FrameSlot[] stackSlots = code.getStackSlotsUnsafe();
        boolean addedSeparator = false;
        final FrameDescriptor frameDescriptor = code.getFrameDescriptor();
        final int initialStackp;
        if (code instanceof CompiledBlockObject) {
            initialStackp = code.getNumArgsAndCopied();
            for (int i = numArgs; i < initialStackp; i++) {
                final Object value = frameArguments[i + 4];
                b.append(zeroBasedStackp == i ? "\t\t\t\t-> c" : "\t\t\t\t\tc");
                b.append(i);
                b.append("\t");
                b.append(value);
                b.append("\n");
            }
        } else {
            initialStackp = code.getNumTemps();
            for (int i = numArgs; i < initialStackp; i++) {
                final FrameSlot slot = stackSlots[i];
                final Object value = frame.getValue(slot);
                b.append(zeroBasedStackp == i ? "\t\t\t\t-> t" : "\t\t\t\t\tt");
                b.append(i);
                b.append("\t");
                b.append(value);
                b.append("\n");
            }
        }
        int j = initialStackp;
        for (int i = initialStackp; i < stackSlots.length; i++) {
            final FrameSlot slot = stackSlots[i];
            Object value = null;
            if (slot != null && frameDescriptor.getFrameSlotKind(slot) != FrameSlotKind.Illegal && (value = frame.getValue(slot)) != null) {
                if (!addedSeparator) {
                    addedSeparator = true;
                    b.append("\t\t\t\t\t------------------------------------------------\n");
                }
                b.append(zeroBasedStackp == i ? "\t\t\t\t\t->\t" : "\t\t\t\t\t\t\t");
                b.append(value);
                b.append("\n");
            } else {
                j = i;
                if (zeroBasedStackp == i) {
                    if (!addedSeparator) {
                        addedSeparator = true;
                        b.append("\t\t\t\t\t------------------------------------------------\n");
                    }
                    b.append("\t\t\t\t\t->\tnull\n");
                }
                break; // This and all following slots are not in use.
            }
        }
        if (j == 0 && !addedSeparator) {
            b.deleteCharAt(b.length() - 1);
            b.append(" is empty\n");
        } else if (!addedSeparator) {
            b.append("\t\t\t\t\t------------------------------------------------\n");
        }
        return b.toString();
    }

    /**
     * {@link System#gc()} does not force a garbage collect, but the diagnostics command
     * "gcClassHistogram" does.
     */
    public static void forceGcWithHistogram() {
        final DiagnosticCommandMBean dcmd = ManagementFactoryHelper.getDiagnosticCommandMBean();
        final Map<String, Long> enabledBeansCounts = new HashMap<>();
        final Map<String, Long> enabledBeansTimes = new HashMap<>();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactoryHelper.getGarbageCollectorMXBeans();
        for (final GarbageCollectorMXBean bean : gcBeans) {
            final long count = bean.getCollectionCount();
            if (count != -1) {
                enabledBeansCounts.put(bean.getName(), count);
                final long accumulatedCollectionTime = bean.getCollectionTime();
                if (accumulatedCollectionTime != -1) {
                    enabledBeansTimes.put(bean.getName(), accumulatedCollectionTime);
                }
            }
        }
        long elapsed = 0;
        final long start = System.nanoTime();
        Object histogram = null;
        try {
            histogram = dcmd.invoke("gcClassHistogram", new Object[]{new String[]{}}, new String[]{"[Ljava.lang.String;"});
            elapsed = System.nanoTime() - start;
            // just in case the diagnostics command materialized some new collectors/beans
            gcBeans = ManagementFactoryHelper.getGarbageCollectorMXBeans();
        } catch (final JMException e) {
            e.printStackTrace();
        }
        assert elapsed > 0 && histogram != null;
        boolean atLeastOne = false;
        for (final GarbageCollectorMXBean bean : gcBeans) {
            final long count = bean.getCollectionCount();
            if (count != -1) {
                long previousCount = 0;
                if (enabledBeansCounts.containsKey(bean.getName())) {
                    previousCount = enabledBeansCounts.get(bean.getName());
                }
                if (count == previousCount) {
                    continue;
                }
                atLeastOne = true;
                final StringBuilder b = new StringBuilder("Memory manager ");
                b.append(bean.getName());
                b.append(" has performed ");
                b.append(count - previousCount);
                b.append(" garbage collection");
                if (count - previousCount > 1) {
                    b.append("s");
                }
                final long accumulatedCollectionTime = bean.getCollectionTime();
                if (accumulatedCollectionTime != -1) {
                    long previousAccumulatedCollectionTime = 0;
                    if (enabledBeansTimes.containsKey(bean.getName())) {
                        previousAccumulatedCollectionTime = enabledBeansTimes.get(bean.getName());
                    }
                    assert accumulatedCollectionTime > previousAccumulatedCollectionTime;
                    b.append(" in ");
                    b.append(accumulatedCollectionTime - previousAccumulatedCollectionTime);
                    b.append("ms (out of ");
                    b.append(elapsed / 1000000);
                    b.append(")");
                }
                final String[] names = bean.getMemoryPoolNames();
                if (names.length > 0) {
                    b.append(" for pools [");
                    b.append(names[0]);
                    for (int i = 1; i < names.length; i++) {
                        b.append(", ");
                        b.append(names[i]);
                    }
                    b.append("]");
                }
                System.out.println(b.toString());
            }
        }
        if (!atLeastOne) {
            System.out.println("No garbage collection occurred! Class histogram follows:");
        } else {
            System.out.println("Successfully forced a garbage collect! Class histogram follows:");
        }
        System.out.println(histogram);
    }

    public static final boolean underDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

}

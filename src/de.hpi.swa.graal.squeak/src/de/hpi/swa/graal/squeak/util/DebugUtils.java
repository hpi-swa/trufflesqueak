package de.hpi.swa.graal.squeak.util;

import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
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

/**
 * Helper functions for debugging purposes.
 */
public final class DebugUtils {

    public static void printSqMaterializedStackTraceOn(final StringBuilder b, final ContextObject context) {
        ContextObject current = context;
        while (current != null) {
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

    @TruffleBoundary
    public static void printSqStackTrace(final ContextObject context) {
        final StringBuilder b = new StringBuilder();
        printSqMaterializedStackTraceOn(b, context);
        context.image.getOutput().println(b.toString());
    }

    public static String currentState(final SqueakImageContext image) {
        final StringBuilder b = new StringBuilder();
        b.append("\nImage processes state\n");
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final long activePriority = getPriority(activeProcess);
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
        final Object[] lists = AbstractPointersObjectReadNode.getUncached().executeArray(image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS).getObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            printLinkedList(b, "*Quiescent processes list at priority " + (i + 1), (PointersObject) lists[i]);
        }
        return b.toString();
    }

    private static boolean printLinkedList(final StringBuilder b, final String label, final PointersObject linkedList) {
        Object temp = getFirstLink(linkedList);
        if (temp instanceof PointersObject) {
            b.append(label);
            b.append(" and process");
            if (temp != getLastLink(linkedList)) {
                b.append("es:\n");
            } else {
                b.append(":\n");
            }
            while (temp instanceof PointersObject) {
                final PointersObject aProcess = (PointersObject) temp;
                final Object aContext = getSuspendedContext(aProcess);
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
                temp = getNextLink(aProcess);
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
            b.append(getExcessSignals((PointersObject) semaphoreOrNil));
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
                            PrintWriter err = null;
                            if (depth[0]++ > 50 && isTravisBuild) {
                                return;
                            } else if (!truffleFrames[0]) {
                                truffleFrames[0] = true;
                                err = FrameAccess.getMethod(frame).image.getError();
                                err.println("== Truffle stack trace ===========================================================");
                            }
                            final Object sender = FrameAccess.getSender(frame);
                            final Object marker = FrameAccess.getMarker(frame, code);
                            final Object context = FrameAccess.getContext(frame, code);
                            final String argumentsString = ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame));
                            err.println(MiscUtils.format("%s #(%s) [marker: %s, sender: %s]", context, argumentsString, marker, sender));
                        },
                        (context) -> {
                            PrintWriter err = null;
                            if (depth[0]++ > 50 && isTravisBuild) {
                                return;
                            } else if (truffleFrames[0]) {
                                truffleFrames[0] = false;
                                err = context.image.getError();
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
        b.append(getPriority(currentProcess));
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
        b.append(getPriority(newProcess));
        final AbstractSqueakObject newContext = getSuspendedContext(newProcess);
        if (newContext == NilObject.SINGLETON) {
            b.append(" and nil suspendedContext\n");
        } else {
            b.append(" and stack\n");
            printSqMaterializedStackTraceOn(b, (ContextObject) newContext);
        }
        b.append("\n...because it hs a lower priority than the currently active process @");
        final PointersObject currentProcess = newProcess.image.getActiveProcessSlow();
        b.append(Integer.toHexString(currentProcess.hashCode()));
        b.append(" with priority ");
        b.append(getPriority(currentProcess));
        return b.toString();
    }

    private static AbstractSqueakObject getFirstLink(final PointersObject aLinkedList) {
        return (AbstractSqueakObject) AbstractPointersObjectReadNode.getUncached().execute(aLinkedList, LINKED_LIST.FIRST_LINK);
    }

    private static AbstractSqueakObject getLastLink(final PointersObject aLinkedList) {
        return (AbstractSqueakObject) AbstractPointersObjectReadNode.getUncached().execute(aLinkedList, LINKED_LIST.LAST_LINK);
    }

    private static AbstractSqueakObject getNextLink(final PointersObject aProcess) {
        return (AbstractSqueakObject) AbstractPointersObjectReadNode.getUncached().execute(aProcess, PROCESS.NEXT_LINK);
    }

    private static AbstractSqueakObject getSuspendedContext(final PointersObject aProcess) {
        return (AbstractSqueakObject) AbstractPointersObjectReadNode.getUncached().execute(aProcess, PROCESS.SUSPENDED_CONTEXT);
    }

    private static long getPriority(final PointersObject aProcess) {
        return AbstractPointersObjectReadNode.getUncached().executeLong(aProcess, PROCESS.PRIORITY);
    }

    private static long getExcessSignals(final PointersObject aSemaphore) {
        return AbstractPointersObjectReadNode.getUncached().executeLong(aSemaphore, SEMAPHORE.EXCESS_SIGNALS);
    }
}

/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.interpreter.PushToStackNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/**
 * Experimental support for switching Smalltalk processes with JDK continuations instead of
 * unwinding the Java stack with a {@link ProcessSwitch} exception.
 *
 * <p>
 * Each running Smalltalk process executes on its own continuation ("process stack") on the
 * VM thread. On a process switch, the active process stack is parked (yield)
 * and the top-level loop runs the stack of the next process. Parking keeps Java frames (and thus
 * compiled code and virtual contexts) alive, so a parked process is not fully represented by
 * heap-allocated contexts. Whenever the image needs the heap representation (e.g., to walk or
 * modify the contexts of another process, to trace objects, or to store a snapshot), parked stacks
 * are flushed: they are resumed with a request to unwind via {@link ProcessSwitch}, which
 * materializes all frames just like a regular exception-based process switch.
 *
 * <p>
 * Whenever a stack cannot be parked (continuations unavailable, nested execution, pinned
 * continuation, frames materialized via stack walks), the switch falls back to
 * {@link ProcessSwitch}.
 */
public abstract class ProcessContinuations {

    @FunctionalInterface
    public interface ProcessBody {
        /** Runs the process starting at the given context until its Java stack is unwound. */
        void run(ContextObject initialContext);
    }

    /**
     * Run the process with the given suspended context until it parks or its stack is unwound.
     */
    public abstract void run(ContextObject context, ProcessBody processBody);

    /**
     * Try to park the current process stack.
     *
     * @return true if the stack was parked and has now been resumed, false if the caller must
     *         unwind the stack with a {@link ProcessSwitch} instead.
     */
    protected abstract boolean parkCurrent(ContextObject suspendedContext);

    /**
     * Unwind all parked process stacks to the heap. Afterwards, all suspended processes are fully
     * represented by materialized contexts.
     *
     * @return true if at least one stack was unwound
     */
    public abstract boolean flushAll();

    /** Unwind the parked process stack of the given context, if any. */
    public abstract void flush(ContextObject context);

    /**
     * Must be called when frames of the current stack are materialized or modified through
     * Truffle's stack walking API. Such stacks cannot be frozen, so the next process switch
     * unwinds.
     */
    public abstract void notifyFramesAccessed();

    /**
     * Enter a nested execution (e.g., an interop call or DoIt) that schedules processes on its own
     * and unwinds with {@link ProcessSwitch}. Such executions resume processes from the heap.
     */
    public abstract void enterNestedExecution();

    public abstract void exitNestedExecution();

    public abstract String getStatistics();

    /**
     * Complete a process switch initiated by a primitive. The scheduler has already transferred
     * to the new process. Pushes the primitive's result to keep the suspended context consistent
     * and either parks the current process stack (and returns the result when the process is
     * resumed) or unwinds it with a {@link ProcessSwitch}.
     */
    public static Object switchFromPrimitive(final VirtualFrame frame, final SqueakImageContext image, final PushToStackNode pushNode, final Object result) {
        pushNode.execute(frame, result);
        final ProcessContinuations continuations = image.processContinuations;
        if (continuations != null && continuations.parkCurrent(FrameAccess.getContext(frame))) {
            pushNode.undo(frame);
            return result;
        }
        throw ProcessSwitch.SINGLETON;
    }

    /**
     * Complete a process switch initiated by an interrupt check. Returns normally when the process
     * is resumed, or unwinds with a {@link ProcessSwitch}.
     */
    public static void switchFromInterrupt(final VirtualFrame frame, final SqueakImageContext image) {
        final ProcessContinuations continuations = image.processContinuations;
        if (continuations != null && continuations.parkCurrent(FrameAccess.getContext(frame))) {
            return;
        }
        throw ProcessSwitch.SINGLETON;
    }
}

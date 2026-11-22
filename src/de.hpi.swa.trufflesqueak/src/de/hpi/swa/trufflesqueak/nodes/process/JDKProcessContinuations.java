/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

/**
 * {@link ProcessContinuations} based on {@code jdk.internal.vm.Continuation}. This is the only class
 * that references JDK-internal continuation classes, it must only be loaded on the JVM (requires
 * {@code --add-exports=java.base/jdk.internal.vm=de.hpi.swa.trufflesqueak}).
 *
 * <p>
 * Known limitations of this experiment:
 * <ul>
 * <li>After a process stack has been resumed, its caller frames are thawed lazily. HotSpot cannot
 * materialize the virtual objects of a compiled frame that is still frozen, so walking the stack
 * with {@code FrameInstance.FrameAccess.MATERIALIZE} or {@code READ_WRITE} (e.g.,
 * {@code thisContext sender} or {@code become:}) fails with "stack frame not found" for such
 * frames. A resumed stack would have to be unwound to the heap before these walks.</li>
 * <li>Parked stacks are strongly referenced until they are resumed or flushed, so the stack of a
 * process that is never resumed is only released by the next flush.</li>
 * <li>Not available in native images: SubstrateVM disables continuations when runtime compilation
 * is enabled.</li>
 * </ul>
 */
public final class JDKProcessContinuations extends ProcessContinuations {
    private static final ContinuationScope SCOPE = new ContinuationScope("TruffleSqueakProcess");

    private static final class ProcessStack extends Continuation {
        private ProcessStack previousParked;
        private ProcessStack nextParked;
        private boolean isParked;
        /** The suspended (materialized) context on top of this stack while it is parked. */
        private ContextObject topContext;
        /** Set to request that the stack unwinds itself when it is resumed. */
        private boolean flushRequested;
        /** Set if the stack must not be parked (e.g., frames were materialized by a stack walk). */
        private boolean mustUnwind;
        private int savedContextStackDepth;
        private Throwable pendingException;

        private ProcessStack(final Body body) {
            super(SCOPE, body);
            body.stack = this;
        }

        @Override
        protected void onPinned(final Pinned reason) {
            /* Do not throw, Continuation#yield returns false and the caller falls back. */
            LogUtils.SCHEDULING.fine(() -> "Process stack is pinned: " + reason);
        }
    }

    private static final class Body implements Runnable {
        private final ProcessBody processBody;
        private ContextObject initialContext;
        private ProcessStack stack;

        private Body(final ProcessBody processBody, final ContextObject initialContext) {
            this.processBody = processBody;
            this.initialContext = initialContext;
        }

        @Override
        public void run() {
            final ContextObject context = initialContext;
            initialContext = null;
            try {
                processBody.run(context);
            } catch (final Throwable t) {
                /* Propagate through the continuation boundary explicitly. */
                stack.pendingException = t;
            }
        }
    }

    private final SqueakImageContext image;
    /** The innermost process stack that is currently mounted, or null. */
    private ProcessStack current;
    private ProcessStack parkedHead;
    private int yieldDisabled;
    private boolean isFlushing;

    /* Statistics */
    private long numStacks;
    private long numParks;
    private long numUnwinds;
    private long numPinned;
    private long numFlushedStacks;

    private JDKProcessContinuations(final SqueakImageContext image) {
        this.image = image;
    }

    public static ProcessContinuations create(final SqueakImageContext image) {
        return new JDKProcessContinuations(image);
    }

    /**
     * Run the process with the given suspended context until it parks or its stack is unwound.
     */
    @Override
    @TruffleBoundary
    public void run(final ContextObject context, final ProcessBody processBody) {
        assert current == null : "Process stacks must be started from the top-level loop";
        ProcessStack stack = (ProcessStack) context.getParkedStack();
        if (stack != null) {
            unpark(stack);
        } else {
            stack = new ProcessStack(new Body(processBody, context));
            numStacks++;
        }
        current = stack;
        try {
            stack.run();
        } finally {
            current = null;
        }
        rethrowPendingException(stack);
    }

    /**
     * Try to park the current process stack.
     *
     * @return true if the stack was parked and has now been resumed, false if the caller must
     *         unwind the stack with a {@link ProcessSwitch} instead.
     */
    @Override
    @TruffleBoundary
    protected boolean parkCurrent(final ContextObject suspendedContext) {
        final ProcessStack stack = current;
        if (stack == null || yieldDisabled > 0 || stack.mustUnwind || suspendedContext == null) {
            numUnwinds++;
            return false;
        }
        park(stack, suspendedContext);
        stack.savedContextStackDepth = image.getContextStackDepth();
        boolean success;
        try {
            success = Continuation.yield(SCOPE);
        } catch (final StackOverflowError e) {
            /* For example, HotSpot fails to allocate humongous stack chunks with G1. */
            success = false;
        }
        /* The process stack has been resumed (or could not be parked in the first place). */
        if (!success) {
            unpark(stack);
            numPinned++;
            return false;
        }
        if (stack.flushRequested) {
            return false;
        }
        image.setContextStackDepth(stack.savedContextStackDepth);
        numParks++;
        return true;
    }

    /**
     * Unwind all parked process stacks to the heap. Afterwards, all suspended processes are fully
     * represented by materialized contexts.
     */
    @Override
    public boolean flushAll() {
        if (parkedHead != null) {
            return flushAllSlow();
        }
        return false;
    }

    /** Unwind the parked process stack of the given context, if any. */
    @Override
    @TruffleBoundary
    public void flush(final ContextObject context) {
        final ProcessStack stack = (ProcessStack) context.getParkedStack();
        if (stack != null) {
            assert !isFlushing;
            isFlushing = true;
            try {
                flushStack(stack);
            } finally {
                isFlushing = false;
            }
        }
    }

    @TruffleBoundary
    private boolean flushAllSlow() {
        if (isFlushing) {
            return false;
        }
        isFlushing = true;
        try {
            while (parkedHead != null) {
                flushStack(parkedHead);
            }
        } finally {
            isFlushing = false;
        }
        return true;
    }

    private void flushStack(final ProcessStack stack) {
        final ProcessStack outer = current;
        final int savedDepth = image.getContextStackDepth();
        unpark(stack);
        stack.flushRequested = true;
        numFlushedStacks++;
        current = stack;
        try {
            stack.run();
        } finally {
            current = outer;
            image.setContextStackDepth(savedDepth);
        }
        assert stack.isDone() : "Flushed process stack did not unwind";
        rethrowPendingException(stack);
    }

    /**
     * Must be called when frames of the current stack are materialized or modified through
     * Truffle's stack walking API. Such stacks cannot be frozen, so the next process switch
     * unwinds.
     */
    @Override
    public void notifyFramesAccessed() {
        if (current != null) {
            current.mustUnwind = true;
        }
    }

    /**
     * Enter a nested execution (e.g., an interop call or DoIt) that schedules processes on its own
     * and unwinds with {@link ProcessSwitch}. Such executions resume processes from the heap.
     */
    @Override
    public void enterNestedExecution() {
        flushAll();
        yieldDisabled++;
    }

    @Override
    public void exitNestedExecution() {
        yieldDisabled--;
        assert yieldDisabled >= 0;
    }

    private void park(final ProcessStack stack, final ContextObject suspendedContext) {
        assert !stack.isParked;
        stack.isParked = true;
        stack.topContext = suspendedContext;
        suspendedContext.setParkedStack(stack);
        stack.previousParked = null;
        stack.nextParked = parkedHead;
        if (parkedHead != null) {
            parkedHead.previousParked = stack;
        }
        parkedHead = stack;
    }

    private void unpark(final ProcessStack stack) {
        assert stack.isParked;
        stack.isParked = false;
        stack.topContext.setParkedStack(null);
        stack.topContext = null;
        if (stack.previousParked != null) {
            stack.previousParked.nextParked = stack.nextParked;
        } else {
            parkedHead = stack.nextParked;
        }
        if (stack.nextParked != null) {
            stack.nextParked.previousParked = stack.previousParked;
        }
        stack.previousParked = null;
        stack.nextParked = null;
    }

    private static void rethrowPendingException(final ProcessStack stack) {
        final Throwable t = stack.pendingException;
        if (t != null) {
            stack.pendingException = null;
            throw sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(final Throwable t) throws E {
        throw (E) t;
    }

    @Override
    public String getStatistics() {
        return "process stacks created: " + numStacks + ", parked switches: " + numParks + ", unwinding switches: " + numUnwinds + ", failed parks: " + numPinned + ", flushed stacks: " +
                        numFlushedStacks;
    }
}

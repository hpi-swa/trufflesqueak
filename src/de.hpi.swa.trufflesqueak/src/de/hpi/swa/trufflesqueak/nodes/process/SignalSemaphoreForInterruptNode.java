/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

/**
 * Signals the given semaphore and evaluates whether the newly awakened Process preempts
 * the currently active Process.
 * <p>
 * If a waiting Process is awakened and has a strictly higher priority than the provided
 * nextActiveProcess, the current nextActiveProcess is put to sleep and the new Process
 * is returned. Otherwise, the awakened Process is put to sleep and the original
 * nextActiveProcess is returned.
 */
@GenerateInline(false)
@GenerateCached
public abstract class SignalSemaphoreForInterruptNode extends AbstractNode {

    @NeverDefault
    public static SignalSemaphoreForInterruptNode create() {
        return SignalSemaphoreForInterruptNodeGen.create();
    }

    public static final PointersObject executeUncached(final SqueakImageContext image, final Object semaphoreOrNil, final PointersObject nextActiveProcess, final boolean nextActiveProcessYields) {
        if (!(semaphoreOrNil instanceof final PointersObject semaphore) || !image.isSemaphoreClass(semaphore.getSqueakClass())) {
            return nextActiveProcess;
        }
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();

        if (semaphore.isEmptyList(readNode)) {
            writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
            return nextActiveProcess;
        } else {
            final PointersObject newProcess = semaphore.removeFirstLinkOfList(readNode, writeNode);
            final long newPriority = readNode.executeLong(newProcess, PROCESS.PRIORITY);
            final long winnerPriority = readNode.executeLong(nextActiveProcess, PROCESS.PRIORITY);

            if (newPriority > winnerPriority) {
                PutToSleepNode.executeUncached(image, nextActiveProcess, nextActiveProcessYields);
                return newProcess;
            } else {
                PutToSleepNode.executeUncached(image, newProcess, true);
                return nextActiveProcess;
            }
        }
    }

    public abstract PointersObject executeSignal(Object semaphoreOrNil, PointersObject nextActiveProcess, boolean nextActiveProcessYields);

    @Specialization(guards = {"isSemaphore(semaphore)", "semaphore.isEmptyList(readNode)"}, limit = "1")
    protected static final PointersObject doSignalEmpty(final PointersObject semaphore, final PointersObject nextActiveProcess, @SuppressWarnings("unused") final boolean nextActiveProcessYields,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
        return nextActiveProcess;
    }

    @Specialization(guards = {"isSemaphore(semaphore)", "!semaphore.isEmptyList(readNode)"}, limit = "1")
    protected static final PointersObject doSignal(final PointersObject semaphore, final PointersObject nextActiveProcess, final boolean nextActiveProcessYields,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached final PutToSleepNode putToSleepNode) {

        final PointersObject newProcess = semaphore.removeFirstLinkOfList(readNode, writeNode);
        final long newPriority = readNode.executeLong(newProcess, PROCESS.PRIORITY);
        final long winnerPriority = readNode.executeLong(nextActiveProcess, PROCESS.PRIORITY);

        if (newPriority > winnerPriority) {
            putToSleepNode.executePutToSleep(nextActiveProcess, nextActiveProcessYields);
            return newProcess;
        } else {
            putToSleepNode.executePutToSleep(newProcess, true);
            return nextActiveProcess;
        }
    }

    @Specialization
    protected static final PointersObject doNothing(@SuppressWarnings("unused") final NilObject nil, final PointersObject nextActiveProcess,
                    @SuppressWarnings("unused") final boolean nextActiveProcessYields) {
        return nextActiveProcess;
    }
}

/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import java.util.logging.Level;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public abstract class SignalSemaphoreNode extends AbstractNode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, ControlPrimitives.class);
    private static final boolean isLoggingEnabled = LOG.isLoggable(Level.FINE);

    @Child private ResumeProcessNode resumeProcessNode;

    protected SignalSemaphoreNode(final CompiledCodeObject code) {
        resumeProcessNode = ResumeProcessNode.create(code);
    }

    public static SignalSemaphoreNode create(final CompiledCodeObject code) {
        return SignalSemaphoreNodeGen.create(code);
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = {"semaphore.getSqueakClass().isSemaphoreClass()", "semaphore.isEmptyList()"})
    public static final void doSignalEmpty(final PointersObject semaphore,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
        final long excessSignals = semaphore.getExcessSignals();
        if (isLoggingEnabled) {
            LOG.fine(() -> "Signalling empty semaphore @" + Integer.toHexString(semaphore.hashCode()) + " with initially " + excessSignals + " excessSignals");
        }
        writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, excessSignals + 1);
    }

    @Specialization(guards = {"semaphore.getSqueakClass().isSemaphoreClass()", "!semaphore.isEmptyList()"})
    public final void doSignal(final VirtualFrame frame, final PointersObject semaphore,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
        if (isLoggingEnabled) {
            LOG.fine(() -> "Attempting to resume process after non-empty semaphore @" + Integer.toHexString(semaphore.hashCode()) + " signal");
        }
        resumeProcessNode.executeResume(frame, semaphore.removeFirstLinkOfList(writeNode));
    }

    @Specialization
    protected static final void doNothing(@SuppressWarnings("unused") final NilObject nil) {
        // nothing to do
    }

    @Specialization(guards = "object == null")
    protected static final void doNothing(@SuppressWarnings("unused") final Object object) {
        // nothing to do
    }
}

/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
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
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

/**
 * Returns the new active Context or null if the current active Context has not been preempted.
 */
@GenerateInline(false)
@GenerateCached
public abstract class SignalSemaphoreNode extends AbstractNode {

    @NeverDefault
    public static SignalSemaphoreNode create() {
        return SignalSemaphoreNodeGen.create();
    }

    public static final boolean executeUncached(final VirtualFrame frame, final SqueakImageContext image, final Object semaphoreOrNil) {
        if (!(semaphoreOrNil instanceof final PointersObject semaphore) || !image.isSemaphoreClass(semaphore.getSqueakClass())) {
            return false;
        }
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (semaphore.isEmptyList(AbstractPointersObjectReadNode.getUncached())) {
            return doSignalEmpty(semaphore, readNode, writeNode);
        } else {
            return ResumeProcessNode.executeUncached(frame, image, semaphore.removeFirstLinkOfList(readNode, writeNode));
        }
    }

    public abstract boolean executeSignal(VirtualFrame frame, Object semaphoreOrNil);

    @Specialization(guards = {"isSemaphore(semaphore)", "semaphore.isEmptyList(readNode)"}, limit = "1")
    protected static final boolean doSignalEmpty(final PointersObject semaphore,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
        return false;
    }

    @Specialization(guards = {"isSemaphore(semaphore)", "!semaphore.isEmptyList(readNode)"}, limit = "1")
    protected static final boolean doSignal(final VirtualFrame frame, final PointersObject semaphore,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached final ResumeProcessNode resumeProcessNode) {
        return resumeProcessNode.executeResume(frame, semaphore.removeFirstLinkOfList(readNode, writeNode));
    }

    @Specialization
    protected static final boolean doNothing(@SuppressWarnings("unused") final NilObject nil) {
        // nothing to do
        return false;
    }
}

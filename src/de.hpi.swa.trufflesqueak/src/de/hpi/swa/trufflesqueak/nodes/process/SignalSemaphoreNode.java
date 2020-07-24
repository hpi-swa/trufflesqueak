/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

public abstract class SignalSemaphoreNode extends AbstractNode {

    public static SignalSemaphoreNode create() {
        return SignalSemaphoreNodeGen.create();
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = {"semaphore.getSqueakClass().isSemaphoreClass()", "semaphore.isEmptyList(readNode)"}, limit = "1")
    public static final void doSignalEmpty(final PointersObject semaphore,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
    }

    @Specialization(guards = {"semaphore.getSqueakClass().isSemaphoreClass()", "!semaphore.isEmptyList(readNode)"}, limit = "1")
    public static final void doSignal(final VirtualFrame frame, final PointersObject semaphore,
                    @SuppressWarnings("unused") @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final RemoveFirstLinkOfListNode removeFirstLinkOfListNode,
                    @Cached final ResumeProcessNode resumeProcessNode) {
        resumeProcessNode.executeResume(frame, removeFirstLinkOfListNode.execute(semaphore));
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

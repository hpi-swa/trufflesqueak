/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

public abstract class SignalSemaphoreNode extends AbstractNode {

    @NeverDefault
    public static SignalSemaphoreNode create() {
        return SignalSemaphoreNodeGen.create();
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = {"isSemaphore(semaphore)", "semaphore.isEmptyList(readNode)"})
    protected static final void doSignalEmpty(final PointersObject semaphore,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
    }

    @Specialization(guards = {"isSemaphore(semaphore)", "!semaphore.isEmptyList(readNode)"})
    protected static final void doSignal(final VirtualFrame frame, final PointersObject semaphore,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached final ResumeProcessNode resumeProcessNode) {
        resumeProcessNode.executeResume(frame, semaphore.removeFirstLinkOfList(readNode, writeNode));
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

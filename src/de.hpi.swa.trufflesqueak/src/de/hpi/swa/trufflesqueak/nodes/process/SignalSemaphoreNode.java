/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

@GenerateInline
@GenerateCached(true)
public abstract class SignalSemaphoreNode extends AbstractNode {

    @NeverDefault
    public static SignalSemaphoreNode create() {
        return SignalSemaphoreNodeGen.create();
    }

    public static final ProcessSwitch executeUncached(final VirtualFrame frame, final SqueakImageContext image, final Object semaphoreOrNil) {
        if (!(semaphoreOrNil instanceof final PointersObject semaphore) || !image.isSemaphoreClass(semaphore.getSqueakClass())) {
            return null;
        }
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (semaphore.isEmptyList(AbstractPointersObjectReadNode.getUncached(), null)) {
            writeNode.execute(null, semaphore, SEMAPHORE.EXCESS_SIGNALS,
                            readNode.executeLong(null, semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
            return null;
        } else {
            return ResumeProcessNode.executeUncached(frame, image, semaphore.removeFirstLinkOfList(readNode, writeNode, null), false);
        }
    }

    public abstract ProcessSwitch executeSignal(VirtualFrame frame, Node node, Object semaphoreOrNil);

    @Specialization(guards = {"isSemaphore(semaphore)", "semaphore.isEmptyList(readNode, node)"}, limit = "1")
    protected static final ProcessSwitch doSignalEmpty(final Node node, final PointersObject semaphore,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(node, semaphore, SEMAPHORE.EXCESS_SIGNALS, readNode.executeLong(node, semaphore, SEMAPHORE.EXCESS_SIGNALS) + 1);
        return null;
    }

    @Specialization(guards = {"isSemaphore(semaphore)", "!semaphore.isEmptyList(readNode, node)"}, limit = "1")
    protected static final ProcessSwitch doSignal(final VirtualFrame frame, final Node node, final PointersObject semaphore,
                    @Exclusive @Cached final AbstractPointersObjectReadNode readNode,
                    @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached final ResumeProcessNode resumeProcessNode) {
        return resumeProcessNode.executeResume(frame, node, semaphore.removeFirstLinkOfList(readNode, writeNode, node), false);
    }

    @Specialization
    protected static final ProcessSwitch doNothing(@SuppressWarnings("unused") final NilObject nil) {
        // nothing to do
        return null;
    }
}

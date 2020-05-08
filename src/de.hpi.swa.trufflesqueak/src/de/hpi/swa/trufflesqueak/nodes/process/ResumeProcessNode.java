/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetOrCreateContextNode;

public abstract class ResumeProcessNode extends AbstractNode {
    @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
    @Child private PutToSleepNode putToSleepNode = PutToSleepNode.create();

    public abstract void executeResume(VirtualFrame frame, PointersObject newProcess);

    @Specialization(guards = "hasHigherPriority(image, newProcess)")
    protected final void doTransferTo(final VirtualFrame frame, final PointersObject newProcess,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached("create(true)") final GetOrCreateContextNode contextNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        putToSleepNode.executePutToSleep(image.getActiveProcess(pointersReadNode));
        contextNode.executeGet(frame).transferTo(pointersReadNode, pointersWriteNode, newProcess);
    }

    @Specialization(guards = "!hasHigherPriority(image, newProcess)")
    protected final void doSleep(final PointersObject newProcess,
                    @SuppressWarnings("unused") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        putToSleepNode.executePutToSleep(newProcess);
    }

    protected final boolean hasHigherPriority(final SqueakImageContext image, final PointersObject newProcess) {
        return pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(image.getActiveProcess(pointersReadNode), PROCESS.PRIORITY);
    }
}

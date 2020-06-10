/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;

public abstract class ResumeProcessNode extends AbstractNode {
    @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
    @Child private PutToSleepNode putToSleepNode = PutToSleepNode.create();
    @Child private GetActiveProcessNode getActiveProcessNode = GetActiveProcessNode.create();

    public abstract void executeResume(VirtualFrame frame, PointersObject newProcess);

    @Specialization(guards = "hasHigherPriority(newProcess)")
    protected final void doTransferTo(final VirtualFrame frame, final PointersObject newProcess,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached("create(true)") final GetOrCreateContextNode contextNode) {
        putToSleepNode.executePutToSleep(getActiveProcessNode.execute());
        contextNode.executeGet(frame).transferTo(newProcess, pointersReadNode, pointersWriteNode, getActiveProcessNode);
    }

    @Specialization(guards = "!hasHigherPriority(newProcess)")
    protected final void doSleep(final PointersObject newProcess) {
        putToSleepNode.executePutToSleep(newProcess);
    }

    protected final boolean hasHigherPriority(final PointersObject newProcess) {
        return pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(getActiveProcessNode.execute(), PROCESS.PRIORITY);
    }
}

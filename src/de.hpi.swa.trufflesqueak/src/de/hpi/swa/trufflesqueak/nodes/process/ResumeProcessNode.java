/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;

@GenerateInline(true)
@GenerateCached(false)
public abstract class ResumeProcessNode extends AbstractNode {

    public abstract void executeResume(VirtualFrame frame, Node node, PointersObject newProcess);

    @Specialization(guards = "hasHigherPriority(node, pointersReadNode, getActiveProcessNode, newProcess)", limit = "1")
    protected static final void doTransferTo(final VirtualFrame frame, final Node node, final PointersObject newProcess,
                    @Shared("pointersReadNode") @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Shared("putToSleepNode") @Cached final PutToSleepNode putToSleepNode,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached final GetOrCreateContextNode contextNode) {
        putToSleepNode.executePutToSleep(node, getActiveProcessNode.execute(node));
        contextNode.executeGet(frame, node).transferTo(node, getContext(node), newProcess, pointersReadNode, pointersWriteNode, getActiveProcessNode);
    }

    @Specialization(guards = "!hasHigherPriority(node, pointersReadNode, getActiveProcessNode, newProcess)", limit = "1")
    protected static final void doSleep(final Node node, final PointersObject newProcess,
                    @SuppressWarnings("unused") @Shared("pointersReadNode") @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @SuppressWarnings("unused") @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Shared("putToSleepNode") @Cached final PutToSleepNode putToSleepNode) {
        putToSleepNode.executePutToSleep(node, newProcess);
    }

    protected static final boolean hasHigherPriority(final Node node, final AbstractPointersObjectReadNode pointersReadNode, final GetActiveProcessNode getActiveProcessNode,
                    final PointersObject newProcess) {
        return pointersReadNode.executeLong(node, newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(node, getActiveProcessNode.execute(node), PROCESS.PRIORITY);
    }
}

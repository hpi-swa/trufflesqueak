/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@GenerateInline
@GenerateCached(false)
public abstract class ResumeProcessNode extends AbstractNode {

    public static final void executeUncached(final VirtualFrame frame, final SqueakImageContext image, final PointersObject newProcess) {
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final long activePriority = readNode.executeLong(null, activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(null, newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            PutToSleepNode.executeUncached(image, activeProcess);
            throw TransferToNode.executeUncached(frame, newProcess);
        } else {
            PutToSleepNode.executeUncached(image, newProcess);
        }
    }

    public abstract void executeResume(VirtualFrame frame, Node node, PointersObject newProcess);

    @Specialization
    protected static final void resumeProcess(final VirtualFrame frame, final Node node, final PointersObject newProcess,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Cached final PutToSleepNode putToSleepNode,
                    @Cached final TransferToNode transferToNode) {
        final PointersObject activeProcess = getActiveProcessNode.execute(node);
        final long activePriority = readNode.executeLong(node, activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(node, newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(node, activeProcess);
            throw transferToNode.execute(frame, node, newProcess);
        } else {
            putToSleepNode.executePutToSleep(node, newProcess);
        }
    }
}

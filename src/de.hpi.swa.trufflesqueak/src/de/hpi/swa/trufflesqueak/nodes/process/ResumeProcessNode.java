/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Bind;
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

/**
 * Returns the new active Context or null if the current active Context has not been preempted.
 */
@GenerateInline(false)
@GenerateCached
public abstract class ResumeProcessNode extends AbstractNode {

    public static final boolean executeUncached(final VirtualFrame frame, final SqueakImageContext image, final PointersObject newProcess) {
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final long activePriority = readNode.executeLong(activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            PutToSleepNode.executeUncached(image, activeProcess, image.flags.preemptionYields());
            TransferToNode.executeUncached(frame, newProcess);
            return true;
        } else {
            PutToSleepNode.executeUncached(image, newProcess, true);
            return false;
        }
    }

    public abstract boolean executeResume(VirtualFrame frame, PointersObject newProcess);

    @Specialization
    protected static final boolean resumeProcess(final VirtualFrame frame, final PointersObject newProcess,
                    @Bind final Node node,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Cached final PutToSleepNode putToSleepNode,
                    @Cached final TransferToNode transferToNode) {
        final PointersObject activeProcess = getActiveProcessNode.execute(node);
        final long activePriority = readNode.executeLong(activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(activeProcess, getContext(node).flags.preemptionYields());
            transferToNode.execute(frame, newProcess);
            return true;
        } else {
            putToSleepNode.executePutToSleep(newProcess, true);
            return false;
        }
    }
}

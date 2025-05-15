/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@GenerateInline
@GenerateCached(false)
public abstract class ResumeProcessNode extends AbstractNode {

    public static final ProcessSwitch executeUncached(final VirtualFrame frame, final SqueakImageContext image, final PointersObject newProcess, final boolean doThrow) {
        final PointersObject activeProcess = image.getActiveProcessSlow();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final long activePriority = readNode.executeLong(null, activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(null, newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            PutToSleepNode.executeUncached(image, activeProcess);
            final ProcessSwitch processSwitch = TransferToNode.executeUncached(frame, newProcess);
            if (doThrow) {
                throw processSwitch;
            }
            return processSwitch;
        } else {
            PutToSleepNode.executeUncached(image, newProcess);
            return null;
        }
    }

    public abstract ProcessSwitch executeResume(VirtualFrame frame, Node node, PointersObject newProcess, final boolean doThrow);

    @Specialization
    protected static final ProcessSwitch resumeProcess(final VirtualFrame frame, final Node node, final PointersObject newProcess, final boolean doThrow,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Cached final PutToSleepNode putToSleepNode,
                    @Cached final TransferToNode transferToNode) {
        final PointersObject activeProcess = getActiveProcessNode.execute(node);
        final long activePriority = readNode.executeLong(node, activeProcess, PROCESS.PRIORITY);
        final long newPriority = readNode.executeLong(node, newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(node, activeProcess);
            final ProcessSwitch processSwitch = transferToNode.execute(frame, node, newProcess);
            if (doThrow) {
                throw processSwitch;
            }
            return processSwitch;
        } else {
            putToSleepNode.executePutToSleep(node, newProcess);
            return null;
        }
    }
}

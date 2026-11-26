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

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;

/**
 * Return the Context for the highest priority process that is ready to run. Suspends the active
 * Context and returns the new active Context.
 */
@GenerateInline(false)
@GenerateCached
public abstract class WakeHighestPriorityNode extends AbstractNode {

    public abstract void executeWake(VirtualFrame frame);

    @Specialization
    protected static final void doWake(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached(inline = true) final ArrayObjectSizeNode arraySizeNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached final TransferToNode transferToNode) {
        // Note: It is a fatal VM error if there is no runnable process.
        final ArrayObject schedLists = pointersReadNode.executeArray(getContext(node).getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        long p = arraySizeNode.execute(node, schedLists) - 1;  // index of last indexable field
        PointersObject processList;
        do {
            if (p < 0) {
                throw SqueakException.create("scheduler could not find a runnable process");
            }
            processList = (PointersObject) arrayReadNode.execute(node, schedLists, p--);
        } while (processList.isEmptyList(pointersReadNode));
        final PointersObject newProcess = processList.removeFirstLinkOfList(pointersReadNode, pointersWriteNode);
        transferToNode.execute(frame, newProcess);
    }
}

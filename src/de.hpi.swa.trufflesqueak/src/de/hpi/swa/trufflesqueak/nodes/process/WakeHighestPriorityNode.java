/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;

@GenerateInline
@GenerateCached(false)
public abstract class WakeHighestPriorityNode extends AbstractNode {

    public abstract void executeWake(VirtualFrame frame, Node node);

    @Specialization
    protected static final void doWake(final VirtualFrame frame, final Node node,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached final ArrayObjectSizeNode arraySizeNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached final GetOrCreateContextNode contextNode,
                    @Cached final GetActiveProcessNode getActiveProcessNode) {
        final SqueakImageContext image = getContext(node);
        // Return the highest priority process that is ready to run.
        // Note: It is a fatal VM error if there is no runnable process.
        final ArrayObject schedLists = pointersReadNode.executeArray(node, image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        for (long p = arraySizeNode.execute(node, schedLists) - 1; p >= 0; p--) {
            final PointersObject processList = (PointersObject) arrayReadNode.execute(node, schedLists, p);
            while (!processList.isEmptyList(pointersReadNode, node)) {
                final PointersObject newProcess = processList.removeFirstLinkOfList(pointersReadNode, pointersWriteNode, node);
                final Object newContext = pointersReadNode.execute(node, newProcess, PROCESS.SUSPENDED_CONTEXT);
                if (newContext instanceof final ContextObject newActiveContext) {
                    contextNode.executeGet(frame, node).transferTo(image, newProcess, newActiveContext, pointersWriteNode, getActiveProcessNode, node);
                    throw SqueakException.create("Should not be reached");
                }
            }
        }
        throw SqueakException.create("scheduler could not find a runnable process");
    }
}

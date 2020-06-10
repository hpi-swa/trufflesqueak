/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
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

public abstract class WakeHighestPriorityNode extends AbstractNode {

    public static WakeHighestPriorityNode create() {
        return WakeHighestPriorityNodeGen.create();
    }

    public abstract void executeWake(VirtualFrame frame);

    @Specialization
    protected static final void doWake(final VirtualFrame frame,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached final ArrayObjectSizeNode arraySizeNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached("create(true)") final GetOrCreateContextNode contextNode,
                    @Cached final GetActiveProcessNode getActiveProcessNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        // Return the highest priority process that is ready to run.
        // Note: It is a fatal VM error if there is no runnable process.
        final ArrayObject schedLists = pointersReadNode.executeArray(image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        for (long p = arraySizeNode.execute(schedLists) - 1; p >= 0; p--) {
            final PointersObject processList = (PointersObject) arrayReadNode.execute(schedLists, p);
            while (!processList.isEmptyList(pointersReadNode)) {
                final PointersObject newProcess = processList.removeFirstLinkOfList(pointersReadNode, pointersWriteNode);
                final Object newContext = pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
                if (newContext instanceof ContextObject) {
                    contextNode.executeGet(frame).transferTo(newProcess, pointersReadNode, pointersWriteNode, getActiveProcessNode);
                    throw SqueakException.create("Should not be reached");
                }
            }
        }
        throw SqueakException.create("scheduler could not find a runnable process");
    }
}

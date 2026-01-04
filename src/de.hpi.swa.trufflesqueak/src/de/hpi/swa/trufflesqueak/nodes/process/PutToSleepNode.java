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
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

/*
 * Save the given process on the scheduler process list for its priority.
 */
@GenerateInline(false)
@GenerateCached
public abstract class PutToSleepNode extends AbstractNode {

    public static final void executeUncached(final SqueakImageContext image, final PointersObject process, final boolean addLast) {
        final long priority = (Long) process.instVarAt0Slow(PROCESS.PRIORITY);
        final ArrayObject processLists = (ArrayObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) processLists.getObject(priority - 1);
        AddLinkToListNode.executeUncached(process, processList, addLast);
    }

    public abstract void executePutToSleep(PointersObject process, boolean addLast);

    @Specialization
    protected static final void putToSleep(final PointersObject process, final boolean addLast,
                    @Bind final Node node,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final AddLinkToListNode addLinkToListNode) {
        final long priority = pointersReadNode.executeLong(process, PROCESS.PRIORITY);
        final ArrayObject processLists = pointersReadNode.executeArray(getContext(node).getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(node, processLists, priority - 1);
        addLinkToListNode.execute(process, processList, addLast);
    }
}

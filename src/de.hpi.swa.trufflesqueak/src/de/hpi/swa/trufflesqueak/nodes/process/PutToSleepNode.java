/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

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
public final class PutToSleepNode extends AbstractNode {
    @Child private ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.create();
    @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
    @Child private AddLastLinkToListNode addLastLinkToListNode = AddLastLinkToListNode.create();

    public static PutToSleepNode create() {
        return new PutToSleepNode();
    }

    public void executePutToSleep(final PointersObject process) {
        final long priority = pointersReadNode.executeLong(process, PROCESS.PRIORITY);
        final ArrayObject processLists = pointersReadNode.executeArray(getContext().getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
        addLastLinkToListNode.execute(process, processList);
    }
}

/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

public abstract class PutToSleepNode extends AbstractNode {

    public static PutToSleepNode create() {
        return PutToSleepNodeGen.create();
    }

    public abstract void executePutToSleep(PointersObject process);

    @Specialization
    protected final void doPutToSleep(final PointersObject process,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final LinkProcessToListNode linkProcessToList) {
        // Save the given process on the scheduler process list for its priority.
        final long priority = pointersReadNode.executeLong(process, PROCESS.PRIORITY);
        final ArrayObject processLists = pointersReadNode.executeArray(getContext().getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

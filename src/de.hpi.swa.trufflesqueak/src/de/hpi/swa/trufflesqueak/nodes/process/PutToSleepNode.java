/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

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
public abstract class PutToSleepNode extends AbstractNode {

    public static PutToSleepNode create() {
        return PutToSleepNodeGen.create();
    }

    public abstract void executePutToSleep(PointersObject process);

    @Specialization
    protected static final void putToSleep(final PointersObject process,
                    @Bind("this") final Node node,
                    @Cached final ArrayObjectReadNode arrayReadNode,
                    @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @Cached final AddLastLinkToListNode addLastLinkToListNode) {
        final long priority = pointersReadNode.executeLong(process, PROCESS.PRIORITY);
        final ArrayObject processLists = pointersReadNode.executeArray(getContext(node).getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(node, processLists, priority - 1);
        addLastLinkToListNode.execute(process, processList);
    }
}

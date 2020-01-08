/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

public final class PutToSleepNode extends AbstractNodeWithImage {
    @Child private ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.create();
    @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
    @Child private LinkProcessToListNode linkProcessToList = LinkProcessToListNode.create();

    private PutToSleepNode(final SqueakImageContext image) {
        super(image);
    }

    public static PutToSleepNode create(final SqueakImageContext image) {
        return new PutToSleepNode(image);
    }

    public void executePutToSleep(final PointersObject process) {
        // Save the given process on the scheduler process list for its priority.
        final long priority = pointersReadNode.executeLong(process, PROCESS.PRIORITY);
        final ArrayObject processLists = pointersReadNode.executeArray(image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

public final class PutToSleepNode extends AbstractNodeWithImage {
    @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();
    @Child private LinkProcessToListNode linkProcessToList = LinkProcessToListNode.create();

    private PutToSleepNode(final SqueakImageContext image) {
        super(image);
    }

    public static PutToSleepNode create(final SqueakImageContext image) {
        return new PutToSleepNode(image);
    }

    public void executePutToSleep(final PointersObject process) {
        // Save the given process on the scheduler process list for its priority.
        final long priority = (long) process.at0(PROCESS.PRIORITY);
        final ArrayObject processLists = (ArrayObject) image.getScheduler().at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) readNode.execute(processLists, priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

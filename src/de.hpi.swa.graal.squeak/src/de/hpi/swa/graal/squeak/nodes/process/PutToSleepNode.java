package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;

public final class PutToSleepNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private LinkProcessToListNode linkProcessToList;

    public static PutToSleepNode create(final SqueakImageContext image) {
        return new PutToSleepNode(image);
    }

    protected PutToSleepNode(final SqueakImageContext image) {
        super(image);
        linkProcessToList = LinkProcessToListNode.create(image);
    }

    public void executePutToSleep(final AbstractSqueakObject process) {
        // Save the given process on the scheduler process list for its priority.
        final long priority = (long) at0Node.execute(process, PROCESS.PRIORITY);
        final Object processLists = at0Node.execute(image.getScheduler(), PROCESS_SCHEDULER.PROCESS_LISTS);
        final Object processList = at0Node.execute(processLists, priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

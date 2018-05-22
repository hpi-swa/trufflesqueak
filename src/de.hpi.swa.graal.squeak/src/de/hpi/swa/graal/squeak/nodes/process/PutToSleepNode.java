package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;

public class PutToSleepNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private LinkProcessToListNode linkProcessToList;
    @Child private GetSchedulerNode getSchedulerNode;

    public static PutToSleepNode create(final SqueakImageContext image) {
        return new PutToSleepNode(image);
    }

    protected PutToSleepNode(final SqueakImageContext image) {
        super(image);
        linkProcessToList = LinkProcessToListNode.create(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    protected void executePutToSleep(final AbstractSqueakObject process) {
        CompilerDirectives.transferToInterpreter();
        // Save the given process on the scheduler process list for its priority.
        final long priority = (long) at0Node.execute(process, PROCESS.PRIORITY);
        final PointersObject scheduler = getSchedulerNode.executeGet();
        final PointersObject processLists = (PointersObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) processLists.at0(priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

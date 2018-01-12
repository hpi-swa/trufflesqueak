package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class PutToSleepNode extends AbstractProcessNode {
    @Child private LinkProcessToListNode linkProcessToList;
    @Child private GetSchedulerNode getSchedulerNode;

    public static PutToSleepNode create(SqueakImageContext image) {
        return new PutToSleepNode(image);
    }

    protected PutToSleepNode(SqueakImageContext image) {
        super(image);
        linkProcessToList = LinkProcessToListNode.create(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    protected void executePutToSleep(BaseSqueakObject process) {
        CompilerDirectives.transferToInterpreter();
        // Save the given process on the scheduler process list for its priority.
        int priority = (int) process.at0(PROCESS.PRIORITY);
        PointersObject scheduler = getSchedulerNode.executeGet();
        ListObject processLists = (ListObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        PointersObject processList = (PointersObject) processLists.at0(priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

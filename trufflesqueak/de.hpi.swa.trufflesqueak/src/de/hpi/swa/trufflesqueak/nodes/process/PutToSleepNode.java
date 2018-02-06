package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class PutToSleepNode extends AbstractNodeWithCode {
    @Child private LinkProcessToListNode linkProcessToList;
    @Child private GetSchedulerNode getSchedulerNode;

    public static PutToSleepNode create(CompiledCodeObject code) {
        return new PutToSleepNode(code);
    }

    protected PutToSleepNode(CompiledCodeObject code) {
        super(code);
        linkProcessToList = LinkProcessToListNode.create(code);
        getSchedulerNode = GetSchedulerNode.create(code);
    }

    protected void executePutToSleep(BaseSqueakObject process) {
        CompilerDirectives.transferToInterpreter();
        // Save the given process on the scheduler process list for its priority.
        long priority = (long) process.at0(PROCESS.PRIORITY);
        PointersObject scheduler = getSchedulerNode.executeGet();
        ListObject processLists = (ListObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        PointersObject processList = (PointersObject) processLists.at0(priority - 1);
        linkProcessToList.executeLink(process, processList);
    }
}

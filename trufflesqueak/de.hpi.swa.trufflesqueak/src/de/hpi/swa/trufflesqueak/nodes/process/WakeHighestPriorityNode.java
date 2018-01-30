package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class WakeHighestPriorityNode extends AbstractProcessNode {
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
    @Child private TransferToNode transferToNode;
    @Child private GetSchedulerNode getSchedulerNode;
    @Child private IsEmptyListNode isEmptyListNode;

    public static WakeHighestPriorityNode create(SqueakImageContext image) {
        return new WakeHighestPriorityNode(image);
    }

    protected WakeHighestPriorityNode(SqueakImageContext image) {
        super(image);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(image);
        getSchedulerNode = GetSchedulerNode.create(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        transferToNode = TransferToNode.create(image);
    }

    public void executeWake(VirtualFrame frame) {
        // Return the highest priority process that is ready to run.
        // Note: It is a fatal VM error if there is no runnable process.
        PointersObject scheduler = getSchedulerNode.executeGet();
        ListObject schedLists = (ListObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        int p = schedLists.size() - 1;  // index of last indexable field
        BaseSqueakObject processList;
        do {
            if (p < 0) {
                throw new SqueakException("scheduler could not find a runnable process");
            }
            processList = (BaseSqueakObject) schedLists.at0(p--);
        } while (isEmptyListNode.executeIsEmpty(processList));
        BaseSqueakObject activeProcess = (BaseSqueakObject) scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
        BaseSqueakObject newProcess = removeFirstLinkOfListNode.executeRemove(processList);
        transferToNode.executeTransferTo(frame, activeProcess, newProcess);
    }
}

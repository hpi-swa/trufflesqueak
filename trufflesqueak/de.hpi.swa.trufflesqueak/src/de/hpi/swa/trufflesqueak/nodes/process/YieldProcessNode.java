package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class YieldProcessNode extends AbstractNodeWithImage {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private LinkProcessToListNode linkProcessToListNode;
    @Child private TransferToNode transferToNode;
    @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

    public static YieldProcessNode create(SqueakImageContext image) {
        return new YieldProcessNode(image);
    }

    protected YieldProcessNode(SqueakImageContext image) {
        super(image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        linkProcessToListNode = LinkProcessToListNode.create(image);
        transferToNode = TransferToNode.create(image);
        wakeHighestPriorityNode = WakeHighestPriorityNode.create(image);
    }

    public void executeYield(VirtualFrame frame, PointersObject scheduler) {
        PointersObject activeProcess = getActiveProcessNode.executeGet();
        long priority = (long) activeProcess.at0(PROCESS.PRIORITY);
        ListObject processLists = (ListObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        PointersObject processList = (PointersObject) processLists.at0(priority - 1);
        if (!isEmptyListNode.executeIsEmpty(processList)) {
            linkProcessToListNode.executeLink(activeProcess, processList);
            wakeHighestPriorityNode.executeWake(frame);
        }
    }
}

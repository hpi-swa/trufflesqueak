package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class WakeHighestPriorityNode extends AbstractNodeWithImage {
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private GetSchedulerNode getSchedulerNode;
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private TransferToNode transferToNode;

    public static WakeHighestPriorityNode create(final SqueakImageContext image) {
        return new WakeHighestPriorityNode(image);
    }

    protected WakeHighestPriorityNode(final SqueakImageContext image) {
        super(image);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        getSchedulerNode = GetSchedulerNode.create(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        transferToNode = TransferToNode.create(image);
    }

    public void executeWake(final VirtualFrame frame) {
        // Return the highest priority process that is ready to run.
        // Note: It is a fatal VM error if there is no runnable process.
        final PointersObject scheduler = getSchedulerNode.executeGet();
        final ListObject schedLists = (ListObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        long p = schedLists.size() - 1;  // index of last indexable field
        BaseSqueakObject processList;
        do {
            if (p < 0) {
                throw new SqueakException("scheduler could not find a runnable process");
            }
            processList = (BaseSqueakObject) schedLists.at0(p--);
        } while (isEmptyListNode.executeIsEmpty(processList));
        final PointersObject activeProcess = getActiveProcessNode.executeGet();
        final BaseSqueakObject newProcess = removeFirstLinkOfListNode.executeRemove(processList);
        transferToNode.executeTransferTo(frame, activeProcess, newProcess);
    }
}

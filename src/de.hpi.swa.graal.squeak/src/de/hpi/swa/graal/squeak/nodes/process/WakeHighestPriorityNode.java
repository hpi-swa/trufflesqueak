package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public final class WakeHighestPriorityNode extends AbstractNodeWithImage {
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private GetSchedulerNode getSchedulerNode;
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private TransferToNode transferToNode;

    public static WakeHighestPriorityNode create(final CompiledCodeObject code) {
        return new WakeHighestPriorityNode(code);
    }

    protected WakeHighestPriorityNode(final CompiledCodeObject code) {
        super(code.image);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        getSchedulerNode = GetSchedulerNode.create(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        transferToNode = TransferToNode.create(code);
    }

    public void executeWake(final VirtualFrame frame) {
        // Return the highest priority process that is ready to run.
        // Note: It is a fatal VM error if there is no runnable process.
        final PointersObject scheduler = getSchedulerNode.executeGet();
        final PointersObject schedLists = (PointersObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        long p = schedLists.size() - 1;  // index of last indexable field
        Object processList;
        do {
            if (p < 0) {
                throw new SqueakException("scheduler could not find a runnable process");
            }
            processList = schedLists.at0(p--);
        } while (isEmptyListNode.executeIsEmpty(processList));
        final PointersObject activeProcess = getActiveProcessNode.executeGet();
        final Object newProcess = removeFirstLinkOfListNode.executeRemove(processList);
        transferToNode.executeTransferTo(frame, activeProcess, newProcess);
    }
}

package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class WakeHighestPriorityNode extends AbstractNodeWithCode {
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;
    @Child private TransferToNode transferToNode;
    @Child private GetSchedulerNode getSchedulerNode;
    @Child private IsEmptyListNode isEmptyListNode;

    public static WakeHighestPriorityNode create(CompiledCodeObject code) {
        return new WakeHighestPriorityNode(code);
    }

    protected WakeHighestPriorityNode(CompiledCodeObject code) {
        super(code);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(code);
        getSchedulerNode = GetSchedulerNode.create(code);
        isEmptyListNode = IsEmptyListNode.create(code);
        transferToNode = TransferToNode.create(code);
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

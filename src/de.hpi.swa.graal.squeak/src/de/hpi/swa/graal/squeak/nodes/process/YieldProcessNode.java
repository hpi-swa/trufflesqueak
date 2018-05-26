package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class YieldProcessNode extends AbstractNodeWithImage {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private LinkProcessToListNode linkProcessToListNode;
    @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

    public static YieldProcessNode create(final CompiledCodeObject image) {
        return new YieldProcessNode(image);
    }

    protected YieldProcessNode(final CompiledCodeObject code) {
        super(code.image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        linkProcessToListNode = LinkProcessToListNode.create(image);
        wakeHighestPriorityNode = WakeHighestPriorityNode.create(code);
    }

    public void executeYield(final VirtualFrame frame, final PointersObject scheduler) {
        final PointersObject activeProcess = getActiveProcessNode.executeGet();
        final long priority = (long) activeProcess.at0(PROCESS.PRIORITY);
        final PointersObject processLists = (PointersObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) processLists.at0(priority - 1);
        if (!isEmptyListNode.executeIsEmpty(processList)) {
            linkProcessToListNode.executeLink(activeProcess, processList);
            wakeHighestPriorityNode.executeWake(frame);
        }
    }
}

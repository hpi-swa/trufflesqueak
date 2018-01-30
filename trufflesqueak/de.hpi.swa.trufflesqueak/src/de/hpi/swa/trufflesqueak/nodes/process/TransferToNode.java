package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class TransferToNode extends AbstractProcessNode {
    @Child private GetSchedulerNode getSchedulerNode;

    public static TransferToNode create(SqueakImageContext image) {
        return new TransferToNode(image);
    }

    protected TransferToNode(SqueakImageContext image) {
        super(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    public void executeTransferTo(VirtualFrame frame, BaseSqueakObject activeProcess, BaseSqueakObject newProcess) {
        ContextObject activeContext = ContextObject.getOrMaterialize(frame);
        assert activeContext != null;
        // Record a process to be awakened on the next interpreter cycle.
        PointersObject scheduler = getSchedulerNode.executeGet();
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, activeContext);
        ContextObject newActiveContext = (ContextObject) newProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        newProcess.atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        throw new ProcessSwitch(newActiveContext);
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;

public class TransferToNode extends AbstractNodeWithImage {
    @Child private GetSchedulerNode getSchedulerNode;

    public static TransferToNode create(final SqueakImageContext image) {
        return new TransferToNode(image);
    }

    protected TransferToNode(final SqueakImageContext image) {
        super(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    public void executeTransferTo(final VirtualFrame frame, final BaseSqueakObject activeProcess, final BaseSqueakObject newProcess) {
        // Record a process to be awakened on the next interpreter cycle.
        final BaseSqueakObject activeContext = GetOrCreateContextNode.getOrCreate(frame);
        final PointersObject scheduler = getSchedulerNode.executeGet();
        assert newProcess != scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS) : "trying to switch to already active process";
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, activeContext);
        final ContextObject newActiveContext = (ContextObject) newProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        newProcess.atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        throw new ProcessSwitch(newActiveContext);
    }
}

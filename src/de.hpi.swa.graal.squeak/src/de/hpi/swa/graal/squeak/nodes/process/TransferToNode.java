package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;

public class TransferToNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private GetSchedulerNode getSchedulerNode;

    public static TransferToNode create(final SqueakImageContext image) {
        return new TransferToNode(image);
    }

    protected TransferToNode(final SqueakImageContext image) {
        super(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    public void executeTransferTo(final VirtualFrame frame, final AbstractSqueakObject activeProcess, final AbstractSqueakObject newProcess) {
        // Record a process to be awakened on the next interpreter cycle.
        final AbstractSqueakObject activeContext = GetOrCreateContextNode.getOrCreateFull(frame, false);
        final PointersObject scheduler = getSchedulerNode.executeGet();
        assert newProcess != scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS) : "trying to switch to already active process";
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        atPut0Node.execute(activeProcess, PROCESS.SUSPENDED_CONTEXT, activeContext);
        final ContextObject newActiveContext = (ContextObject) at0Node.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        atPut0Node.execute(newProcess, PROCESS.SUSPENDED_CONTEXT, image.nil);
        throw new ProcessSwitch(newActiveContext);
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;

public class ResumeProcessNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private PutToSleepNode putToSleepNode;
    @Child private TransferToNode transferToNode;

    public static ResumeProcessNode create(final SqueakImageContext image) {
        return new ResumeProcessNode(image);
    }

    protected ResumeProcessNode(final SqueakImageContext image) {
        super(image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        putToSleepNode = PutToSleepNode.create(image);
        transferToNode = TransferToNode.create(image);
    }

    public void executeResume(final VirtualFrame frame, final AbstractSqueakObject newProcess) {
        final AbstractSqueakObject activeProcess = getActiveProcessNode.executeGet();
        final long activePriority = (long) at0Node.execute(activeProcess, PROCESS.PRIORITY);
        final long newPriority = (long) at0Node.execute(newProcess, PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(activeProcess);
            transferToNode.executeTransferTo(frame, activeProcess, newProcess);
        } else {
            putToSleepNode.executePutToSleep(newProcess);
        }
    }
}

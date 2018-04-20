package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class ResumeProcessNode extends AbstractNodeWithImage {
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

    public void executeResume(final VirtualFrame frame, final BaseSqueakObject newProcess) {
        final BaseSqueakObject activeProcess = getActiveProcessNode.executeGet();
        final long activePriority = (long) activeProcess.at0(PROCESS.PRIORITY);
        final long newPriority = (long) newProcess.at0(PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(activeProcess);
            transferToNode.executeTransferTo(frame, activeProcess, newProcess);
        } else {
            putToSleepNode.executePutToSleep(newProcess);
        }
    }
}

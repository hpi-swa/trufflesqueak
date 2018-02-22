package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class ResumeProcessNode extends AbstractNodeWithImage {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private PutToSleepNode putToSleepNode;
    @Child private TransferToNode transferToNode;

    public static ResumeProcessNode create(SqueakImageContext image) {
        return new ResumeProcessNode(image);
    }

    protected ResumeProcessNode(SqueakImageContext image) {
        super(image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        putToSleepNode = PutToSleepNode.create(image);
        transferToNode = TransferToNode.create(image);
    }

    public void executeResume(VirtualFrame frame, BaseSqueakObject newProcess) {
        BaseSqueakObject activeProcess = getActiveProcessNode.executeGet();
        long activePriority = (long) activeProcess.at0(PROCESS.PRIORITY);
        long newPriority = (long) newProcess.at0(PROCESS.PRIORITY);
        if (newPriority > activePriority) {
            putToSleepNode.executePutToSleep(activeProcess);
            transferToNode.executeTransferTo(frame, activeProcess, newProcess);
        } else {
            putToSleepNode.executePutToSleep(newProcess);
        }
    }
}

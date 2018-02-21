package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class ResumeProcessNode extends AbstractNodeWithCode {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private PutToSleepNode putToSleepNode;
    @Child private TransferToNode transferToNode;

    public static ResumeProcessNode create(CompiledCodeObject code) {
        return new ResumeProcessNode(code);
    }

    protected ResumeProcessNode(CompiledCodeObject code) {
        super(code);
        getActiveProcessNode = GetActiveProcessNode.create(code);
        putToSleepNode = PutToSleepNode.create(code);
        transferToNode = TransferToNode.create(code);
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

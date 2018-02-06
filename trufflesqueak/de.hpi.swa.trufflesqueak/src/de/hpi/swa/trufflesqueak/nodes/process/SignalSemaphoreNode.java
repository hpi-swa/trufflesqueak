package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class SignalSemaphoreNode extends AbstractNodeWithCode {
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private ResumeProcessNode resumeProcessNode;
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;

    public static SignalSemaphoreNode create(CompiledCodeObject code) {
        return new SignalSemaphoreNode(code);
    }

    protected SignalSemaphoreNode(CompiledCodeObject code) {
        super(code);
        isEmptyListNode = IsEmptyListNode.create(code);
        resumeProcessNode = ResumeProcessNode.create(code);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(code);
    }

    public void executeSignal(VirtualFrame frame, PointersObject semaphore) {
        if (isEmptyListNode.executeIsEmpty(semaphore)) { // no process is waiting on this semaphore
            semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
        } else {
            resumeProcessNode.executeResume(frame, removeFirstLinkOfListNode.executeRemove(semaphore));
        }
    }
}

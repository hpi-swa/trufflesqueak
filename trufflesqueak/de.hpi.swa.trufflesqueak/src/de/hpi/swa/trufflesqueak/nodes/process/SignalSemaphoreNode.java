package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class SignalSemaphoreNode extends AbstractNodeWithImage {
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private ResumeProcessNode resumeProcessNode;
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;

    public static SignalSemaphoreNode create(SqueakImageContext image) {
        return new SignalSemaphoreNode(image);
    }

    protected SignalSemaphoreNode(SqueakImageContext image) {
        super(image);
        isEmptyListNode = IsEmptyListNode.create(image);
        resumeProcessNode = ResumeProcessNode.create(image);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(image);
    }

    public void executeSignal(VirtualFrame frame, PointersObject semaphore) {
        if (isEmptyListNode.executeIsEmpty(semaphore)) { // no process is waiting on this semaphore
            semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
        } else {
            resumeProcessNode.executeResume(frame, removeFirstLinkOfListNode.executeRemove(semaphore));
        }
    }
}

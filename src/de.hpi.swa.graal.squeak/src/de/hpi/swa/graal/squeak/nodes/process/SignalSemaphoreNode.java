package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class SignalSemaphoreNode extends AbstractNodeWithImage {
    @Child private IsEmptyListNode isEmptyListNode;
    @Child private ResumeProcessNode resumeProcessNode;
    @Child private RemoveFirstLinkOfListNode removeFirstLinkOfListNode;

    public static SignalSemaphoreNode create(final CompiledCodeObject code) {
        return SignalSemaphoreNodeGen.create(code);
    }

    protected SignalSemaphoreNode(final CompiledCodeObject code) {
        super(code.image);
        isEmptyListNode = IsEmptyListNode.create(image);
        resumeProcessNode = ResumeProcessNode.create(code);
        removeFirstLinkOfListNode = RemoveFirstLinkOfListNode.create(image);
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = "semaphore.isSemaphore()")
    public final void doSignal(final VirtualFrame frame, final PointersObject semaphore) {
        if (isEmptyListNode.executeIsEmpty(semaphore)) { // no process is waiting on this semaphore
            semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
        } else {
            resumeProcessNode.executeResume(frame, removeFirstLinkOfListNode.executeRemove(semaphore));
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doNothing(final VirtualFrame frame, final NilObject nil) {
    }

    @Fallback
    protected static final void doFallback(@SuppressWarnings("unused") final VirtualFrame frame, final Object semaphore) {
        throw new SqueakException("Unexpected object in SignalSemaphoreNode: " + semaphore);
    }
}

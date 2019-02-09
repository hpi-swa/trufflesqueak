package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class SignalSemaphoreNode extends AbstractNodeWithImage {
    @Child private ResumeProcessNode resumeProcessNode;

    protected SignalSemaphoreNode(final CompiledCodeObject code) {
        super(code.image);
        resumeProcessNode = ResumeProcessNode.create(code);
    }

    public static SignalSemaphoreNode create(final CompiledCodeObject code) {
        return SignalSemaphoreNodeGen.create(code);
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = {"semaphore.isSemaphore()", "semaphore.isEmptyList()"})
    public static final void doSignalEmpty(final PointersObject semaphore) {
        semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
    }

    @Specialization(guards = {"semaphore.isSemaphore()", "!semaphore.isEmptyList()"})
    public final void doSignal(final VirtualFrame frame, final PointersObject semaphore) {
        resumeProcessNode.executeResume(frame, semaphore.removeFirstLinkOfList());
    }

    @Specialization
    protected static final void doNothing(@SuppressWarnings("unused") final NilObject nil) {
        // nothing to do
    }

    @Specialization(guards = "object == null")
    protected static final void doNothing(@SuppressWarnings("unused") final Object object) {
        // nothing to do
    }

    @Fallback
    protected static final void doFallback(@SuppressWarnings("unused") final VirtualFrame frame, final Object semaphore) {
        throw SqueakException.create("Unexpected object in SignalSemaphoreNode:", semaphore);
    }
}

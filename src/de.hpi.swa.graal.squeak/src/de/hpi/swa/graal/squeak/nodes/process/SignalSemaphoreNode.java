package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;

public abstract class SignalSemaphoreNode extends AbstractNode {
    @Child private ResumeProcessNode resumeProcessNode;

    protected SignalSemaphoreNode(final CompiledCodeObject code) {
        resumeProcessNode = ResumeProcessNode.create(code);
    }

    public static SignalSemaphoreNode create(final CompiledCodeObject code) {
        return SignalSemaphoreNodeGen.create(code);
    }

    public abstract void executeSignal(VirtualFrame frame, Object semaphore);

    @Specialization(guards = {"classNode.executeClass(semaphore).isSemaphoreClass()", "semaphore.isEmptyList()"}, limit = "1")
    public static final void doSignalEmpty(final PointersObject semaphore,
                    @SuppressWarnings("unused") @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (long) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
    }

    @Specialization(guards = {"classNode.executeClass(semaphore).isSemaphoreClass()", "!semaphore.isEmptyList()"}, limit = "1")
    public final void doSignal(final VirtualFrame frame, final PointersObject semaphore,
                    @SuppressWarnings("unused") @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
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
}

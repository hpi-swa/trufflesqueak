package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;

public abstract class ResumeProcessNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private PutToSleepNode putToSleepNode;
    @Child private TransferToNode transferToNode;

    public static ResumeProcessNode create(final CompiledCodeObject code) {
        return ResumeProcessNodeGen.create(code);
    }

    protected ResumeProcessNode(final CompiledCodeObject code) {
        super(code.image);
        getActiveProcessNode = GetActiveProcessNode.create(image);
        putToSleepNode = PutToSleepNode.create(code);
        transferToNode = TransferToNode.create(code);
    }

    public abstract void executeResume(VirtualFrame frame, Object newProcess);

    @Specialization
    protected final void executeResume(final VirtualFrame frame, final AbstractSqueakObject newProcess) {
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

    @Fallback
    protected static final void doFallback(final Object newProcess) {
        throw new SqueakException("Unexpected process object:", newProcess);
    }
}

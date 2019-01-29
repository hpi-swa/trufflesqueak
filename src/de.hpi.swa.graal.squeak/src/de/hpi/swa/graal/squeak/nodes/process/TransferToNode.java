package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

public abstract class TransferToNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private GetOrCreateContextNode contextNode;

    protected TransferToNode(final CompiledCodeObject code) {
        super(code.image);
        contextNode = GetOrCreateContextNode.create(code);
    }

    public static TransferToNode create(final CompiledCodeObject code) {
        return TransferToNodeGen.create(code);
    }

    public abstract void executeTransferTo(VirtualFrame frame, Object activeProcess, Object newProcess);

    @Specialization
    public final void executeTransferTo(final VirtualFrame frame, final AbstractSqueakObject activeProcess, final AbstractSqueakObject newProcess) {
        // Record a process to be awakened on the next interpreter cycle.
        final ContextObject activeContext = contextNode.executeGet(frame);
        final PointersObject scheduler = image.getScheduler();
        assert newProcess != scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS) : "trying to switch to already active process";
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        atPut0Node.execute(activeProcess, PROCESS.SUSPENDED_CONTEXT, activeContext);
        final ContextObject newActiveContext = (ContextObject) at0Node.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        atPut0Node.execute(newProcess, PROCESS.SUSPENDED_CONTEXT, image.nil);
        if (CompilerDirectives.isPartialEvaluationConstant(newActiveContext)) {
            throw ProcessSwitch.create(newActiveContext);
        } else {
            // Avoid further PE if newActiveContext is not a PE constant.
            throw ProcessSwitch.createWithBoundary(newActiveContext);
        }
    }

    @Fallback
    protected static final void doFallback(final Object activeProcess, final Object newProcess) {
        throw new SqueakException("Unexpected process objects:", activeProcess, "and", newProcess);
    }
}

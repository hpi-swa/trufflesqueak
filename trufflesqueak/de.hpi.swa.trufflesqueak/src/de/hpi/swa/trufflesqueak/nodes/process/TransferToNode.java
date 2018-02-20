package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;

public class TransferToNode extends AbstractNodeWithCode {
    @Child private GetSchedulerNode getSchedulerNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    public static TransferToNode create(CompiledCodeObject code) {
        return new TransferToNode(code);
    }

    protected TransferToNode(CompiledCodeObject code) {
        super(code);
        getSchedulerNode = GetSchedulerNode.create(code);
        getOrCreateContextNode = GetOrCreateContextNode.create(code);
    }

    public void executeTransferTo(VirtualFrame frame, BaseSqueakObject activeProcess, BaseSqueakObject newProcess) {
        BaseSqueakObject oldContext = getOrCreateContextNode.executeGet(frame).getSender();
        assert oldContext != null;
        // Record a process to be awakened on the next interpreter cycle.
        PointersObject scheduler = getSchedulerNode.executeGet();
        assert newProcess != scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS); // trying to switch to already active process
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, oldContext);
        ContextObject newActiveContext = (ContextObject) newProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        newProcess.atput0(PROCESS.SUSPENDED_CONTEXT, code.image.nil);
        throw new ProcessSwitch(newActiveContext);
    }
}

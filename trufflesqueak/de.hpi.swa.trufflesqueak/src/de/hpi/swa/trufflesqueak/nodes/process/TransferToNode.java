package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public abstract class TransferToNode extends AbstractProcessNode {
    @Child private GetSchedulerNode getSchedulerNode;

    public static TransferToNode create(SqueakImageContext image) {
        return TransferToNodeGen.create(image);
    }

    protected TransferToNode(SqueakImageContext image) {
        super(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    public abstract void executeTransferTo(VirtualFrame frame, BaseSqueakObject activeProcess, BaseSqueakObject newProcess);

    @Specialization
    protected void transferTo(VirtualFrame frame, BaseSqueakObject activeProcess, BaseSqueakObject newProcess) {
        CompilerDirectives.transferToInterpreter();
        MethodContextObject activeContext = MethodContextObject.createReadOnlyContextObject(image, frame);
        assert activeContext != null;
        // Record a process to be awakened on the next interpreter cycle.
        PointersObject scheduler = getSchedulerNode.executeGet();
        scheduler.atput0(PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, activeContext);
        MethodContextObject newActiveContext = (MethodContextObject) newProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        newProcess.atput0(PROCESS.SUSPENDED_CONTEXT, image.nil);
        throw new ProcessSwitch(newActiveContext);
    }
}

package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class GetActiveProcessNode extends AbstractNodeWithCode {
    @Child private GetSchedulerNode getSchedulerNode;

    public static GetActiveProcessNode create(CompiledCodeObject code) {
        return new GetActiveProcessNode(code);
    }

    protected GetActiveProcessNode(CompiledCodeObject code) {
        super(code);
        getSchedulerNode = GetSchedulerNode.create(code);
    }

    public PointersObject executeGet() {
        PointersObject scheduler = getSchedulerNode.executeGet();
        return (PointersObject) scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }
}

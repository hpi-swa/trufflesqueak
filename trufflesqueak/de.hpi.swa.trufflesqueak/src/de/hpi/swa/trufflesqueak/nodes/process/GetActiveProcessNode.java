package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class GetActiveProcessNode extends AbstractNodeWithImage {
    @Child private GetSchedulerNode getSchedulerNode;

    public static GetActiveProcessNode create(final SqueakImageContext image) {
        return new GetActiveProcessNode(image);
    }

    protected GetActiveProcessNode(final SqueakImageContext image) {
        super(image);
        getSchedulerNode = GetSchedulerNode.create(image);
    }

    public PointersObject executeGet() {
        final PointersObject scheduler = getSchedulerNode.executeGet();
        return (PointersObject) scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }
}

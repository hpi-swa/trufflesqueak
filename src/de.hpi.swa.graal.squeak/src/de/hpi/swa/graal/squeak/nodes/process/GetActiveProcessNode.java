package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

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

package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public final class GetActiveProcessNode extends AbstractNodeWithImage {
    protected GetActiveProcessNode(final SqueakImageContext image) {
        super(image);
    }

    public static GetActiveProcessNode create(final SqueakImageContext image) {
        return new GetActiveProcessNode(image);
    }

    public PointersObject executeGet() {
        return (PointersObject) image.getScheduler().at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class GetSchedulerNode extends AbstractNodeWithImage {

    public static GetSchedulerNode create(final SqueakImageContext image) {
        return new GetSchedulerNode(image);
    }

    protected GetSchedulerNode(final SqueakImageContext image) {
        super(image);
    }

    protected PointersObject executeGet() {
        final PointersObject association = (PointersObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
        return (PointersObject) association.at0(ASSOCIATION.VALUE);
    }
}

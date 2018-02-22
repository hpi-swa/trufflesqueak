package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class GetSchedulerNode extends AbstractNodeWithImage {

    public static GetSchedulerNode create(SqueakImageContext image) {
        return new GetSchedulerNode(image);
    }

    protected GetSchedulerNode(SqueakImageContext image) {
        super(image);
    }

    protected PointersObject executeGet() {
        PointersObject association = (PointersObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
        return (PointersObject) association.at0(ASSOCIATION.VALUE);
    }
}

package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public abstract class GetSchedulerNode extends AbstractProcessNode {

    public static GetSchedulerNode create(SqueakImageContext image) {
        return GetSchedulerNodeGen.create(image);
    }

    protected GetSchedulerNode(SqueakImageContext image) {
        super(image);
    }

    public abstract PointersObject executeGet();

    @Specialization
    protected PointersObject getScheduler() {
        PointersObject association = (PointersObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
        return (PointersObject) association.at0(ASSOCIATION.VALUE);
    }
}

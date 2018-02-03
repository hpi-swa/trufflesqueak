package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class GetSchedulerNode extends AbstractNodeWithCode {

    public static GetSchedulerNode create(CompiledCodeObject code) {
        return new GetSchedulerNode(code);
    }

    protected GetSchedulerNode(CompiledCodeObject code) {
        super(code);
    }

    protected PointersObject executeGet() {
        PointersObject association = (PointersObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
        return (PointersObject) association.at0(ASSOCIATION.VALUE);
    }
}

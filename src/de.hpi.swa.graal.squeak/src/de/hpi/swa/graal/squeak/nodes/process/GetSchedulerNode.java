package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public final class GetSchedulerNode extends AbstractNodeWithImage {
    @CompilationFinal private static PointersObject scheduler;

    public static GetSchedulerNode create(final SqueakImageContext image) {
        return new GetSchedulerNode(image);
    }

    protected GetSchedulerNode(final SqueakImageContext image) {
        super(image);
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final PointersObject association = (PointersObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
            scheduler = (PointersObject) association.at0(ASSOCIATION.VALUE);
        }
    }

    protected static PointersObject executeGet() {
        return scheduler;
    }
}

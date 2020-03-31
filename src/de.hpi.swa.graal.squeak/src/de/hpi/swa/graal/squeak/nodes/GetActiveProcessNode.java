package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class GetActiveProcessNode extends AbstractNode {
    public static GetActiveProcessNode create() {
        return GetActiveProcessNodeGen.create();
    }

    public abstract PointersObject execute();

    @Specialization
    protected static final PointersObject doGetActiveProcess(@CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                    @Cached final AbstractPointersObjectReadNode readNode) {
        return image.getActiveProcess(readNode);
    }
}

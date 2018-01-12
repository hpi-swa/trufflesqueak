package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;

public abstract class IsEmptyListNode extends AbstractProcessNode {
    public static IsEmptyListNode create(SqueakImageContext image) {
        return IsEmptyListNodeGen.create(image);
    }

    protected IsEmptyListNode(SqueakImageContext image) {
        super(image);
    }

    public abstract boolean executeIsEmpty(BaseSqueakObject list);

    @Specialization
    protected boolean isEmptyList(BaseSqueakObject list) {
        return list.at0(LINKED_LIST.FIRST_LINK) == image.nil;
    }
}

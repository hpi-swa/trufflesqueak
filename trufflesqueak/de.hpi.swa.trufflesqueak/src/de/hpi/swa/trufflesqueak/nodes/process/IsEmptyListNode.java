package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;

public class IsEmptyListNode extends AbstractProcessNode {
    public static IsEmptyListNode create(SqueakImageContext image) {
        return new IsEmptyListNode(image);
    }

    protected IsEmptyListNode(SqueakImageContext image) {
        super(image);
    }

    public boolean executeIsEmpty(BaseSqueakObject list) {
        return list.at0(LINKED_LIST.FIRST_LINK) == image.nil;
    }
}

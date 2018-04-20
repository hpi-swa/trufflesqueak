package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class IsEmptyListNode extends AbstractNodeWithImage {
    public static IsEmptyListNode create(final SqueakImageContext image) {
        return new IsEmptyListNode(image);
    }

    protected IsEmptyListNode(final SqueakImageContext image) {
        super(image);
    }

    public boolean executeIsEmpty(final BaseSqueakObject list) {
        return list.at0(LINKED_LIST.FIRST_LINK) == image.nil;
    }
}

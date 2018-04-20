package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

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

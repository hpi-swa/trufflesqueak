package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;

public class IsEmptyListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

    public static IsEmptyListNode create(final SqueakImageContext image) {
        return new IsEmptyListNode(image);
    }

    protected IsEmptyListNode(final SqueakImageContext image) {
        super(image);
    }

    public boolean executeIsEmpty(final AbstractSqueakObject list) {
        return at0Node.execute(list, LINKED_LIST.FIRST_LINK) == image.nil;
    }
}

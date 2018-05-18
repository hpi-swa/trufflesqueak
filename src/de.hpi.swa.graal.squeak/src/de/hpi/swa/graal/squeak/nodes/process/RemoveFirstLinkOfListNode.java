package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;

public class RemoveFirstLinkOfListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

    public static RemoveFirstLinkOfListNode create(final SqueakImageContext image) {
        return new RemoveFirstLinkOfListNode(image);
    }

    protected RemoveFirstLinkOfListNode(final SqueakImageContext image) {
        super(image);
    }

    public AbstractSqueakObject executeRemove(final AbstractSqueakObject list) {
        // Remove the first process from the given linked list.
        final AbstractSqueakObject first = (AbstractSqueakObject) at0Node.execute(list, LINKED_LIST.FIRST_LINK);
        final AbstractSqueakObject last = (AbstractSqueakObject) at0Node.execute(list, LINKED_LIST.LAST_LINK);
        if (first.equals(last)) {
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, image.nil);
            atPut0Node.execute(list, LINKED_LIST.LAST_LINK, image.nil);
        } else {
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, at0Node.execute(first, LINK.NEXT_LINK));
        }
        atPut0Node.execute(first, LINK.NEXT_LINK, image.nil);
        return first;
    }
}

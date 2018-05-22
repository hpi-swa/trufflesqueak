package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;

public class RemoveProcessFromListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

    public static RemoveProcessFromListNode create(final SqueakImageContext image) {
        return new RemoveProcessFromListNode(image);
    }

    protected RemoveProcessFromListNode(final SqueakImageContext image) {
        super(image);
    }

    public void executeRemove(final AbstractSqueakObject process, final AbstractSqueakObject list) {
        final AbstractSqueakObject first = (AbstractSqueakObject) at0Node.execute(list, LINKED_LIST.FIRST_LINK);
        final AbstractSqueakObject last = (AbstractSqueakObject) at0Node.execute(list, LINKED_LIST.LAST_LINK);
        if (process.equals(first)) {
            final Object next = at0Node.execute(process, LINK.NEXT_LINK);
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, next);
            if (process.equals(last)) {
                atPut0Node.execute(list, LINKED_LIST.LAST_LINK, image.nil);
            }
        } else {
            AbstractSqueakObject temp = first;
            AbstractSqueakObject next;
            while (true) {
                if (temp.isNil()) {
                    throw new PrimitiveFailed();
                }
                next = (AbstractSqueakObject) at0Node.execute(temp, LINK.NEXT_LINK);
                if (next.equals(process)) {
                    break;
                }
                temp = next;
            }
            next = (AbstractSqueakObject) at0Node.execute(process, LINK.NEXT_LINK);
            atPut0Node.execute(temp, LINK.NEXT_LINK, next);
            if (process.equals(last)) {
                atPut0Node.execute(list, LINKED_LIST.LAST_LINK, temp);
            }
        }
        atPut0Node.execute(process, LINK.NEXT_LINK, image.nil);
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class RemoveProcessFromListNode extends AbstractNodeWithImage {

    public static RemoveProcessFromListNode create(final SqueakImageContext image) {
        return new RemoveProcessFromListNode(image);
    }

    protected RemoveProcessFromListNode(final SqueakImageContext image) {
        super(image);
    }

    public void executeRemove(final BaseSqueakObject process, final BaseSqueakObject list) {
        final BaseSqueakObject first = (BaseSqueakObject) list.at0(LINKED_LIST.FIRST_LINK);
        final BaseSqueakObject last = (BaseSqueakObject) list.at0(LINKED_LIST.LAST_LINK);
        if (process.equals(first)) {
            final Object next = process.at0(LINK.NEXT_LINK);
            list.atput0(LINKED_LIST.FIRST_LINK, next);
            if (process.equals(last)) {
                list.atput0(LINKED_LIST.LAST_LINK, image.nil);
            }
        } else {
            BaseSqueakObject temp = first;
            BaseSqueakObject next;
            while (true) {
                if (temp.isNil()) {
                    throw new PrimitiveFailed();
                }
                next = (BaseSqueakObject) temp.at0(LINK.NEXT_LINK);
                if (next.equals(process)) {
                    break;
                }
                temp = next;
            }
            next = (BaseSqueakObject) process.at0(LINK.NEXT_LINK);
            temp.atput0(LINK.NEXT_LINK, next);
            if (process.equals(last)) {
                list.atput0(LINKED_LIST.LAST_LINK, temp);
            }
        }
        process.atput0(LINK.NEXT_LINK, image.nil);
    }
}

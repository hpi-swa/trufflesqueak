package de.hpi.swa.graal.squeak.nodes.process;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class LinkProcessToListNode extends AbstractNodeWithImage {
    @Child private IsEmptyListNode isEmptyListNode;

    public static LinkProcessToListNode create(final SqueakImageContext image) {
        return new LinkProcessToListNode(image);
    }

    protected LinkProcessToListNode(final SqueakImageContext image) {
        super(image);
        isEmptyListNode = IsEmptyListNode.create(image);
    }

    public void executeLink(final BaseSqueakObject process, final PointersObject list) {
        // Add the given process to the given linked list and set the backpointer
        // of process to its new list.
        if (isEmptyListNode.executeIsEmpty(list)) {
            list.atput0(LINKED_LIST.FIRST_LINK, process);
        } else {
            final PointersObject lastLink = (PointersObject) list.at0(LINKED_LIST.LAST_LINK);
            lastLink.atput0(LINK.NEXT_LINK, process);
        }
        list.atput0(LINKED_LIST.LAST_LINK, process);
        process.atput0(PROCESS.LIST, list);
    }
}

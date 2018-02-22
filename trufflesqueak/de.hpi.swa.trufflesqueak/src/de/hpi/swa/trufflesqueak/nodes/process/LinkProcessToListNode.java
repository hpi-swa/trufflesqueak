package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINK;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public class LinkProcessToListNode extends AbstractNodeWithImage {
    @Child private IsEmptyListNode isEmptyListNode;

    public static LinkProcessToListNode create(SqueakImageContext image) {
        return new LinkProcessToListNode(image);
    }

    protected LinkProcessToListNode(SqueakImageContext image) {
        super(image);
        isEmptyListNode = IsEmptyListNode.create(image);
    }

    public void executeLink(BaseSqueakObject process, PointersObject list) {
        // Add the given process to the given linked list and set the backpointer
        // of process to its new list.
        if (isEmptyListNode.executeIsEmpty(list)) {
            list.atput0(LINKED_LIST.FIRST_LINK, process);
        } else {
            PointersObject lastLink = (PointersObject) list.at0(LINKED_LIST.LAST_LINK);
            lastLink.atput0(LINK.NEXT_LINK, process);
        }
        list.atput0(LINKED_LIST.LAST_LINK, process);
        process.atput0(PROCESS.LIST, list);
    }
}

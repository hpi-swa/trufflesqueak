package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINK;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class LinkProcessToListNode extends AbstractNodeWithCode {
    @Child private IsEmptyListNode isEmptyListNode;

    public static LinkProcessToListNode create(CompiledCodeObject code) {
        return new LinkProcessToListNode(code);
    }

    protected LinkProcessToListNode(CompiledCodeObject code) {
        super(code);
        isEmptyListNode = IsEmptyListNode.create(code);
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

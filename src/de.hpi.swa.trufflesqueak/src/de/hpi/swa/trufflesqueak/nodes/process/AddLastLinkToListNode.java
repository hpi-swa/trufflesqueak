/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

/*
 * Add the given process to the given linked list and set the backpointer of process to its new list.
 */
public final class AddLastLinkToListNode extends AbstractNode {
    @Child private AbstractPointersObjectReadNode readEmptyNode = AbstractPointersObjectReadNode.create();
    @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();
    @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();
    @Child private AbstractPointersObjectWriteNode writeLastLinkNode = AbstractPointersObjectWriteNode.create();
    @Child private AbstractPointersObjectWriteNode writeListNode = AbstractPointersObjectWriteNode.create();

    public static AddLastLinkToListNode create() {
        return new AddLastLinkToListNode();
    }

    public void execute(final PointersObject process, final PointersObject list) {
        if (list.isEmptyList(readEmptyNode)) {
            writeNode.execute(list, LINKED_LIST.FIRST_LINK, process);
        } else {
            final PointersObject lastLink = readNode.executePointers(list, LINKED_LIST.LAST_LINK);
            writeNode.execute(lastLink, PROCESS.NEXT_LINK, process);
        }
        writeLastLinkNode.execute(list, LINKED_LIST.LAST_LINK, process);
        writeListNode.execute(process, PROCESS.LIST, list);
    }
}

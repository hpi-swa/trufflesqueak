/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

/*
 * Add the given process to the given end of the given linked list.
 */
@GenerateInline(false)
@GenerateCached
public abstract class AddLinkToListNode extends AbstractNode {

    public static void executeUncached(final PointersObject process, final PointersObject list, final boolean addLast) {
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        addLinkToList(process, list, addLast, readNode, readNode, readNode, writeNode, writeNode, writeNode, writeNode);
    }

    public abstract void execute(PointersObject process, PointersObject list, boolean addLast);

    /**
     * <pre>
     * Adding as the firstLink versus the lastLink differ in two ways.
     * 1. LAST_LINK and FIRST_LINK are interchanged
     * 2. process.nextLink = firstLink versus lastLink.nextLink = process
     * </pre>
     */
    @Specialization
    protected static final void addLinkToList(final PointersObject process, final PointersObject list, final boolean addLast,
                    @Cached final AbstractPointersObjectReadNode readEmptyNode,
                    @Cached final AbstractPointersObjectReadNode readFirstLinkNode,
                    @Cached final AbstractPointersObjectReadNode readLastLinkNode,
                    @Cached final AbstractPointersObjectWriteNode writeFirstLinkNode,
                    @Cached final AbstractPointersObjectWriteNode writeLastLinkNode,
                    @Cached final AbstractPointersObjectWriteNode writeNextLinkNode,
                    @Cached final AbstractPointersObjectWriteNode writeListNode) {
        writeListNode.execute(process, PROCESS.LIST, list);
        if (list.isEmptyList(readEmptyNode)) {
            writeFirstLinkNode.execute(list, LINKED_LIST.FIRST_LINK, process);
            writeLastLinkNode.execute(list, LINKED_LIST.LAST_LINK, process);
        } else if (addLast) {
            final PointersObject lastLink = readLastLinkNode.executePointers(list, LINKED_LIST.LAST_LINK);
            writeLastLinkNode.execute(list, LINKED_LIST.LAST_LINK, process);
            writeNextLinkNode.execute(lastLink, PROCESS.NEXT_LINK, process);
        } else {
            final PointersObject firstLink = readFirstLinkNode.executePointers(list, LINKED_LIST.FIRST_LINK);
            writeFirstLinkNode.execute(list, LINKED_LIST.FIRST_LINK, process);
            writeNextLinkNode.execute(process, PROCESS.NEXT_LINK, firstLink);
        }
    }
}

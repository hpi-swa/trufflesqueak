/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

/*
 * Add the given process to the given linked list and set the backpointer of process to its new list.
 */
@GenerateInline
@GenerateCached(false)
public abstract class AddLastLinkToListNode extends AbstractNode {

    public abstract void execute(Node node, PointersObject process, PointersObject list);

    @Specialization
    protected static final void addLastLinkToList(final PointersObject process, final PointersObject list,
                    @Cached final AbstractPointersObjectReadNode readEmptyNode,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached final AbstractPointersObjectWriteNode writeLastLinkNode,
                    @Cached final AbstractPointersObjectWriteNode writeListNode) {
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

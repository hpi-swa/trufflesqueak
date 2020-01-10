/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

public abstract class LinkProcessToListNode extends AbstractNode {
    @Child private AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.create();

    public static LinkProcessToListNode create() {
        return LinkProcessToListNodeGen.create();
    }

    public abstract void executeLink(PointersObject process, PointersObject list);

    @Specialization(guards = "!list.isEmptyList()")
    protected void doLinkNotEmptyList(final PointersObject process, final PointersObject list) {
        writeNode.execute((AbstractPointersObject) list.getLastLink(), PROCESS.NEXT_LINK, process);
        writeNode.execute(list, LINKED_LIST.LAST_LINK, process);
        writeNode.execute(process, PROCESS.LIST, list);
    }

    @Specialization(guards = "list.isEmptyList()")
    protected void doLinkEmptyList(final PointersObject process, final PointersObject list) {
        // Add the given process to the given linked list and set the backpointer
        // of process to its new list.
        writeNode.execute(list, LINKED_LIST.FIRST_LINK, process);
        writeNode.execute(list, LINKED_LIST.LAST_LINK, process);
        writeNode.execute(process, PROCESS.LIST, list);
    }
}

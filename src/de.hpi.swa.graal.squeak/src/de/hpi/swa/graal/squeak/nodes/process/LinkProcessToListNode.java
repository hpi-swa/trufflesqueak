/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class LinkProcessToListNode extends AbstractNode {
    public static LinkProcessToListNode create() {
        return LinkProcessToListNodeGen.create();
    }

    public abstract void executeLink(PointersObject process, PointersObject list);

    @Specialization(guards = "list.isEmptyList()")
    protected void doLinkEmptyList(final PointersObject process, final PointersObject list) {
        // Add the given process to the given linked list and set the backpointer
        // of process to its new list.
        list.atput0(LINKED_LIST.FIRST_LINK, process);
        list.atput0(LINKED_LIST.LAST_LINK, process);
        process.atput0(PROCESS.LIST, list);
    }

    @Fallback
    protected void doLinkNotEmptyList(final PointersObject process, final PointersObject list) {
        ((PointersObject) list.at0(LINKED_LIST.LAST_LINK)).atput0(PROCESS.NEXT_LINK, process);
        list.atput0(LINKED_LIST.LAST_LINK, process);
        process.atput0(PROCESS.LIST, list);
    }
}

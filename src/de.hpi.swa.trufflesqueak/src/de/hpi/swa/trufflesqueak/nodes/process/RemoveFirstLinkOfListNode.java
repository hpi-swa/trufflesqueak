/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

public abstract class RemoveFirstLinkOfListNode extends AbstractNode {
    @Child protected AbstractPointersObjectReadNode readFirstNode = AbstractPointersObjectReadNode.create();
    @Child protected AbstractPointersObjectReadNode readLastNode = AbstractPointersObjectReadNode.create();
    @Child private AbstractPointersObjectWriteNode writeFirstNode = AbstractPointersObjectWriteNode.create();
    @Child private AbstractPointersObjectWriteNode writeNextNode = AbstractPointersObjectWriteNode.create();

    public static RemoveFirstLinkOfListNode create() {
        return RemoveFirstLinkOfListNodeGen.create();
    }

    public final PointersObject execute(final PointersObject list) {
        final PointersObject first = readFirstNode.executePointers(list, LINKED_LIST.FIRST_LINK);
        executeSpecialized(list, first);
        writeNextNode.executeNil(first, PROCESS.NEXT_LINK);
        return first;
    }

    public abstract void executeSpecialized(PointersObject process, PointersObject first);

    @Specialization(guards = "isLast(list, first)")
    protected final void doFirstLastIdentical(final PointersObject list, @SuppressWarnings("unused") final PointersObject first,
                    @Cached final AbstractPointersObjectWriteNode writeLastNode) {
        writeFirstNode.executeNil(list, LINKED_LIST.FIRST_LINK);
        writeLastNode.executeNil(list, LINKED_LIST.LAST_LINK);
    }

    @Specialization(guards = "!isLast(list, first)")
    protected final void doFirstLastNotIdentical(final PointersObject list, final PointersObject first,
                    @Cached final AbstractPointersObjectReadNode readNextNode) {
        writeFirstNode.execute(list, LINKED_LIST.FIRST_LINK, readNextNode.execute(first, PROCESS.NEXT_LINK));
    }

    protected final boolean isLast(final PointersObject list, final PointersObject first) {
        return first == readLastNode.execute(list, LINKED_LIST.LAST_LINK);
    }
}

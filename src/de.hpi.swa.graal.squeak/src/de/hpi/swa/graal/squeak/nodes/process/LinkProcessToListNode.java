package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

public abstract class LinkProcessToListNode extends AbstractNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child protected IsEmptyListNode isEmptyListNode;

    public static LinkProcessToListNode create(final SqueakImageContext image) {
        return LinkProcessToListNodeGen.create(image);
    }

    protected LinkProcessToListNode(final SqueakImageContext image) {
        isEmptyListNode = IsEmptyListNode.create(image);
    }

    public abstract void executeLink(Object process, Object list);

    @Specialization(guards = "isEmptyListNode.executeIsEmpty(list)")
    protected void doLinkEmptyList(final AbstractSqueakObject process, final PointersObject list) {
        // Add the given process to the given linked list and set the backpointer
        // of process to its new list.
        list.atput0(LINKED_LIST.FIRST_LINK, process);
        list.atput0(LINKED_LIST.LAST_LINK, process);
        atPut0Node.execute(process, PROCESS.LIST, list);
    }

    @Specialization(guards = "!isEmptyListNode.executeIsEmpty(list)")
    protected void doLinkNotEmptyList(final AbstractSqueakObject process, final PointersObject list) {
        atPut0Node.execute(list.at0(LINKED_LIST.LAST_LINK), LINK.NEXT_LINK, process);
        list.atput0(LINKED_LIST.LAST_LINK, process);
        atPut0Node.execute(process, PROCESS.LIST, list);
    }

    @Fallback
    protected void doFallback(final Object process, final Object list) {
        throw new SqueakException("Expected [AbstractSqueakObject, PointersObject], got [" + process + ", " + list + "]");
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

public abstract class RemoveFirstLinkOfListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

    public static RemoveFirstLinkOfListNode create(final SqueakImageContext image) {
        return RemoveFirstLinkOfListNodeGen.create(image);
    }

    protected RemoveFirstLinkOfListNode(final SqueakImageContext image) {
        super(image);
    }

    public abstract Object executeRemove(Object list);

    @Specialization
    protected final Object executeRemove(final AbstractSqueakObject list) {
        // Remove the first process from the given linked list.
        final Object first = at0Node.execute(list, LINKED_LIST.FIRST_LINK);
        final Object last = at0Node.execute(list, LINKED_LIST.LAST_LINK);
        if (first == last) {
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, image.nil);
            atPut0Node.execute(list, LINKED_LIST.LAST_LINK, image.nil);
        } else {
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, at0Node.execute(first, PROCESS.NEXT_LINK));
        }
        atPut0Node.execute(first, PROCESS.NEXT_LINK, image.nil);
        return first;
    }

    @Fallback
    protected static final Object doFallback(final Object list) {
        throw new SqueakException("Unexpected list object:", list);
    }
}

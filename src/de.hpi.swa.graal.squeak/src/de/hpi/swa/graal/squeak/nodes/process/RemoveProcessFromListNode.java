package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

public abstract class RemoveProcessFromListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

    public static RemoveProcessFromListNode create(final SqueakImageContext image) {
        return RemoveProcessFromListNodeGen.create(image);
    }

    protected RemoveProcessFromListNode(final SqueakImageContext image) {
        super(image);
    }

    public abstract void executeRemove(Object process, Object list);

    @Specialization
    public void executeRemove(final AbstractSqueakObject process, final AbstractSqueakObject list) {
        final Object first = at0Node.execute(list, LINKED_LIST.FIRST_LINK);
        final Object last = at0Node.execute(list, LINKED_LIST.LAST_LINK);
        if (process.equals(first)) {
            final Object next = at0Node.execute(process, LINK.NEXT_LINK);
            atPut0Node.execute(list, LINKED_LIST.FIRST_LINK, next);
            if (process.equals(last)) {
                atPut0Node.execute(list, LINKED_LIST.LAST_LINK, image.nil);
            }
        } else {
            Object temp = first;
            Object next;
            while (true) {
                if (temp == image.nil) {
                    throw new PrimitiveFailed();
                }
                next = at0Node.execute(temp, LINK.NEXT_LINK);
                if (next.equals(process)) {
                    break;
                }
                temp = next;
            }
            next = at0Node.execute(process, LINK.NEXT_LINK);
            atPut0Node.execute(temp, LINK.NEXT_LINK, next);
            if (process.equals(last)) {
                atPut0Node.execute(list, LINKED_LIST.LAST_LINK, temp);
            }
        }
        atPut0Node.execute(process, LINK.NEXT_LINK, image.nil);
    }

    @Fallback
    protected static final void doFallback(final Object process, final Object list) {
        throw new SqueakException("Unexpected process and list: " + process + " and " + list);
    }
}

package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;

public abstract class IsEmptyListNode extends AbstractNodeWithImage {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

    public static IsEmptyListNode create(final SqueakImageContext image) {
        return IsEmptyListNodeGen.create(image);
    }

    protected IsEmptyListNode(final SqueakImageContext image) {
        super(image);
    }

    public abstract boolean executeIsEmpty(Object list);

    @Specialization
    public boolean executeIsEmpty(final AbstractSqueakObject list) {
        return at0Node.execute(list, LINKED_LIST.FIRST_LINK) == image.nil;
    }

    @Fallback
    protected static final boolean doFallback(final Object list) {
        throw new SqueakException("Unexpected list object: " + list);
    }
}

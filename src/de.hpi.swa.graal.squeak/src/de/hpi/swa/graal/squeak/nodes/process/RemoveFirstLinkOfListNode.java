package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINK;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public class RemoveFirstLinkOfListNode extends AbstractNodeWithImage {

    public static RemoveFirstLinkOfListNode create(final SqueakImageContext image) {
        return new RemoveFirstLinkOfListNode(image);
    }

    protected RemoveFirstLinkOfListNode(final SqueakImageContext image) {
        super(image);
    }

    public BaseSqueakObject executeRemove(final BaseSqueakObject list) {
        CompilerDirectives.transferToInterpreter();
        // Remove the first process from the given linked list.
        final BaseSqueakObject first = (BaseSqueakObject) list.at0(LINKED_LIST.FIRST_LINK);
        final BaseSqueakObject last = (BaseSqueakObject) list.at0(LINKED_LIST.LAST_LINK);
        if (first.equals(last)) {
            list.atput0(LINKED_LIST.FIRST_LINK, image.nil);
            list.atput0(LINKED_LIST.LAST_LINK, image.nil);
        } else {
            list.atput0(LINKED_LIST.FIRST_LINK, first.at0(LINK.NEXT_LINK));
        }
        first.atput0(LINK.NEXT_LINK, image.nil);
        return first;
    }
}

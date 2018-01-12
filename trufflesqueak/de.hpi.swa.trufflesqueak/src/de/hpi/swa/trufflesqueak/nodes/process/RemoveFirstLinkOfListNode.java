package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINK;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;

public class RemoveFirstLinkOfListNode extends AbstractProcessNode {
    public static RemoveFirstLinkOfListNode create(SqueakImageContext image) {
        return new RemoveFirstLinkOfListNode(image);
    }

    protected RemoveFirstLinkOfListNode(SqueakImageContext image) {
        super(image);
    }

    public BaseSqueakObject executeRemove(BaseSqueakObject list) {
        CompilerDirectives.transferToInterpreter();
        // Remove the first process from the given linked list.
        BaseSqueakObject first = (BaseSqueakObject) list.at0(LINKED_LIST.FIRST_LINK);
        BaseSqueakObject last = (BaseSqueakObject) list.at0(LINKED_LIST.LAST_LINK);
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

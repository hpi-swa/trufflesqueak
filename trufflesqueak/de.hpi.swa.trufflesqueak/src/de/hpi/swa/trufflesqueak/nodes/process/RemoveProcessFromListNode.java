package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINK;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;

public abstract class RemoveProcessFromListNode extends AbstractProcessNode {

    public static RemoveProcessFromListNode create(SqueakImageContext image) {
        return RemoveProcessFromListNodeGen.create(image);
    }

    protected RemoveProcessFromListNode(SqueakImageContext image) {
        super(image);
    }

    public abstract void executeRemove(BaseSqueakObject process, BaseSqueakObject list);

    @Specialization
    protected void removeProcessFromList(BaseSqueakObject process, BaseSqueakObject list) {
        BaseSqueakObject first = (BaseSqueakObject) list.at0(LINKED_LIST.FIRST_LINK);
        BaseSqueakObject last = (BaseSqueakObject) list.at0(LINKED_LIST.LAST_LINK);
        if (process.equals(first)) {
            Object next = process.at0(LINK.NEXT_LINK);
            list.atput0(LINKED_LIST.FIRST_LINK, next);
            if (process.equals(last)) {
                list.atput0(LINKED_LIST.LAST_LINK, image.nil);
            }
        } else {
            BaseSqueakObject temp = first;
            BaseSqueakObject next;
            while (true) {
                if (temp == image.nil) {
                    throw new PrimitiveFailed();
                }
                next = (BaseSqueakObject) temp.at0(LINK.NEXT_LINK);
                if (next.equals(process)) {
                    break;
                }
                temp = next;
            }
            next = (BaseSqueakObject) process.at0(LINK.NEXT_LINK);
            temp.atput0(LINK.NEXT_LINK, next);
            if (process.equals(last)) {
                list.atput0(LINKED_LIST.LAST_LINK, temp);
            }
        }
        process.atput0(LINK.NEXT_LINK, image.nil);
    }
}

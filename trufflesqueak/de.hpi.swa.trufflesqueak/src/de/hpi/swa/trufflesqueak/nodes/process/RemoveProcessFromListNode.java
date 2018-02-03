package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINK;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class RemoveProcessFromListNode extends AbstractNodeWithCode {

    public static RemoveProcessFromListNode create(CompiledCodeObject code) {
        return new RemoveProcessFromListNode(code);
    }

    protected RemoveProcessFromListNode(CompiledCodeObject code) {
        super(code);
    }

    public void executeRemove(BaseSqueakObject process, BaseSqueakObject list) {
        BaseSqueakObject first = (BaseSqueakObject) list.at0(LINKED_LIST.FIRST_LINK);
        BaseSqueakObject last = (BaseSqueakObject) list.at0(LINKED_LIST.LAST_LINK);
        if (process.equals(first)) {
            Object next = process.at0(LINK.NEXT_LINK);
            list.atput0(LINKED_LIST.FIRST_LINK, next);
            if (process.equals(last)) {
                list.atput0(LINKED_LIST.LAST_LINK, code.image.nil);
            }
        } else {
            BaseSqueakObject temp = first;
            BaseSqueakObject next;
            while (true) {
                if (temp == code.image.nil) {
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
        process.atput0(LINK.NEXT_LINK, code.image.nil);
    }
}

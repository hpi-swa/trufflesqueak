package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public class IsEmptyListNode extends AbstractNodeWithCode {
    public static IsEmptyListNode create(CompiledCodeObject code) {
        return new IsEmptyListNode(code);
    }

    protected IsEmptyListNode(CompiledCodeObject code) {
        super(code);
    }

    public boolean executeIsEmpty(BaseSqueakObject list) {
        return list.at0(LINKED_LIST.FIRST_LINK) == code.image.nil;
    }
}

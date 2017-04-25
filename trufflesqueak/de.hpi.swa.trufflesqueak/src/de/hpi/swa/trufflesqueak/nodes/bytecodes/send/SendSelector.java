package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SendSelector extends AbstractSend {
    public SendSelector(CompiledMethodObject cm, int idx, BaseSqueakObject sel, int argcount) {
        super(cm, idx, sel, argcount);
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SingleExtendedSend extends AbstractSend {
    public SingleExtendedSend(CompiledMethodObject cm, int idx, int param) {
        super(cm, idx, cm.getLiteral(param & 31), param >> 5);
    }
}

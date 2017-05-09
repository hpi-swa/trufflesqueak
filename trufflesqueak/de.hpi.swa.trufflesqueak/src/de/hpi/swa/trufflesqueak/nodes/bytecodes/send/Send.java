package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class Send extends AbstractSend {
    public Send(CompiledCodeObject cm, int idx, int b) {
        super(cm, idx, cm.getLiteral(b & 15), ((b >> 4) & 3) - 1);
    }
}

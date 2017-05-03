package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SecondExtendedSendNode extends AbstractSend {
    public SecondExtendedSendNode(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, cm.getLiteral(i & 63), i >> 6);
    }
}

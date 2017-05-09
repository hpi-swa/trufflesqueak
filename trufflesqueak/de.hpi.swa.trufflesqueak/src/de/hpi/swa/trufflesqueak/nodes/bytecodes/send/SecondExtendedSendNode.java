package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SecondExtendedSendNode extends AbstractSend {
    public SecondExtendedSendNode(CompiledCodeObject method, int idx, int i) {
        super(method, idx, method.getLiteral(i & 63), i >> 6);
    }
}

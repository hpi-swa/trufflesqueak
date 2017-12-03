package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SecondExtendedSendNode extends AbstractSend {
    public SecondExtendedSendNode(CompiledCodeObject code, int idx, int i) {
        super(code, idx, code.getLiteral(i & 63), i >> 6);
    }
}

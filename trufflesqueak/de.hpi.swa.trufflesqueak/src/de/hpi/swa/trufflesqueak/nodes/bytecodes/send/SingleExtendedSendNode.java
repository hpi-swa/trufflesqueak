package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SingleExtendedSendNode extends AbstractSend {
    public SingleExtendedSendNode(CompiledCodeObject method, int idx, int param) {
        super(method, idx, method.getLiteral(param & 31), param >> 5);
    }
}

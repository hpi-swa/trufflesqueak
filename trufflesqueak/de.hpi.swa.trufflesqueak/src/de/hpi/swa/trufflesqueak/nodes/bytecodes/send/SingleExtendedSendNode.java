package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SingleExtendedSendNode extends AbstractSendNode {
    public SingleExtendedSendNode(CompiledCodeObject code, int idx, int param) {
        super(code, idx, code.getLiteral(param & 31), param >> 5);
    }
}

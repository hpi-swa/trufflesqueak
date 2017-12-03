package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendNode extends AbstractSendNode {
    public SendNode(CompiledCodeObject code, int index, int b) {
        super(code, index, code.getLiteral(b & 15), ((b >> 4) & 3) - 1);
    }
}

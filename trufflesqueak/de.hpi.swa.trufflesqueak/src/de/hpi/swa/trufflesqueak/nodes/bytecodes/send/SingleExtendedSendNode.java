package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SingleExtendedSendNode extends AbstractSendNode {
    public SingleExtendedSendNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
        super(code, index, numBytecodes, code.getLiteral(param & 31), param >> 5);
    }
}

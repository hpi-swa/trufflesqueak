package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SecondExtendedSendNode extends AbstractSendNode {
    public SecondExtendedSendNode(CompiledCodeObject code, int index, int numBytecodes, int i) {
        super(code, index, numBytecodes, code.getLiteral(i & 63), i >> 6);
    }
}

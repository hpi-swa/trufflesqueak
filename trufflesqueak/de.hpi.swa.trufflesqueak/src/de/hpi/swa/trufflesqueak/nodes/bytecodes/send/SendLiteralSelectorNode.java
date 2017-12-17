package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendLiteralSelectorNode extends AbstractSendNode {
    public SendLiteralSelectorNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int argCount) {
        super(code, index, numBytecodes, code.getLiteral(literalIndex), argCount);
    }
}

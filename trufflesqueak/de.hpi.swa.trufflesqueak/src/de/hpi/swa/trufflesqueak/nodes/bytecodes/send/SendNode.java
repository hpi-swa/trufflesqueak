package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendNode extends AbstractSendNode {
    public SendNode(CompiledCodeObject code, int index, int literalIndex, int argCount) {
        super(code, index, code.getLiteral(literalIndex), argCount);
    }
}

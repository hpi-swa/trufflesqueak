package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.HaltNode;

public class SendLiteralSelectorNode extends AbstractSendNode {
    public SendLiteralSelectorNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int argCount) {
        super(code, index, numBytecodes, code.getLiteral(literalIndex), argCount);
    }

    public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int argCount) {
        if (code.getLiteral(literalIndex).toString().equals("halt")) {
            return new HaltNode(code, index);
        }
        return new SendLiteralSelectorNode(code, index, numBytecodes, literalIndex, argCount);
    }
}

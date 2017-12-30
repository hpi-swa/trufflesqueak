package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.HaltNode;

public class SendLiteralSelectorNode extends AbstractSendNode {
    public SendLiteralSelectorNode(CompiledCodeObject code, int index, int numBytecodes, Object selector, int argCount) {
        super(code, index, numBytecodes, selector, argCount);
    }

    public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int argCount) {
        Object selector = code.getLiteral(literalIndex);
        if (selector != null && selector.toString().equals("halt")) {
            return new HaltNode(code, index);
        }
        return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
    }
}

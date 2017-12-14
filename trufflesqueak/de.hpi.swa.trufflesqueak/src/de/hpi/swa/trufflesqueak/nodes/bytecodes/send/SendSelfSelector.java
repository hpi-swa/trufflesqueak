package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendSelfSelector extends AbstractSendNode {
    public SendSelfSelector(CompiledCodeObject code, int index, int numBytecodes, Object selector, int numArgs) {
        super(code, index, numBytecodes, selector, numArgs);
    }
}

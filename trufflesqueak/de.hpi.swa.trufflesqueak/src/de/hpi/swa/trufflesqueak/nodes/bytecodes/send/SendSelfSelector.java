package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendSelfSelector extends AbstractSend {
    public SendSelfSelector(CompiledCodeObject code, int index, Object selector, int numArgs) {
        super(code, index, selector, numArgs);
    }
}

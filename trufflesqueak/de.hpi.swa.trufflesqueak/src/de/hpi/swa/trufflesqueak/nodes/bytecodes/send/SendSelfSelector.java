package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SendSelfSelector extends AbstractSend {
    public SendSelfSelector(CompiledCodeObject method, int idx, Object selector, int numArgs) {
        super(method, idx, selector, numArgs);
    }
}

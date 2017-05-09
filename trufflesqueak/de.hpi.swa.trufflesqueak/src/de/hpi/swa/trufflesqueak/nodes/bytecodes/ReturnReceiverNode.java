package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnReceiverNode extends ReturnNode {
    public ReturnReceiverNode(CompiledCodeObject method, int idx) {
        super(method, idx);
        valueNode = new ReceiverNode(method, idx);
    }
}

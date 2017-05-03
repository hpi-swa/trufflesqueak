package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnReceiverNode extends ReturnNode {
    public ReturnReceiverNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
        valueNode = new ReceiverNode(cm, idx);
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.CompiledInClass;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakClassNodeGen;

public class SingleExtendedSuper extends AbstractSend {

    public SingleExtendedSuper(CompiledMethodObject cm, int idx, int param) {
        super(cm, idx, cm.getLiteral(param & 31), param >> 5);
        receiverNode = new ReceiverNode(cm);
        lookupClassNode = SqueakClassNodeGen.create(cm, new CompiledInClass(cm));
    }
}

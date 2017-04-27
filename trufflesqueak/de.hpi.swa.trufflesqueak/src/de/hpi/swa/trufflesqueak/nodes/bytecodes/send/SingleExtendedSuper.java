package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.CompiledInClass;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakClassNodeGen;

public class SingleExtendedSuper extends AbstractSend {

    public SingleExtendedSuper(CompiledMethodObject cm, int idx, int param) {
        super(cm, idx, cm.getLiteral(param & 31), param >> 5);
        receiverNode = FrameSlotReadNode.receiver(cm);
        lookupClassNode = SqueakClassNodeGen.create(cm, new CompiledInClass(cm));
    }

    public SingleExtendedSuper(CompiledMethodObject cm, int idx, BaseSqueakObject selector, int numArgs) {
        super(cm, idx, selector, numArgs);
        receiverNode = FrameSlotReadNode.receiver(cm);
        lookupClassNode = SqueakClassNodeGen.create(cm, new CompiledInClass(cm));
    }
}

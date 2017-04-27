package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakClassNodeGen;

public class SendSelfSelector extends AbstractSend {

    public SendSelfSelector(CompiledMethodObject cm, int idx, BaseSqueakObject selector, int numArgs) {
        super(cm, idx, selector, numArgs,
                        FrameSlotReadNode.receiver(cm),
                        SqueakClassNodeGen.create(cm, FrameSlotReadNode.receiver(cm)));
    }
}

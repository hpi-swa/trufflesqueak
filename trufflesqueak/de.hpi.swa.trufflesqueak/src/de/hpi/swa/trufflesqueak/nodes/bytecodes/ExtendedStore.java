package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class ExtendedStore extends ExtendedAccess {
    public ExtendedStore(CompiledMethodObject cm, int index, int i) {
        super(cm, index, i);
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        FrameSlotReadNode top = FrameSlotReadNode.top(cm);
        switch (type) {
            case 0:
                return ObjectAtPutNodeGen.create(cm, storeIdx, FrameSlotReadNode.receiver(cm), top);
            case 1:
                return FrameSlotWriteNode.temp(cm, storeIdx, top);
            case 2:
                throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2");
            case 3:
                return ObjectAtPutNodeGen.create(cm, 1, new MethodLiteralNode(cm, storeIdx), top);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }
}

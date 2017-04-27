package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class ExtendedPush extends ExtendedAccess {
    public ExtendedPush(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
    }

    @Override
    public FrameSlotNode createChild(CompiledMethodObject cm) {
        switch (type) {
            case 0:
                return FrameSlotWriteNode.push(cm, FrameSlotReadNode.receiver(cm));
            case 1:
                return FrameSlotWriteNode.push(cm, FrameSlotReadNode.temp(cm, storeIdx));
            case 2:
                return FrameSlotWriteNode.push(cm, new MethodLiteralNode(cm, storeIdx));
            case 3:
                return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, 1, new MethodLiteralNode(cm, storeIdx)));
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}

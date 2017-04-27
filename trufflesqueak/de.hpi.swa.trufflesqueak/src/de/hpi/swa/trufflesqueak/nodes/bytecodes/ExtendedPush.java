package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class ExtendedPush extends ExtendedAccess {
    public ExtendedPush(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx, createChild(cm, i), +1);
    }

    public static FrameSlotNode createChild(CompiledMethodObject cm, int i) {
        int type = extractType(i);
        int storeIdx = extractIndex(i);
        switch (type) {
            case 0:
                return FrameSlotWriteNode.push(cm, FrameSlotReadNode.receiver(cm));
            case 1:
                return FrameSlotWriteNode.push(cm, FrameSlotReadNode.temp(cm, storeIdx));
            case 2:
                return FrameSlotWriteNode.push(cm, MethodLiteralNodeGen.create(cm, storeIdx));
            case 3:
                return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, 1, MethodLiteralNodeGen.create(cm, storeIdx)));
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }
}

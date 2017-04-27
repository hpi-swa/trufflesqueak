package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class StoreRemoteTemp extends RemoteTempBytecode {
    public StoreRemoteTemp(CompiledMethodObject cm, int idx, int indexInArray, int indexOfArray) {
        super(cm, idx, createChild(cm, indexInArray, indexOfArray), 0);
    }

    public StoreRemoteTemp(CompiledMethodObject cm, int idx, int indexInArray, int indexOfArray, int effect) {
        super(cm, idx, createChild(cm, indexInArray, indexOfArray), effect);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int indexInArray, int indexOfArray) {
        return ObjectAtPutNodeGen.create(cm, indexInArray, getTempArray(cm, indexOfArray), FrameSlotReadNode.top(cm));
    }
}

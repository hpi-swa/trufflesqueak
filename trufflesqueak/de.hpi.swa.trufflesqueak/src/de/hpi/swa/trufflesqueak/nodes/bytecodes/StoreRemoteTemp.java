package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class StoreRemoteTemp extends RemoteTempBytecode {
    public StoreRemoteTemp(CompiledMethodObject compiledMethodObject, int idx, int indexInArray, int indexOfArray) {
        super(compiledMethodObject, idx, indexInArray, indexOfArray);
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return ObjectAtPutNodeGen.create(cm, indexInArray, getTempArray(cm), FrameSlotReadNode.top(cm));
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class PushRemoteTemp extends RemoteTempBytecode {
    public PushRemoteTemp(CompiledMethodObject compiledMethodObject, int idx, int indexInArray, int indexOfArray) {
        super(compiledMethodObject, idx, indexInArray, indexOfArray);
    }

    @Override
    public ContextAccessNode createChild(CompiledMethodObject cm) {
        return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, indexInArray, getTempArray(cm)));
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNodeGen;

public class PushRemoteTemp extends RemoteTempBytecode {
    public PushRemoteTemp(CompiledMethodObject cm, int idx, int indexInArray, int indexOfArray) {
        super(cm, idx, createChild(cm, indexInArray, indexOfArray), +1);
    }

    public static ContextAccessNode createChild(CompiledMethodObject cm, int indexInArray, int indexOfArray) {
        return FrameSlotWriteNode.push(cm, ObjectAtNodeGen.create(cm, indexInArray, getTempArray(cm, indexOfArray)));
    }
}

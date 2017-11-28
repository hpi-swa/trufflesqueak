package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;

public class PushRemoteTempNode extends RemoteTempBytecodeNode {
    public PushRemoteTempNode(CompiledCodeObject code, int idx, int indexInArray, int indexOfArray) {
        super(code, idx);
        execNode = ObjectAtNode.create(indexInArray, getTempArray(code, indexOfArray));
    }
}

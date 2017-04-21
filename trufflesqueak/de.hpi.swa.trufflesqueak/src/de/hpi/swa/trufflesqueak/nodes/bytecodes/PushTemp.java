package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class PushTemp extends SqueakBytecodeNode {
    private final int tempIndex;

    public PushTemp(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        tempIndex = i & 15;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object temp = getTemp(frame, tempIndex);
        push(frame, temp);
        return temp;
    }

}

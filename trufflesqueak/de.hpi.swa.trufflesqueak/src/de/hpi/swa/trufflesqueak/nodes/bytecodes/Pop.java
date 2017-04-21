package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class Pop extends SqueakBytecodeNode {

    public Pop(CompiledMethodObject cm, int idx) {
        super(cm, idx);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pop(frame);
    }

}

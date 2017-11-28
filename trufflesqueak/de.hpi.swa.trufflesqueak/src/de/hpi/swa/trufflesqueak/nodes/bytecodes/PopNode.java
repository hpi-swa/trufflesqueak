package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopNode extends UnknownBytecodeNode {

    public PopNode(CompiledCodeObject code, int idx) {
        super(code, idx, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pop(frame);
    }

}

package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PopNode extends UnknownBytecodeNode {

    public PopNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code, index, numBytecodes, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return pop(frame);
    }

    @Override
    public String toString() {
        return "pop";
    }

}

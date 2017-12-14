package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class DupNode extends UnknownBytecodeNode {

    public DupNode(CompiledCodeObject method, int index, int numBytecodes) {
        super(method, index, numBytecodes, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, top(frame));
    }

    @Override
    public String toString() {
        return "dup";
    }
}

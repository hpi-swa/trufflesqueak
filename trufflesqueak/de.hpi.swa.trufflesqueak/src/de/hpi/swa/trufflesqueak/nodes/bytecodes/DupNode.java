package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class DupNode extends UnknownBytecodeNode {

    public DupNode(CompiledCodeObject method, int idx) {
        super(method, idx, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, top(frame));
    }
}

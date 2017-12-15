package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class DupNode extends UnknownBytecodeNode {
    @Child private TopStackNode topNode;

    public DupNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code, index, numBytecodes, -1);
        topNode = new TopStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, topNode.execute(frame));
    }

    @Override
    public String toString() {
        return "dup";
    }
}

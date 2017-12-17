package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopStackNode;

public class PopNode extends UnknownBytecodeNode {
    @Child private PopStackNode popNode;

    public PopNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code, index, numBytecodes, -1);
        popNode = new PopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        popNode.execute(frame);
    }

    @Override
    public String toString() {
        return "pop";
    }

}

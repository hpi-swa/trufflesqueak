package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class DupNode extends UnknownBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private TopStackNode topNode;

    public DupNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code, index, numBytecodes, -1);
        topNode = new TopStackNode(code);
        pushNode = new PushStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, topNode.execute(frame));
    }

    @Override
    public String toString() {
        return "dup";
    }
}

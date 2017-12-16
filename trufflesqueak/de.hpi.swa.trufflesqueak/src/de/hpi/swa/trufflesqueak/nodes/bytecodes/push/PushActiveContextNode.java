package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushActiveContextNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;

    public PushActiveContextNode(CompiledCodeObject code, int idx) {
        super(code, idx);
        pushNode = new PushStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, ContextObject.createReadOnlyContextObject(code.image, frame));
    }

    @Override
    public String toString() {
        return "pushThisContext:";
    }
}

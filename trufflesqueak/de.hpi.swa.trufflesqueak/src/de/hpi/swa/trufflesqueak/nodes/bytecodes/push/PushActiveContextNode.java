package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public class PushActiveContextNode extends AbstractPushNode {

    public PushActiveContextNode(CompiledCodeObject code, int idx) {
        super(code, idx);
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

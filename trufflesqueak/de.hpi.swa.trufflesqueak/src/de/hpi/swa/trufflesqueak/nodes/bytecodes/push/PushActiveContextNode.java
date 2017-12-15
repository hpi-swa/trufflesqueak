package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PushActiveContextNode extends SqueakBytecodeNode {

    public PushActiveContextNode(CompiledCodeObject code, int idx) {
        super(code, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, ContextObject.createReadOnlyContextObject(code.image, frame));
    }

    @Override
    public String toString() {
        return "pushThisContext:";
    }
}

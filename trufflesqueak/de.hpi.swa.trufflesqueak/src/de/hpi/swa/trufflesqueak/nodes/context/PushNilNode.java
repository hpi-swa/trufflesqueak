package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushNilNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;

    public PushNilNode(CompiledCodeObject code) {
        super(code, -1);
        pushNode = new PushStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, code.image.nil);
    }
}

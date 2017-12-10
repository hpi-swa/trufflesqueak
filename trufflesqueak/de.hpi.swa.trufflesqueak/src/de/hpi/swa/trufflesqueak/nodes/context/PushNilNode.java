package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PushNilNode extends SqueakBytecodeNode {

    public PushNilNode(CompiledCodeObject code) {
        super(code, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, code.image.nil);
    }
}

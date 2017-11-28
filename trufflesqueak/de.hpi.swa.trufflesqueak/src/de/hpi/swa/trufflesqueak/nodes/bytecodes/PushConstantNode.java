package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PushConstantNode extends SqueakBytecodeNode {
    public final Object constant;

    public PushConstantNode(CompiledCodeObject code, int idx, Object obj) {
        super(code, idx);
        constant = obj;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, constant);
    }
}

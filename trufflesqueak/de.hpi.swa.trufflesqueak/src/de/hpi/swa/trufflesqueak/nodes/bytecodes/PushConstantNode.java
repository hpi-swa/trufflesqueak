package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class PushConstantNode extends SqueakBytecodeNode {
    public final Object constant;

    public PushConstantNode(CompiledCodeObject code, int index, Object obj) {
        super(code, index);
        constant = obj;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, constant);
    }
}

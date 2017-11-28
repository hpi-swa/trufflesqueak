package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ConstantNode extends SqueakNode {
    private final Object constant;

    public ConstantNode(Object constant) {
        this.constant = constant;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return constant;
    }
}

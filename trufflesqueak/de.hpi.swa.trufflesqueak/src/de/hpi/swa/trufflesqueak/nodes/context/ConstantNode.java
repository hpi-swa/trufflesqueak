package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ConstantNode extends ContextAccessNode {
    private final Object constant;

    public ConstantNode(Object constant) {
        super(null);
        this.constant = constant;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return constant;
    }
}

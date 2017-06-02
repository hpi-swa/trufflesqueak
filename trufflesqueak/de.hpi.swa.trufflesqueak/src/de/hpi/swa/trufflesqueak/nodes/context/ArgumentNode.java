package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class ArgumentNode extends SqueakNode {
    private final int idx;

    public ArgumentNode(int index) {
        idx = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        return args[idx];
    }
}

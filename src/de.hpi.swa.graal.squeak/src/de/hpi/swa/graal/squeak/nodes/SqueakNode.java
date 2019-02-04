package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class SqueakNode extends AbstractNode {
    public abstract Object executeRead(VirtualFrame frame);
}

package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class AbstractWriteNode extends AbstractNode {

    public abstract void executeWrite(VirtualFrame frame, Object value);
}

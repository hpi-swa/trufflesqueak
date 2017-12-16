package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;

public interface WriteNode extends NodeInterface {

    public abstract void executeWrite(VirtualFrame frame, Object value);
}

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class SqueakNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

}
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public abstract class PrimitiveNode extends Node {
    public abstract BaseSqueakObject executeGeneric(VirtualFrame frame) throws PrimitiveFailed;
}

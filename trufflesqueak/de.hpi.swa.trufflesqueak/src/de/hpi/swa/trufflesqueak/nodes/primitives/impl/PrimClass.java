package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimClass extends PrimitiveNode {
    @Child SqueakLookupClassNode node;
    @Child ArgumentNode arg;

    public PrimClass(CompiledMethodObject cm) {
        super(cm);
        node = SqueakLookupClassNodeGen.create(cm);
        arg = new ArgumentNode(0);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return node.executeLookup(arg.executeGeneric(frame));
    }
}

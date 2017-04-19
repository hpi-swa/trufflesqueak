package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;

public class PrimNotSupported extends PrimitiveNode {

    public PrimNotSupported() {
        super(null, 0);
        // TODO Auto-generated constructor stub
    }

    public PrimNotSupported(CompiledMethodObject method, int index) {
        super(method, index);
    }

    @Override
    public void executeGeneric(VirtualFrame frame) {
        // do nothing, just run the rest of the method
    }
}

package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class PrimitiveNode extends SqueakNodeWithMethod {
    protected static boolean isNull(Object obj) {
        return obj == null;
    }

    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (method.image.config.isVerbose()) {
            System.out.println("Primitive not yet written: " + method.toString());
        }
        throw new PrimitiveFailed();
    }
}

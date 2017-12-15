package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class PrimitiveNode extends SqueakNodeWithCode {
    protected static boolean isNil(Object obj) {
        return obj instanceof NilObject;
    }

    public PrimitiveNode(CompiledMethodObject code) {
        super(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (code.image.config.isVerbose()) {
            System.out.println("Primitive not yet written: " + code.toString());
        }
        throw new PrimitiveFailed();
    }
}

package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class CompiledInClassNode extends SqueakNodeWithMethod {
    public CompiledInClassNode(CompiledCodeObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return method.getCompiledInClass();
    }
}

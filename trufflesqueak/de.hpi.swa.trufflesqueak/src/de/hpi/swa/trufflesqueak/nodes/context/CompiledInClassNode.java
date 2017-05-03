package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public class CompiledInClassNode extends SqueakExecutionNode {
    public CompiledInClassNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return method.getCompiledInClass();
    }
}

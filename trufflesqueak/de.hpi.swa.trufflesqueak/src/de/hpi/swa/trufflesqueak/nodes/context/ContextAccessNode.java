package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public abstract class ContextAccessNode extends SqueakExecutionNode {
    public ContextAccessNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public abstract Object executeGeneric(VirtualFrame frame);
}
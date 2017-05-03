package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNode;

public class PrimitiveNode extends SqueakExecutionNode {
    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm);
    }

    public static SqueakNode arg(int index) {
        return new ArgumentNode(index);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        return null;
    }

    public PrimitiveNode mostSpecializedVersion() {
        return this;
    }
}

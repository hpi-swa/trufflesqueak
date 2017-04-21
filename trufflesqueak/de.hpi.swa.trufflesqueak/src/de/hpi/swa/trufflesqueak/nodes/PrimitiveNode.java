package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.helper.Top;

public class PrimitiveNode extends SqueakExecutionNode {
    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm);
    }

    public static Top arg(CompiledMethodObject cm, int offset) {
        return new Top(cm, offset);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return null;
    }

    public PrimitiveNode mostSpecializedVersion() {
        return this;
    }
}

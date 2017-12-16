package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ReturnReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimQuickReturnReceiverVariableNode extends PrimitiveNode {
    private @Child ReturnReceiverVariableNode actual;

    public PrimQuickReturnReceiverVariableNode(CompiledMethodObject code, int variableIdx) {
        super(code);
        actual = new ReturnReceiverVariableNode(code, variableIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return actual.executeGeneric(frame);
    }

}

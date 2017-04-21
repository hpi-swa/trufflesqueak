package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PrimitiveBytecodeNode extends SqueakBytecodeNode {
    @Child private PrimitiveNode primitiveNode;

    public PrimitiveBytecodeNode(CompiledMethodObject cm, int idx, PrimitiveNode primNode) {
        super(cm, idx);
        primitiveNode = primNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, LocalReturn, ProcessSwitch {
        Object result = primitiveNode.executeGeneric(frame);
        push(frame, result);
        return result;
    }
}

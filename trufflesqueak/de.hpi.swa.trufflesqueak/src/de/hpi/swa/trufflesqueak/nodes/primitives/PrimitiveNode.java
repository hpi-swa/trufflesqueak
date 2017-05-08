package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PrimitiveNode extends SqueakBytecodeNode {
    public PrimitiveNode(CompiledMethodObject cm) {
        super(cm, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws LocalReturn {
        throw new PrimitiveFailed();
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        // TODO Auto-generated method stub

    }
}

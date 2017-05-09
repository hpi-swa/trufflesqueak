package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;

public class CallPrimitiveNode extends SqueakBytecodeNode {
    @Child private PrimitiveNode primitive;

    public CallPrimitiveNode(CompiledCodeObject method, int idx, int i, int j) {
        super(method, idx);
        int primitiveIdx = i + (j << 8);
        primitive = PrimitiveNodeFactory.forIdx(method, primitiveIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object result = null;
        try {
            result = primitive.executeGeneric(frame);
        } catch (UnsupportedSpecializationException | PrimitiveFailed e) {
            return null;
        }
        if (index == 0) {
            throw new LocalReturn(result);
        } else {
            return result;
        }
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        // classic primitive method, takes its arguments from the frame and returns
        if (method.hasPrimitive() && index == 0) {
            sequence.push(this);
        } else {
            stack.push(this);
        }
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append("<prim: ").append(primitive).append('>');
    }
}

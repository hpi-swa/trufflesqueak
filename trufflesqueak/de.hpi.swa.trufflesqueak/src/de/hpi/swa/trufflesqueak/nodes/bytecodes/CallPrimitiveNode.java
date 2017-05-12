package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuickReturnNode;

public class CallPrimitiveNode extends SqueakBytecodeNode {
    @Child PrimitiveNode primitive;

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
            method.image.debugPrint("Primitive failed: ", method);
            method.image.debugPrint(frame.getArguments());
            method.image.debugPrint(Arrays.stream(frame.getArguments()).map(Object::getClass).toArray());
            return null;
        }
        if (index == 0) {
            throw new LocalReturn(result);
        } else {
            return result;
        }
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements) {
        // classic primitive method, takes its arguments from the frame and
        // returns
        if (method.hasPrimitive() && index == 0) {
            statements.push(this);
        } else {
            stack.push(this);
        }
    }

    @Override
    public int interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, List<SqueakBytecodeNode> sequence) {
        super.interpretOn(stack, statements, sequence);
        if (index == 0) {
            for (int i = sequence.indexOf(this) + 1; i < sequence.size(); i++) {
                SqueakBytecodeNode node = sequence.get(i);
                if (node != null) {
                    if (node instanceof ExtendedStoreNode) {
                        // an error code is requested, we'll handle that
                        // eventually TODO: FIXME
                        stack.push(new ConstantNode(method, index,
                                        method.image.wrapString("prim error codes not supported")));
                        break;
                    }
                }
            }
        }
        return sequence.indexOf(this) + 1;
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append("<prim: ").append(primitive).append('>');
    }

    @Override
    public boolean isReturn() {
        return primitive instanceof PrimitiveQuickReturnNode;
    }
}

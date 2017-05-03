package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Children final SqueakNode[] popIntoArrayNodes;
    @Child SqueakNode pushArrayNode;
    private final int arraySize;

    public PushNewArrayNode(CompiledMethodObject cm, int idx, int param) {
        super(cm, idx);
        arraySize = (param >> 1) & 0xFF;
        if ((param & 1) == 1) {
            popIntoArrayNodes = new SqueakNode[arraySize];
        } else {
            popIntoArrayNodes = null;
        }
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        BaseSqueakObject[] ptrs = new BaseSqueakObject[arraySize];
        if (popIntoArrayNodes != null) {
            for (int i = 0; i < arraySize; i++) {
                // TODO: FIXME: popping primitive values into an array
                ptrs[i] = (BaseSqueakObject) popIntoArrayNodes[i].executeGeneric(frame);
            }
        }
        return new PointersObject(method.image, ptrs, method.image.arrayClass);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        if (popIntoArrayNodes != null) {
            for (int i = 0; i < arraySize; i++) {
                popIntoArrayNodes[i] = stack.pop();
            }
        }
        stack.push(this);
    }
}

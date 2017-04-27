package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNodeGen;

public class PushNewArray extends SqueakBytecodeNode {
    @Children final ContextAccessNode[] popIntoArrayNodes;
    @Child ContextAccessNode pushArrayNode;
    private final int arraySize;

    public PushNewArray(CompiledMethodObject cm, int idx, int param) {
        // TODO: finish
        super(cm, idx);
        arraySize = (param >> 1) & 0xFF;
        if ((param & 1) == 1) {
            popIntoArrayNodes = new ContextAccessNode[arraySize];
            for (int i = 0; i < arraySize; i++) {
                popIntoArrayNodes[i] = ObjectAtPutNodeGen.create(cm, i, FrameSlotReadNode.top(cm), FrameSlotReadNode.peek(cm, arraySize - i));
            }
        } else {
            popIntoArrayNodes = null;
        }
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        BaseSqueakObject[] ptrs = new BaseSqueakObject[arraySize];
        PointersObject ary = new PointersObject(ptrs, getImage().arrayClass);
        if (popIntoArrayNodes != null) {
            for (ContextAccessNode node : popIntoArrayNodes) {
                node.executeGeneric(frame);
            }
            decSP(frame, arraySize);
        }
        return ary;
    }
}

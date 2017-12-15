package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Child SqueakNode pushArrayNode;
    public final boolean popValues;
    public final int arraySize;

    public PushNewArrayNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
        super(code, index, numBytecodes);
        arraySize = param & 127;
        popValues = param > 127;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] array;
        if (popValues) {
            array = popNReversed(frame, arraySize);
        } else {
            array = new Object[arraySize];
        }
        return push(frame, code.image.wrap(array));
    }

    @Override
    public String toString() {
        return String.format("push: (Array new: %d)", arraySize);
    }
}

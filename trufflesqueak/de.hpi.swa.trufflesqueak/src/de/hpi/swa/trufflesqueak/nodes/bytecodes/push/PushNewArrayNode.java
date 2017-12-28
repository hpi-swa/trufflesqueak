package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;

public class PushNewArrayNode extends AbstractPushNode {
    @Child private PopNReversedStackNode popNReversedNode;
    @CompilationFinal private final int arraySize;

    public PushNewArrayNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
        super(code, index, numBytecodes);
        arraySize = param & 127;
        popNReversedNode = param > 127 ? new PopNReversedStackNode(code, arraySize) : null;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object[] array;
        if (popNReversedNode != null) {
            array = popNReversedNode.execute(frame);
        } else {
            array = new Object[arraySize];
            Arrays.fill(array, code.image.nil); // Initialize with nil
        }
        pushNode.executeWrite(frame, code.image.wrap(array));
    }

    @Override
    public String toString() {
        return String.format("push: (Array new: %d)", arraySize);
    }
}

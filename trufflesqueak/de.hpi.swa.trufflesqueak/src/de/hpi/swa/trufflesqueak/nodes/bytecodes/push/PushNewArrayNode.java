package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private PopNReversedStackNode popNReversedNode;
    @CompilationFinal private final int arraySize;

    public PushNewArrayNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
        super(code, index, numBytecodes);
        arraySize = param & 127;
        pushNode = new PushStackNode(code);
        popNReversedNode = param > 127 ? new PopNReversedStackNode(code, arraySize) : null;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object[] array;
        if (popNReversedNode != null) {
            array = popNReversedNode.execute(frame);
        } else {
            array = new Object[arraySize];
        }
        pushNode.executeWrite(frame, code.image.wrap(array));
    }

    @Override
    public String toString() {
        return String.format("push: (Array new: %d)", arraySize);
    }
}

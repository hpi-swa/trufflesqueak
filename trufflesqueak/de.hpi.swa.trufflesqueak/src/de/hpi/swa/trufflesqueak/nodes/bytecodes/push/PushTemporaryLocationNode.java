package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameTemporaryReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushTemporaryLocationNode extends AbstractBytecodeNode {
    @Child private PushStackNode pushNode;
    @Child private SqueakNode tempNode;
    @CompilationFinal private final int tempIndex;

    public PushTemporaryLocationNode(CompiledCodeObject code, int index, int numBytecodes, int tempIndex) {
        super(code, index, numBytecodes);
        this.tempIndex = tempIndex;
        pushNode = new PushStackNode(code);
        if (code.getNumStackSlots() <= tempIndex) {
            // sometimes we'll decode more bytecodes than we have slots ... that's fine
        } else {
            tempNode = FrameTemporaryReadNode.create(code, tempIndex);
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, tempNode.executeGeneric(frame));
    }

    @Override
    public String toString() {
        return "pushTemp: " + this.tempIndex;
    }
}

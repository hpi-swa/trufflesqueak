package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public abstract class SqueakBytecodeNode extends SqueakExecutionNode {
    private final int index;

    public SqueakBytecodeNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    public int stepBytecode(VirtualFrame frame) {
        executeGeneric(frame);
        return getIndex() + 1;
    }

    public int getIndex() {
        return index;
    }
}

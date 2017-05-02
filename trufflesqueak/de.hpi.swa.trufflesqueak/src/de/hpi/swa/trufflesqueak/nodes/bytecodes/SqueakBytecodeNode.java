package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Vector;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;

public abstract class SqueakBytecodeNode extends SqueakExecutionNode {
    private final int index;

    public SqueakBytecodeNode(CompiledMethodObject cm, int idx) {
        super(cm);
        index = idx;
    }

    public int getIndex() {
        return index;
    }

    public int getJump() {
        return 0;
    }

    public void decompileOnSequence(Vector<SqueakBytecodeNode> sequence) {
        sequence.set(getIndex(), this.decompileFrom(sequence));
    }

    public SqueakBytecodeNode decompileFrom(@SuppressWarnings("unused") Vector<SqueakBytecodeNode> sequence) {
        assert getJump() == 0;
        return this;
    }
}

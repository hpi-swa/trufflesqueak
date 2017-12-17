package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;

public abstract class AbstractRemoteTemporaryLocationNode extends SqueakNode {
    @Child protected FrameSlotReadNode getTempArrayNode;
    @CompilationFinal protected final int indexOfArray;
    @CompilationFinal protected final int indexInArray;

    public AbstractRemoteTemporaryLocationNode(CompiledCodeObject code, int indexInArray, int indexOfArray) {
        super();
        this.indexOfArray = indexOfArray;
        this.indexInArray = indexInArray;
        getTempArrayNode = FrameSlotReadNode.create(code.getTempSlot(indexOfArray));
    }

    public final int getIndexOfArray() {
        return indexOfArray;
    }

    public final int getIndexInArray() {
        return indexInArray;
    }
}

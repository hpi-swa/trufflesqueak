package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class BytecodeSequence extends SqueakBytecodeNode {
    @Children private final SqueakBytecodeNode[] nodes;

    public BytecodeSequence(CompiledMethodObject compiledMethodObject, SqueakBytecodeNode[] squeakBytecodeNodes) {
        super(compiledMethodObject);
        nodes = squeakBytecodeNodes;
    }

    @Override
    @ExplodeLoop
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        for (SqueakBytecodeNode node : nodes) {
            BaseSqueakObject result = node.executeGeneric(frame);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}

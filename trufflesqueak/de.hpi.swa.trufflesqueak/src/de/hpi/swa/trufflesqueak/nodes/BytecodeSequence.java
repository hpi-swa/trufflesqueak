package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class BytecodeSequence extends Node {
    @Children private final SqueakBytecodeNode[] nodes;

    public BytecodeSequence(SqueakBytecodeNode[] squeakBytecodeNodes) {
        nodes = squeakBytecodeNodes;
    }

    @ExplodeLoop
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws ProcessSwitch, NonLocalReturn, NonVirtualReturn {
        for (SqueakBytecodeNode node : nodes) {
            try {
                node.executeGeneric(frame);
            } catch (LocalReturn e) {
                return e.returnValue;
            }
        }
        throw new RuntimeException("Method did not return");
    }
}

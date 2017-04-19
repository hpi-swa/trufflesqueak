package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class BytecodeSequence extends Node {
    private final CompiledMethodObject method;
    @Children private final SqueakBytecodeNode[] nodes;

    public BytecodeSequence(CompiledMethodObject cm, SqueakBytecodeNode[] squeakBytecodeNodes) {
        method = cm;
        nodes = squeakBytecodeNodes;
    }

    public BaseSqueakObject executeGeneric(VirtualFrame frame, int initialPC) throws ProcessSwitch, NonLocalReturn, NonVirtualReturn {
        int offset = method.getBytecodeOffset();
        int pc = initialPC - 1 - offset;
        while (pc >= 0 && pc < nodes.length) {
            SqueakBytecodeNode node = nodes[pc];
            if (node == null) {
                pc += 1;
            } else {
                try {
                    pc = node.execute(frame);
                } catch (LocalReturn e) {
                    return e.returnValue;
                }
            }
        }
        throw new RuntimeException("Method did not return");
    }

    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws ProcessSwitch, NonLocalReturn, NonVirtualReturn {
        return executeGeneric(frame, 0);
    }
}

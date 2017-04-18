package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class SqueakBytecodeNode extends Node {
    protected SqueakBytecodeNode next;
    protected final CompiledMethodObject method;

    public SqueakBytecodeNode(CompiledMethodObject cm) {
        method = cm;
    }

    public abstract BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch;

    public void setNext(SqueakBytecodeNode node) {
        next = node;
    }
}

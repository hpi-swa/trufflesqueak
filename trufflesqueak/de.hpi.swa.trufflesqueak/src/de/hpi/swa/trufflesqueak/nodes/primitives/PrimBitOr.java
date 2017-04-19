package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;

public class PrimBitOr extends PrimitiveNode {

    public PrimBitOr(CompiledMethodObject cm, int idx) {
        super(cm, idx);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        // TODO Auto-generated method stub
        return;
    }

}

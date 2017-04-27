package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class StoreAndPopRemoteTemp extends StoreRemoteTemp {
    public StoreAndPopRemoteTemp(CompiledMethodObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx, i, j);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return super.executeGeneric(frame);
        } finally {
            decSP(frame);
        }
    }
}

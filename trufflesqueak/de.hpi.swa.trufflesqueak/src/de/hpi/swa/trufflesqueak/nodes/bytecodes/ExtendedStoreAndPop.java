package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ExtendedStoreAndPop extends ExtendedStore {
    public ExtendedStoreAndPop(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
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

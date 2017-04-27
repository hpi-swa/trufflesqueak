package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class StoreAndPopRemoteTemp extends StoreRemoteTemp {
    public StoreAndPopRemoteTemp(CompiledMethodObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx, i, j, -1);
    }
}

package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ExtendedStoreAndPop extends ExtendedStore {
    public ExtendedStoreAndPop(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i, -1);
    }
}

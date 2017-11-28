package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class StoreAndPopRemoteTempNode extends StoreRemoteTempNode {
    public StoreAndPopRemoteTempNode(CompiledCodeObject code, int idx, int i, int j) {
        super(code, idx, i, j);
    }
}

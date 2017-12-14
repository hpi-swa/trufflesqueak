package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class ReturnNode extends SqueakBytecodeNode {
    public ReturnNode(CompiledCodeObject code, int index) {
        super(code, index);
    }
}

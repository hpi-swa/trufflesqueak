package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public abstract class ReturnNode extends SqueakBytecodeNode {
    public ReturnNode(CompiledCodeObject code, int index) {
        super(code, index);
    }
}

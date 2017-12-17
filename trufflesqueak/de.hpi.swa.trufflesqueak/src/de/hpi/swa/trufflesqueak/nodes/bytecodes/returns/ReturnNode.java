package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;

public abstract class ReturnNode extends AbstractBytecodeNode {
    public ReturnNode(CompiledCodeObject code, int index) {
        super(code, index);
    }
}

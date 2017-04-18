package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public abstract class ConditionalJump extends Jump {
    public ConditionalJump(CompiledMethodObject cm) {
        super(cm);
    }
}
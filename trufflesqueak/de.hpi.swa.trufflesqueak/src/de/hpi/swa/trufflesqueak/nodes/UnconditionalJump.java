package de.hpi.swa.trufflesqueak.nodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.Jump;

public abstract class UnconditionalJump extends Jump {
    public UnconditionalJump(CompiledMethodObject cm) {
        super(cm);
    }
}
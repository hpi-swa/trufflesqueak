package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class UnconditionalJump extends Jump {
    public UnconditionalJump(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }
}
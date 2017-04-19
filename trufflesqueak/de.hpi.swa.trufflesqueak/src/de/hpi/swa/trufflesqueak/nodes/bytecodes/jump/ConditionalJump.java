package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class ConditionalJump extends Jump {
    public ConditionalJump(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }
}
package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public abstract class Jump extends SqueakBytecodeNode {
    public Jump(CompiledMethodObject cm) {
        super(cm);
    }

    public abstract int getTargetPC();
}
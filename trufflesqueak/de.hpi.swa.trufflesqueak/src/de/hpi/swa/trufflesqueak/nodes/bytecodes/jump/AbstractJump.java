package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public abstract class AbstractJump extends SqueakBytecodeNode {

    protected static int shortJumpOffset(int code) {
        return (code & 7) + 1;
    }

    protected static int longJumpOffset(int code, int param) {
        return ((code & 3) << 8) + param;
    }

    public AbstractJump(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

}
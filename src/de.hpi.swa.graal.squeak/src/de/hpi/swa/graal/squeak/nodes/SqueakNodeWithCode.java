package de.hpi.swa.graal.squeak.nodes;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class SqueakNodeWithCode extends SqueakNode {
    protected final CompiledCodeObject code;

    public SqueakNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }
}

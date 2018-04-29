package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class AbstractWriteNode extends AbstractNodeWithCode {
    public AbstractWriteNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);
}

package de.hpi.swa.graal.squeak.nodes.helpers;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

public abstract class AbstractWriteNode extends AbstractNodeWithCode {
    public AbstractWriteNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);
}

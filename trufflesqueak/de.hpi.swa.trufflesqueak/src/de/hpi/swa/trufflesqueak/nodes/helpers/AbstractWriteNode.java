package de.hpi.swa.trufflesqueak.nodes.helpers;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public abstract class AbstractWriteNode extends AbstractNodeWithCode {
    public AbstractWriteNode(CompiledCodeObject code) {
        super(code);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);
}

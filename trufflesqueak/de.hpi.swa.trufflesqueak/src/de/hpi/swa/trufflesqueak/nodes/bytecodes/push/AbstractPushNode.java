package de.hpi.swa.trufflesqueak.nodes.bytecodes.push;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public abstract class AbstractPushNode extends SqueakBytecodeNode {
    @Child protected PushStackNode pushNode;

    public AbstractPushNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code, index, numBytecodes);
        pushNode = new PushStackNode(code);
    }

    public AbstractPushNode(CompiledCodeObject code, int index) {
        this(code, index, 1);
    }

    @Override
    public abstract void executeVoid(VirtualFrame frame);
}

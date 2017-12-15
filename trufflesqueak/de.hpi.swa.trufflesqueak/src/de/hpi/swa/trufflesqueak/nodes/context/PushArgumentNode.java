package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class PushArgumentNode extends SqueakBytecodeNode {
    private final int argumentIndex;
    @Child private PushStackNode pushNode;

    public PushArgumentNode(CompiledCodeObject code, int index) {
        super(code, -1);
        argumentIndex = index;
        pushNode = new PushStackNode(code);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return pushNode.executeWrite(frame, frame.getArguments()[argumentIndex]);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("Tried to access arg #" + argumentIndex + ", but there are only " + frame.getArguments().length + " in total.");
        }
    }
}

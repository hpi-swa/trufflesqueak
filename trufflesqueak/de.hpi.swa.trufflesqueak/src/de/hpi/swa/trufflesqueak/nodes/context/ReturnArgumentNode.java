package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class ReturnArgumentNode extends SqueakBytecodeNode {
    private final int argumentIndex;

    public ReturnArgumentNode(CompiledCodeObject code, int index) {
        super(code, -1);
        argumentIndex = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            return frame.getArguments()[argumentIndex];
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("Tried to access arg #" + argumentIndex + ", but there are only " + frame.getArguments().length + " in total.");
        }
    }
}

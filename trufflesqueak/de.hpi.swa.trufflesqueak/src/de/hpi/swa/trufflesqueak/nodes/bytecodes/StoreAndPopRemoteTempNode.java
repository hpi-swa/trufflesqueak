package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class StoreAndPopRemoteTempNode extends StoreRemoteTempNode {
    public StoreAndPopRemoteTempNode(CompiledCodeObject code, int index, int indexInArray, int indexOfArray) {
        super(code, index, indexInArray, indexOfArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        super.executeGeneric(frame);
        pop(frame);
        return code.image.nil;
    }
}

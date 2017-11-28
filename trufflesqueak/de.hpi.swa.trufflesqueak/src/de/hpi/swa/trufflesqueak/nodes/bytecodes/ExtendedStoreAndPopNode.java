package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ExtendedStoreAndPopNode extends SqueakBytecodeNode {
    @Child SqueakBytecodeNode storeNode;

    public ExtendedStoreAndPopNode(CompiledCodeObject code, int idx, int i) {
        super(code, idx);
        storeNode = ExtendedStoreNode.create(code, idx, i);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        storeNode.executeGeneric(frame);
        return pop(frame);
    }
}

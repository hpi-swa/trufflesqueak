package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class StoreReceiverFromArgumentsNode extends SqueakBytecodeNode {

    public StoreReceiverFromArgumentsNode(CompiledCodeObject code) {
        super(code, -1);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        frame.setObject(code.receiverSlot, frame.getArguments()[0]);
        return code.image.nil;
    }
}

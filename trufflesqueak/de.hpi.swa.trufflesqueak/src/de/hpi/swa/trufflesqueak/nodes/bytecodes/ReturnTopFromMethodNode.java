package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ReturnTopFromMethodNode extends ReturnTopFromBlockNode {
    public ReturnTopFromMethodNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (getClosure(frame) == method.image.nil) {
            return super.executeGeneric(frame);
        } else {
            throw new NonLocalReturn(valueNode.executeGeneric(frame), getClosure(frame));
        }
    }
}

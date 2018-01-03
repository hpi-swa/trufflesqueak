package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class ReturnTopFromMethodNode extends ReturnTopFromBlockNode {

    public ReturnTopFromMethodNode(CompiledCodeObject code, int idx) {
        super(code, idx);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        if (getClosure(frame) == code.image.nil) {
            super.executeVoid(frame);
        } else {
            throw new NonLocalReturn(popNode.executeGeneric(frame), ((BlockClosure) getClosure(frame)).getFrameMarker());
        }
    }

    @Override
    public String toString() {
        return "returnTop";
    }
}

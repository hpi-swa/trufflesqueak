package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public class SingleExtendedSuperNode extends AbstractSendNode {
    public static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
        public SqueakLookupClassSuperNode(CompiledCodeObject code) {
            super(code);
        }

        @Override
        public Object executeLookup(Object receiver) {
            return code.getCompiledInClass().getSuperclass();
        }
    }

    public SingleExtendedSuperNode(CompiledCodeObject code, int index, int selectorLiteralIdx, int numArgs) {
        super(code, index, code.getLiteral(selectorLiteralIdx), numArgs);
    }

    public SingleExtendedSuperNode(CompiledCodeObject code, int index, int rawByte) {
        this(code, index, rawByte & 31, rawByte >> 5);
    }

    @Override
    public Object executeSend(VirtualFrame frame) {
        lookupClassNode = new SqueakLookupClassSuperNode(code);
        return super.executeSend(frame);
    }
}
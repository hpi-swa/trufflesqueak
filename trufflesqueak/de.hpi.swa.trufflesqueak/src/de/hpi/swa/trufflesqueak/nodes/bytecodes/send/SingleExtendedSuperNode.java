package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public class SingleExtendedSuperNode extends AbstractSendNode {
    private static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
        public SqueakLookupClassSuperNode(CompiledCodeObject code) {
            super(code);
        }

        @Override
        public Object executeLookup(Object receiver) {
            return code.getCompiledInClass().getSuperclass();
        }
    }

    public SingleExtendedSuperNode(CompiledCodeObject code, int index, int numBytecodes, int selectorLiteralIdx, int numArgs) {
        super(code, index, numBytecodes, code.getLiteral(selectorLiteralIdx), numArgs);
        lookupClassNode = new SqueakLookupClassSuperNode(code);
    }

    public SingleExtendedSuperNode(CompiledCodeObject code, int index, int numBytecodes, int rawByte) {
        this(code, index, numBytecodes, rawByte & 31, rawByte >> 5);
    }

    @Override
    public String toString() {
        return "sendSuper: " + selector.toString();
    }
}
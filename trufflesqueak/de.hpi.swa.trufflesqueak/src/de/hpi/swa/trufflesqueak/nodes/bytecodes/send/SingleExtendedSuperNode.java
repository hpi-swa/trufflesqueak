package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public class SingleExtendedSuperNode extends AbstractSend {
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
}
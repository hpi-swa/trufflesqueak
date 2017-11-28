package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public class SingleExtendedSuperNode extends AbstractSend {
    public static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
        public SqueakLookupClassSuperNode(CompiledCodeObject method) {
            super(method);
        }

        @Override
        public Object executeLookup(Object receiver) {
            return code.getCompiledInClass().getSuperclass();
        }
    }

    public SingleExtendedSuperNode(CompiledCodeObject method, int idx, int selectorLiteralIdx, int numArgs) {
        super(method, idx, method.getLiteral(selectorLiteralIdx), numArgs);
    }
}
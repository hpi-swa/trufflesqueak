package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import java.util.Stack;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public class SingleExtendedSuperNode extends AbstractSend {
    public static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
        public SqueakLookupClassSuperNode(CompiledMethodObject cm) {
            super(cm);
        }

        @Override
        public Object executeLookup(Object receiver) {
            return method.getCompiledInClass().getSuperclass();
        }
    }

    public SingleExtendedSuperNode(CompiledMethodObject cm, int idx, int selectorLiteralIdx, int numArgs) {
        super(cm, idx, cm.getLiteral(selectorLiteralIdx), numArgs);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        super.interpretOn(stack, sequence);
        lookupClassNode = new SqueakLookupClassSuperNode(method);
    }

    @Override
    public void prettyPrintOn(StringBuilder b) {
        b.append("super ").append(selector).append("@args(");
        for (SqueakNode node : argumentNodes) {
            node.prettyPrintOn(b);
            b.append('.');
        }
        b.append(')');
    }
}

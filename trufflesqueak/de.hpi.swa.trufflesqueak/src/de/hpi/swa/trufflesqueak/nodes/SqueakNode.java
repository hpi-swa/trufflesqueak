package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.instrumentation.SourceStringBuilder;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LoopRepeatingNode;

@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public void prettyPrintOn(SourceStringBuilder str) {
        getChildren().forEach(node -> {
            if (node instanceof WrapperNode) {
                // no op
            } else if (node instanceof SqueakNode) {
                ((SqueakNode) node).prettyPrintOn(str);
            } else if (node instanceof LoopNode) {
                ((LoopRepeatingNode) ((LoopNode) node).getRepeatingNode()).prettyPrintOn(str);
            } else {
                str.append(node);
            }
        });
    }

    public void prettyPrintWithParensOn(SourceStringBuilder str) {
        str.append('(');
        prettyPrintOn(str);
        str.append(')');
    }

    public void prettyPrintStatementOn(SourceStringBuilder str) {
        prettyPrintOn(str);
        str.append('.').newline();
    }

    public String prettyPrint() {
        SourceStringBuilder stringBuilder = new SourceStringBuilder();
        prettyPrintOn(stringBuilder);
        return stringBuilder.build();
    }
}

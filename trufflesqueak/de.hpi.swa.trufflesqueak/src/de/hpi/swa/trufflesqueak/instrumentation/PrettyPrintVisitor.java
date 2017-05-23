package de.hpi.swa.trufflesqueak.instrumentation;

import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LoopRepeatingNode;

public class PrettyPrintVisitor {
    private static final String INDENT = "  ";
    private final StringBuilder sb = new StringBuilder();
    private int padding = 0;

    public void visit(Node node) {
        if (node instanceof WrapperNode) {
            visitWrapper((WrapperNode) node);
        } else if (node instanceof SqueakNode) {
            visitSqueakNode((SqueakNode) node);
        } else if (node instanceof LoopNode) {
            visitLoopNode((LoopNode) node);
        } else {
            append(node);
        }
    }

    @SuppressWarnings("unused")
    void visitWrapper(WrapperNode node) {
        // no op
    }

    void visitSqueakNode(SqueakNode node) {
        node.prettyPrintOn(this);
    }

    void visitLoopNode(LoopNode node) {
        visitSqueakNode((LoopRepeatingNode) node.getRepeatingNode());
    }

    public void visitWithParens(Node node) {
        append('(');
        visit(node);
        append(')');
    }

    public void visitStatement(Node node) {
        visit(node);
        append('.').newline();
    }

    public PrettyPrintVisitor newline() {
        sb.append('\n');
        for (int i = 0; i < padding; i++) {
            sb.append(INDENT);
        }
        return this;
    }

    public PrettyPrintVisitor append(String string) {
        sb.append(string);
        return this;
    }

    public PrettyPrintVisitor append(char c) {
        sb.append(c);
        return this;
    }

    public PrettyPrintVisitor append(int arraySize) {
        sb.append(arraySize);
        return this;
    }

    public PrettyPrintVisitor append(Object obj) {
        sb.append(obj);
        return this;
    }

    public PrettyPrintVisitor indent() {
        padding += 2;
        sb.append(INDENT).append(INDENT);
        return this;
    }

    public PrettyPrintVisitor dedent() {
        padding -= 2;
        int length = sb.length();
        int dedentLength = INDENT.length() * 2;
        // only delete whitespace
        if (sb.subSequence(length - dedentLength, length).toString().trim().length() == 0) {
            sb.delete(length - dedentLength, length);
        }
        return this;
    }

    @Override
    public String toString() {
        throw new RuntimeException("shouldn't use");
    }

    public String build() {
        return sb.toString();
    }

    public int length() {
        return sb.length();
    }

}
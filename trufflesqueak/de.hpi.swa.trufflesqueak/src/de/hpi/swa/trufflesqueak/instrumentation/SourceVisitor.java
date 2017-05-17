package de.hpi.swa.trufflesqueak.instrumentation;

import com.oracle.truffle.api.source.Source;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class SourceVisitor extends PrettyPrintVisitor {
    final Source source;

    public SourceVisitor(Source source) {
        this.source = source;
    }

    @Override
    void visitSqueakNode(SqueakNode node) {
        int pos = length() - 1;
        super.visitSqueakNode(node);
        node.setSourceSection(source.createSection(pos, length() - pos - 1));
    }
}

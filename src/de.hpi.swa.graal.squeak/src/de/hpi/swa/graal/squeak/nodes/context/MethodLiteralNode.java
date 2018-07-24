package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

public final class MethodLiteralNode extends SqueakNode {
    private final Object literal;

    public MethodLiteralNode(final CompiledCodeObject code, final long literalIndex) {
        literal = code.getLiteral(literalIndex);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        return literal;
    }
}

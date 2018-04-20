package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

public class MethodLiteralNode extends SqueakNode {
    @CompilationFinal private final Object literal;

    public MethodLiteralNode(final CompiledCodeObject code, final long literalIndex) {
        super();
        literal = code.getLiteral(literalIndex);
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        return literal;
    }
}

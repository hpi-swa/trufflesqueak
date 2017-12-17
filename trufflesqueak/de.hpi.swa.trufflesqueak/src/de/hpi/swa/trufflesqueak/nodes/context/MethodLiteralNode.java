package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class MethodLiteralNode extends SqueakNode {
    @CompilationFinal private final Object literal;

    public MethodLiteralNode(CompiledCodeObject code, int literalIndex) {
        super();
        literal = code.getLiteral(literalIndex);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return literal;
    }
}

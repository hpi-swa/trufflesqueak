package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

@NodeChild(value = "arguments", type = SqueakNode[].class)
public abstract class AbstractPrimitiveNode extends SqueakNodeWithCode {
    @CompilationFinal public static final int NUM_ARGUMENTS = 0;

    public AbstractPrimitiveNode(CompiledMethodObject method) {
        super(method);
    }

    protected static boolean isNil(Object obj) {
        return obj instanceof NilObject;
    }
}

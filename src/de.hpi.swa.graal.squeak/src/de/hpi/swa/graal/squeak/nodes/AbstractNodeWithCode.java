package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNodeWithCode extends AbstractNode {
    @CompilationFinal protected final CompiledCodeObject code;

    protected AbstractNodeWithCode(final CompiledCodeObject code) {
        this.code = code;
    }

    protected AbstractNodeWithCode(final AbstractNodeWithCode original) {
        this(original.code);
    }
}

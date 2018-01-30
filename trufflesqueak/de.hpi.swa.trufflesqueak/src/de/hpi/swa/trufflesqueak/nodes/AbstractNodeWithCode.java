package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class AbstractNodeWithCode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;

    protected AbstractNodeWithCode(CompiledCodeObject code) {
        this.code = code;
    }
}

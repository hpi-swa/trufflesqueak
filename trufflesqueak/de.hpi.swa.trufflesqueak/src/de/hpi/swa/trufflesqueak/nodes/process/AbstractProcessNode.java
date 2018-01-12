package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class AbstractProcessNode extends Node {
    @CompilationFinal protected final SqueakImageContext image;

    protected AbstractProcessNode(SqueakImageContext image) {
        this.image = image;
    }
}

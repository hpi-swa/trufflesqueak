/*
 * Copyright (c) 2023-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractInterpreterInstrumentableNode;
import de.hpi.swa.trufflesqueak.nodes.interpreter.InterpreterSistaV1Node;
import de.hpi.swa.trufflesqueak.nodes.interpreter.InterpreterV3PlusClosuresNode;

public abstract class AbstractRootNode extends RootNode {
    @Child protected AbstractInterpreterInstrumentableNode interpreterNode;

    protected AbstractRootNode(final SqueakImageContext image, final CompiledCodeObject code) {
        super(image.getLanguage(), code.getFrameDescriptor());
        interpreterNode = code.getSignFlag() ? new InterpreterV3PlusClosuresNode(code) : new InterpreterSistaV1Node(code);
    }

    protected AbstractRootNode(final AbstractRootNode original) {
        super(SqueakImageContext.get(original).getLanguage(), original.getFrameDescriptor());
        interpreterNode = switch (original.interpreterNode) {
            case InterpreterSistaV1Node n -> new InterpreterSistaV1Node(n);
            case InterpreterV3PlusClosuresNode n -> new InterpreterV3PlusClosuresNode(n);
            default -> throw CompilerDirectives.shouldNotReachHere("Unknown node " + original);
        };
    }

    public final CompiledCodeObject getCode() {
        return interpreterNode.getCodeObject();
    }

    @Override
    public final String getName() {
        return toString();
    }

    @Override
    public final boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected final boolean isCloneUninitializedSupported() {
        return true;
    }
}

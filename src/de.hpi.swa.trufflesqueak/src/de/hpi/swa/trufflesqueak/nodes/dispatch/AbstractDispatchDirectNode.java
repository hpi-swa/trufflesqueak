/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

abstract class AbstractDispatchDirectNode extends AbstractNode {
    @CompilationFinal(dimensions = 1) private final Assumption[] assumptions;

    AbstractDispatchDirectNode(final Assumption[] assumptions) {
        this.assumptions = assumptions;
    }

    public final Assumption[] getAssumptions() {
        return assumptions;
    }
}

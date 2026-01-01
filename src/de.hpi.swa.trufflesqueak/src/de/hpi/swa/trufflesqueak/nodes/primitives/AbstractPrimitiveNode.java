/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractPrimitiveNode extends AbstractNode implements Primitive {
    public boolean acceptsMethod(@SuppressWarnings("unused") final CompiledCodeObject method) {
        CompilerAsserts.neverPartOfCompilation();
        return true;
    }

    public boolean needsFrame() {
        return false;
    }

    public abstract static class AbstractPrimitiveWithFrameNode extends AbstractPrimitiveNode {
        @Override
        public final boolean needsFrame() {
            return true;
        }
    }
}

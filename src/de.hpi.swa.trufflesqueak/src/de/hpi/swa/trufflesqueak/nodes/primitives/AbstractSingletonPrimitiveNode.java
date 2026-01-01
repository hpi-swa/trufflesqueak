/*
 * Copyright (c) 2021-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnadoptableNode;

public abstract class AbstractSingletonPrimitiveNode extends AbstractPrimitiveNode implements UnadoptableNode {
    @Override
    public final Node copy() {
        return this;
    }

    @Override
    public final Node deepCopy() {
        return this;
    }

    @Override
    public final boolean needsFrame() {
        return false;
    }
}

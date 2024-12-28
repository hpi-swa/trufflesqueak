/*
 * Copyright (c) 2021-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnadoptableNode;

public abstract class AbstractSingletonPrimitiveNode extends AbstractPrimitiveNode implements UnadoptableNode {

    @Override
    public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
        return execute();
    }

    protected abstract Object execute();

    @Override
    public final Node copy() {
        return this;
    }

    @Override
    public final Node deepCopy() {
        return this;
    }
}

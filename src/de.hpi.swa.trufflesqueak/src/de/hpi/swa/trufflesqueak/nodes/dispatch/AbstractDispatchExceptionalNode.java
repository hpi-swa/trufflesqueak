/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import de.hpi.swa.trufflesqueak.model.NativeObject;

abstract class AbstractDispatchExceptionalNode extends DispatchSelectorNode {
    protected final NativeObject selector;
    protected final int numArgs;

    AbstractDispatchExceptionalNode(final NativeObject selector, final int numArgs) {
        this.selector = selector;
        this.numArgs = numArgs;
    }

    @Override
    public final NativeObject getSelector() {
        return selector;
    }
}

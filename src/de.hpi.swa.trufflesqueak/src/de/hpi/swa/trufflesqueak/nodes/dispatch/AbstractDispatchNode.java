/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractDispatchNode extends AbstractNode {
    protected final NativeObject selector;

    AbstractDispatchNode(final NativeObject selector) {
        this.selector = selector;
    }
}

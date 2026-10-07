/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractDispatchSelectorNode extends AbstractNode {
    protected static final boolean checkArgumentCount(final CompiledCodeObject method, final int expectedNumArgs) {
        assert method.getNumArgs() == expectedNumArgs : "Unexpected number of arguments: " + method.getNumArgs();
        return true;
    }
}

/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.nodes.UnadoptableNode;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

abstract class HandlePrimitiveFailedNode extends AbstractNode implements UnadoptableNode {
    private static final HandlePrimitiveFailedNode STORE_INSTANCE = new HandlePrimitiveFailedNode() {
        @Override
        protected void execute(final PrimitiveFailed pf) {
            getContext().setPrimFailCode(pf);
        }
    };

    private static final HandlePrimitiveFailedNode NOP_INSTANCE = new HandlePrimitiveFailedNode() {
        @Override
        protected void execute(final PrimitiveFailed pf) {
            // Nothing to do
        }
    };

    @NeverDefault
    public static final HandlePrimitiveFailedNode create(final CompiledCodeObject method) {
        return method.hasStoreIntoTemp1AfterCallPrimitive() ? STORE_INSTANCE : NOP_INSTANCE;
    }

    protected abstract void execute(PrimitiveFailed pf);
}

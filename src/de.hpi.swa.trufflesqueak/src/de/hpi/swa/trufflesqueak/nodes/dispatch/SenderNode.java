/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;

abstract class SenderNode extends AbstractNode {
    protected final Assumption doesNotNeedSenderAssumption;

    SenderNode(final CompiledCodeObject method) {
        doesNotNeedSenderAssumption = method.getDoesNotNeedSenderAssumption();
    }

    protected abstract Object execute(VirtualFrame frame);

    @Specialization(assumptions = "doesNotNeedSenderAssumption")
    protected static final Object doContextOrMarker(final VirtualFrame frame,
                    @Cached final GetContextOrMarkerNode getContextOrMarkerNode) {
        return getContextOrMarkerNode.execute(frame);
    }

    @Specialization(replaces = "doContextOrMarker")
    protected static final Object doContext(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached(inline = true) final GetOrCreateContextNode getOrCreateContextNode) {
        return getOrCreateContextNode.executeGet(frame, node);
    }
}

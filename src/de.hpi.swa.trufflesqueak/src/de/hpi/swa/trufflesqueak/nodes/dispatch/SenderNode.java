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
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithoutFrameNode;

abstract class SenderNode extends AbstractNode {
    protected final Assumption doesNotNeedSenderAssumption;

    SenderNode(final CompiledCodeObject method) {
        doesNotNeedSenderAssumption = method.getDoesNotNeedSenderAssumption();
    }

    protected abstract Object execute(VirtualFrame frame);

    @Specialization(assumptions = "doesNotNeedSenderAssumption")
    protected static final Object doContextWithoutFrame(final VirtualFrame frame,
                    @Cached final GetOrCreateContextWithoutFrameNode getOrCreateContextWithoutFrameNode) {
        return getOrCreateContextWithoutFrameNode.execute(frame);
    }

    @Specialization(replaces = "doContextWithoutFrame")
    protected static final Object doContextWithFrame(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached(inline = true) final GetOrCreateContextWithFrameNode getOrCreateContextNode) {
        return getOrCreateContextNode.executeGet(frame, node);
    }
}

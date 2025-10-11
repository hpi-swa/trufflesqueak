/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextWithoutFrameNode;

@GenerateInline
@GenerateCached(false)
public abstract class GetOrCreateContextForDispatchNode extends AbstractNode {

    public abstract Object execute(VirtualFrame frame, Node node, CompiledCodeObject code);

    @Specialization(guards = "doesNotNeedSender(code, assumptionProfile, node)")
    protected static final Object doGetOrCreateContextWithoutFrame(final VirtualFrame frame, @SuppressWarnings("unused") final Node node, @SuppressWarnings("unused") final CompiledCodeObject code,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached final InlinedExactClassProfile assumptionProfile,
                    @Cached(inline = false) final GetOrCreateContextWithoutFrameNode getOrCreateContextWithoutFrameNode) {
        return getOrCreateContextWithoutFrameNode.execute(frame);
    }

    @Specialization(guards = "!doesNotNeedSender(code, assumptionProfile, node)")
    protected static final ContextObject doGetOrCreateContextWithFrame(final VirtualFrame frame, @SuppressWarnings("unused") final Node node, @SuppressWarnings("unused") final CompiledCodeObject code,
                    @SuppressWarnings("unused") @Shared("assumptionProfile") @Cached final InlinedExactClassProfile assumptionProfile,
                    @Cached final GetOrCreateContextWithFrameNode getOrCreateContextWithFrameNode) {
        return getOrCreateContextWithFrameNode.executeGet(frame, node);
    }

    protected static final boolean doesNotNeedSender(final CompiledCodeObject code, final InlinedExactClassProfile assumptionProfile, final Node node) {
        return assumptionProfile.profile(node, code.getDoesNotNeedSenderAssumption()).isValid();
    }
}

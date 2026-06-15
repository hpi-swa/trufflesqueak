/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import com.oracle.truffle.api.nodes.Node;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_SELECTOR;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithoutFrameNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateInline(false)
public abstract class DispatchValueNode extends AbstractNode {

    protected DispatchValueNode() {
    }

    public abstract Object execute(VirtualFrame frame, Object receiver);

    // --- Block Receivers ---

    @Specialization(guards = {
                    "closure.getCompiledBlock() == cachedBlock",
                    "cachedBlock.getNumArgs() == 0"
    }, limit = "INLINE_BLOCK_CACHE_LIMIT", assumptions = "cachedBlock.getCallTargetStable()")
    protected static final Object doBlock(final VirtualFrame frame, final BlockClosureObject closure,
                    @Bind final Node node,
                    @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode getOrCreateContextNode,
                    @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached("closure.getNumCopied()") final int cachedNumCopied,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
        final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, getOrCreateContextNode.execute(frame, node), 0, cachedNumCopied);
        return directCallNode.call(args);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"closure.getNumArgs() == 0"}, replaces = "doBlock")
    protected static final Object doBlockMegamorphic(final VirtualFrame frame, final BlockClosureObject closure,
                    @Bind final Node node,
                    @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode getOrCreateContextNode,
                    @Cached final IndirectCallNode indirectCallNode) {
        final CompiledCodeObject block = closure.getCompiledBlock();
        final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextNode.execute(frame, node), 0);
        return indirectCallNode.call(block.getCallTarget(), args);
    }

    // --- Non-Block Receivers (Associations, MessageSends, etc.) ---

    @Fallback
    protected static final Object doSend(final VirtualFrame frame, final Object receiver,
                    @Cached("create(getValueSelector())") final Dis0Node genericDispatchNode) {
        return genericDispatchNode.execute(frame, receiver);
    }

    protected static final NativeObject getValueSelector() {
        return SqueakImageContext.getSlow().getSpecialSelector(SPECIAL_SELECTOR.VALUE);
    }
}

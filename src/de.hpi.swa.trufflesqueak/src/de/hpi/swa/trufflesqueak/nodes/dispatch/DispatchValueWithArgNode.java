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
public abstract class DispatchValueWithArgNode extends AbstractNode {

    protected DispatchValueWithArgNode() {
    }

    public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1);

    // --- Block Receivers ---

    @Specialization(guards = {
                    "closure.getCompiledBlock() == cachedBlock",
                    "cachedBlock.getNumArgs() == 1"
    }, limit = "INLINE_BLOCK_CACHE_LIMIT", assumptions = "cachedBlock.getCallTargetStable()")
    protected static final Object doBlock(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @Bind final Node node,
                    @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode getOrCreateContextNode,
                    @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached("closure.getNumCopied()") final int cachedNumCopied,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
        final Object[] closureArgs = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, getOrCreateContextNode.execute(frame, node), 1, cachedNumCopied);
        FrameAccess.fillClosureTemplateWith(closureArgs, arg1);
        return directCallNode.call(closureArgs);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"closure.getNumArgs() == 1"}, replaces = "doBlock")
    protected static final Object doBlockMegamorphic(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @Bind final Node node,
                    @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode getOrCreateContextNode,
                    @Cached final IndirectCallNode indirectCallNode) {
        final CompiledCodeObject block = closure.getCompiledBlock();
        final Object[] closureArgs = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextNode.execute(frame, node), 1);
        FrameAccess.fillClosureTemplateWith(closureArgs, arg1);
        return indirectCallNode.call(block.getCallTarget(), closureArgs);
    }

    // --- Non-Block Receivers (Associations, MessageSends, etc.) ---

    @Fallback
    protected static final Object doSend(final VirtualFrame frame, final Object receiver, final Object arg1,
                    @Cached("create(getValueWitArgSelector())") final Dis1Node genericDispatchNode) {
        return genericDispatchNode.execute(frame, receiver, arg1);
    }

    protected static final NativeObject getValueWitArgSelector() {
        return SqueakImageContext.getSlow().getSpecialSelector(SPECIAL_SELECTOR.VALUE_WITH_ARG);
    }
}

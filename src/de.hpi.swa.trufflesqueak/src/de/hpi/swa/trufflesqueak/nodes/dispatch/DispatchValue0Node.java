/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateInline(false)
public abstract class DispatchValue0Node extends AbstractDispatchValueNode {

    protected DispatchValue0Node(final NativeObject selector) {
        super(selector);
    }

    public abstract Object execute(VirtualFrame frame, Object receiver);

    // --- Block Receivers ---

    @Specialization(guards = {
                    "closure.getCompiledBlock() == cachedBlock",
                    "cachedBlock.getNumArgs() == 0"
    }, limit = "INLINE_BLOCK_CACHE_LIMIT", assumptions = "cachedBlock.getCallTargetStable()")
    protected final Object doBlock(final VirtualFrame frame, final BlockClosureObject closure,
                    @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached("closure.getNumCopied()") final int cachedNumCopied,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {

        final Object[] args = FrameAccess.newClosureArgumentsUnrolled0(closure, getOrCreateContextNode.execute(frame), cachedNumCopied);
        return directCallNode.call(args);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"closure.getNumArgs() == 0"}, replaces = "doBlock")
    protected final Object doBlockMegamorphic(final VirtualFrame frame, final BlockClosureObject closure,
                    @Cached final IndirectCallNode indirectCallNode) {

        final CompiledCodeObject block = closure.getCompiledBlock();
        final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextNode.execute(frame), 0);
        return indirectCallNode.call(block.getCallTarget(), args);
    }

    protected static boolean is0ArgBlock(final Object receiver) {
        return receiver instanceof BlockClosureObject closure && closure.getNumArgs() == 0;
    }

    // --- Non-Block Receivers (Associations, MessageSends, etc.) ---

    @Specialization(guards = "!is0ArgBlock(receiver)")
    protected static final Object doGeneric(final VirtualFrame frame, final Object receiver,
                    @Cached("create(selector)") final DispatchSelector0Node.Dispatch0Node genericDispatchNode) {
        return genericDispatchNode.execute(frame, receiver);
    }
}

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
public abstract class DispatchValue1Node extends AbstractDispatchValueNode {

    protected DispatchValue1Node(final NativeObject selector) {
        super(selector);
    }

    public abstract Object execute(VirtualFrame frame, Object receiver, Object arg1);

    // --- Classic Blocks ---

    @Specialization(guards = {
                    "closure.getCompiledBlock() == cachedBlock",
                    "cachedBlock.getNumArgs() == 1",
                    "!closure.isAFullBlockClosure()"
    }, limit = "INLINE_BLOCK_CACHE_LIMIT", assumptions = "cachedBlock.getCallTargetStable()")
    protected final Object doClassicBlock(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached("closure.getNumCopied()") final int cachedNumCopied,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {

        final Object[] args = FrameAccess.newClosureArgumentsUnrolled1(closure, getOrCreateContextNode.execute(frame), cachedNumCopied, arg1);
        return directCallNode.call(args);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"closure.getNumArgs() == 1", "!closure.isAFullBlockClosure()"}, replaces = "doClassicBlock")
    protected final Object doClassicBlockMegamorphic(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @Shared("indirectCall") @Cached final IndirectCallNode indirectCallNode) {

        final CompiledCodeObject block = closure.getCompiledBlock();
        final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextNode.execute(frame), 1);
        args[FrameAccess.getArgumentStartIndex()] = arg1;

        return indirectCallNode.call(block.getCallTarget(), args);
    }

    // --- Full Blocks (Modern Squeak) ---

    @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 1",
                    "closure.isAFullBlockClosure()"}, limit = "INLINE_BLOCK_CACHE_LIMIT", assumptions = "cachedBlock.getCallTargetStable()")
    protected final Object doFullBlock(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached("closure.getNumCopied()") final int cachedNumCopied,
                    @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {

        final Object[] args = FrameAccess.newClosureArgumentsUnrolled1(closure, getOrCreateContextNode.execute(frame), cachedNumCopied, arg1);
        return directCallNode.call(args);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"closure.getNumArgs() == 1", "closure.isAFullBlockClosure()"}, replaces = "doFullBlock")
    protected final Object doFullBlockMegamorphic(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                    @Shared("indirectCall") @Cached final IndirectCallNode indirectCallNode) {

        final CompiledCodeObject block = closure.getCompiledBlock();
        final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextNode.execute(frame), 1);
        args[FrameAccess.getArgumentStartIndex()] = arg1;

        return indirectCallNode.call(block.getCallTarget(), args);
    }

    protected static boolean is1ArgBlock(final Object receiver) {
        return receiver instanceof BlockClosureObject closure && closure.getNumArgs() == 1;
    }

    // --- Non-Block Receivers (Associations, MessageSends, etc.) ---

    @Specialization(guards = "!is1ArgBlock(receiver)")
    protected static final Object doGeneric(final VirtualFrame frame, final Object receiver, final Object arg1,
                    @Cached("create(selector)") final DispatchSelector1Node.Dispatch1Node genericDispatchNode) {
        return genericDispatchNode.execute(frame, receiver, arg1);
    }
}

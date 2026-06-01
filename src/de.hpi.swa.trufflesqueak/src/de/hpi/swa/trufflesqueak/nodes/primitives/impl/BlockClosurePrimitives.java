/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectCopyIntoObjectArrayNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithoutFrameNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode.AbstractPrimitiveWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    public abstract static class AbstractClosurePrimitiveNode extends AbstractPrimitiveWithFrameNode {

        protected static Object[] setFrameArguments(final Object[] arguments, final Object arg1) {
            arguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            return arguments;
        }

        protected static Object[] setFrameArguments(final Object[] arguments, final Object arg1, final Object arg2) {
            arguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            arguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return arguments;
        }

        protected static Object[] setFrameArguments(final Object[] arguments, final Object arg1, final Object arg2, final Object arg3) {
            arguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            arguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            arguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return arguments;
        }

        protected static Object[] setFrameArguments(final Object[] arguments, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            arguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            arguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            arguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            arguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return arguments;
        }

        protected static Object[] setFrameArguments(final Object[] arguments, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            arguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            arguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            arguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            arguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            arguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return arguments;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221, /* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue0Node extends AbstractClosurePrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 0"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 0, cachedNumCopied);
            return directCallNode.call(args);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 0"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 0);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), args);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {202, /* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue1Node extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 1"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 1, cachedNumCopied);
            return directCallNode.call(setFrameArguments(args, arg1));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 1"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 1);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), setFrameArguments(args, arg1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {203, /* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue2Node extends AbstractClosurePrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 2"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 2, cachedNumCopied);
            return directCallNode.call(setFrameArguments(args, arg1, arg2));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 2"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 2);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), setFrameArguments(args, arg1, arg2));
        }
    }

    // --- Arity 3 ---

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {204, /* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue3Node extends AbstractClosurePrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 3"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 3, cachedNumCopied);
            return directCallNode.call(setFrameArguments(args, arg1, arg2, arg3));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 3"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 3);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), setFrameArguments(args, arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {205, /* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue4Node extends AbstractClosurePrimitiveNode implements Primitive4WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 4"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 4, cachedNumCopied);
            return directCallNode.call(setFrameArguments(args, arg1, arg2, arg3, arg4));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 4"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 4);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), setFrameArguments(args, arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {/* FULL=> */ 207, 209})
    public abstract static class PrimClosureValue5Node extends AbstractClosurePrimitiveNode implements Primitive5WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 5"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), 5, cachedNumCopied);
            return directCallNode.call(setFrameArguments(args, arg1, arg2, arg3, arg4, arg5));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 5"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] args = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), 5);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), setFrameArguments(args, arg1, arg2, arg3, arg4, arg5));
        }
    }

    // --- N-ary (With Args) ---

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222, /* FULL=> */ 208})
    public abstract static class PrimClosureValueWithArgsNode extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == sizeNode.execute(node, argArray)"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected static final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("cachedBlock.getNumArgs()") final int cachedNumArgs,
                        @Cached("closure.getNumCopied()") final int cachedNumCopied,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplateUnrolled(closure, contextNode.execute(frame, node), cachedNumArgs, cachedNumCopied);
            copyIntoNode.execute(frameArguments, argArray);
            return directCallNode.call(frameArguments);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == sizeNode.execute(node, argArray)"}, replaces = "doValueDirect")
        protected static final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Bind final Node node,
                        @Cached(inline = true) @Shared("contextNode") final GetOrCreateContextWithoutFrameNode contextNode,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, contextNode.execute(frame, node), closure.getNumArgs());
            copyIntoNode.execute(frameArguments, argArray);
            return indirectCallNode.call(block.getCallTarget(), frameArguments);
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return List.copyOf(BlockClosurePrimitivesFactory.getFactories());
    }
}

/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    public abstract static class AbstractClosurePrimitiveNode extends AbstractPrimitiveNode {
        @Child protected GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure) {
            return FrameAccess.createFrameArguments(frame, closure, getContextOrMarkerNode);
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1) {
            return FrameAccess.createFrameArguments(frame, closure, getContextOrMarkerNode, arg1);
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return frameArguments;
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return frameArguments;
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return frameArguments;
        }

        protected final Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 5);
            frameArguments[FrameAccess.getArgumentStartIndex() + 0] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return frameArguments;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    public abstract static class PrimClosureValue0Node extends AbstractClosurePrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 0"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 0"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 1"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 1"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractClosurePrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 2"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 2"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractClosurePrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 3"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 3"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValue4Node extends AbstractClosurePrimitiveNode implements Primitive4WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 4"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3, arg4));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 4"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValue5Node extends AbstractClosurePrimitiveNode implements Primitive5WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 5"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue5Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3, arg4, arg5));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == 5"}, replaces = "doValue5Direct")
        protected final Object doValue5Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3, arg4, arg5));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222})
    protected abstract static class PrimClosureValueAryNode extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == sizeNode.execute(node, argArray)"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), cachedBlock.getNumArgs());
            copyIntoNode.execute(frameArguments, argArray);
            return directCallNode.call(frameArguments);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"closure.getNumArgs() == sizeNode.execute(node, argArray)"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Bind final Node node,
                        @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final CompiledCodeObject block = closure.getCompiledBlock();
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), sizeNode.execute(node, argArray));
            copyIntoNode.execute(frameArguments, argArray);
            return indirectCallNode.call(block.getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue0Node extends AbstractClosurePrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 0"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue0Direct(final VirtualFrame frame, final BlockClosureObject closure,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 0"}, replaces = "doValue0Direct")
        protected final Object doValue0Indirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue1Node extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 1"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue1Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 1"}, replaces = "doValue1Direct")
        protected final Object doValue1Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue2Node extends AbstractClosurePrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 2"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue2Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 2"}, replaces = "doValue2Direct")
        protected final Object doValue2Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue3Node extends AbstractClosurePrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 3"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue3Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 3"}, replaces = "doValue3Direct")
        protected final Object doValue3Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue4Node extends AbstractClosurePrimitiveNode implements Primitive4WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 4"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue4Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3, arg4));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 4"}, replaces = "doValue4Direct")
        protected final Object doValue4Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3, arg4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue5Node extends AbstractClosurePrimitiveNode implements Primitive5WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 5"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValue5Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @SuppressWarnings("unused") @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(createFrameArguments(frame, closure, arg1, arg2, arg3, arg4, arg5));
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == 5"}, replaces = "doValue5Direct")
        protected final Object doValue5Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(block.getCallTarget(), createFrameArguments(frame, closure, arg1, arg2, arg3, arg4, arg5));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 208)
    public abstract static class PrimFullClosureValueWithArgsNode extends AbstractClosurePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == sizeNode.execute(node, argArray)"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_BLOCK_CACHE_LIMIT")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), cachedBlock.getNumArgs());
            copyIntoNode.execute(frameArguments, argArray);
            return directCallNode.call(frameArguments);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"block.getNumArgs() == sizeNode.execute(node, argArray)"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @Bind("closure.getCompiledBlock()") final CompiledCodeObject block,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode,
                        @Shared("copyIntoNode") @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), block.getNumArgs());
            copyIntoNode.execute(frameArguments, argArray);
            return indirectCallNode.call(block.getCallTarget(), frameArguments);
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return List.copyOf(BlockClosurePrimitivesFactory.getFactories());
    }
}

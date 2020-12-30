/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectCopyIntoObjectArrayNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuaternaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.SenaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @ReportPolymorphism
    public abstract static class AbstractClosurePrimitiveNode extends AbstractPrimitiveNode {
        protected static final int INLINE_CACHE_SIZE = 3;

        @Child protected GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    @ImportStatic(AbstractClosurePrimitiveNode.class)
    public abstract static class PrimClosureValue0Node extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Child private GetContextOrMarkerNode getContextOrMarkerNode = GetContextOrMarkerNode.create();

        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 0"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 0));
        }

        @Specialization(guards = {"closure.getNumArgs() == 0"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractClosurePrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 1"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 1"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractClosurePrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 2"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 2"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractClosurePrimitiveNode implements QuaternaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 3"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 3"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValue4Node extends AbstractClosurePrimitiveNode implements QuinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 4"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 3"}, replaces = "doValueDirect")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValue5Node extends AbstractClosurePrimitiveNode implements SenaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == 5"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue5Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 5);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 5"}, replaces = "doValue5Direct")
        protected final Object doValue5Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222})
    protected abstract static class PrimClosureValueAryNode extends AbstractClosurePrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "closure.getNumArgs() == sizeNode.execute(argArray)"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), sizeNode.execute(argArray));
            copyIntoNode.execute(frameArguments, argArray);
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == sizeNode.execute(argArray)"}, replaces = "doValueDirect", limit = "1")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), sizeNode.execute(argArray));
            copyIntoNode.execute(frameArguments, argArray);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue0Node extends AbstractClosurePrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 0"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue0Direct(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            return directCallNode.call(FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 0));
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 0"}, replaces = "doValue0Direct")
        protected final Object doValue0Indirect(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached final IndirectCallNode indirectCallNode) {
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue1Node extends AbstractClosurePrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 1"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue1Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 1"}, replaces = "doValue1Direct")
        protected final Object doValue1Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue2Node extends AbstractClosurePrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 2"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue2Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 2"}, replaces = "doValue2Direct")
        protected final Object doValue2Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue3Node extends AbstractClosurePrimitiveNode implements QuaternaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 3"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue3Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 3"}, replaces = "doValue3Direct")
        protected final Object doValue3Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue4Node extends AbstractClosurePrimitiveNode implements QuinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 4"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue4Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 4"}, replaces = "doValue4Direct")
        protected final Object doValue4Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @SqueakPrimitive(indices = {207, 209})
    public abstract static class PrimFullClosureValue5Node extends AbstractClosurePrimitiveNode implements SenaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == 5"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValue5Direct(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), 5);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == 5"}, replaces = "doValue5Direct")
        protected final Object doValue5Indirect(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 208)
    protected abstract static class PrimFullClosureValueWithArgsNode extends AbstractClosurePrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock", "cachedBlock.getNumArgs() == sizeNode.execute(argArray)"}, assumptions = {
                        "cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
        protected final Object doValueDirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                        @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached("create(cachedBlock.getCallTarget())") final DirectCallNode directCallNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, cachedBlock, getContextOrMarkerNode.execute(frame), sizeNode.execute(argArray));
            copyIntoNode.execute(frameArguments, argArray);
            return directCallNode.call(frameArguments);
        }

        @Specialization(guards = {"closure.getCompiledBlock().getNumArgs() == sizeNode.execute(argArray)"}, replaces = "doValueDirect", limit = "1")
        protected final Object doValueIndirect(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached("createForFrameArguments()") final ArrayObjectCopyIntoObjectArrayNode copyIntoNode,
                        @Cached final IndirectCallNode indirectCallNode) {
            final Object[] frameArguments = FrameAccess.newFullClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), sizeNode.execute(argArray));
            copyIntoNode.execute(frameArguments, argArray);
            return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), frameArguments);
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }
}

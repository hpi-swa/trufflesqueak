package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.BlockActivationNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 202, 203, 204, 205})
    protected abstract static class PrimClosureValueNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        @Child private BlockActivationNode activationNode = BlockActivationNode.create();

        protected PrimClosureValueNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doClosure(final VirtualFrame frame, final BlockClosureObject block, final NotProvided arg1, final NotProvided arg2, final NotProvided arg3, final NotProvided arg4,
                        final NotProvided arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), ArrayUtils.EMPTY_ARRAY));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 1", "!isNotProvided(arg1)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final NotProvided arg2, final NotProvided arg3, final NotProvided arg4,
                        final NotProvided arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), new Object[]{arg1}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 2", "!isNotProvided(arg1)", "!isNotProvided(arg2)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final NotProvided arg3, final NotProvided arg4,
                        final NotProvided arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), new Object[]{arg1, arg2}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 3", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final NotProvided arg4,
                        final NotProvided arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 4", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3, arg4}));
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 5", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), new Object[]{arg1, arg2, arg3, arg4, arg5}));
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 206)
    protected abstract static class PrimClosureValueAryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final BlockActivationNode activationNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            return activationNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), getObjectArrayNode.execute(argArray)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 221)
    public abstract static class PrimClosureValueNoContextSwitchNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValueNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block,
                        @Cached final BlockActivationNode activationNode) {
            final Object[] arguments = FrameAccess.newClosureArguments(block, getContextOrMarker(frame), ArrayUtils.EMPTY_ARRAY);
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return activationNode.executeBlock(block, arguments);
            } finally {
                if (wasActive) {
                    method.image.interrupt.activate();
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 222)
    protected abstract static class PrimClosureValueAryNoContextSwitchNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getCompiledBlock().getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final BlockActivationNode activationNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            final Object[] arguments = FrameAccess.newClosureArguments(block, getContextOrMarker(frame), getObjectArrayNode.execute(argArray));
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return activationNode.executeBlock(block, arguments);
            } finally {
                if (wasActive) {
                    method.image.interrupt.activate();
                }
            }
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BlockClosurePrimitivesFactory.getFactories();
    }
}

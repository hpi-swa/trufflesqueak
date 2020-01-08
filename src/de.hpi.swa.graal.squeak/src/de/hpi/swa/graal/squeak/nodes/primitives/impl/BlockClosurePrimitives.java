/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.DispatchBlockNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives.PrimRelinquishProcessorNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.NotProvided;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {201, 221})
    public abstract static class PrimClosureValue0Node extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValue0Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block,
                        @Cached final DispatchBlockNode dispatchNode) {
            return dispatchNode.executeBlock(block, FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 0));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValue1Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == 1"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg,
                        @Cached final DispatchBlockNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg;
            return dispatchNode.executeBlock(block, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimClosureValue2Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == 2"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2,
                        @Cached final DispatchBlockNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return dispatchNode.executeBlock(block, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        protected PrimClosureValue3Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == 3"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final DispatchBlockNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return dispatchNode.executeBlock(block, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValueNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimClosureValueNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"block.getNumArgs() == 4"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided arg5,
                        @Shared("dispatchNode") @Cached final DispatchBlockNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return dispatchNode.executeBlock(block, frameArguments);
        }

        @Specialization(guards = {"block.getNumArgs() == 5", "!isNotProvided(arg5)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5,
                        @Shared("dispatchNode") @Cached final DispatchBlockNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 5);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return dispatchNode.executeBlock(block, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {206, 222})
    protected abstract static class PrimClosureValueAryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final DispatchBlockNode dispatchNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            return dispatchNode.executeBlock(block, FrameAccess.newClosureArguments(block, getContextOrMarker(frame), getObjectArrayNode.execute(argArray)));
        }
    }

    /**
     * Non-context-switching closureValue primitives are not in use because interrupt checks only
     * happen in the idleProcess (see {@link PrimRelinquishProcessorNode}). Using standard
     * closureValue primitives instead.
     */

    // @GenerateNodeFactory
    // @SqueakPrimitive(indices = 221)
    public abstract static class PrimClosureValueNoContextSwitchNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValueNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block,
                        @Cached final DispatchBlockNode dispatchNode) {
            final Object[] arguments = FrameAccess.newClosureArgumentsTemplate(block, getContextOrMarker(frame), 0);
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return dispatchNode.executeBlock(block, arguments);
            } finally {
                if (wasActive) {
                    method.image.interrupt.activate();
                }
            }
        }
    }

    // @GenerateNodeFactory
    // @SqueakPrimitive(indices = 222)
    protected abstract static class PrimClosureValueAryNoContextSwitchNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"block.getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject block, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final DispatchBlockNode dispatchNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            final Object[] frameArguments = FrameAccess.newClosureArguments(block, getContextOrMarker(frame), getObjectArrayNode.execute(argArray));
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return dispatchNode.executeBlock(block, frameArguments);
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

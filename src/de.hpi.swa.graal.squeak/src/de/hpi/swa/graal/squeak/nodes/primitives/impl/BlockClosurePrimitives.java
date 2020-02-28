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
import de.hpi.swa.graal.squeak.nodes.DispatchClosureNode;
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
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.NotProvided;

public final class BlockClosurePrimitives extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 201)
    public abstract static class PrimClosureValue0Node extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValue0Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached final DispatchClosureNode dispatchNode) {
            return dispatchNode.execute(closure, FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 0));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 202)
    protected abstract static class PrimClosureValue1Node extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValue1Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == 1"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final Object arg,
                        @Cached final DispatchClosureNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 1);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg;
            return dispatchNode.execute(closure, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 203)
    protected abstract static class PrimClosureValue2Node extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimClosureValue2Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == 2"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2,
                        @Cached final DispatchClosureNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 2);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            return dispatchNode.execute(closure, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 204)
    protected abstract static class PrimClosureValue3Node extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        protected PrimClosureValue3Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == 3"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3,
                        @Cached final DispatchClosureNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 3);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            return dispatchNode.execute(closure, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 205)
    protected abstract static class PrimClosureValueNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimClosureValueNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"closure.getNumArgs() == 4"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided arg5,
                        @Shared("dispatchNode") @Cached final DispatchClosureNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 4);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            return dispatchNode.execute(closure, frameArguments);
        }

        @Specialization(guards = {"closure.getNumArgs() == 5", "!isNotProvided(arg5)"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5,
                        @Shared("dispatchNode") @Cached final DispatchClosureNode dispatchNode) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 5);
            frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
            frameArguments[FrameAccess.getArgumentStartIndex() + 1] = arg2;
            frameArguments[FrameAccess.getArgumentStartIndex() + 2] = arg3;
            frameArguments[FrameAccess.getArgumentStartIndex() + 3] = arg4;
            frameArguments[FrameAccess.getArgumentStartIndex() + 4] = arg5;
            return dispatchNode.execute(closure, frameArguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 206)
    protected abstract static class PrimClosureValueAryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimClosureValueAryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final DispatchClosureNode dispatchNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            return dispatchNode.execute(closure, FrameAccess.newClosureArguments(closure, getContextOrMarker(frame), getObjectArrayNode.execute(argArray)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 221)
    public abstract static class PrimClosureValueNoContextSwitchNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimClosureValueNoContextSwitchNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"closure.getNumArgs() == 0"})
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure,
                        @Cached final DispatchClosureNode dispatchNode) {
            final Object[] arguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarker(frame), 0);
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return dispatchNode.execute(closure, arguments);
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

        @Specialization(guards = {"closure.getNumArgs() == sizeNode.execute(argArray)"}, limit = "1")
        protected final Object doValue(final VirtualFrame frame, final BlockClosureObject closure, final ArrayObject argArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final DispatchClosureNode dispatchNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            final Object[] frameArguments = FrameAccess.newClosureArguments(closure, getContextOrMarker(frame), getObjectArrayNode.execute(argArray));
            final boolean wasActive = method.image.interrupt.isActive();
            method.image.interrupt.deactivate();
            try {
                return dispatchNode.execute(closure, frameArguments);
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

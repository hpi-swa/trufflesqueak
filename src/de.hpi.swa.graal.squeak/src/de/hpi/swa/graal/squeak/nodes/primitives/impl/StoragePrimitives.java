/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.Collection;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectBecomeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectHashNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectPointersBecomeOneWayNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.NotProvided;

public final class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    private abstract static class AbstractInstancesPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected ObjectGraphNode objectGraphNode;

        protected AbstractInstancesPrimitiveNode(final CompiledMethodObject method) {
            super(method);
            objectGraphNode = ObjectGraphNode.create(method.image);
        }
    }

    protected abstract static class AbstractArrayBecomeOneWayPrimitiveNode extends AbstractInstancesPrimitiveNode {
        @Child private SqueakObjectPointersBecomeOneWayNode pointersBecomeNode = SqueakObjectPointersBecomeOneWayNode.create();
        @Child private UpdateSqueakObjectHashNode updateHashNode = UpdateSqueakObjectHashNode.create();

        protected AbstractArrayBecomeOneWayPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final ArrayObject performPointersBecomeOneWay(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash) {
            final Object[] fromPointers = fromArray.getObjectStorage();
            final Object[] toPointers = toArray.getObjectStorage();
            // Need to operate on copy of `fromPointers` because itself will also be changed.
            final Object[] fromPointersClone = fromPointers.clone();
            objectGraphNode.executePointersBecomeOneWay(pointersBecomeNode, fromPointersClone, toPointers, copyHash);
            patchTruffleFrames(fromPointersClone, toPointers, copyHash);
            return fromArray;
        }

        @TruffleBoundary
        private void patchTruffleFrames(final Object[] fromPointers, final Object[] toPointers, final boolean copyHash) {
            final int fromPointersLength = fromPointers.length;

            Truffle.getRuntime().iterateFrames((frameInstance) -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                if (!FrameAccess.isGraalSqueakFrame(current)) {
                    return null;
                }
                final Object[] arguments = current.getArguments();
                for (int i = 0; i < arguments.length; i++) {
                    final Object argument = arguments[i];
                    if (argument != null) {
                        for (int j = 0; j < fromPointersLength; j++) {
                            final Object fromPointer = fromPointers[j];
                            if (argument == fromPointer) {
                                final Object toPointer = toPointers[j];
                                arguments[i] = toPointer;
                                updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                            } else {
                                pointersBecomeNode.execute(argument, fromPointers, toPointers, copyHash);
                            }
                        }
                    }
                }

                final CompiledCodeObject blockOrMethod = arguments[2] != null ? ((BlockClosureObject) arguments[2]).getCompiledBlock() : (CompiledMethodObject) arguments[0];
                final ContextObject context = (ContextObject) FrameUtil.getObjectSafe(current, blockOrMethod.getThisContextSlot());
                if (context != null) {
                    for (int j = 0; j < fromPointersLength; j++) {
                        final Object fromPointer = fromPointers[j];
                        if (context == fromPointer) {
                            final Object toPointer = toPointers[j];
                            FrameAccess.setContext(current, blockOrMethod, (ContextObject) toPointer);
                            updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                        } else {
                            pointersBecomeNode.execute(context, fromPointers, toPointers, copyHash);
                        }
                    }
                }

                final int stackp = FrameUtil.getIntSafe(current, blockOrMethod.getStackPointerSlot());
                final FrameSlot[] stackSlots = blockOrMethod.getStackSlotsUnsafe();
                final FrameDescriptor frameDescriptor = blockOrMethod.getFrameDescriptor();
                for (int i = 0; i < stackp; i++) {
                    final FrameSlot frameSlot = stackSlots[i];
                    if (frameSlot == null) {
                        return null; // Stop here, slot has not (yet) been created.
                    }
                    final FrameSlotKind frameSlotKind = frameDescriptor.getFrameSlotKind(frameSlot);
                    if (frameSlotKind == FrameSlotKind.Object) {
                        final Object stackObject = FrameUtil.getObjectSafe(current, frameSlot);
                        if (stackObject == null) {
                            return null;
                        }
                        for (int j = 0; j < fromPointersLength; j++) {
                            final Object fromPointer = fromPointers[j];
                            if (stackObject == fromPointer) {
                                final Object toPointer = toPointers[j];
                                assert toPointer != null : "Unexpected `null` value";
                                current.setObject(frameSlot, toPointer);
                                updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                            } else {
                                pointersBecomeNode.execute(stackObject, fromPointers, toPointers, copyHash);
                            }
                        }
                    } else if (frameSlotKind == FrameSlotKind.Illegal) {
                        /** this slot and all following ones are not initialized, done. */
                        return null;
                    }
                }
                return null;
            });
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 68)
    protected abstract static class PrimCompiledMethodObjectAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimCompiledMethodObjectAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object literalAt(final CompiledCodeObject receiver, final long index) {
            // Use getLiterals() instead of getLiteral(i), the latter skips the header.
            return receiver.getLiterals()[(int) index - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 69)
    protected abstract static class PrimCompiledMethodObjectAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimCompiledMethodObjectAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object setLiteral(final CompiledCodeObject code, final long index, final Object value) {
            code.setLiteral(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 70)
    protected abstract static class PrimNewNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected static final int NEW_CACHE_SIZE = 3;
        @Child private SqueakObjectNewNode newNode;

        protected PrimNewNode(final CompiledMethodObject method) {
            super(method);
            newNode = SqueakObjectNewNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected AbstractSqueakObjectWithHash newDirect(final ClassObject receiver,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached final BranchProfile outOfMemProfile) {
            try {
                return newNode.execute(cachedReceiver);
            } catch (final OutOfMemoryError e) {
                outOfMemProfile.enter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @Specialization(replaces = "newDirect")
        protected final AbstractSqueakObjectWithHash newIndirect(final ClassObject receiver,
                        @Cached final BranchProfile outOfMemProfile) {
            try {
                return newNode.execute(receiver);
            } catch (final OutOfMemoryError e) {
                outOfMemProfile.enter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 71)
    protected abstract static class PrimNewWithArgNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected static final int NEW_CACHE_SIZE = 3;
        @Child private SqueakObjectNewNode newNode;

        protected PrimNewWithArgNode(final CompiledMethodObject method) {
            super(method);
            newNode = SqueakObjectNewNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver", "isInstantiable(cachedReceiver, size)"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected final AbstractSqueakObjectWithHash newWithArgDirect(final ClassObject receiver, final long size,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached final BranchProfile outOfMemProfile) {
            try {
                return newNode.execute(cachedReceiver, (int) size);
            } catch (final OutOfMemoryError e) {
                outOfMemProfile.enter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @Specialization(replaces = "newWithArgDirect", guards = "isInstantiable(receiver, size)")
        protected final AbstractSqueakObjectWithHash newWithArg(final ClassObject receiver, final long size,
                        @Cached final BranchProfile outOfMemProfile) {
            try {
                return newNode.execute(receiver, (int) size);
            } catch (final OutOfMemoryError e) {
                outOfMemProfile.enter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        protected static final boolean isInstantiable(final ClassObject receiver, final long size) {
            return size == 0 || receiver.isVariable() && 0 <= size && size <= Integer.MAX_VALUE;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final Object doBadArgument(final Object receiver, final Object value) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 72)
    protected abstract static class PrimArrayBecomeOneWayNode extends AbstractArrayBecomeOneWayPrimitiveNode implements BinaryPrimitive {

        protected PrimArrayBecomeOneWayNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"sizeNode.execute(fromArray) == sizeNode.execute(toArray)", "fromArray.isTraceable()", "toArray.isTraceable()"}, limit = "1")
        protected final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            return performPointersBecomeOneWay(fromArray, toArray, true);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"sizeNode.execute(fromArray) == sizeNode.execute(toArray)", "!fromArray.isTraceable() || !toArray.isTraceable()"}, limit = "1")
        protected static final ArrayObject doInapproriateOperation(final ArrayObject fromArray, final ArrayObject toArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            throw PrimitiveFailed.INAPPROPRIATE_OPERATION;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "sizeNode.execute(fromArray) != sizeNode.execute(toArray)", limit = "1")
        protected static final ArrayObject doBadArgument(final ArrayObject fromArray, final ArrayObject toArray,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isArrayObject(receiver)"})
        protected static final ArrayObject doBadReceiver(final Object receiver, final ArrayObject argument) {
            throw PrimitiveFailed.BAD_RECEIVER;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isArrayObject(argument)"})
        protected static final ArrayObject doBadArgument(final ArrayObject receiver, final Object argument) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 73)
    protected abstract static class PrimInstVarAtNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimInstVarAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))")
        protected final Object doAt(final Object receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return at0Node.execute(receiver, index - 1);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))") // Context>>#object:instVarAt:
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index) {
            return at0Node.execute(target, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 74)
    protected abstract static class PrimInstVarAtPutNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimInstVarAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))")
        protected final Object doAtPut(final Object receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided) {
            atPut0Node.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))") // Context>>#object:instVarAt:put:
        protected final Object doAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value) {
            atPut0Node.execute(target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 75)
    protected abstract static class PrimIdentityHashNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIdentityHashNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doHash(final Object receiver, @Cached final SqueakObjectHashNode hashNode) {
            return hashNode.execute(receiver);
        }
    }

    /* primitiveNextInstance (#78) deprecated in favor of primitiveAllInstances (#177). */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 79)
    protected abstract static class PrimNewMethodNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimNewMethodNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Instantiating a {@link CompiledMethodObject} allocates a {@link FrameDescriptor} which
         * should never be part of compilation, thus the <code>@TruffleBoundary</code>.
         */
        @TruffleBoundary
        @Specialization(guards = "receiver.isCompiledMethodClass()")
        protected final CompiledMethodObject newMethod(final ClassObject receiver, final long bytecodeCount, final long header) {
            assert receiver.getBasicInstanceSize() == 0;
            final CompiledMethodObject newMethod = CompiledMethodObject.newOfSize(method.image, (int) bytecodeCount);
            newMethod.setHeader(header);
            return newMethod;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 128)
    protected abstract static class PrimArrayBecomeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private SqueakObjectBecomeNode becomeNode = SqueakObjectBecomeNode.create();
        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();
        private final BranchProfile failProfile = BranchProfile.create();

        protected PrimArrayBecomeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"sizeNode.execute(receiver) == sizeNode.execute(other)"})
        protected final ArrayObject doBecome(final ArrayObject receiver, final ArrayObject other) {
            final int receiverSize = sizeNode.execute(receiver);
            int numBecomes = 0;
            final Object[] lefts = new Object[receiverSize];
            final Object[] rights = new Object[receiverSize];
            for (int i = 0; i < receiverSize; i++) {
                final Object left = readNode.execute(receiver, i);
                final Object right = readNode.execute(other, i);
                if (becomeNode.execute(left, right)) {
                    lefts[numBecomes] = left;
                    rights[numBecomes] = right;
                    numBecomes++;
                } else {
                    failProfile.enter();
                    for (int j = 0; j < numBecomes; j++) {
                        becomeNode.execute(lefts[j], rights[j]);
                    }
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
            }
            return receiver;
        }

        @Specialization(guards = {"sizeNode.execute(receiver) != sizeNode.execute(other)"})
        @SuppressWarnings("unused")
        protected static final ArrayObject doBadSize(final ArrayObject receiver, final ArrayObject other) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        @Specialization(guards = {"!isArrayObject(receiver)"})
        @SuppressWarnings("unused")
        protected static final ArrayObject doBadReceiver(final Object receiver, final ArrayObject other) {
            throw PrimitiveFailed.BAD_RECEIVER;
        }

        @Specialization(guards = {"!isArrayObject(other)"})
        @SuppressWarnings("unused")
        protected static final ArrayObject doBadArgument(final ArrayObject receiver, final Object other) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 129)
    protected abstract static class PrimSpecialObjectsArrayNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimSpecialObjectsArrayNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver) {
            return method.image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 138)
    protected abstract static class PrimSomeObjectNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimSomeObjectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final ArrayObject doSome(@SuppressWarnings("unused") final Object receiver) {
            return method.image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 139)
    protected abstract static class PrimNextObjectNode extends AbstractInstancesPrimitiveNode implements UnaryPrimitive {

        protected PrimNextObjectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final AbstractSqueakObject doNext(final AbstractSqueakObjectWithClassAndHash receiver) {
            return getNext(receiver, objectGraphNode.executeAllInstances());
        }

        @TruffleBoundary
        private static AbstractSqueakObject getNext(final AbstractSqueakObjectWithClassAndHash receiver, final Collection<AbstractSqueakObjectWithHash> allInstances) {
            boolean foundMyself = false;
            for (final AbstractSqueakObjectWithHash instance : allInstances) {
                if (instance == receiver) {
                    foundMyself = true;
                } else if (foundMyself) {
                    return instance;
                }
            }
            return allInstances.iterator().next(); // first
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 170)
    protected abstract static class PrimCharacterValueNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimCharacterValueNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doLong(final long receiver, @SuppressWarnings("unused") final NotProvided target,
                        @Shared("isFiniteProfile") @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return CharacterObject.valueOf(Math.toIntExact(receiver), isFiniteProfile);
        }

        /* Character class>>#value: */
        @Specialization
        protected static final Object doLong(@SuppressWarnings("unused") final Object receiver, final long target,
                        @Shared("isFiniteProfile") @Cached("createBinaryProfile()") final ConditionProfile isFiniteProfile) {
            return CharacterObject.valueOf(Math.toIntExact(target), isFiniteProfile);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 171)
    protected abstract static class PrimImmediateAsIntegerNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimImmediateAsIntegerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doChar(final char receiver) {
            return receiver;
        }

        @Specialization
        protected static final long doCharacterObject(final CharacterObject receiver) {
            return receiver.getValue();
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            return Double.doubleToRawLongBits(receiver);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 173)
    protected abstract static class PrimSlotAtNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimSlotAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))")
        protected final Object doSlotAt(final Object receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return at0Node.execute(receiver, index - 1);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))")
        protected final Object doSlotAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index) {
            return at0Node.execute(target, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 174)
    protected abstract static class PrimSlotAtPutNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimSlotAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))")
        protected final Object doSlotAtPut(final Object receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided) {
            atPut0Node.execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))")
        protected final Object doSlotAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value) {
            atPut0Node.execute(target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 175)
    protected abstract static class PrimBehaviorHashNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimBehaviorHashNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doClass(final ClassObject receiver) {
            return receiver.getSqueakHash();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 178)
    protected abstract static class PrimAllObjectsNode extends AbstractInstancesPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimAllObjectsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final ArrayObject doAll(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asArrayOfObjects(ArrayUtils.toArray(objectGraphNode.executeAllInstances()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 181)
    protected abstract static class PrimSizeInBytesOfInstanceNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSizeInBytesOfInstanceNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doBasicSize(final ClassObject receiver, @SuppressWarnings("unused") final NotProvided value) {
            return postProcessSize(receiver.getBasicInstanceSize());
        }

        @Specialization(guards = "receiver.getInstanceSpecification() == 9")
        protected static final long doSize64bit(final ClassObject receiver, final long numElements) {
            return postProcessSize(receiver.getBasicInstanceSize() + numElements * 2);
        }

        @Specialization(guards = {"receiver.getInstanceSpecification() != 9", "receiver.getInstanceSpecification() < 12"})
        protected static final long doSize32bit(final ClassObject receiver, final long numElements) {
            return postProcessSize(receiver.getBasicInstanceSize() + numElements);
        }

        @Specialization(guards = "between(receiver.getInstanceSpecification(), 12, 15)")
        protected static final long doSize16bit(final ClassObject receiver, final long numElements) {
            return postProcessSize(receiver.getBasicInstanceSize() + ((numElements + 1) / 2 | 0));
        }

        @Specialization(guards = "receiver.getInstanceSpecification() >= 16")
        protected static final long doSize8bit(final ClassObject receiver, final long numElements) {
            return postProcessSize(receiver.getBasicInstanceSize() + ((numElements + 3) / 4 | 0));
        }

        private static long postProcessSize(final long originalSize) {
            long size = originalSize;
            size += size & 1;             // align to 64 bits
            size += size >= 255 ? 4 : 2;  // header words
            if (size < 4) {
                size = 4;                 // minimum object size
            }
            return size;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 249)
    protected abstract static class PrimArrayBecomeOneWayCopyHashNode extends AbstractArrayBecomeOneWayPrimitiveNode implements TernaryPrimitive {

        protected PrimArrayBecomeOneWayCopyHashNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"sizeNode.execute(fromArray) == sizeNode.execute(toArray)", "fromArray.isTraceable()", "toArray.isTraceable()"}, limit = "1")
        protected final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            return performPointersBecomeOneWay(fromArray, toArray, copyHash);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"sizeNode.execute(fromArray) == sizeNode.execute(toArray)", "!fromArray.isTraceable() || !toArray.isTraceable()"}, limit = "1")
        protected static final ArrayObject doInapproriateOperation(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            throw PrimitiveFailed.INAPPROPRIATE_OPERATION;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "sizeNode.execute(fromArray) != sizeNode.execute(toArray)", limit = "1")
        protected static final ArrayObject doBadArgument(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isArrayObject(receiver)"})
        protected static final ArrayObject doBadReceiver(final Object receiver, final ArrayObject argument, final boolean copyHash) {
            throw PrimitiveFailed.BAD_RECEIVER;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isArrayObject(argument)"})
        protected static final ArrayObject doBadArgument(final ArrayObject receiver, final Object argument, final boolean copyHash) {
            throw PrimitiveFailed.BAD_ARGUMENT;
        }
    }
}

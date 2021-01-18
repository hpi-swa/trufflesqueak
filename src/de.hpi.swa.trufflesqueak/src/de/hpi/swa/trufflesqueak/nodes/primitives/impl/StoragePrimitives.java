/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Collection;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectBecomeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuaternaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils;

public final class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractArrayBecomeOneWayPrimitiveNode extends AbstractPrimitiveNode {

        protected static final ArrayObject performPointersBecomeOneWay(final SqueakImageContext image, final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash) {
            if (!fromArray.isObjectType() || !toArray.isObjectType() || fromArray.getObjectLength() != toArray.getObjectLength()) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_ARGUMENT;
            }

            final Object[] fromPointers = fromArray.getObjectStorage();
            final Object[] toPointers = toArray.getObjectStorage();

            if (copyHash) {
                copyHash(fromPointers, toPointers);
            }
            // Need to operate on copy of `fromPointers` because itself will also be changed.
            final Object[] fromPointersClone = fromPointers.clone();
            ObjectGraphUtils.pointersBecomeOneWay(image, fromPointersClone, toPointers);
            patchTruffleFrames(fromPointersClone, toPointers);
            image.flushMethodCacheAfterBecome();
            return fromArray;
        }

        private static void copyHash(final Object[] fromPointers, final Object[] toPointers) {
            for (int i = 0; i < fromPointers.length; i++) {
                final Object from = fromPointers[i];
                final Object to = toPointers[i];
                if (from instanceof AbstractSqueakObjectWithClassAndHash && to instanceof AbstractSqueakObjectWithClassAndHash) {
                    ((AbstractSqueakObjectWithClassAndHash) to).setSqueakHash(((AbstractSqueakObjectWithClassAndHash) from).getSqueakHash());
                }
            }
        }

        @TruffleBoundary
        private static void patchTruffleFrames(final Object[] fromPointers, final Object[] toPointers) {
            final int fromPointersLength = fromPointers.length;

            Truffle.getRuntime().iterateFrames((frameInstance) -> {
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                if (!FrameAccess.isTruffleSqueakFrame(current)) {
                    return null;
                }
                final Object[] arguments = current.getArguments();
                for (int i = 0; i < arguments.length; i++) {
                    final Object argument = arguments[i];
                    for (int j = 0; j < fromPointersLength; j++) {
                        final Object fromPointer = fromPointers[j];
                        if (argument == fromPointer) {
                            final Object toPointer = toPointers[j];
                            arguments[i] = toPointer;
                        } else if (argument instanceof AbstractSqueakObjectWithClassAndHash) {
                            ((AbstractSqueakObjectWithClassAndHash) argument).pointersBecomeOneWay(fromPointers, toPointers);
                        }
                    }
                }

                final CompiledCodeObject code = FrameAccess.getMethodOrBlock(current);
                final ContextObject context = FrameAccess.getContext(current, code);
                if (context != null) {
                    for (int j = 0; j < fromPointersLength; j++) {
                        final Object fromPointer = fromPointers[j];
                        if (context == fromPointer) {
                            final Object toPointer = toPointers[j];
                            FrameAccess.setContext(current, code, (ContextObject) toPointer);
                        } else {
                            context.pointersBecomeOneWay(fromPointers, toPointers);
                        }
                    }
                }

                /*
                 * Iterate over all stack slots here instead of stackPointer because in rare cases,
                 * the stack is accessed behind the stackPointer.
                 */
                FrameAccess.iterateStackSlots(current, slot -> {
                    if (current.isObject(slot)) {
                        final Object stackObject = FrameUtil.getObjectSafe(current, slot);
                        for (int j = 0; j < fromPointersLength; j++) {
                            final Object fromPointer = fromPointers[j];
                            if (stackObject == fromPointer) {
                                final Object toPointer = toPointers[j];
                                assert toPointer != null : "Unexpected `null` value";
                                current.setObject(slot, toPointer);
                            } else if (stackObject instanceof AbstractSqueakObjectWithClassAndHash) {
                                ((AbstractSqueakObjectWithClassAndHash) stackObject).pointersBecomeOneWay(fromPointers, toPointers);
                            }
                        }
                    }
                });
                return null;
            });
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 68)
    protected abstract static class PrimCompiledMethodObjectAtNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "to0(index) < receiver.getLiterals().length")
        protected static final Object literalAt(final CompiledCodeObject receiver, final long index) {
            // Use getLiterals() instead of getLiteral(i), the latter skips the header.
            return receiver.getLiterals()[(int) index - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 69)
    protected abstract static class PrimCompiledMethodObjectAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "to0(index) < receiver.getLiterals().length")
        protected static final Object setLiteral(final CompiledCodeObject receiver, final long index, final Object value) {
            receiver.setLiteral(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 70)
    public abstract static class PrimNewNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        public static final int NEW_CACHE_SIZE = 6;

        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected static final AbstractSqueakObjectWithClassAndHash newDirect(@SuppressWarnings("unused") final ClassObject receiver,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached final SqueakObjectNewNode newNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return newNode.execute(image, cachedReceiver);
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @Specialization(replaces = "newDirect")
        protected static final AbstractSqueakObjectWithClassAndHash newIndirect(final ClassObject receiver,
                        @Cached final SqueakObjectNewNode newNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return newNode.execute(image, receiver);
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 71)
    protected abstract static class PrimNewWithArgNode extends AbstractPrimitiveNode {
        public static final int NEW_CACHE_SIZE = 6;

        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver", "isInstantiable(cachedReceiver, size)"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected static final AbstractSqueakObjectWithClassAndHash newWithArgDirect(@SuppressWarnings("unused") final ClassObject receiver, final long size,
                        @Cached("createIdentityProfile()") final IntValueProfile sizeProfile,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached final SqueakObjectNewNode newNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return newNode.execute(image, cachedReceiver, sizeProfile.profile((int) size));
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @Specialization(replaces = "newWithArgDirect", guards = "isInstantiable(receiver, size)")
        protected static final AbstractSqueakObjectWithClassAndHash newWithArg(final ClassObject receiver, final long size,
                        @Cached final SqueakObjectNewNode newNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return newNode.execute(image, receiver, (int) size);
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
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
    protected abstract static class PrimArrayBecomeOneWayNode extends AbstractArrayBecomeOneWayPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return performPointersBecomeOneWay(image, fromArray, toArray, true);
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
    protected abstract static class PrimInstVarAt2Node extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))", limit = "1")
        protected static final Object doAt(final Object receiver, final long index,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(receiver, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 73)
    /* Context>>#object:instVarAt: */
    protected abstract static class PrimInstVarAt3Node extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))", limit = "1")
        protected static final Object doAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(target, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 74)
    protected abstract static class PrimInstVarAtPut3Node extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))", limit = "1")
        protected static final Object doAtPut(final Object receiver, final long index, final Object value,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(receiver, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 74)
    /* Context>>#object:instVarAt:put: */
    protected abstract static class PrimInstVarAtPut4Node extends AbstractPrimitiveNode implements QuaternaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))", limit = "1")
        protected static final Object doAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 75)
    protected abstract static class PrimIdentityHashNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected static final long doNil(@SuppressWarnings("unused") final NilObject object) {
            return NilObject.SQUEAK_HASH;
        }

        @Specialization(guards = "!object")
        protected static final long doBooleanFalse(@SuppressWarnings("unused") final boolean object) {
            return BooleanObject.FALSE_SQUEAK_HASH;
        }

        @Specialization(guards = "object")
        protected static final long doBooleanTrue(@SuppressWarnings("unused") final boolean object) {
            return BooleanObject.TRUE_SQUEAK_HASH;
        }

        @Specialization
        protected static final long doAbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash object,
                        @Cached final BranchProfile needsHashProfile) {
            return object.getSqueakHash(needsHashProfile);
        }
    }

    /* primitiveNextInstance (#78) deprecated in favor of primitiveAllInstances (#177). */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 79)
    protected abstract static class PrimNewMethodNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        /**
         * Instantiating a {@link CompiledCodeObject} allocates a {@link FrameDescriptor} which
         * should never be part of compilation, thus the <code>@TruffleBoundary</code>.
         */
        @TruffleBoundary
        @Specialization(guards = "receiver.isCompiledMethodClassType()")
        protected static final CompiledCodeObject newMethod(final ClassObject receiver, final long bytecodeCount, final long header,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            assert receiver.getBasicInstanceSize() == 0;
            assert receiver.getSuperclassOrNull() == image.compiledMethodClass.getSuperclassOrNull() : "Receiver must be subclass of CompiledCode";
            final CompiledCodeObject newMethod = CompiledCodeObject.newOfSize(image, (int) bytecodeCount, receiver);
            newMethod.setHeader(header);
            return newMethod;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 128)
    protected abstract static class PrimArrayBecomeNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
        @Child private SqueakObjectBecomeNode becomeNode = SqueakObjectBecomeNode.create();
        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();
        private final BranchProfile failProfile = BranchProfile.create();

        @Specialization(guards = {"sizeNode.execute(receiver) == sizeNode.execute(other)"})
        protected final ArrayObject doBecome(final ArrayObject receiver, final ArrayObject other,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
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
            image.flushMethodCacheAfterBecome();
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
    protected abstract static class PrimSpecialObjectsArrayNode extends AbstractPrimitiveNode {

        @Specialization
        protected static final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 138)
    protected abstract static class PrimSomeObjectNode extends AbstractPrimitiveNode {

        @Specialization
        protected static final ArrayObject doSome(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 139)
    protected abstract static class PrimNextObjectNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final AbstractSqueakObject doNext(final AbstractSqueakObjectWithClassAndHash receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return getNext(receiver, ObjectGraphUtils.allInstances(image));
        }

        @TruffleBoundary
        private static AbstractSqueakObject getNext(final AbstractSqueakObjectWithClassAndHash receiver, final Collection<AbstractSqueakObjectWithClassAndHash> allInstances) {
            boolean foundMyself = false;
            for (final AbstractSqueakObjectWithClassAndHash instance : allInstances) {
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
    @ImportStatic(Integer.class)
    @SqueakPrimitive(indices = 170)
    protected abstract static class PrimCharacterValue1Node extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization(guards = {"0 <= receiver", "receiver <= MAX_VALUE"}, rewriteOn = RespecializeException.class)
        protected static final char doLongExact(final long receiver) throws RespecializeException {
            return CharacterObject.valueExactOf(receiver);
        }

        @Specialization(guards = {"0 <= receiver", "receiver <= MAX_VALUE"}, replaces = "doLongExact")
        protected static final Object doLong(final long receiver,
                        @Cached final ConditionProfile isImmediateProfile) {
            return CharacterObject.valueOf(receiver, isImmediateProfile);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(Integer.class)
    @SqueakPrimitive(indices = 170)
    protected abstract static class PrimCharacterValue2Node extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = {"0 <= target", "target <= MAX_VALUE"}, rewriteOn = RespecializeException.class)
        protected static final char doClassLongExact(@SuppressWarnings("unused") final Object receiver, final long target) throws RespecializeException {
            return CharacterObject.valueExactOf(target);
        }

        @Specialization(guards = {"0 <= target", "target <= MAX_VALUE"}, replaces = "doClassLongExact")
        protected static final Object doClassLong(@SuppressWarnings("unused") final Object receiver, final long target,
                        @Cached final ConditionProfile isImmediateProfile) {
            return CharacterObject.valueOf(target, isImmediateProfile);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 171)
    protected abstract static class PrimImmediateAsIntegerNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

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
    protected abstract static class PrimSlotAt2Node extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))", limit = "1")
        protected static final Object doSlotAt(final Object receiver, final long index,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(receiver, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 173)
    protected abstract static class PrimSlotAt3Node extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))", limit = "1")
        protected static final Object doSlotAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(target, index - 1);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 174)
    protected abstract static class PrimSlotAtPut3Node extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(receiver))", limit = "1")
        protected static final Object doSlotAtPut(final Object receiver, final long index, final Object value,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(receiver, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 174)
    protected abstract static class PrimSlotAtPut4Node extends AbstractPrimitiveNode implements QuaternaryPrimitiveFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(target))", limit = "1")
        protected static final Object doSlotAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 175)
    protected abstract static class PrimBehaviorHashNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final long doClass(final ClassObject receiver,
                        @Cached final BranchProfile needsHashProfile) {
            return receiver.getSqueakHash(needsHashProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 178)
    protected abstract static class PrimAllObjectsNode extends AbstractPrimitiveNode {

        @Specialization
        protected static final ArrayObject doAll(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asArrayOfObjects(ArrayUtils.toArray(ObjectGraphUtils.allInstances(image)));
        }
    }

    private abstract static class AbstractPrimSizeInBytesOfInstance1Node extends AbstractPrimitiveNode {
        protected static final long postProcessSize(final long originalSize) {
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
    @SqueakPrimitive(indices = 181)
    protected abstract static class PrimSizeInBytesOfInstance1Node extends AbstractPrimSizeInBytesOfInstance1Node implements UnaryPrimitiveFallback {
        @Specialization
        protected static final long doBasicSize(final ClassObject receiver) {
            return postProcessSize(receiver.getBasicInstanceSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 181)
    protected abstract static class PrimSizeInBytesOfInstance2Node extends AbstractPrimSizeInBytesOfInstance1Node implements BinaryPrimitiveFallback {
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
            return postProcessSize(receiver.getBasicInstanceSize() + (numElements + 1) / 2);
        }

        @Specialization(guards = "receiver.getInstanceSpecification() >= 16")
        protected static final long doSize8bit(final ClassObject receiver, final long numElements) {
            return postProcessSize(receiver.getBasicInstanceSize() + (numElements + 3) / 4);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 248)
    protected abstract static class PrimArrayBecomeOneWayNoCopyHashNode extends AbstractArrayBecomeOneWayPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return performPointersBecomeOneWay(image, fromArray, toArray, false);
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
    @SqueakPrimitive(indices = 249)
    protected abstract static class PrimArrayBecomeOneWayCopyHashArgNode extends AbstractArrayBecomeOneWayPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization
        protected static final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash,
                        @Cached final ConditionProfile copyHashProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return performPointersBecomeOneWay(image, fromArray, toArray, copyHashProfile.profile(copyHash));
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

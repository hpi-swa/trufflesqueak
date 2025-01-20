/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Collection;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedIntValueProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractSingletonPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils;

public final class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    protected abstract static class AbstractArrayBecomeOneWayPrimitiveNode extends AbstractPrimitiveNode {

        protected final ArrayObject performPointersBecomeOneWay(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash) {
            final SqueakImageContext image = getContext();
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
                if (from instanceof final AbstractSqueakObjectWithClassAndHash f && to instanceof final AbstractSqueakObjectWithClassAndHash t) {
                    t.setSqueakHash(MiscUtils.toIntExact(f.getOrCreateSqueakHash()));
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
                        } else if (argument instanceof final AbstractSqueakObjectWithClassAndHash o) {
                            o.pointersBecomeOneWay(fromPointers, toPointers);
                        }
                    }
                }

                final ContextObject context = FrameAccess.getContext(current);
                if (context != null) {
                    for (int j = 0; j < fromPointersLength; j++) {
                        final Object fromPointer = fromPointers[j];
                        if (context == fromPointer) {
                            final Object toPointer = toPointers[j];
                            FrameAccess.setContext(current, (ContextObject) toPointer);
                        } else {
                            context.pointersBecomeOneWay(fromPointers, toPointers);
                        }
                    }
                }

                /*
                 * Iterate over all stack slots here instead of stackPointer because in rare cases,
                 * the stack is accessed behind the stackPointer.
                 */
                FrameAccess.iterateStackSlots(current, slotIndex -> {
                    if (current.isObject(slotIndex)) {
                        final Object stackObject = current.getObject(slotIndex);
                        for (int j = 0; j < fromPointersLength; j++) {
                            final Object fromPointer = fromPointers[j];
                            if (stackObject == fromPointer) {
                                final Object toPointer = toPointers[j];
                                assert toPointer != null : "Unexpected `null` value";
                                current.setObject(slotIndex, toPointer);
                            } else if (stackObject instanceof final AbstractSqueakObjectWithClassAndHash o) {
                                o.pointersBecomeOneWay(fromPointers, toPointers);
                            }
                        }
                    }
                });
                return null;
            });
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 68)
    protected abstract static class PrimCompiledMethodObjectAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "index0 < receiver.getNumHeaderAndLiterals()")
        protected static final Object literalAt(final CompiledCodeObject receiver, @SuppressWarnings("unused") final long index,
                        @Bind("to0(index)") final long index0) {
            return index0 == 0 ? receiver.getHeader() : receiver.getLiteral(index0 - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 69)
    protected abstract static class PrimCompiledMethodObjectAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "index0 < receiver.getNumHeaderAndLiterals()")
        protected static final Object literalAtPut(final CompiledCodeObject receiver, @SuppressWarnings("unused") final long index, final Object value,
                        @Bind("to0(index)") final long index0) {
            receiver.setLiteral(index0, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 70)
    public abstract static class PrimNewNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        public static final int NEW_CACHE_SIZE = 6;

        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected static final AbstractSqueakObjectWithClassAndHash newDirect(@SuppressWarnings("unused") final ClassObject receiver,
                        @Bind final Node node,
                        @Cached("receiver.withEnsuredBehaviorHash()") final ClassObject cachedReceiver,
                        @Exclusive @Cached final SqueakObjectNewNode newNode) {
            try {
                return newNode.execute(node, getContext(node), cachedReceiver);
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "newDirect")
        protected static final AbstractSqueakObjectWithClassAndHash newIndirect(final ClassObject receiver,
                        @Bind final Node node,
                        @Exclusive @Cached final SqueakObjectNewNode newNode) {
            receiver.ensureBehaviorHash();
            try {
                return newNode.execute(node, getContext(node), receiver);
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 71)
    protected abstract static class PrimNewWithArgNode extends AbstractPrimitiveNode implements Primitive1 {
        public static final int NEW_CACHE_SIZE = 6;

        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver", "isInstantiable(cachedReceiver, size)"}, assumptions = {"cachedReceiver.getClassFormatStable()"})
        protected static final AbstractSqueakObjectWithClassAndHash newWithArgDirect(@SuppressWarnings("unused") final ClassObject receiver, final long size,
                        @Bind final Node node,
                        @Cached(value = "createIdentityProfile()", inline = true) final InlinedIntValueProfile sizeProfile,
                        @Cached("receiver.withEnsuredBehaviorHash()") final ClassObject cachedReceiver,
                        @Exclusive @Cached final SqueakObjectNewNode newNode) {
            try {
                return newNode.execute(node, getContext(node), cachedReceiver, sizeProfile.profile(node, (int) size));
            } catch (final OutOfMemoryError e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.INSUFFICIENT_OBJECT_MEMORY;
            }
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "newWithArgDirect", guards = "isInstantiable(receiver, size)")
        protected static final AbstractSqueakObjectWithClassAndHash newWithArg(final ClassObject receiver, final long size,
                        @Bind final Node node,
                        @Exclusive @Cached final SqueakObjectNewNode newNode) {
            receiver.ensureBehaviorHash();
            try {
                return newNode.execute(node, getContext(node), receiver, (int) size);
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
    protected abstract static class PrimArrayBecomeOneWayNode extends AbstractArrayBecomeOneWayPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray) {
            return performPointersBecomeOneWay(fromArray, toArray, true);
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
    @SqueakPrimitive(indices = 73)
    protected abstract static class PrimInstVarAt2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, receiver))", limit = "1")
        protected static final Object doAt(final Object receiver, final long index,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, receiver, index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 73)
    /* Context>>#object:instVarAt: */
    protected abstract static class PrimInstVarAt3Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, target))", limit = "1")
        protected static final Object doAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, target, index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 74)
    protected abstract static class PrimInstVarAtPut3Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, receiver))", limit = "1")
        protected static final Object doAtPut(final Object receiver, final long index, final Object value,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, receiver, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 74)
    /* Context>>#object:instVarAt:put: */
    protected abstract static class PrimInstVarAtPut4Node extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, target))", limit = "1")
        protected static final Object doAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 75)
    protected abstract static class PrimIdentityHashNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
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
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile needsHashProfile) {
            return object.getOrCreateSqueakHash(needsHashProfile, node);
        }
    }

    /* primitiveNextInstance (#78) deprecated in favor of primitiveAllInstances (#177). */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 79)
    protected abstract static class PrimNewMethodNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isCompiledMethodClassType()")
        protected final CompiledCodeObject newMethod(final ClassObject receiver, final long bytecodeCount, final long header) {
            final SqueakImageContext image = getContext();
            assert receiver.getBasicInstanceSize() == 0;
            assert receiver.getSuperclassOrNull() == image.compiledMethodClass.getSuperclassOrNull() : "Receiver must be subclass of CompiledCode";
            final CompiledCodeObject newMethod = CompiledCodeObject.newOfSize(image, (int) bytecodeCount, receiver);
            newMethod.setHeader(header);
            return newMethod;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 128)
    protected abstract static class PrimArrayBecomeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"sizeNode.execute(node, receiver) == sizeNode.execute(node, other)"})
        protected final ArrayObject doBecome(final ArrayObject receiver, final ArrayObject other,
                        @Bind final Node node,
                        @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final SqueakObjectBecomeNode becomeNode,
                        @Cached final ArrayObjectReadNode readNode,
                        @Cached final InlinedBranchProfile failProfile) {
            final int receiverSize = sizeNode.execute(node, receiver);
            int numBecomes = 0;
            final Object[] lefts = new Object[receiverSize];
            final Object[] rights = new Object[receiverSize];
            for (int i = 0; i < receiverSize; i++) {
                final Object left = readNode.execute(node, receiver, i);
                final Object right = readNode.execute(node, other, i);
                if (becomeNode.execute(node, left, right)) {
                    lefts[numBecomes] = left;
                    rights[numBecomes] = right;
                    numBecomes++;
                } else {
                    failProfile.enter(node);
                    for (int j = 0; j < numBecomes; j++) {
                        becomeNode.execute(node, lefts[j], rights[j]);
                    }
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
            }
            getContext().flushMethodCacheAfterBecome();
            return receiver;
        }

        @Specialization(guards = {"sizeNode.execute(node, receiver) != sizeNode.execute(node, other)"})
        @SuppressWarnings("unused")
        protected static final ArrayObject doBadSize(final ArrayObject receiver, final ArrayObject other,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode) {
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

    @DenyReplace
    @SqueakPrimitive(indices = 129)
    public static final class PrimSpecialObjectsArrayNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return getContext().specialObjectsArray;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 138)
    public static final class PrimSomeObjectNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 139)
    protected abstract static class PrimNextObjectNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final AbstractSqueakObject doNext(final AbstractSqueakObjectWithClassAndHash receiver) {
            return getNext(receiver, ObjectGraphUtils.allInstances(getContext()));
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
    protected abstract static class PrimCharacterValue1Node extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"0 <= receiver", "receiver <= MAX_VALUE"}, rewriteOn = RespecializeException.class)
        protected static final char doLongExact(final long receiver) throws RespecializeException {
            return CharacterObject.valueExactOf(receiver);
        }

        @Specialization(guards = {"0 <= receiver", "receiver <= MAX_VALUE"}, replaces = "doLongExact")
        protected static final Object doLong(final long receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isImmediateProfile) {
            return CharacterObject.valueOf(receiver, isImmediateProfile, node);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(Integer.class)
    @SqueakPrimitive(indices = 170)
    protected abstract static class PrimCharacterValue2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"0 <= target", "target <= MAX_VALUE"}, rewriteOn = RespecializeException.class)
        protected static final char doClassLongExact(@SuppressWarnings("unused") final Object receiver, final long target) throws RespecializeException {
            return CharacterObject.valueExactOf(target);
        }

        @Specialization(guards = {"0 <= target", "target <= MAX_VALUE"}, replaces = "doClassLongExact")
        protected static final Object doClassLong(@SuppressWarnings("unused") final Object receiver, final long target,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isImmediateProfile) {
            return CharacterObject.valueOf(target, isImmediateProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 171)
    protected abstract static class PrimImmediateAsIntegerNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

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
    @SqueakPrimitive(indices = 173)
    protected abstract static class PrimSlotAt2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, receiver))", limit = "1")
        protected static final Object doSlotAt(final Object receiver, final long index,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, receiver, index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 173)
    protected abstract static class PrimSlotAt3Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, target))", limit = "1")
        protected static final Object doSlotAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAt0Node at0Node) {
            return at0Node.execute(node, target, index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 174)
    protected abstract static class PrimSlotAtPut3Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, receiver))", limit = "1")
        protected static final Object doSlotAtPut(final Object receiver, final long index, final Object value,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, receiver, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 174)
    protected abstract static class PrimSlotAtPut4Node extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "inBounds1(index, sizeNode.execute(node, target))", limit = "1")
        protected static final Object doSlotAtPut(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index, final Object value,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atPut0Node) {
            atPut0Node.execute(node, target, index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 175)
    protected abstract static class PrimBehaviorHashNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        /* Cache one class hash (helps String>>#hash). */
        @SuppressWarnings("unused")
        @Specialization(guards = "receiver == cachedClass", limit = "1")
        protected static final long doClassCached(final ClassObject receiver,
                        @Cached("receiver") final ClassObject cachedClass,
                        @Cached("doClassUncached(receiver)") final long cachedHash) {
            return cachedHash;
        }

        @Specialization(replaces = "doClassCached")
        protected static final long doClassUncached(final ClassObject receiver) {
            receiver.ensureBehaviorHash();
            return receiver.getSqueakHash();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 178)
    protected abstract static class PrimAllObjectsNode extends AbstractPrimitiveNode implements Primitive0 {

        @Specialization
        protected final ArrayObject doAll(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
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
    protected abstract static class PrimSizeInBytesOfInstance1Node extends AbstractPrimSizeInBytesOfInstance1Node implements Primitive0WithFallback {
        @Specialization
        protected static final long doBasicSize(final ClassObject receiver) {
            return postProcessSize(receiver.getBasicInstanceSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 181)
    protected abstract static class PrimSizeInBytesOfInstance2Node extends AbstractPrimSizeInBytesOfInstance1Node implements Primitive1WithFallback {
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
    protected abstract static class PrimArrayBecomeOneWayNoCopyHashNode extends AbstractArrayBecomeOneWayPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray) {
            return performPointersBecomeOneWay(fromArray, toArray, false);
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
    protected abstract static class PrimArrayBecomeOneWayCopyHashArgNode extends AbstractArrayBecomeOneWayPrimitiveNode implements Primitive2WithFallback {

        @Specialization
        protected final ArrayObject doForward(final ArrayObject fromArray, final ArrayObject toArray, final boolean copyHash,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile copyHashProfile) {
            return performPointersBecomeOneWay(fromArray, toArray, copyHashProfile.profile(node, copyHash));
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

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    @Override
    public List<? extends AbstractSingletonPrimitiveNode> getSingletonPrimitives() {
        return List.of(new PrimSpecialObjectsArrayNode(), new PrimSomeObjectNode());
    }
}

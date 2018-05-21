package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.GetAllInstancesNode;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectPointersBecomeOneWayNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    private abstract static class AbstractInstancesPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected GetAllInstancesNode getAllInstancesNode;

        protected AbstractInstancesPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            getAllInstancesNode = GetAllInstancesNode.create(method);
        }
    }

    protected abstract static class AbstractArrayBecomeOneWayPrimitiveNode extends AbstractInstancesPrimitiveNode {
        @Child private SqueakObjectPointersBecomeOneWayNode pointersBecomeNode = SqueakObjectPointersBecomeOneWayNode.create();
        @Child private FrameStackReadNode stackReadNode = FrameStackReadNode.create();
        @Child private FrameStackWriteNode stackWriteNode = FrameStackWriteNode.create();
        @Child private FrameSlotReadNode stackPointerReadNode;

        protected AbstractArrayBecomeOneWayPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            stackPointerReadNode = FrameSlotReadNode.create(method.stackPointerSlot);
        }

        protected static final boolean isPointers(final Object obj) {
            return obj instanceof PointersObject;
        }

        protected final AbstractSqueakObject performPointersBecomeOneWay(final VirtualFrame frame, final PointersObject fromArray, final PointersObject toArray, final boolean copyHash) {
            if (fromArray.size() != toArray.size()) {
                throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
            }
            final Object[] fromPointers = fromArray.getPointers();
            final Object[] toPointers = toArray.getPointers();
            migrateInstances(copyHash, fromPointers, toPointers, getAllInstancesNode.execute(frame));
            patchTruffleFrames(fromPointers, toPointers);
            return fromArray;
        }

        @TruffleBoundary
        private void migrateInstances(final boolean copyHash, final Object[] fromPointers, final Object[] toPointers, final List<AbstractSqueakObject> instances) {
            for (Iterator<AbstractSqueakObject> iterator = instances.iterator(); iterator.hasNext();) {
                final AbstractSqueakObject instance = iterator.next();
                if (instance != null && instance.getSqClass() != null) {
                    pointersBecomeNode.execute(instance, fromPointers, toPointers, copyHash);
                }
            }
        }

        @TruffleBoundary
        private void patchTruffleFrames(final Object[] fromPointers, final Object[] toPointers) {
            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
                @Override
                public Frame visitFrame(final FrameInstance frameInstance) {
                    final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                    final Object stackPointer = stackPointerReadNode.executeRead(current);
                    if (stackPointer == null || current.getFrameDescriptor().getSize() <= FrameAccess.RECEIVER) {
                        return null;
                    }
                    final CompiledCodeObject codeObject = FrameAccess.getMethod(current);
                    for (int i = 0; i < codeObject.frameSize(); i++) {
                        final Object stackObject = stackReadNode.execute(current, i);
                        if (stackObject == null) {
                            return null; // this slot and all following have not been used
                        }
                        for (int j = 0; j < fromPointers.length; j++) {
                            final Object fromPointer = fromPointers[j];
                            if (stackObject == fromPointer) {
                                final Object toPointer = toPointers[j];
                                stackWriteNode.execute(current, i, toPointer);
                                if (fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                                    ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 18)
    protected abstract static class PrimMakePointNode extends AbstractPrimitiveNode {
        protected PrimMakePointNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doObject(final Object xPos, final Object yPos) {
            return code.image.newPoint(xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 68)
    protected abstract static class PrimCompiledMethodObjectAtNode extends AbstractPrimitiveNode {
        protected PrimCompiledMethodObjectAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object literalAt(final CompiledCodeObject receiver, final long index) {
            // Use getLiterals() instead of getLiteral(i), the latter skips the header.
            return receiver.getLiterals()[(int) (index) - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 69)
    protected abstract static class PrimCompiledMethodObjectAtPutNode extends AbstractPrimitiveNode {
        protected PrimCompiledMethodObjectAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object setLiteral(final CompiledCodeObject code, final long index, final Object value) {
            code.setLiteral(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 70)
    protected abstract static class PrimNewNode extends AbstractPrimitiveNode {
        @CompilationFinal protected static final int NEW_CACHE_SIZE = 3;

        protected PrimNewNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
        protected Object newDirect(final ClassObject receiver,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") final Assumption classFormatStable) {
            return cachedReceiver.newInstance();
        }

        @Specialization(replaces = "newDirect")
        protected Object newIndirect(final ClassObject receiver) {
            return receiver.newInstance();
        }

        @Specialization
        protected Object doPointers(final PointersObject receiver) {
            return receiver.shallowCopy(); // FIXME: BehaviorTest>>#testChange
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 71)
    protected abstract static class PrimNewArgNode extends AbstractPrimitiveNode {
        @CompilationFinal protected static final int NEW_CACHE_SIZE = 3;

        protected PrimNewArgNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", assumptions = {"classFormatStable"}, guards = {"receiver == cachedReceiver", "isInstantiable(receiver, size)"})
        protected static final Object newWithArgDirect(final ClassObject receiver, final long size,
                        @Cached("receiver") final ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") final Assumption classFormatStable) {
            return cachedReceiver.newInstance(size);
        }

        @Specialization(replaces = "newWithArgDirect", guards = "isInstantiable(receiver, size)")
        protected static final Object newWithArg(final ClassObject receiver, final long size) {
            return receiver.newInstance(size);
        }

        protected static final boolean isInstantiable(final ClassObject receiver, final long size) {
            return size == 0 || (receiver.isVariable() && size >= 0);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final Object doBadArgument(final Object receiver, final Object value) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 72)
    protected abstract static class PrimArrayBecomeOneWayNode extends AbstractArrayBecomeOneWayPrimitiveNode {

        protected PrimArrayBecomeOneWayNode(final CompiledMethodObject method, final int numArguments) {
            // FIXME: this primitive does not correctly perform a one way become yet
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doForward(final VirtualFrame frame, final PointersObject fromArray, final PointersObject toArray) {
            return performPointersBecomeOneWay(frame, fromArray, toArray, true);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPointers(receiver)"})
        protected AbstractSqueakObject arrayBecome(final VirtualFrame frame, final Object receiver, final PointersObject argument) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPointers(argument)"})
        protected AbstractSqueakObject arrayBecome(final VirtualFrame frame, final PointersObject receiver, final Object argument) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 73)
    protected abstract static class PrimInstVarAtNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimInstVarAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doAt(final AbstractSqueakObject receiver, final long index, @SuppressWarnings("unused") final NotProvided value) {
            try {
                return at0Node.execute(receiver, index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final AbstractSqueakObject target, final long index) {
            try {
                return at0Node.execute(target, index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 74)
    protected abstract static class PrimInstVarAtPutNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimInstVarAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doAtPut(final AbstractSqueakObject receiver, final long index, final Object value) {
            try {
                atPut0Node.execute(receiver, index - 1, value);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {75, 171, 175})
    protected abstract static class PrimIdentityHashNode extends AbstractPrimitiveNode {

        protected PrimIdentityHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long doBoolean(final boolean obj) {
            if (obj == code.image.sqFalse) {
                return 2L;
            } else {
                return 3L;
            }
        }

        @Specialization
        protected static final long doChar(final char obj) {
            return obj;
        }

        @Specialization
        protected static final long doLong(final long obj) {
            return obj;
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            return (long) receiver;
        }

        @Specialization
        protected static final long doBaseSqueakObject(final AbstractSqueakObject obj) {
            return obj.squeakHash();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 76)
    protected abstract static class PrimStoreStackPointerNode extends AbstractPrimitiveNode {
        protected PrimStoreStackPointerNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject store(final ContextObject receiver, final long value) {
            receiver.atput0(CONTEXT.STACKPOINTER, value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 78)
    protected abstract static class PrimNextInstanceNode extends AbstractPrimitiveNode {

        protected PrimNextInstanceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected boolean hasNoInstances(final AbstractSqueakObject sqObject) {
            return code.image.objects.getClassesWithNoInstances().contains(sqObject.getSqClass());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(sqObject)")
        protected AbstractSqueakObject noInstances(final AbstractSqueakObject sqObject) {
            return code.image.nil;
        }

        @Specialization(guards = "!hasNoInstances(sqObject)")
        protected AbstractSqueakObject someInstance(final AbstractSqueakObject sqObject) {
            final List<AbstractSqueakObject> instances = code.image.objects.allInstances(sqObject.getSqClass());
            int index;
            try {
                index = instances.indexOf(sqObject);
            } catch (NullPointerException e) {
                index = -1;
            }
            try {
                return instances.get(index + 1);
            } catch (IndexOutOfBoundsException e) {
                return code.image.nil;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 79)
    protected abstract static class PrimNewMethodNode extends AbstractPrimitiveNode {

        protected PrimNewMethodNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected boolean isCompiledMethodClass(final ClassObject receiver) {
            return receiver.isSpecialClassAt(SPECIAL_OBJECT_INDEX.ClassCompiledMethod);
        }

        @Specialization(guards = "isCompiledMethodClass(receiver)")
        protected AbstractSqueakObject newMethod(final ClassObject receiver, final long bytecodeCount, final long header) {
            final CompiledMethodObject newMethod = (CompiledMethodObject) receiver.newInstance(bytecodeCount);
            newMethod.setHeader(header);
            return newMethod;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 128)
    protected abstract static class PrimBecomeNode extends AbstractPrimitiveNode {

        protected PrimBecomeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doBecome(final PointersObject receiver, final PointersObject other) {
            final int receiverSize = receiver.size();
            if (receiverSize != other.size()) {
                throw new PrimitiveFailed();
            }
            int numBecomes = 0;
            final AbstractSqueakObject[] lefts = new AbstractSqueakObject[receiverSize];
            final AbstractSqueakObject[] rights = new AbstractSqueakObject[receiverSize];
            for (int i = 0; i < receiverSize; i++) {
                final AbstractSqueakObject left = (AbstractSqueakObject) receiver.at0(i);
                final AbstractSqueakObject right = (AbstractSqueakObject) other.at0(i);
                if (left.become(right)) {
                    lefts[numBecomes] = left;
                    rights[numBecomes] = right;
                    numBecomes++;
                } else {
                    for (int j = 0; j < numBecomes; j++) {
                        lefts[j].become(rights[j]);
                    }
                    throw new PrimitiveFailed();
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 129)
    protected abstract static class PrimSpecialObjectsArrayNode extends AbstractPrimitiveNode {

        protected PrimSpecialObjectsArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject get(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 138)
    protected abstract static class PrimSomeObjectNode extends AbstractInstancesPrimitiveNode {

        protected PrimSomeObjectNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doSome(final VirtualFrame frame, @SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return getAllInstancesNode.execute(frame).get(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 139)
    protected abstract static class PrimNextObjectNode extends AbstractInstancesPrimitiveNode {

        protected PrimNextObjectNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doNext(final VirtualFrame frame, final AbstractSqueakObject receiver) {
            return getNext(receiver, getAllInstancesNode.execute(frame));
        }

        @TruffleBoundary
        private static AbstractSqueakObject getNext(final AbstractSqueakObject receiver, final List<AbstractSqueakObject> allInstances) {
            final int index = allInstances.indexOf(receiver);
            if (0 <= index && index + 1 < allInstances.size()) {
                return allInstances.get(index + 1);
            } else {
                return allInstances.get(0);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 170)
    protected abstract static class PrimCharacterValueNode extends AbstractPrimitiveNode {

        protected PrimCharacterValueNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected char doLong(final long receiver, @SuppressWarnings("unused") final NotProvided target) {
            return (char) Math.toIntExact(receiver);
        }

        @Specialization
        protected char doLargeInteger(final LargeIntegerObject receiver, @SuppressWarnings("unused") final NotProvided target) {
            try {
                return (char) Math.toIntExact(receiver.reduceToLong());
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected char doLong(@SuppressWarnings("unused") final Object receiver, final long target) {
            return (char) Math.toIntExact(target);
        }

        @Specialization
        protected char doLargeInteger(@SuppressWarnings("unused") final Object receiver, final LargeIntegerObject target) {
            try {
                return (char) Math.toIntExact(target.reduceToLong());
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 173)
    protected abstract static class PrimSlotAtNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimSlotAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doSlotAt(final AbstractSqueakObject receiver, final long index) {
            try {
                return at0Node.execute(receiver, index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 174)
    protected abstract static class PrimSlotAtPutNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimSlotAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doSlotAtPut(final AbstractSqueakObject receiver, final long index, final Object value) {
            try {
                atPut0Node.execute(receiver, index - 1, value);
                return value;
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 178)
    protected abstract static class PrimAllObjectsNode extends AbstractInstancesPrimitiveNode {

        protected PrimAllObjectsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject doAll(final VirtualFrame frame, @SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.newList(ArrayUtils.toArray(getAllInstancesNode.execute(frame)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 181)
    protected abstract static class PrimSizeInBytesOfInstanceNode extends AbstractPrimitiveNode {

        protected PrimSizeInBytesOfInstanceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doSize(final ClassObject receiver, @SuppressWarnings("unused") final NotProvided value) {
            return receiver.classByteSizeOfInstance(0);
        }

        @Specialization
        protected long doSize(final ClassObject receiver, final long size) {
            return receiver.classByteSizeOfInstance(size);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 249)
    protected abstract static class PrimArrayBecomeOneWayCopyHashNode extends AbstractArrayBecomeOneWayPrimitiveNode {
        @Child private FrameStackReadNode stackReadNode = FrameStackReadNode.create();
        @Child private FrameStackWriteNode stackWriteNode = FrameStackWriteNode.create();

        protected PrimArrayBecomeOneWayCopyHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doForward(final VirtualFrame frame, final PointersObject fromArray, final PointersObject toArray, final boolean copyHash) {
            return performPointersBecomeOneWay(frame, fromArray, toArray, copyHash);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPointers(receiver)"})
        protected AbstractSqueakObject arrayBecome(final VirtualFrame frame, final Object receiver, final PointersObject argument, final boolean copyHash) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPointers(argument)"})
        protected AbstractSqueakObject arrayBecome(final VirtualFrame frame, final PointersObject receiver, final Object argument, final boolean copyHash) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }
}

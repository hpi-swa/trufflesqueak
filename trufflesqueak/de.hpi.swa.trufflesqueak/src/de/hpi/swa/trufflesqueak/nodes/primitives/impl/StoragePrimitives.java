package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.nodes.GetAllInstancesNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    private static abstract class AbstractInstancesPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected GetAllInstancesNode getAllInstancesNode;

        protected AbstractInstancesPrimitiveNode(CompiledMethodObject method) {
            super(method);
            getAllInstancesNode = GetAllInstancesNode.create(method);
        }
    }

    private static abstract class AbstractArrayBecomeOneWayPrimitiveNode extends AbstractInstancesPrimitiveNode {
        @Child private FrameStackReadNode stackReadNode = FrameStackReadNode.create();
        @Child private FrameStackWriteNode stackWriteNode = FrameStackWriteNode.create();
        @Child private FrameSlotReadNode stackPointerReadNode;

        protected AbstractArrayBecomeOneWayPrimitiveNode(CompiledMethodObject method) {
            super(method);
            stackPointerReadNode = FrameSlotReadNode.create(method.stackPointerSlot);
        }

        protected final BaseSqueakObject performPointersBecomeOneWay(VirtualFrame frame, ListObject fromArray, ListObject toArray, boolean copyHash) {
            if (fromArray.size() != toArray.size()) {
                throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
            }
            Object[] fromPointers = fromArray.getPointers();
            Object[] toPointers = toArray.getPointers();
            List<BaseSqueakObject> instances = getAllInstancesNode.execute(frame);
            for (Iterator<BaseSqueakObject> iterator = instances.iterator(); iterator.hasNext();) {
                BaseSqueakObject instance = iterator.next();
                if (instance != null && instance.getSqClass() != null) {
                    instance.pointersBecomeOneWay(fromPointers, toPointers, copyHash);
                }
            }
            patchTruffleFrames(fromPointers, toPointers);
            return fromArray;
        }

        @TruffleBoundary
        private final void patchTruffleFrames(Object[] fromPointers, Object[] toPointers) {
            Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Frame>() {
                @Override
                public Frame visitFrame(FrameInstance frameInstance) {
                    Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
                    Object stackPointer = stackPointerReadNode.executeRead(current);
                    if (stackPointer == null || current.getFrameDescriptor().getSize() <= FrameAccess.RCVR_AND_ARGS_START) {
                        return null;
                    }
                    CompiledCodeObject codeObject = FrameAccess.getMethod(current);
                    for (int i = 0; i < codeObject.getNumArgsAndCopiedValues() + codeObject.getNumTemps() + (long) stackPointer; i++) {
                        Object stackObject = stackReadNode.execute(current, i);
                        for (int j = 0; j < fromPointers.length; j++) {
                            Object fromPointer = fromPointers[j];
                            if (stackObject == fromPointer) {
                                Object toPointer = toPointers[j];
                                stackWriteNode.execute(current, i, toPointer);
                                if (fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                                    ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
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
    @SqueakPrimitive(index = 18, numArguments = 2)
    protected static abstract class PrimMakePointNode extends AbstractPrimitiveNode {
        protected PrimMakePointNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doLong(final long xPos, final long yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLongLargeInteger(final long xPos, final LargeIntegerObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLongDouble(final long xPos, final double yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLongFloat(final long xPos, final FloatObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLargeIntegerLong(final LargeIntegerObject xPos, final long yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLargeInteger(final LargeIntegerObject xPos, final LargeIntegerObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLargeIntegerDouble(final LargeIntegerObject xPos, final double yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doLargeIntegerFloat(final LargeIntegerObject xPos, final FloatObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doDoubleLong(final double xPos, final long yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doDoubleLargeInteger(final double xPos, final LargeIntegerObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doDouble(final double xPos, final double yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doDoubleFloat(final double xPos, final FloatObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doFloatLong(final FloatObject xPos, final long yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doFloatDouble(final FloatObject xPos, final double yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doFloatLargeInteger(final FloatObject xPos, final LargeIntegerObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }

        @Specialization
        protected final Object doFloat(final FloatObject xPos, final FloatObject yPos) {
            return code.image.newPoint(xPos, yPos);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 68, numArguments = 2)
    protected static abstract class PrimCompiledMethodObjectAtNode extends AbstractPrimitiveNode {
        protected PrimCompiledMethodObjectAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object literalAt(CompiledCodeObject receiver, long index) {
            // Use getLiterals() instead of getLiteral(i), the latter skips the header.
            return receiver.getLiterals()[(int) (index) - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 69, numArguments = 3)
    protected static abstract class PrimCompiledMethodObjectAtPutNode extends AbstractPrimitiveNode {
        protected PrimCompiledMethodObjectAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object setLiteral(CompiledCodeObject code, long index, Object value) {
            code.setLiteral(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 70)
    protected static abstract class PrimNewNode extends AbstractPrimitiveNode {
        final static int NEW_CACHE_SIZE = 3;

        protected PrimNewNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
        protected Object newDirect(ClassObject receiver,
                        @Cached("receiver") ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
            return cachedReceiver.newInstance();
        }

        @Specialization(replaces = "newDirect")
        protected Object newIndirect(ClassObject receiver) {
            return receiver.newInstance();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 71, numArguments = 2)
    protected static abstract class PrimNewArgNode extends AbstractPrimitiveNode {
        final static int NEW_CACHE_SIZE = 3;

        protected PrimNewArgNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
        protected Object newWithArgDirect(ClassObject receiver, long size,
                        @Cached("receiver") ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
            if (!cachedReceiver.isVariable() && size != 0) {
                throw new PrimitiveFailed();
            }
            if (size < 0) {
                throw new PrimitiveFailed();
            }
            return cachedReceiver.newInstance(size);
        }

        @Specialization(replaces = "newWithArgDirect")
        protected Object newWithArg(ClassObject receiver, long size) {
            if (!receiver.isVariable() && size != 0) {
                throw new PrimitiveFailed();
            }
            if (size < 0) {
                throw new PrimitiveFailed();
            }
            return receiver.newInstance(size);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 72, numArguments = 2)
    protected static abstract class PrimArrayBecomeOneWayNode extends AbstractArrayBecomeOneWayPrimitiveNode {

        protected PrimArrayBecomeOneWayNode(CompiledMethodObject method) {
            // FIXME: this primitive does not correctly perform a one way become yet
            super(method);
        }

        @Specialization
        protected final BaseSqueakObject doForward(VirtualFrame frame, ListObject fromArray, ListObject toArray) {
            return performPointersBecomeOneWay(frame, fromArray, toArray, true);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(VirtualFrame frame, Object receiver, ListObject argument) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(VirtualFrame frame, ListObject receiver, Object argument) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 73, variableArguments = true)
    protected static abstract class PrimInstVarAtNode extends AbstractPrimitiveNode {
        protected PrimInstVarAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doAt(rcvrAndArgs);
        }

        @Specialization
        protected Object doAt(Object[] rcvrAndArgs) {
            BaseSqueakObject receiver;
            long index;
            switch (rcvrAndArgs.length) {
                case 2:
                    receiver = (BaseSqueakObject) rcvrAndArgs[0];
                    index = (long) rcvrAndArgs[1];
                    break;
                case 3:
                    receiver = (BaseSqueakObject) rcvrAndArgs[1];
                    index = (long) rcvrAndArgs[2];
                    break;
                default:
                    throw new PrimitiveFailed();
            }
            try {
                return receiver.at0(index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 74, numArguments = 3)
    protected static abstract class PrimInstVarAtPutNode extends AbstractPrimitiveNode {
        protected PrimInstVarAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object atput(BaseSqueakObject receiver, long idx, Object value) {
            try {
                receiver.atput0(idx - 1, value);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {75, 171, 175})
    protected static abstract class PrimIdentityHashNode extends AbstractPrimitiveNode {
        protected PrimIdentityHashNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doNil(@SuppressWarnings("unused") final NilObject obj) {
            return 1L;
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
        protected static final long doLargeInteger(final LargeIntegerObject obj) {
            return obj.hashCode();
        }

        @Specialization
        protected static final long doDouble(final double receiver) {
            return (long) receiver;
        }

        @Specialization
        protected static final long doFloat(final FloatObject receiver) {
            return doDouble(receiver.getValue());
        }

        @Specialization
        protected static final long doBaseSqueakObject(final BaseSqueakObject obj) {
            return obj.squeakHash();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 76, numArguments = 2)
    protected static abstract class PrimStoreStackPointerNode extends AbstractPrimitiveNode {
        protected PrimStoreStackPointerNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject store(ContextObject receiver, long value) {
            receiver.atput0(CONTEXT.STACKPOINTER, value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 78)
    protected static abstract class PrimNextInstanceNode extends AbstractPrimitiveNode {

        protected PrimNextInstanceNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean hasNoInstances(BaseSqueakObject sqObject) {
            return code.image.objects.getClassesWithNoInstances().contains(sqObject.getSqClass());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(sqObject)")
        protected BaseSqueakObject noInstances(BaseSqueakObject sqObject) {
            return code.image.nil;
        }

        @Specialization
        protected BaseSqueakObject someInstance(BaseSqueakObject sqObject) {
            List<BaseSqueakObject> instances = code.image.objects.allInstances(sqObject.getSqClass());
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
    @SqueakPrimitive(index = 79, numArguments = 3)
    protected static abstract class PrimNewMethodNode extends AbstractPrimitiveNode {

        protected PrimNewMethodNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean isCompiledMethodClass(ClassObject receiver) {
            return receiver.isSpecialClassAt(SPECIAL_OBJECT_INDEX.ClassCompiledMethod);
        }

        @Specialization(guards = "isCompiledMethodClass(receiver)")
        protected BaseSqueakObject newMethod(ClassObject receiver, long bytecodeCount, long header) {
            CompiledMethodObject newMethod = (CompiledMethodObject) receiver.newInstance(bytecodeCount);
            newMethod.setHeader(header);
            return newMethod;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 128, numArguments = 2)
    protected static abstract class PrimBecomeNode extends AbstractPrimitiveNode {

        protected PrimBecomeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final BaseSqueakObject doBecome(ListObject receiver, ListObject other) {
            int receiverSize = receiver.size();
            if (receiverSize != other.size()) {
                throw new PrimitiveFailed();
            }
            int numBecomes = 0;
            BaseSqueakObject[] lefts = new BaseSqueakObject[receiverSize];
            BaseSqueakObject[] rights = new BaseSqueakObject[receiverSize];
            for (int i = 0; i < receiverSize; i++) {
                BaseSqueakObject left = (BaseSqueakObject) receiver.at0(i);
                BaseSqueakObject right = (BaseSqueakObject) other.at0(i);
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
    protected static abstract class PrimSpecialObjectsArrayNode extends AbstractPrimitiveNode {

        protected PrimSpecialObjectsArrayNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.specialObjectsArray;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 138)
    protected static abstract class PrimSomeObjectNode extends AbstractInstancesPrimitiveNode {

        protected PrimSomeObjectNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doSome(VirtualFrame frame, @SuppressWarnings("unused") BaseSqueakObject receiver) {
            return getAllInstancesNode.execute(frame).get(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 139)
    protected static abstract class PrimNextObjectNode extends AbstractInstancesPrimitiveNode {

        protected PrimNextObjectNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doNext(VirtualFrame frame, @SuppressWarnings("unused") BaseSqueakObject receiver) {
            List<BaseSqueakObject> allInstances = getAllInstancesNode.execute(frame);
            int index = allInstances.indexOf(receiver);
            if (0 <= index && index + 1 < allInstances.size()) {
                return allInstances.get(index + 1);
            } else {
                return allInstances.get(0);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 170, variableArguments = true)
    protected static abstract class PrimCharacterValueNode extends AbstractPrimitiveNode {

        protected PrimCharacterValueNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doCharValue(rcvrAndArgs);
        }

        @Specialization
        protected char doCharValue(Object[] rcvrAndArgs) {
            Object value;
            switch (rcvrAndArgs.length) {
                case 1:
                    value = rcvrAndArgs[0];
                    break;
                case 2:
                    value = rcvrAndArgs[1];
                    break;
                default:
                    throw new PrimitiveFailed();
            }
            long longValue;
            try {
                if (value instanceof Long) {
                    longValue = (long) value;
                } else if (value instanceof LargeIntegerObject) {
                    try {
                        longValue = ((LargeIntegerObject) value).reduceToLong();
                    } catch (ArithmeticException e) {
                        code.image.getError().println("Letting primitive 170 fail: " + e.toString());
                        throw new PrimitiveFailed();
                    }
                } else {
                    throw new PrimitiveFailed();
                }
                return (char) Math.toIntExact(longValue);
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 173, numArguments = 2)
    protected static abstract class PrimSlotAtNode extends AbstractPrimitiveNode {

        protected PrimSlotAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BaseSqueakObject receiver, long index) {
            try {
                return receiver.at0(index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 174, numArguments = 3)
    protected static abstract class PrimSlotAtPutNode extends AbstractPrimitiveNode {

        protected PrimSlotAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object value(BaseSqueakObject receiver, long index, Object value) {
            try {
                receiver.atput0(index - 1, value);
                return value;
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 178)
    protected static abstract class PrimAllObjectsNode extends AbstractInstancesPrimitiveNode {

        protected PrimAllObjectsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doAll(VirtualFrame frame, @SuppressWarnings("unused") BaseSqueakObject receiver) {
            List<BaseSqueakObject> allInstances = getAllInstancesNode.execute(frame);
            return code.image.newList(allInstances.toArray());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 181, variableArguments = true)
    protected static abstract class PrimSizeInBytesOfInstanceNode extends AbstractPrimitiveNode {

        protected PrimSizeInBytesOfInstanceNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doSize(rcvrAndArgs);
        }

        @Specialization
        protected long doSize(Object[] rcvrAndArgs) {
            if (!(rcvrAndArgs[0] instanceof ClassObject)) {
                throw new PrimitiveFailed();
            }
            ClassObject receiver = (ClassObject) rcvrAndArgs[0];
            switch (rcvrAndArgs.length) {
                case 1:
                    return receiver.classByteSizeOfInstance(0);
                case 2:
                    return receiver.classByteSizeOfInstance((long) rcvrAndArgs[1]);
                default:
                    throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 249, numArguments = 3)
    protected static abstract class PrimArrayBecomeOneWayCopyHashNode extends AbstractArrayBecomeOneWayPrimitiveNode {
        @Child private FrameStackReadNode stackReadNode = FrameStackReadNode.create();
        @Child private FrameStackWriteNode stackWriteNode = FrameStackWriteNode.create();
        @Child private FrameSlotReadNode stackPointerReadNode;

        protected PrimArrayBecomeOneWayCopyHashNode(CompiledMethodObject method) {
            super(method);
            stackPointerReadNode = FrameSlotReadNode.create(method.stackPointerSlot);
        }

        @Specialization
        protected final BaseSqueakObject doForward(VirtualFrame frame, ListObject fromArray, ListObject toArray, boolean copyHash) {
            return performPointersBecomeOneWay(frame, fromArray, toArray, copyHash);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(VirtualFrame frame, Object receiver, ListObject argument, boolean copyHash) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_RECEIVER);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(VirtualFrame frame, ListObject receiver, Object argument, boolean copyHash) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }
}

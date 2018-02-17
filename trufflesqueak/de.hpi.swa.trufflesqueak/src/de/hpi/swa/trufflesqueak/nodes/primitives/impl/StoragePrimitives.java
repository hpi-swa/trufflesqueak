package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;

public class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 18, numArguments = 2)
    protected static abstract class PrimMakePointNode extends AbstractPrimitiveNode {
        protected PrimMakePointNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object makePoint(long xPos, long yPos) {
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
            return receiver.getLiterals()[(int) (index - 1)];
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
    protected static abstract class PrimForwardIdentity extends AbstractPrimitiveNode {
        @Child private GetActiveProcessNode getActiveProcessNode;

        protected PrimForwardIdentity(CompiledMethodObject method) {
            // TODO: this primitive does not correctly perform a one way become yet, FIXME!
            super(method);
            getActiveProcessNode = GetActiveProcessNode.create(method);
        }

        @Specialization
        protected BaseSqueakObject arrayBecome(ListObject receiver, ListObject argument) {
            if (receiver.size() != argument.size()) {
                throw new PrimitiveFailed("bad argument");
            }
            List<BaseSqueakObject> instances = getInstancesArray();
            for (Iterator<BaseSqueakObject> iterator = instances.iterator(); iterator.hasNext();) {
                BaseSqueakObject instance = iterator.next();
                if (instance != null && instance.getSqClass() != null) {
                    instance.pointersBecomeOneWay(receiver.getPointers(), argument.getPointers());
                }
            }
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(Object receiver, ListObject argument) {
            throw new PrimitiveFailed("bad receiver");
        }

        @SuppressWarnings("unused")
        @Specialization
        protected BaseSqueakObject arrayBecome(ListObject receiver, Object argument) {
            throw new PrimitiveFailed("bad argument");
        }

        private List<BaseSqueakObject> getInstancesArray() {
            PointersObject activeProcess = getActiveProcessNode.executeGet();
            // TODO: activeProcess.storeSuspendedContext(frame)
            try {
                return code.image.objects.allInstances();
            } finally {
                // TODO: activeProcess.storeSuspendedContext(code.image.nil)
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 73, numArguments = 2)
    protected static abstract class PrimAtNode extends AbstractPrimitiveNode {
        protected PrimAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(VirtualFrame frame) {
            try {
                return executeAt(frame);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeAt(VirtualFrame frame);

        @Specialization
        protected long at(char receiver, long idx) {
            if (idx == 1) {
                return receiver;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected Object at(LargeIntegerObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected long at(double receiver, long idx) {
            long doubleBits = Double.doubleToLongBits(receiver);
            if (idx == 1) {
                return 0xFFFFFFFF & (doubleBits >> 32);
            } else if (idx == 2) {
                return 0xFFFFFFFF & doubleBits;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected long longAt(NativeObject receiver, long idx) {
            return receiver.getNativeAt0(idx - 1);
        }

        @Specialization
        protected Object at(BlockClosureObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(CompiledCodeObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(EmptyObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(AbstractPointersObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(BaseSqueakObject receiver, long idx) {
            return receiver.at0(idx - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 74, numArguments = 3)
    protected static abstract class PrimAtPutNode extends AbstractPrimitiveNode {
        protected PrimAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(VirtualFrame frame) {
            try {
                return executeAtPut(frame);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeAtPut(VirtualFrame frame);

        @Specialization
        protected char atput(LargeIntegerObject receiver, long idx, char value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected long atput(LargeIntegerObject receiver, long idx, long value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected char atput(NativeObject receiver, long idx, char value) {
            receiver.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected long atput(NativeObject receiver, long idx, long value) {
            receiver.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(BlockClosureObject receiver, long idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(ClassObject receiver, long idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(CompiledCodeObject receiver, long idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object atput(EmptyObject receiver, long idx, Object value) {
            throw new PrimitiveFailed();
        }

        @Specialization
        protected Object atput(AbstractPointersObject receiver, long idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(NativeObject receiver, long idx, LargeIntegerObject value) {
            try {
                receiver.atput0(idx - 1, value.reduceToLong());
                return value;
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected Object atput(BaseSqueakObject receiver, long idx, Object value) {
            receiver.atput0(idx - 1, value);
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
        protected long doChar(char obj) {
            return obj;
        }

        @Specialization
        protected long doLong(long obj) {
            return obj;
        }

        @Specialization
        protected long doLargeInteger(LargeIntegerObject obj) {
            return obj.hashCode();
        }

        @Specialization
        protected long doDouble(double receiver) {
            return (long) receiver;
        }

        @Specialization
        protected long doBoolean(boolean obj) {
            if (obj == code.image.sqTrue) {
                return 3L;
            } else {
                return 2L;
            }
        }

        @Specialization
        protected long doBaseSqueakObject(BaseSqueakObject obj) {
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
        protected BaseSqueakObject doBecome(ListObject receiver, ListObject other) {
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
    @SqueakPrimitive(index = 170, variableArguments = true)
    protected static abstract class PrimCharacterValueNode extends AbstractPrimitiveNode {

        protected PrimCharacterValueNode(CompiledMethodObject method) {
            super(method);
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
                    longValue = ((LargeIntegerObject) value).reduceToLong();
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
}

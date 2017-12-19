package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class StoragePrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return StoragePrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 68, numArguments = 2)
    public static abstract class PrimObjectAtNode extends AbstractPrimitiveNode {
        public PrimObjectAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object literalAt(CompiledCodeObject receiver, int idx) {
            return receiver.getLiteral(idx - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 69, numArguments = 3)
    public static abstract class PrimObjectAtPutNode extends AbstractPrimitiveNode {
        public PrimObjectAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object setLiteral(CompiledCodeObject cc, int idx, Object value) {
            cc.setLiteral(idx, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 70)
    public static abstract class PrimNewNode extends AbstractPrimitiveNode {
        final static int NEW_CACHE_SIZE = 3;

        public PrimNewNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
        BaseSqueakObject newDirect(ClassObject receiver,
                        @Cached("receiver") ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
            return cachedReceiver.newInstance();
        }

        @Specialization(replaces = "newDirect")
        BaseSqueakObject newIndirect(ClassObject receiver) {
            return receiver.newInstance();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 71, numArguments = 2)
    public static abstract class PrimNewArgNode extends AbstractPrimitiveNode {
        final static int NEW_CACHE_SIZE = 3;

        public PrimNewArgNode(CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
        BaseSqueakObject newWithArgDirect(ClassObject receiver, int size,
                        @Cached("receiver") ClassObject cachedReceiver,
                        @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
            if (size == 0 || !cachedReceiver.isVariable())
                throw new PrimitiveFailed();
            return cachedReceiver.newInstance(size);
        }

        @Specialization(replaces = "newWithArgDirect")
        BaseSqueakObject newWithArg(ClassObject receiver, int size) {
            if (size == 0)
                return null;
            if (!receiver.isVariable())
                return null;
            return receiver.newInstance(size);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 73, numArguments = 2)
    public static abstract class PrimAtNode extends AbstractPrimitiveNode {
        public PrimAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int at(char receiver, int idx) {
            if (idx == 1) {
                return receiver;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization
        protected Object at(LargeInteger receiver, int idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected long intAt(BigInteger receiver, int idx) {
            return LargeInteger.byteAt0(receiver, idx - 1);
        }

        @Specialization
        protected long at(double receiver, int idx) {
            long doubleBits = Double.doubleToLongBits(receiver);
            if (idx == 1) {
                return 0xFFFFFFFF & (doubleBits >> 32);
            } else if (idx == 2) {
                return 0xFFFFFFFF & doubleBits;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected int intAt(NativeObject receiver, int idx) throws ArithmeticException {
            return Math.toIntExact(receiver.getNativeAt0(idx - 1));
        }

        @Specialization
        protected long longAt(NativeObject receiver, int idx) {
            return receiver.getNativeAt0(idx - 1);
        }

        @Specialization
        protected Object at(BlockClosure receiver, int idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(CompiledCodeObject receiver, int idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(EmptyObject receiver, int idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(AbstractPointersObject receiver, int idx) {
            return receiver.at0(idx - 1);
        }

        @Specialization
        protected Object at(BaseSqueakObject receiver, int idx) {
            return receiver.at0(idx - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 74, numArguments = 3)
    public static abstract class PrimAtPutNode extends AbstractPrimitiveNode {
        public PrimAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected char atput(LargeInteger receiver, int idx, char value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected int atput(LargeInteger receiver, int idx, int value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected char atput(NativeObject receiver, int idx, char value) {
            receiver.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected int atput(NativeObject receiver, int idx, int value) {
            receiver.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected long atput(NativeObject receiver, int idx, long value) {
            receiver.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(BlockClosure receiver, int idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(ClassObject receiver, int idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(CompiledCodeObject receiver, int idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object atput(EmptyObject receiver, int idx, Object value) {
            throw new PrimitiveFailed();
        }

        @Specialization
        protected Object atput(AbstractPointersObject receiver, int idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }

        @Specialization
        protected Object atput(BaseSqueakObject receiver, int idx, Object value) {
            receiver.atput0(idx - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 75)
    public static abstract class PrimIdentityHashNode extends AbstractPrimitiveNode {
        public PrimIdentityHashNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        int hash(char obj) {
            return obj;
        }

        @Specialization
        int hash(int obj) {
            return obj;
        }

        @Specialization
        int hash(long obj) {
            return (int) obj;
        }

        @Specialization
        int hash(BigInteger obj) {
            return obj.hashCode();
        }

        @Specialization
        int hash(BaseSqueakObject obj) {
            return obj.squeakHash();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 78)
    public static abstract class PrimNextInstanceNode extends AbstractPrimitiveNode {
        private final ObjectGraph objectGraph;

        public PrimNextInstanceNode(CompiledMethodObject method) {
            super(method);
            objectGraph = new ObjectGraph(code);
        }

        protected boolean hasNoInstances(BaseSqueakObject sqObject) {
            return objectGraph.getClassesWithNoInstances().contains(sqObject.getSqClass());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(sqObject)")
        BaseSqueakObject noInstances(BaseSqueakObject sqObject) {
            return code.image.nil;
        }

        @Specialization
        BaseSqueakObject someInstance(BaseSqueakObject sqObject) {
            List<BaseSqueakObject> instances = objectGraph.allInstances(sqObject.getSqClass());
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
    @SqueakPrimitive(index = 170, numArguments = 2)
    public static abstract class PrimCharacterValueNode extends AbstractPrimitiveNode {

        public PrimCharacterValueNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected char value(@SuppressWarnings("unused") BaseSqueakObject ignored, char value) {
            return value;
        }

        @Specialization
        protected char value(@SuppressWarnings("unused") BaseSqueakObject ignored, int value) {
            return (char) value;
        }
    }
}

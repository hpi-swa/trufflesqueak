package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives.PrimAtNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives.PrimAtPutNode;

public class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {60, 210}, numArguments = 2)
    protected static abstract class PrimBasicAtNode extends PrimAtNode {
        protected PrimBasicAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected Object at(AbstractPointersObject receiver, int index) {
            return receiver.at0(index - 1 + receiver.instsize());
        }

        @Specialization
        protected Object at(AbstractPointersObject receiver, long index) {
            return receiver.at0((int) index - 1 + receiver.instsize());
        }

        @Override
        @Specialization
        protected Object at(BaseSqueakObject receiver, int index) {
            return super.at(receiver, index);
        }

        @Specialization
        protected Object at(BaseSqueakObject receiver, long index) {
            return super.at(receiver, (int) index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = {61, 211}, numArguments = 3)
    protected static abstract class PrimBasicAtPutNode extends PrimAtPutNode {
        protected PrimBasicAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        @Specialization
        protected Object atput(AbstractPointersObject receiver, int index, Object value) {
            try {
                receiver.atput0(index - 1 + receiver.instsize(), value);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Override
        @Specialization
        protected Object atput(BaseSqueakObject receiver, int index, Object value) {
            return super.atput(receiver, index, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 62)
    protected static abstract class PrimSizeNode extends AbstractPrimitiveNode {
        protected PrimSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected int size(@SuppressWarnings("unused") char obj) {
            return 0;
        }

        @Specialization
        protected int size(@SuppressWarnings("unused") boolean o) {
            return 0;
        }

        @Specialization
        protected int size(@SuppressWarnings("unused") int o) {
            return 0;
        }

        @Specialization
        protected int size(@SuppressWarnings("unused") long o) {
            return 0;
        }

        @Specialization
        protected int size(String s) {
            return s.getBytes().length;
        }

        @Specialization
        @TruffleBoundary
        protected int size(BigInteger i) {
            return LargeIntegerObject.byteSize(i);
        }

        @Specialization
        protected int size(@SuppressWarnings("unused") double o) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!isNil(obj)", "hasVariableClass(obj)"})
        protected int size(BaseSqueakObject obj) {
            return obj.varsize();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 63, numArguments = 2)
    protected static abstract class PrimStringAtNode extends AbstractPrimitiveNode {
        protected PrimStringAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected char stringAt(NativeObject obj, int idx) {
            byte nativeAt0 = ((Long) obj.getNativeAt0(idx - 1)).byteValue();
            return (char) nativeAt0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 64, numArguments = 3)
    protected static abstract class PrimStringAtPutNode extends AbstractPrimitiveNode {
        protected PrimStringAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected char atput(NativeObject obj, int idx, char value) {
            obj.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected char atput(NativeObject obj, int idx, int value) {
            char charValue = (char) ((Integer) value).byteValue();
            obj.setNativeAt0(idx - 1, charValue);
            return charValue;
        }
    }
}

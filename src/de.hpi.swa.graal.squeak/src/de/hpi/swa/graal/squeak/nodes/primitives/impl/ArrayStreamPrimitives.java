package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;

public class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 60)
    protected abstract static class PrimBasicAtNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimBasicAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeAt(frame);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeAt(VirtualFrame frame);

        @Specialization
        protected static final long doCharacter(final char receiver, final long index) {
            if (index == 1) {
                return receiver;
            } else {
                throw new PrimitiveFailed();
            }
        }

        @Specialization(guards = "!isSmallInteger(receiver)")
        protected final Object doLong(final long receiver, final long index) {
            return asLargeInteger(receiver).getNativeAt0(index - 1);
        }

        @Specialization
        protected static final long doNative(final NativeObject receiver, final long index) {
            return receiver.getNativeAt0(index - 1);
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject receiver, final long index) {
            return receiver.getNativeAt0(index - 1);
        }

        @Specialization
        protected static final long doFloat(final FloatObject receiver, final long index) {
            return receiver.getNativeAt0(index - 1);
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isLargeInteger(receiver)", "!isFloat(receiver)"})
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index) {
            return at0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimBasicAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeAtPut(frame);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeAtPut(VirtualFrame frame);

        @Specialization
        protected char doNativeChar(final NativeObject receiver, final long index, final char value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected long doNativeLong(final NativeObject receiver, final long index, final long value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected Object doNativeLargeInteger(final NativeObject receiver, final long index, final LargeIntegerObject value) {
            try {
                receiver.setNativeAt0(index - 1, value.reduceToLong());
            } catch (IllegalArgumentException | ArithmeticException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected char doLargeIntegerChar(final LargeIntegerObject receiver, final long index, final char value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected long doLargeIntegerLong(final LargeIntegerObject receiver, final long index, final long value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected Object doLargeInteger(final LargeIntegerObject receiver, final long index, final LargeIntegerObject value) {
            try {
                receiver.setNativeAt0(index - 1, value.reduceToLong());
            } catch (IllegalArgumentException | ArithmeticException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected char doFloatChar(final FloatObject receiver, final long index, final char value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected long doFloatLong(final FloatObject receiver, final long index, final long value) {
            try {
                receiver.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected Object doFloatLargeInteger(final FloatObject receiver, final long index, final LargeIntegerObject value) {
            try {
                receiver.atput0(index - 1, value.reduceToLong());
            } catch (IllegalArgumentException | ArithmeticException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doEmptyObject(final EmptyObject receiver, final long idx, final Object value) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "!isSmallInteger(receiver)")
        protected Object doSqueakObject(final long receiver, final long index, final long value) {
            try {
                asLargeInteger(receiver).setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isEmptyObject(receiver)"})
        protected Object doSqueakObject(final AbstractSqueakObject receiver, final long index, final Object value) {
            atPut0Node.execute(receiver, index - 1 + instSizeNode.execute(receiver), value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 62)
    protected abstract static class PrimSizeNode extends AbstractArithmeticPrimitiveNode {
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();

        protected PrimSizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final char obj) {
            return 0;
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final boolean o) {
            return 0;
        }

        @Specialization(guards = "!isSmallInteger(value)")
        protected final long doLong(final long value) {
            return asLargeInteger(value).size();
        }

        @Specialization
        protected static final long doString(final String s) {
            return s.getBytes().length;
        }

        @Specialization
        protected static final long doNative(final NativeObject obj) {
            return obj.size();
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject obj) {
            return obj.size();
        }

        @Specialization
        protected static final long doFloat(@SuppressWarnings("unused") final FloatObject obj) {
            return FloatObject.size();
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final double o) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!isNil(obj)", "hasVariableClass(obj)"})
        protected final long size(final AbstractSqueakObject obj) {
            return sizeNode.execute(obj) - instSizeNode.execute(obj);
        }

        /*
         * Quick return 0 to allow eager primitive calls.
         * "The number of indexable fields of fixed-length objects is 0" (see Object>>basicSize).
         */
        @Fallback
        protected static final long doObject(@SuppressWarnings("unused") final Object receiver) {
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveNode {
        protected PrimStringAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeStringAt(frame);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeStringAt(VirtualFrame frame);

        @Specialization
        protected static final char doNativeObject(final NativeObject obj, final long idx) {
            final int intValue = ((Long) obj.getNativeAt0(idx - 1)).intValue();
            return (char) intValue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveNode {
        protected PrimStringAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeStringAtPut(frame);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeStringAtPut(VirtualFrame frame);

        @Specialization
        protected static final char doNativeObject(final NativeObject obj, final long idx, final char value) {
            obj.setNativeAt0(idx - 1, value);
            return value;
        }

        @Specialization
        protected static final char doNativeObject(final NativeObject obj, final long idx, final long value) {
            assert value >= 0;
            obj.setNativeAt0(idx - 1, value);
            return (char) ((Long) value).intValue();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 143)
    protected abstract static class PrimShortAtNode extends AbstractPrimitiveNode {
        protected PrimShortAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doNativeObject(final NativeObject receiver, final long index) {
            try {
                return receiver.shortAt0(index);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 144)
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode {
        protected PrimShortAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doNativeObject(final NativeObject receiver, final long index, final long value) {
            if (!(-0x8000 <= value && value <= 0x8000)) {
                throw new PrimitiveFailed();
            }
            receiver.shortAtPut0(index, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode {
        protected PrimContextAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doContextObject(final ContextObject receiver, final long index) {
            try {
                return receiver.atTemp(index - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode {
        protected PrimContextAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value) {
            try {
                receiver.atTempPut(index - 1, value);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }
    }

}

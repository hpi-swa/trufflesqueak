package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeAcceptsValueNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveWithSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives.AbstractArithmeticPrimitiveNode;

public final class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractBasicAtOrAtPutNode extends AbstractPrimitiveWithSizeNode {
        @Child protected SqueakObjectInstSizeNode instSizeNode;

        public AbstractBasicAtOrAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected final boolean inBoundsOfSqueakObject(final long index, final AbstractSqueakObject target) {
            return SqueakGuards.inBounds1(index + getInstSizeNode().execute(target), sizeNode.execute(target));
        }

        protected final SqueakObjectInstSizeNode getInstSizeNode() {
            if (instSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instSizeNode = insert(SqueakObjectInstSizeNode.create());
            }
            return instSizeNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 60)
    protected abstract static class PrimBasicAtNode extends AbstractBasicAtOrAtPutNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimBasicAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        public abstract Object executeAt(VirtualFrame frame);

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doCharacter(final char receiver, final long index, final NotProvided notProvided) {
            return receiver;
        }

        @Specialization(guards = "!isSmallInteger(receiver)")
        protected final long doLong(final long receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            try {
                return asLargeInteger(receiver).getNativeAt0(index - 1);
            } catch (IndexOutOfBoundsException e) {
                return 0L; // inline fallback code
            }
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected final Object doNative(final NativeObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return at0Node.execute(receiver, index - 1);
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected static final long doLargeInteger(final LargeIntegerObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return receiver.getNativeAt0(index - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doFloatHigh(final FloatObject receiver, final long index, final NotProvided notProvided) {
            return receiver.getHigh();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected static final long doFloatLow(final FloatObject receiver, final long index, final NotProvided notProvided) {
            return receiver.getLow();
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected final Object doArray(final ArrayObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getAt0Node().execute(receiver, index - 1);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)",
                        "!isNativeObject(receiver)", "!isLargeIntegerObject(receiver)", "!isFloatObject(receiver)", "!isArrayObject(receiver)"})
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getAt0Node().execute(receiver, index - 1 + instSizeNode.execute(receiver));
        }

        /*
         * Context>>#object:basicAt:
         */

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doCharacter(final AbstractSqueakObject receiver, final char target, final long index) {
            return target;
        }

        @Specialization(guards = "!isSmallInteger(target)")
        protected final Object doLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long target, final long index) {
            return asLargeInteger(target).getNativeAt0(index - 1);
        }

        @Specialization(guards = "inBounds(index, target)")
        protected final Object doNative(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index) {
            return getAt0Node().execute(target, index - 1);
        }

        @Specialization(guards = "inBounds(index, target)")
        protected static final long doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index) {
            return target.getNativeAt0(index - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doFloatHigh(final AbstractSqueakObject receiver, final FloatObject target, final long index) {
            return target.getHigh();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected static final long doFloatLow(final AbstractSqueakObject receiver, final FloatObject target, final long index) {
            return target.getLow();
        }

        @Specialization(guards = "inBounds(index, target)")
        protected final Object doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target, final long index) {
            return getAt0Node().execute(target, index - 1);
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)",
                        "!isNativeObject(target)", "!isLargeIntegerObject(target)", "!isFloatObject(target)", "!isArrayObject(target)"})
        protected final Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject target, final long index) {
            return getAt0Node().execute(target, index - 1 + getInstSizeNode().execute(target));
        }

        private SqueakObjectAt0Node getAt0Node() {
            if (at0Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                at0Node = insert(SqueakObjectAt0Node.create());
            }
            return at0Node;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractBasicAtOrAtPutNode {
        @Child private SqueakObjectAtPut0Node atPut0Node;
        @Child private NativeAcceptsValueNode acceptsValueNode;

        protected PrimBasicAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        public abstract Object executeAtPut(VirtualFrame frame);

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected char doNativeChar(final NativeObject receiver, final long index, final char value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getAtPut0Node().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected long doNativeLong(final NativeObject receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getAtPut0Node().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "value.fitsIntoLong()", "getAcceptsValueNode().execute(receiver, value.longValueExact())"})
        protected Object doNativeLargeInteger(final NativeObject receiver, final long index, final LargeIntegerObject value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getAtPut0Node().execute(receiver, index - 1, value.longValueExact());
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected char doLargeIntegerChar(final LargeIntegerObject receiver, final long index, final char value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected long doLargeIntegerLong(final LargeIntegerObject receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected Object doLargeInteger(final LargeIntegerObject receiver, final long index, final LargeIntegerObject value, @SuppressWarnings("unused") final NotProvided notProvided) {
            receiver.setNativeAt0(index - 1, value.longValueExact());
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected char doFloatHighChar(final FloatObject receiver, final long index, final char value, final NotProvided notProvided) {
            receiver.setHigh(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected char doFloatLowChar(final FloatObject receiver, final long index, final char value, final NotProvided notProvided) {
            receiver.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected long doFloatHighLong(final FloatObject receiver, final long index, final long value, final NotProvided notProvided) {
            receiver.setHigh(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected long doFloatLowLong(final FloatObject receiver, final long index, final long value, final NotProvided notProvided) {
            receiver.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected Object doFloatHighLargeInteger(final FloatObject receiver, final long index, final LargeIntegerObject value, final NotProvided notProvided) {
            receiver.setHigh(value.longValueExact());
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected Object doFloatLowLargeInteger(final FloatObject receiver, final long index, final LargeIntegerObject value, final NotProvided notProvided) {
            receiver.setLow(value.longValueExact());
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doEmptyObject(final EmptyObject receiver, final long idx, final Object value, final NotProvided notProvided) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "!isSmallInteger(receiver)")
        protected Object doSqueakObject(final long receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            try {
                asLargeInteger(receiver).setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected final Object doArray(final ArrayObject receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getAtPut0Node().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, receiver)",
                        "!isNativeObject(receiver)", "!isEmptyObject(receiver)", "!isArrayObject(receiver)"})
        protected final Object doSqueakObject(final AbstractSqueakObject receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getAtPut0Node().execute(receiver, index - 1 + getInstSizeNode().execute(receiver), value);
            return value;
        }

        /*
         * Context>>#object:basicAt:put:
         */

        @Specialization(guards = {"inBounds(index, target)", "getAcceptsValueNode().execute(target, value)"})
        protected char doNativeChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final char value) {
            getAtPut0Node().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getAcceptsValueNode().execute(target, value)"})
        protected long doNativeLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final long value) {
            getAtPut0Node().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "value.fitsIntoLong()", "getAcceptsValueNode().execute(target, value.longValueExact())"})
        protected Object doNativeLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final LargeIntegerObject value) {
            getAtPut0Node().execute(target, index - 1, value.longValueExact());
            return value;
        }

        @Specialization
        protected char doLargeIntegerChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final char value) {
            try {
                target.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected long doLargeIntegerLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final long value) {
            try {
                target.setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization
        protected Object doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final LargeIntegerObject value) {
            try {
                target.setNativeAt0(index - 1, value.longValueExact());
            } catch (IllegalArgumentException | ArithmeticException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected char doFloatHighChar(final AbstractSqueakObject receiver, final FloatObject target, final long index, final char value) {
            target.setHigh(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected char doFloatLowChar(final AbstractSqueakObject receiver, final FloatObject target, final long index, final char value) {
            target.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected long doFloatHighLong(final AbstractSqueakObject receiver, final FloatObject target, final long index, final long value) {
            target.setHigh(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected long doFloatLowLong(final AbstractSqueakObject receiver, final FloatObject target, final long index, final long value) {
            target.setLow(value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected Object doFloatHighLargeInteger(final AbstractSqueakObject receiver, final FloatObject target, final long index, final LargeIntegerObject value) {
            target.setHigh(value.longValueExact());
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 2")
        protected Object doFloatLowLargeInteger(final AbstractSqueakObject receiver, final FloatObject target, final long index, final LargeIntegerObject value) {
            target.setLow(value.longValueExact());
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doEmptyObject(final AbstractSqueakObject receiver, final EmptyObject target, final long idx, final Object value) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "!isSmallInteger(target)")
        protected Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long target, final long index, final long value) {
            try {
                asLargeInteger(target).setNativeAt0(index - 1, value);
            } catch (IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected final Object doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target, final long index, final Object value) {
            getAtPut0Node().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBoundsOfSqueakObject(index, target)",
                        "!isNativeObject(target)", "!isEmptyObject(target)", "!isArrayObject(target)"})
        protected Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject target, final long index, final Object value) {
            getAtPut0Node().execute(target, index - 1 + getInstSizeNode().execute(target), value);
            return value;
        }

        private SqueakObjectAtPut0Node getAtPut0Node() {
            if (atPut0Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                atPut0Node = insert(SqueakObjectAtPut0Node.create());
            }
            return atPut0Node;
        }

        protected final NativeAcceptsValueNode getAcceptsValueNode() {
            if (acceptsValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                acceptsValueNode = insert(NativeAcceptsValueNode.create());
            }
            return acceptsValueNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 62)
    protected abstract static class PrimSizeNode extends AbstractArithmeticPrimitiveNode {
        @Child private SqueakObjectSizeNode sizeNode;
        @Child private SqueakObjectInstSizeNode instSizeNode;

        protected PrimSizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final char obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return 0;
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final boolean o, @SuppressWarnings("unused") final NotProvided notProvided) {
            return 0;
        }

        @Specialization(guards = "!isSmallInteger(value)")
        protected final long doLong(final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            return asLargeInteger(value).size();
        }

        @Specialization
        protected static final long doString(final String s, @SuppressWarnings("unused") final NotProvided notProvided) {
            return s.getBytes().length;
        }

        @Specialization
        protected final long doNative(final NativeObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getSizeNode().execute(obj);
        }

        @Specialization
        protected static final long doLargeInteger(final LargeIntegerObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return obj.size();
        }

        @Specialization
        protected static final long doFloat(@SuppressWarnings("unused") final FloatObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return FloatObject.size();
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final double o, @SuppressWarnings("unused") final NotProvided notProvided) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!obj.isNil()", "obj.getSqueakClass().isVariable()"})
        protected final long size(final AbstractSqueakObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getSizeNode().execute(obj) - getInstSizeNode().execute(obj);
        }

        /*
         * Context>>#objectSize:
         */

        @SuppressWarnings("unused")
        @Specialization
        protected static final long doChar(final AbstractSqueakObject receiver, final char obj) {
            return 0;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final long doBoolean(final AbstractSqueakObject receiver, final boolean o) {
            return 0;
        }

        @Specialization(guards = "!isSmallInteger(value)")
        protected final long doLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long value) {
            return asLargeInteger(value).size();
        }

        @Specialization
        protected static final long doString(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final String s) {
            return s.getBytes().length;
        }

        @Specialization
        protected final long doNative(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject obj) {
            return getSizeNode().execute(obj);
        }

        @Specialization
        protected static final long doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject obj) {
            return obj.size();
        }

        @Specialization
        protected static final long doFloat(@SuppressWarnings("unused") final AbstractSqueakObject receiver, @SuppressWarnings("unused") final FloatObject obj) {
            return FloatObject.size();
        }

        @Specialization
        protected static final long doDouble(@SuppressWarnings("unused") final AbstractSqueakObject receiver, @SuppressWarnings("unused") final double o) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!obj.isNil()", "obj.getSqueakClass().isVariable()"})
        protected final long doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject obj) {
            return getSizeNode().execute(obj) - getInstSizeNode().execute(obj);
        }

        /*
         * Quick return 0 to allow eager primitive calls.
         * "The number of indexable fields of fixed-length objects is 0" (see Object>>basicSize).
         */
        @SuppressWarnings("unused")
        @Fallback
        protected static final long doObject(final Object receiver, final Object anything) {
            return 0;
        }

        private SqueakObjectSizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sizeNode = insert(SqueakObjectSizeNode.create());
            }
            return sizeNode;
        }

        private SqueakObjectInstSizeNode getInstSizeNode() {
            if (instSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instSizeNode = insert(SqueakObjectInstSizeNode.create());
            }
            return instSizeNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveWithSizeNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimStringAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        public abstract Object executeStringAt(VirtualFrame frame);

        @Specialization(guards = {"inBounds(index, obj)"})
        protected final char doNativeObject(final NativeObject obj, final long index) {
            return (char) ((long) at0Node.execute(obj, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveWithSizeNode {
        @Child protected NativeAcceptsValueNode acceptsValueNode = NativeAcceptsValueNode.create();
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

        protected PrimStringAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        public abstract Object executeStringAtPut(VirtualFrame frame);

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final char doNativeObject(final NativeObject obj, final long index, final char value) {
            atPut0Node.execute(obj, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final char doNativeObject(final NativeObject obj, final long index, final long value) {
            atPut0Node.execute(obj, index - 1, value);
            return (char) value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 143)
    protected abstract static class PrimShortAtNode extends AbstractPrimitiveNode {

        protected PrimShortAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds0(largerOffset(index), receiver.getByteLength())"})
        protected static final long doNativeBytes(final NativeObject receiver, final long index) {
            final int offset = minusOneAndDouble(index);
            final byte[] bytes = receiver.getByteStorage();
            final int byte0 = (byte) Byte.toUnsignedLong(bytes[offset]);
            int byte1 = (int) Byte.toUnsignedLong(bytes[offset + 1]) << 8;
            if ((byte1 & 0x8000) != 0) {
                byte1 = 0xffff0000 | byte1;
            }
            return byte1 | byte0;
        }

        @Specialization(guards = {"receiver.isShortType()", "inBounds1(index, receiver.getShortLength())"})
        protected static final long doNativeShorts(final NativeObject receiver, final long index) {
            return Short.toUnsignedLong(receiver.getShortStorage()[(int) index]);
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds0(minusOneAndCutInHalf(index), receiver.getIntLength())"})
        protected static final long doNativeInts(final NativeObject receiver, final long index) {
            final int word = receiver.getIntStorage()[minusOneAndCutInHalf(index)];
            int shortValue;
            if ((index - 1) % 2 == 0) {
                shortValue = word & 0xffff;
            } else {
                shortValue = (word >> 16) & 0xffff;
            }
            if ((shortValue & 0x8000) != 0) {
                shortValue = 0xffff0000 | shortValue;
            }
            return shortValue;
        }

        protected static final int minusOneAndDouble(final long index) {
            return (int) ((index - 1) * 2);
        }

        protected static final int largerOffset(final long index) {
            return minusOneAndDouble(index) + 1;
        }

        protected static final int minusOneAndCutInHalf(final long index) {
            return ((int) index - 1) / 2;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "receiver.isLongType()")
        protected static final long doNativeLongs(final NativeObject receiver, final long index) {
            throw new SqueakException("Not yet implemented: shortAtPut0"); // TODO: implement
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 144)
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode {

        protected PrimShortAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isByteType()"})
        protected static final long doNativeBytes(final NativeObject receiver, final long index, final long value) {
            final int offset = (int) ((index - 1) * 2);
            final byte[] bytes = receiver.getByteStorage();
            bytes[offset] = (byte) value;
            bytes[offset + 1] = (byte) (value >> 8);
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isShortType()"})
        protected static final long doNativeShorts(final NativeObject receiver, final long index, final long value) {
            receiver.getShortStorage()[(int) index] = (short) value;
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isIntType()", "isEven(index)"})
        protected static final long doNativeIntsEven(final NativeObject receiver, final long index, final long value) {
            final int wordIndex = (int) ((index - 1) / 2);
            final int[] ints = receiver.getIntStorage();
            final int word = (int) Integer.toUnsignedLong(ints[wordIndex]);
            ints[wordIndex] = (word & 0xffff0000) | ((int) value & 0xffff);
            return value;
        }

        @Specialization(guards = {"inShortRange(value)", "receiver.isIntType()", "!isEven(index)"})
        protected static final long doNativeIntsOdd(final NativeObject receiver, final long index, final long value) {
            final int wordIndex = (int) ((index - 1) / 2);
            final int[] ints = receiver.getIntStorage();
            final int word = (int) Integer.toUnsignedLong(ints[wordIndex]);
            ints[wordIndex] = ((int) value << 16) | (word & 0xffff);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"inShortRange(value)", "receiver.isLongType()"})
        protected static final long doNativeLongs(final NativeObject receiver, final long index, final long value) {
            throw new SqueakException("Not yet implemented: shortAtPut0"); // TODO: implement
        }

        protected static final boolean inShortRange(final long value) {
            return -0x8000 <= value && value <= 0x8000;
        }

        protected static final boolean isEven(final long index) {
            return (index - 1) % 2 == 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 165)
    protected abstract static class PrimIntegerAtNode extends AbstractPrimitiveNode {

        protected PrimIntegerAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())"})
        protected static final long doNativeInt(final NativeObject receiver, final long index) {
            return receiver.getIntStorage()[(int) index - 1];
        }
    }

    @ImportStatic(Integer.class)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 166)
    protected abstract static class PrimIntegerAtPutNode extends AbstractPrimitiveNode {

        protected PrimIntegerAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())", "value >= MIN_VALUE", "value <= MAX_VALUE"})
        protected static final long doNativeInt(final NativeObject receiver, final long index, final long value) {
            receiver.getIntStorage()[(int) index - 1] = (int) value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode {
        protected PrimContextAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index) {
            return receiver.atTemp(index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode {

        protected PrimContextAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value) {
            receiver.atTempPut(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 212)
    protected abstract static class PrimContextSizeNode extends AbstractPrimitiveNode {

        protected PrimContextSizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doSize(final ContextObject receiver) {
            return receiver.size() - receiver.instsize();
        }
    }
}

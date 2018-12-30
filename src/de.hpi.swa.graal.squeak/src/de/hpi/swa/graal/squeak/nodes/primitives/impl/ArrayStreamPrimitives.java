package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.WriteArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeAcceptsValueNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.ReadNativeObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.WriteNativeObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveWithSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ArrayStreamPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ArrayStreamPrimitivesFactory.getFactories();
    }

    protected abstract static class AbstractBasicAtOrAtPutNode extends AbstractPrimitiveWithSizeNode {
        @Child protected SqueakObjectInstSizeNode instSizeNode;

        public AbstractBasicAtOrAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final boolean inBoundsOfSqueakObject(final long index, final AbstractSqueakObject target) {
            return SqueakGuards.inBounds1(index + getInstSizeNode().execute(target), getSizeNode().execute(target));
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
    @SqueakPrimitive(indices = 60)
    protected abstract static class PrimBasicAtNode extends AbstractBasicAtOrAtPutNode implements TernaryPrimitive {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private ReadArrayObjectNode readArrayObjectNode;
        @Child private ReadNativeObjectNode readNativeObjectNode;

        protected PrimBasicAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doCharacter(final char receiver, final long index, final NotProvided notProvided) {
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doCharacter(final CharacterObject receiver, final long index, final NotProvided notProvided) {
            return receiver.getValue();
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
            return getNativeArrayObjectNode().execute(receiver, index - 1);
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
            return getReadArrayObjectNode().execute(receiver, index - 1);
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

        @SuppressWarnings("unused")
        @Specialization(guards = "index == 1")
        protected static final long doCharacter(final AbstractSqueakObject receiver, final CharacterObject target, final long index) {
            return target.getValue();
        }

        @Specialization(guards = "!isSmallInteger(target)")
        protected final Object doLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long target, final long index) {
            return asLargeInteger(target).getNativeAt0(index - 1);
        }

        @Specialization(guards = "inBounds(index, target)")
        protected final Object doNative(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index) {
            return getNativeArrayObjectNode().execute(target, index - 1);
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
            return getReadArrayObjectNode().execute(target, index - 1);
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

        private ReadArrayObjectNode getReadArrayObjectNode() {
            if (readArrayObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readArrayObjectNode = insert(ReadArrayObjectNode.create());
            }
            return readArrayObjectNode;
        }

        private ReadNativeObjectNode getNativeArrayObjectNode() {
            if (readNativeObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNativeObjectNode = insert(ReadNativeObjectNode.create());
            }
            return readNativeObjectNode;
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 61)
    protected abstract static class PrimBasicAtPutNode extends AbstractBasicAtOrAtPutNode implements QuaternaryPrimitive {
        @Child private SqueakObjectAtPut0Node atPut0Node;
        @Child private NativeAcceptsValueNode acceptsValueNode;
        @Child private WriteArrayObjectNode writeArrayObjectNode;
        @Child private WriteNativeObjectNode writeNativeObjectNode;

        protected PrimBasicAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected char doNativeChar(final NativeObject receiver, final long index, final char value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getWriteNativeObjectNode().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected Object doNativeChar(final NativeObject receiver, final long index, final CharacterObject value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getWriteNativeObjectNode().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected long doNativeLong(final NativeObject receiver, final long index, final long value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getWriteNativeObjectNode().execute(receiver, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, receiver)", "getAcceptsValueNode().execute(receiver, value)"})
        protected Object doNativeLargeInteger(final NativeObject receiver, final long index, final LargeIntegerObject value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getWriteNativeObjectNode().execute(receiver, index - 1, value);
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

        @Specialization// (guards = "inBounds(index, receiver)")
        protected final Object doArray(final ArrayObject receiver, final long index, final Object value, @SuppressWarnings("unused") final NotProvided notProvided) {
            getWriteArrayObjectNode().execute(receiver, index - 1, value);
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
            getWriteNativeObjectNode().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getAcceptsValueNode().execute(target, value)"})
        protected long doNativeLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final long value) {
            getWriteNativeObjectNode().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, target)", "getAcceptsValueNode().execute(target, value)"})
        protected Object doNativeLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject target, final long index, final LargeIntegerObject value) {
            getWriteNativeObjectNode().execute(target, index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(value, BYTE_MAX)")
        protected char doLargeIntegerChar(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final char value) {
            target.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds1(value, BYTE_MAX)")
        protected long doLargeIntegerLong(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final long value) {
            target.setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = {"value.fitsIntoLong()", "inBounds1(value.longValueExact(), BYTE_MAX)"})
        protected Object doLargeInteger(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final LargeIntegerObject target, final long index, final LargeIntegerObject value) {
            target.setNativeAt0(index - 1, value.longValueExact());
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

        @Specialization(guards = {"!isSmallInteger(target)", "inBounds1(value, BYTE_MAX)"})
        protected Object doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long target, final long index, final long value) {
            asLargeInteger(target).setNativeAt0(index - 1, value);
            return value;
        }

        @Specialization(guards = "inBounds(index, receiver)")
        protected final Object doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject target, final long index, final Object value) {
            getWriteArrayObjectNode().execute(target, index - 1, value);
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

        private WriteArrayObjectNode getWriteArrayObjectNode() {
            if (writeArrayObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeArrayObjectNode = insert(WriteArrayObjectNode.create());
            }
            return writeArrayObjectNode;
        }

        private WriteNativeObjectNode getWriteNativeObjectNode() {
            if (writeNativeObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNativeObjectNode = insert(WriteNativeObjectNode.create());
            }
            return writeNativeObjectNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 62)
    protected abstract static class PrimSizeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private SqueakObjectSizeNode sizeNode;
        @Child private ArrayObjectSizeNode arrayObjectSizeNode;
        @Child private NativeObjectSizeNode nativeObjectSizeNode;
        @Child private SqueakObjectInstSizeNode instSizeNode;

        protected PrimSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final char obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return 0;
        }

        @Specialization
        protected static final long size(@SuppressWarnings("unused") final CharacterObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
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
        protected final long doArray(final ArrayObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getArrayObjectSizeNode().execute(obj);
        }

        @Specialization
        protected final long doNative(final NativeObject obj, @SuppressWarnings("unused") final NotProvided notProvided) {
            return getNativeObjectSizeNode().execute(obj);
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
        protected static final long doDouble(@SuppressWarnings("unused") final double o, @SuppressWarnings("unused") final NotProvided notProvided) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!obj.isNil()", "!isArrayObject(obj)", "!isNativeObject(obj)", "obj.getSqueakClass().isVariable()"})
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
        protected static final long doChar(final AbstractSqueakObject receiver, final CharacterObject obj) {
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
        protected final long doArray(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final ArrayObject obj) {
            return getArrayObjectSizeNode().execute(obj);
        }

        @Specialization
        protected final long doNative(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject obj) {
            return getNativeObjectSizeNode().execute(obj);
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
        protected static final long doDouble(@SuppressWarnings("unused") final AbstractSqueakObject receiver, @SuppressWarnings("unused") final double obj) {
            return 2; // Float in words
        }

        @Specialization(guards = {"!obj.isNil()", "!isArrayObject(obj)", "!isNativeObject(obj)", "obj.getSqueakClass().isVariable()"})
        protected final long doSqueakObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final AbstractSqueakObject obj) {
            return getSizeNode().execute(obj) - getInstSizeNode().execute(obj);
        }

        private SqueakObjectSizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sizeNode = insert(SqueakObjectSizeNode.create());
            }
            return sizeNode;
        }

        private ArrayObjectSizeNode getArrayObjectSizeNode() {
            if (arrayObjectSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayObjectSizeNode = insert(ArrayObjectSizeNode.create());
            }
            return arrayObjectSizeNode;
        }

        private NativeObjectSizeNode getNativeObjectSizeNode() {
            if (nativeObjectSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nativeObjectSizeNode = insert(NativeObjectSizeNode.create());
            }
            return nativeObjectSizeNode;
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
    @SqueakPrimitive(indices = 63)
    protected abstract static class PrimStringAtNode extends AbstractPrimitiveWithSizeNode implements BinaryPrimitive {
        @Child private ReadNativeObjectNode readNode = ReadNativeObjectNode.create();

        protected PrimStringAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, obj)"})
        protected final Object doNativeObject(final NativeObject obj, final long index) {
            return CharacterObject.valueOf(code.image, (int) (long) readNode.execute(obj, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 64)
    protected abstract static class PrimStringAtPutNode extends AbstractPrimitiveWithSizeNode implements TernaryPrimitive {
        @Child protected NativeAcceptsValueNode acceptsValueNode = NativeAcceptsValueNode.create();
        @Child private WriteNativeObjectNode writeNode = WriteNativeObjectNode.create();

        protected PrimStringAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final char doNativeObject(final NativeObject obj, final long index, final char value) {
            writeNode.execute(obj, index - 1, value);
            return value;
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final Object doNativeObject(final NativeObject obj, final long index, final CharacterObject value) {
            writeNode.execute(obj, index - 1, value.getValue());
            return value;
        }

        @Specialization(guards = {"inBounds(index, obj)", "acceptsValueNode.execute(obj, value)"})
        protected final Object doNativeObject(final NativeObject obj, final long index, final long value) {
            writeNode.execute(obj, index - 1, value);
            return CharacterObject.valueOf(code.image, (int) value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 143)
    protected abstract static class PrimShortAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimShortAtNode(final CompiledMethodObject method) {
            super(method);
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
    protected abstract static class PrimShortAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimShortAtPutNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 165)
    protected abstract static class PrimIntegerAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimIntegerAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds1(index, receiver.getByteLength())"})
        protected static final long doNativeByte(final NativeObject receiver, final long index) {
            return receiver.getByteStorage()[(int) index - 1];
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())"})
        protected static final long doNativeInt(final NativeObject receiver, final long index) {
            return receiver.getIntStorage()[(int) index - 1];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 166)
    protected abstract static class PrimIntegerAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimIntegerAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.isByteType()", "inBounds1(index, receiver.getByteLength())", "fitsIntoByte(value)"})
        protected static final long doNativeByte(final NativeObject receiver, final long index, final long value) {
            receiver.getByteStorage()[(int) index - 1] = (byte) value;
            return value;
        }

        @Specialization(guards = {"receiver.isIntType()", "inBounds1(index, receiver.getIntLength())", "fitsIntoInt(value)"})
        protected static final long doNativeInt(final NativeObject receiver, final long index, final long value) {
            receiver.getIntStorage()[(int) index - 1] = (int) value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 210)
    protected abstract static class PrimContextAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimContextAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index) {
            return receiver.atTemp(index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 211)
    protected abstract static class PrimContextAtPutNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimContextAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "index < receiver.getStackSize()")
        protected static final Object doContextObject(final ContextObject receiver, final long index, final Object value) {
            receiver.atTempPut(index - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 212)
    protected abstract static class PrimContextSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimContextSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doSize(final ContextObject receiver) {
            return receiver.size() - receiver.instsize();
        }
    }
}

package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectAt0Node extends Node {

    public static SqueakObjectAt0Node create() {
        return SqueakObjectAt0NodeGen.create();
    }

    public abstract Object execute(Object obj, long index);

    @Specialization(guards = {"obj.isEmptyType()", "index >= 0", "index < obj.getEmptyStorage()"})
    protected static final NilObject doEmptyArray(final ArrayObject obj, @SuppressWarnings("unused") final long index) {
        return obj.getNil();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"obj.isEmptyType()", "index < 0 || index >= obj.getEmptyStorage()"})
    protected static final long doEmptyArrayOutOfBounds(final ArrayObject obj, final long index) {
        throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
    }

    @Specialization(guards = "obj.isAbstractSqueakObjectType()")
    protected static final AbstractSqueakObject doArrayOfSqueakObjects(final ArrayObject obj, final long index) {
        return obj.at0SqueakObject(index);
    }

    @Specialization(guards = "obj.isBooleanType()")
    protected static final Object doArrayOfBooleans(final ArrayObject obj, final long index) {
        return obj.at0Boolean(index);
    }

    @Specialization(guards = "obj.isCharType()")
    protected static final Object doArrayOfChars(final ArrayObject obj, final long index) {
        return obj.at0Char(index);
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final Object doArrayOfLongs(final ArrayObject obj, final long index) {
        return obj.at0Long(index);
    }

    @Specialization(guards = "obj.isDoubleType()")
    protected static final Object doArrayOfDoubles(final ArrayObject obj, final long index) {
        return obj.at0Double(index);
    }

    @Specialization(guards = "obj.isObjectType()")
    protected static final Object doArrayOfObjects(final ArrayObject obj, final long index) {
        return obj.at0Object(index);
    }

    @Specialization
    protected static final Object doPointers(final PointersObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doContext(final ContextObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doClass(final ClassObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doWeakPointers(final WeakPointersObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization(guards = "obj.isByteType()")
    protected static final long doNativeBytes(final NativeObject obj, final long index) {
        return Byte.toUnsignedLong(obj.getByteStorage()[(int) index]);
    }

    @Specialization(guards = "obj.isShortType()")
    protected static final long doNativeShorts(final NativeObject obj, final long index) {
        return Short.toUnsignedLong(obj.getShortStorage()[(int) index]);
    }

    @Specialization(guards = "obj.isIntType()")
    protected static final long doNativeInts(final NativeObject obj, final long index) {
        return Integer.toUnsignedLong(obj.getIntStorage()[(int) index]);
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final long doNativeLongs(final NativeObject obj, final long index) {
        return obj.getLongStorage()[(int) index];
    }

    @Specialization(guards = "index == 1")
    protected static final Object doFloatHigh(final FloatObject obj, @SuppressWarnings("unused") final long index) {
        return obj.getHigh();
    }

    @Specialization(guards = "index == 2")
    protected static final Object doFloatLow(final FloatObject obj, @SuppressWarnings("unused") final long index) {
        return obj.getLow();
    }

    @Specialization
    protected static final Object doLargeInteger(final LargeIntegerObject obj, final long index) {
        return obj.getNativeAt0(index);
    }

    @Specialization
    protected static final Object doBlock(final CompiledBlockObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doMethod(final CompiledMethodObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doClosure(final BlockClosureObject obj, final long index) {
        return obj.at0(index);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final Object doEmpty(final EmptyObject obj, final long index) {
        throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final Object doNil(final NilObject obj, final long index) {
        throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object doFallback(final Object obj, final long index) {
        throw new SqueakException("Object does not support at0:", obj);
    }
}

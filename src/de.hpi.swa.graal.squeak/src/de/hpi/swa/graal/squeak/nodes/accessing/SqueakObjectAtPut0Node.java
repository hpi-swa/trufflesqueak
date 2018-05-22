package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectAtPut0Node extends Node {
    private final ValueProfile storageType = ValueProfile.createClassProfile();

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(Object obj, long index, Object value);

    @Specialization(guards = {"!obj.isClass()"})
    protected static final void doAbstractPointers(final PointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClass(final ClassObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization(guards = "obj.isByteType()")
    protected final void doNativeBytes(final NativeObject obj, final long index, final long value) {
        if (value < 0 || value > NativeObject.BYTE_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for BytesObject: " + value);
        }
        obj.getByteStorage(storageType)[(int) index] = (byte) value;
    }

    @Specialization(guards = "obj.isShortType()")
    protected final void doNativeShorts(final NativeObject obj, final long index, final long value) {
        if (value < 0 || value > NativeObject.SHORT_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for ShortsObject: " + value);
        }
        obj.getShortStorage(storageType)[(int) index] = (short) value;
    }

    @Specialization(guards = "obj.isIntType()")
    protected final void doNativeInts(final NativeObject obj, final long index, final long value) {
        if (value < 0 || value > NativeObject.INTEGER_MAX) { // check for overflow
            throw new IllegalArgumentException("Illegal value for WordsObject: " + value);
        }
        obj.getIntStorage(storageType)[(int) index] = (int) value;
    }

    @Specialization(guards = "obj.isLongType()")
    protected final void doNativeLongs(final NativeObject obj, final long index, final long value) {
        obj.getLongStorage(storageType)[(int) index] = value;
    }

    @Specialization(guards = "obj.isByteType()")
    protected final void doNativeBytesChar(final NativeObject obj, final long index, final char value) {
        doNativeBytes(obj, index, value);
    }

    @Specialization(guards = "obj.isShortType()")
    protected final void doNativeShortsChar(final NativeObject obj, final long index, final char value) {
        doNativeShorts(obj, index, value);
    }

    @Specialization(guards = "obj.isIntType()")
    protected final void doNativeIntsChar(final NativeObject obj, final long index, final char value) {
        doNativeInts(obj, index, value);
    }

    @Specialization(guards = "obj.isLongType()")
    protected final void doNativeLongsChar(final NativeObject obj, final long index, final char value) {
        doNativeLongs(obj, index, value);
    }

    @Specialization(guards = "obj.isByteType()")
    protected final void doNativeBytesLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeBytes(obj, index, value.reduceToLong());
    }

    @Specialization(guards = "obj.isShortType()")
    protected final void doNativeShortsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeShorts(obj, index, value.reduceToLong());
    }

    @Specialization(guards = "obj.isIntType()")
    protected final void doNativeIntsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeInts(obj, index, value.reduceToLong());
    }

    @Specialization(guards = "obj.isLongType()")
    protected final void doNativeLongsLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        doNativeLongs(obj, index, value.reduceToLong());
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.reduceToLong());
    }

    @Specialization
    protected static final void doFloat(final FloatObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doFloat(final FloatObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.reduceToLong());
    }

    @Specialization
    protected static final void doCode(final CompiledCodeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClosure(final BlockClosureObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doEmpty(final EmptyObject obj, final long index, final Object value) {
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doNil(final NilObject obj, final long index, final Object value) {
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFallback(final Object obj, final long index, final Object value) {
        throw new SqueakException("Object does not support atput0: " + obj);
    }
}

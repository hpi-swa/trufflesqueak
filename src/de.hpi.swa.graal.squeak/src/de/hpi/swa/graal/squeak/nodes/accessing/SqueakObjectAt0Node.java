package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
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
    private final ValueProfile storageType = ValueProfile.createClassProfile();

    public static SqueakObjectAt0Node create() {
        return SqueakObjectAt0NodeGen.create();
    }

    public abstract Object execute(Object obj, long index);

    @Specialization
    protected static final Object doAbstractPointers(final PointersObject obj, final long index) {
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
    protected final long doNativeBytes(final NativeObject obj, final long index) {
        return Byte.toUnsignedLong(obj.getByteStorage(storageType)[(int) index]);
    }

    @Specialization(guards = "obj.isShortType()")
    protected final long doNativeShorts(final NativeObject obj, final long index) {
        return Short.toUnsignedLong(obj.getShortStorage(storageType)[(int) index]);
    }

    @Specialization(guards = "obj.isIntType()")
    protected final long doNativeInts(final NativeObject obj, final long index) {
        return Integer.toUnsignedLong(obj.getIntStorage(storageType)[(int) index]);
    }

    @Specialization(guards = "obj.isLongType()")
    protected final long doNativeLongs(final NativeObject obj, final long index) {
        return obj.getLongStorage(storageType)[(int) index];
    }

    @Specialization
    protected static final Object doFloat(final FloatObject obj, final long index) {
        return obj.getNativeAt0(index);
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
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final Object doNil(final NilObject obj, final long index) {
        throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object doFallback(final Object obj, final long index) {
        throw new SqueakException("Object does not support at0:", obj);
    }
}

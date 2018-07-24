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

public abstract class SqueakObjectSizeNode extends Node {
    private final ValueProfile storageType = ValueProfile.createClassProfile();

    public static SqueakObjectSizeNode create() {
        return SqueakObjectSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doPointers(final PointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doWeakPointers(final WeakPointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doMethod(final CompiledMethodObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doBlock(final CompiledBlockObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doEmpty(@SuppressWarnings("unused") final EmptyObject obj) {
        return 0;
    }

    @Specialization(guards = "obj.isByteType()")
    protected final int doNativeBytes(final NativeObject obj) {
        return obj.getByteStorage(storageType).length;
    }

    @Specialization(guards = "obj.isShortType()")
    protected final int doNativeShorts(final NativeObject obj) {
        return obj.getShortStorage(storageType).length;
    }

    @Specialization(guards = "obj.isIntType()")
    protected final int doNativeInts(final NativeObject obj) {
        return obj.getIntStorage(storageType).length;
    }

    @Specialization(guards = "obj.isLongType()")
    protected final int doNativeLongs(final NativeObject obj) {
        return obj.getLongStorage(storageType).length;
    }

    @Specialization
    protected static final int doFloat(@SuppressWarnings("unused") final FloatObject obj) {
        return FloatObject.size();
    }

    @Specialization
    protected static final int doLargeInteger(final LargeIntegerObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return 0;
    }

    @Fallback
    protected static final int doFallback(final Object obj) {
        throw new SqueakException("Object does not support size:", obj);
    }
}

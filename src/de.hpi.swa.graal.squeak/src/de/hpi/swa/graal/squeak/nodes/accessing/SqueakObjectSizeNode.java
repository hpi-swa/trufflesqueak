package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;

public abstract class SqueakObjectSizeNode extends Node {

    public static SqueakObjectSizeNode create() {
        return SqueakObjectSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization(guards = "obj.isEmptyType()")
    protected static final int doEmptyArrayObject(final ArrayObject obj) {
        return obj.getEmptyStorage();
    }

    @Specialization(guards = "obj.isAbstractSqueakObjectType()")
    protected static final int doArrayObjectOfSqueakObjects(final ArrayObject obj) {
        return obj.getAbstractSqueakObjectStorage().length;
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final int doArrayObjectOfLongs(final ArrayObject obj) {
        return obj.getLongStorage().length;
    }

    @Specialization(guards = "obj.isDoubleType()")
    protected static final int doArrayObjectOfDoubles(final ArrayObject obj) {
        return obj.getDoubleStorage().length;
    }

    @Specialization(guards = "obj.isObjectType()")
    protected static final int doArrayObjectOfObjects(final ArrayObject obj) {
        return obj.getObjectStorage().length;
    }

    @Specialization
    protected static final int doAbstractPointers(final AbstractPointersObject obj) {
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
    protected static final int doNativeBytes(final NativeObject obj) {
        return obj.getByteStorage().length;
    }

    @Specialization(guards = "obj.isShortType()")
    protected static final int doNativeShorts(final NativeObject obj) {
        return obj.getShortStorage().length;
    }

    @Specialization(guards = "obj.isIntType()")
    protected static final int doNativeInts(final NativeObject obj) {
        return obj.getIntStorage().length;
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final int doNativeLongs(final NativeObject obj) {
        return obj.getLongStorage().length;
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

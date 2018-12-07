package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
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
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class SqueakObjectShallowCopyNode extends AbstractNodeWithImage {

    public static SqueakObjectShallowCopyNode create(final SqueakImageContext image) {
        return SqueakObjectShallowCopyNodeGen.create(image);
    }

    public final Object execute(final Object object) {
        image.reportNewAllocationRequest();
        return image.reportNewAllocationResult(executeAllocation(object));
    }

    protected SqueakObjectShallowCopyNode(final SqueakImageContext image) {
        super(image);
    }

    protected abstract Object executeAllocation(Object obj);

    @Specialization
    protected static final double doDouble(final double value) {
        return value;
    }

    @Specialization
    protected static final Object doClosure(final BlockClosureObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doClass(final ClassObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doBlock(final CompiledBlockObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doMethod(final CompiledMethodObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doContext(final ContextObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doEmpty(final EmptyObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization(guards = "receiver.isByteType()")
    protected static final Object doNativeBytes(final NativeObject receiver) {
        return NativeObject.newNativeBytes(receiver.image, receiver.getSqueakClass(), receiver.getByteStorage().clone());
    }

    @Specialization(guards = "receiver.isShortType()")
    protected static final Object doNativeShorts(final NativeObject receiver) {
        return NativeObject.newNativeShorts(receiver.image, receiver.getSqueakClass(), receiver.getShortStorage().clone());
    }

    @Specialization(guards = "receiver.isIntType()")
    protected static final Object doNativeInts(final NativeObject receiver) {
        return NativeObject.newNativeInts(receiver.image, receiver.getSqueakClass(), receiver.getIntStorage().clone());
    }

    @Specialization(guards = "receiver.isLongType()")
    protected static final Object doNativeLongs(final NativeObject receiver) {
        return NativeObject.newNativeLongs(receiver.image, receiver.getSqueakClass(), receiver.getLongStorage().clone());
    }

    @Specialization
    protected static final Object doLargeInteger(final LargeIntegerObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doFloat(final FloatObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doNil(final NilObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization(guards = "receiver.isEmptyType()")
    protected static final Object doEmptyArray(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getEmptyStorage());
    }

    @Specialization(guards = "receiver.isAbstractSqueakObjectType()")
    protected static final Object doArrayOfSqueakObjects(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getAbstractSqueakObjectStorage().clone());
    }

    @Specialization(guards = "receiver.isBooleanType()")
    protected static final Object doArrayOfBooleans(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getBooleanStorage().clone());
    }

    @Specialization(guards = "receiver.isCharType()")
    protected static final Object doArrayOfChars(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getCharStorage().clone());
    }

    @Specialization(guards = "receiver.isLongType()")
    protected static final Object doArrayOfLongs(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getLongStorage().clone());
    }

    @Specialization(guards = "receiver.isDoubleType()")
    protected static final Object doArrayOfDoubles(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getDoubleStorage().clone());
    }

    @Specialization(guards = "receiver.isObjectType()")
    protected static final Object doArrayOfObjects(final ArrayObject receiver) {
        return ArrayObject.createWithStorage(receiver.image, receiver.getSqueakClass(), receiver.getObjectStorage().clone());
    }

    @Specialization
    protected static final Object doPointers(final PointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final Object doWeakPointers(final WeakPointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Fallback
    protected static final Object doFail(final Object object) {
        throw new SqueakException("Unsupported value:", object);
    }

}

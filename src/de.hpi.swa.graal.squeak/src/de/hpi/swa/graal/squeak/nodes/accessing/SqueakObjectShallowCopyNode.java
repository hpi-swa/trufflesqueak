package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
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
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectShallowCopyNode;

public abstract class SqueakObjectShallowCopyNode extends AbstractNodeWithImage {

    protected SqueakObjectShallowCopyNode(final SqueakImageContext image) {
        super(image);
    }

    public static SqueakObjectShallowCopyNode create(final SqueakImageContext image) {
        return SqueakObjectShallowCopyNodeGen.create(image);
    }

    public final Object execute(final Object object) {
        image.reportNewAllocationRequest();
        return image.reportNewAllocationResult(executeAllocation(object));
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

    @Specialization(guards = "!receiver.hasInstanceVariables()")
    protected static final Object doClassNoInstanceVariables(final ClassObject receiver) {
        return receiver.shallowCopy(null);
    }

    @Specialization(guards = "receiver.hasInstanceVariables()")
    protected static final Object doClass(final ClassObject receiver,
                    @Cached("create()") final ArrayObjectShallowCopyNode arrayCopyNode) {
        return receiver.shallowCopy(arrayCopyNode.execute(receiver.getInstanceVariablesOrNull()));
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

    @Specialization
    protected static final Object doArray(final ArrayObject receiver,
                    @Cached("create()") final ArrayObjectShallowCopyNode copyNode) {
        return copyNode.execute(receiver);
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
        throw SqueakException.create("Unsupported value:", object);
    }

}

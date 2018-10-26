package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
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
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ClassObjectNodes.ReadClassObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.ReadNativeObjectNode;

public abstract class SqueakObjectAt0Node extends Node {

    public static SqueakObjectAt0Node create() {
        return SqueakObjectAt0NodeGen.create();
    }

    public abstract Object execute(Object obj, long index);

    @Specialization
    protected static final Object doArray(final ArrayObject obj, final long index,
                    @Cached("create()") final ReadArrayObjectNode readNode) {
        return readNode.execute(obj, index);
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
    protected static final Object doClass(final ClassObject obj, final long index,
                    @Cached("create()") final ReadClassObjectNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final Object doWeakPointers(final WeakPointersObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final long doNative(final NativeObject obj, final long index,
                    @Cached("create()") final ReadNativeObjectNode readNode) {
        return readNode.execute(obj, index);
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

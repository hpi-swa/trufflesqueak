package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
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
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.WriteArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ClassObjectNodes.WriteClassObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.WriteNativeObjectNode;

@ImportStatic({NativeObject.class, SqueakGuards.class})
public abstract class SqueakObjectAtPut0Node extends Node {

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(Object obj, long index, Object value);

    @Specialization
    protected static final void doArray(final ArrayObject obj, final long index, final Object value,
                    @Cached("create()") final WriteArrayObjectNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final long index, final Object value) {
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
    protected static final void doClass(final ClassObject obj, final long index, final Object value,
                    @Cached("create()") final WriteClassObjectNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doNative(final NativeObject obj, final long index, final Object value,
                    @Cached("create()") final WriteNativeObjectNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.longValueExact());
    }

    @Specialization(guards = {"index == 0", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doFloatHigh(final FloatObject obj, @SuppressWarnings("unused") final long index, final long value) {
        obj.setHigh(value);
    }

    @Specialization(guards = {"index == 1", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doFloatLow(final FloatObject obj, @SuppressWarnings("unused") final long index, final long value) {
        obj.setLow(value);
    }

    @Specialization(guards = {"index == 0", "!value.inRange(0, INTEGER_MAX)"})
    protected static final void doFloatHigh(final FloatObject obj, @SuppressWarnings("unused") final long index, final LargeIntegerObject value) {
        obj.setHigh(value.longValueExact());
    }

    @Specialization(guards = {"index == 1", "!value.inRange(0, INTEGER_MAX)"})
    protected static final void doFloatLow(final FloatObject obj, @SuppressWarnings("unused") final long index, final LargeIntegerObject value) {
        obj.setLow(value.longValueExact());
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
        throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doNil(final NilObject obj, final long index, final Object value) {
        throw new SqueakException("IndexOutOfBounds:", index, "(validate index before using this node)");
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFallback(final Object obj, final long index, final Object value) {
        throw new SqueakException("Object does not support atput0:", obj);
    }
}

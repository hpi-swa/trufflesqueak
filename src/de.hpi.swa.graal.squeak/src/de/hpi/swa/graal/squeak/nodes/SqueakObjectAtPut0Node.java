package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
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

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(AbstractSqueakObject obj, long index, Object value);

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

    @Specialization
    protected static final void doNativeLong(final NativeObject obj, final long index, final long value) {
        obj.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doNativeLargeInteger(final NativeObject obj, final long index, final LargeIntegerObject value) {
        obj.setNativeAt0(index, value.reduceToLong());
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
    protected static final void doFloat(final FloatObject obj, final long index, final Object value) {
        obj.atput0(index, value);
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

}

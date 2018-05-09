package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectAtPut0Node extends AbstractBaseSqueakObjectNode {

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(AbstractSqueakObject obj, long index, Object value);

    @Specialization(guards = {"!isContext(obj)", "!isWeakPointers(obj)", "!obj.isClass()"})
    protected static final void doAbstractPointers(final AbstractPointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doPointers(final ContextObject obj, final long index, final Object value) {
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

    @Specialization(guards = {"!isFloat(obj)"})
    protected static final void doNative(final NativeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
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

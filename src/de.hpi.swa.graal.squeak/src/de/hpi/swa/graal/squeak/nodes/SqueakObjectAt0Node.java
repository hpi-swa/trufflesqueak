package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectAt0Node extends AbstractBaseSqueakObjectNode {

    public static SqueakObjectAt0Node create() {
        return SqueakObjectAt0NodeGen.create();
    }

    public abstract Object execute(AbstractSqueakObject obj, long index);

    @Specialization(guards = {"!isContext(obj)", "!isWeakPointers(obj)"})
    protected static final Object doAbstractPointers(final AbstractPointersObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doPointers(final ContextObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doWeakPointers(final WeakPointersObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization(guards = {"!isFloat(obj)"})
    protected static final Object doNative(final NativeObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doFloat(final FloatObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization(guards = {"!isBlock(obj)"})
    protected static final Object doCode(final CompiledCodeObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doBlock(final CompiledBlockObject obj, final long index) {
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

}

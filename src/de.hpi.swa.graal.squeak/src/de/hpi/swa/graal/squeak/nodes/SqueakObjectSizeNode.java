package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;

public abstract class SqueakObjectSizeNode extends AbstractBaseSqueakObjectNode {

    public static SqueakObjectSizeNode create() {
        return SqueakObjectSizeNodeGen.create();
    }

    public abstract int execute(AbstractSqueakObject obj);

    @Specialization
    protected static final int doAbstractPointersObject(final AbstractPointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doCode(final CompiledCodeObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doEmpty(@SuppressWarnings("unused") final EmptyObject obj) {
        return 0;
    }

    @Specialization(guards = {"!isFloat(obj)"})
    protected static final int doNative(final NativeObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return 0;
    }

}

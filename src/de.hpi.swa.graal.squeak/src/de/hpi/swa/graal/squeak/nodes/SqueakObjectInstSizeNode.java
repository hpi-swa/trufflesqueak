package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;

public abstract class SqueakObjectInstSizeNode extends AbstractBaseSqueakObjectNode {

    public static SqueakObjectInstSizeNode create() {
        return SqueakObjectInstSizeNodeGen.create();
    }

    public abstract int execute(AbstractSqueakObject obj);

    @Specialization
    protected static final int doAbstractPointersObject(final AbstractPointersObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doClosure(@SuppressWarnings("unused") final BlockClosureObject obj) {
        return BlockClosureObject.instsize();
    }

    @Specialization
    protected static final int doCode(@SuppressWarnings("unused") final CompiledCodeObject obj) {
        return 0;
    }

    @Specialization
    protected static final int doEmpty(@SuppressWarnings("unused") final EmptyObject obj) {
        return 0;
    }

    @Specialization
    protected static final int doNative(@SuppressWarnings("unused") final NativeObject obj) {
        return 0;
    }

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return 0;
    }

}

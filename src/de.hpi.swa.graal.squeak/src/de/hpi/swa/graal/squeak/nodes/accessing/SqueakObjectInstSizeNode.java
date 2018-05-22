package de.hpi.swa.graal.squeak.nodes.accessing;

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

public abstract class SqueakObjectInstSizeNode extends Node {

    public static SqueakObjectInstSizeNode create() {
        return SqueakObjectInstSizeNodeGen.create();
    }

    public abstract int execute(AbstractSqueakObject obj);

    @Specialization
    protected static final int doPointers(final PointersObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doWeakPointers(final WeakPointersObject obj) {
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
    protected static final int doFloat(@SuppressWarnings("unused") final FloatObject obj) {
        return 0;
    }

    @Specialization
    protected static final int doLarge(@SuppressWarnings("unused") final LargeIntegerObject obj) {
        return 0;
    }

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return 0;
    }

}

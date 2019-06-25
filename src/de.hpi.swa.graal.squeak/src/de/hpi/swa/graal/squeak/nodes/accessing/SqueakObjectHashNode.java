package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

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
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@GenerateUncached
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectHashNode extends AbstractNode {

    public static SqueakObjectHashNode create() {
        return SqueakObjectHashNodeGen.create();
    }

    public abstract long execute(Object obj);

    @Specialization
    protected static final long doNil(@SuppressWarnings("unused") final NilObject receiver) {
        return NilObject.getSqueakHash();
    }

    @Specialization(guards = "obj == FALSE")
    protected static final long doBooleanFalse(@SuppressWarnings("unused") final boolean obj) {
        return 2L;
    }

    @Specialization(guards = "obj != FALSE")
    protected static final long doBooleanTrue(@SuppressWarnings("unused") final boolean obj) {
        return 3L;
    }

    @Specialization
    protected static final long doLong(final long obj) {
        return obj;
    }

    @Specialization
    protected static final long doArray(final ArrayObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doClass(final ClassObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doContext(final ContextObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doPointers(final PointersObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doWeakPointers(final WeakPointersObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doClosure(final BlockClosureObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doMethod(final CompiledMethodObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doBlock(final CompiledBlockObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doEmpty(final EmptyObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doNative(final NativeObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doFloat(final FloatObject obj) {
        return obj.getSqueakHash();
    }

    @Specialization
    protected static final long doLargeInteger(final LargeIntegerObject obj) {
        return obj.getSqueakHash();
    }
}

package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class SqueakObjectBecomeNode extends AbstractNode {

    public static SqueakObjectBecomeNode create() {
        return SqueakObjectBecomeNodeGen.create();
    }

    public abstract boolean execute(Object left, Object right);

    @SuppressWarnings("unused")
    @Specialization(guards = {"left == right"})
    protected static final boolean doSameObject(final AbstractSqueakObject left, final AbstractSqueakObject right) {
        return false;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doClosure(final BlockClosureObject left, final BlockClosureObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doClass(final ClassObject left, final ClassObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doCode(final CompiledCodeObject left, final CompiledCodeObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doContext(final ContextObject left, final ContextObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doEmpty(final EmptyObject left, final EmptyObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doNative(final NativeObject left, final NativeObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doArray(final ArrayObject left, final ArrayObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doPointers(final PointersObject left, final PointersObject right) {
        left.become(right);
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"left != right"})
    protected static final boolean doWeakPointers(final WeakPointersObject left, final WeakPointersObject right) {
        // TODO: implement or remove?
        throw SqueakException.create("become not implemented for WeakPointersObjects");
    }
}

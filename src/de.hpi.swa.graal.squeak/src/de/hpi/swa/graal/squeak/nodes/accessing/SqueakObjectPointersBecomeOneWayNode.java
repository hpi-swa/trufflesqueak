package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectPointersBecomeOneWayNode extends Node {

    public static SqueakObjectPointersBecomeOneWayNode create() {
        return SqueakObjectPointersBecomeOneWayNodeGen.create();
    }

    public abstract void execute(Object obj, Object[] from, Object[] to, boolean copyHash);

    @Specialization
    protected static final void doClosure(final BlockClosureObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @Specialization
    protected static final void doClass(final ClassObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @Specialization
    protected static final void doMethod(final CompiledMethodObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(from, to, copyHash);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFallback(final Object obj, final Object[] from, final Object[] to, final boolean copyHash) {
        // nothing to do
    }
}

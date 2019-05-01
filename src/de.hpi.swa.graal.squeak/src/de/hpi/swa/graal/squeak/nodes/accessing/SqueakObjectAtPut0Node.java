package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.BlockClosureObjectNodes.BlockClosureObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ClassObjectNodes.ClassObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.WeakPointersObjectNodes.WeakPointersObjectWriteNode;

@GenerateUncached
@ImportStatic(NativeObject.class)
public abstract class SqueakObjectAtPut0Node extends AbstractNode {

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(Object obj, long index, Object value);

    @Specialization
    protected static final void doNative(final NativeObject obj, final long index, final Object value,
                    @Cached final NativeObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doArray(final ArrayObject obj, final long index, final Object value,
                    @Cached final ArrayObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final long index, final Object value,
                    @Cached final WeakPointersObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doClass(final ClassObject obj, final long index, final Object value,
                    @Cached final ClassObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doCode(final CompiledCodeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClosure(final BlockClosureObject obj, final long index, final Object value,
                    @Cached final BlockClosureObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final long index, final Object value,
                    @Cached final ContextObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization(guards = {"index == 0", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doFloatHigh(final FloatObject obj, @SuppressWarnings("unused") final long index, final long value) {
        obj.setHigh(value);
    }

    @Specialization(guards = {"index == 1", "value >= 0", "value <= INTEGER_MAX"})
    protected static final void doFloatLow(final FloatObject obj, @SuppressWarnings("unused") final long index, final long value) {
        obj.setLow(value);
    }
}

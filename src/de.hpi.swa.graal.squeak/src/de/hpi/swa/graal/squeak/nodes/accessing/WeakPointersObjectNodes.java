package de.hpi.swa.graal.squeak.nodes.accessing;

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.WeakPointersObjectNodesFactory.WeakPointersObjectWriteNodeGen;

public final class WeakPointersObjectNodes {

    @GenerateUncached
    public abstract static class WeakPointersObjectReadNode extends AbstractNode {

        public final Object executeRead(final WeakPointersObject pointers, final long index) {
            return execute(pointers.getPointer((int) index));
        }

        protected abstract Object execute(Object value);

        @Specialization
        protected static final Object doWeakReference(final WeakReference<?> value) {
            return NilObject.nullToNil(value.get());
        }

        @Fallback
        protected static final Object doOther(final Object value) {
            return value;
        }
    }

    @GenerateUncached
    public abstract static class WeakPointersObjectWriteNode extends AbstractNode {

        public static WeakPointersObjectWriteNode getUncached() {
            return WeakPointersObjectWriteNodeGen.getUncached();
        }

        public abstract void execute(WeakPointersObject pointers, long index, Object value);

        @Specialization(guards = "pointers.getSqueakClass().getBasicInstanceSize() <= index")
        protected static final void doWeakInVariablePart(final WeakPointersObject pointers, final long index, final AbstractSqueakObject value) {
            pointers.setWeakPointer((int) index, value);
        }

        @Fallback
        protected static final void doNonWeak(final WeakPointersObject pointers, final long index, final Object value) {
            pointers.setPointer((int) index, value);
        }
    }
}

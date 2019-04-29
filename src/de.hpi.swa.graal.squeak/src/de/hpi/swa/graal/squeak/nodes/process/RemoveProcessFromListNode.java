package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class RemoveProcessFromListNode extends AbstractNode {

    public final void executeRemove(final PointersObject process, final PointersObject list) {
        final Object first = list.at0(LINKED_LIST.FIRST_LINK);
        final Object last = list.at0(LINKED_LIST.LAST_LINK);
        executeRemove(process, list, first, last);
        process.atputNil0(PROCESS.NEXT_LINK);
    }

    protected abstract void executeRemove(PointersObject process, PointersObject list, Object first, Object last);

    @Specialization(guards = "process == first")
    protected static final void doRemoveEqual(final PointersObject process, final PointersObject list, @SuppressWarnings("unused") final PointersObject first,
                    final AbstractSqueakObject last) {
        final Object next = process.at0(PROCESS.NEXT_LINK);
        list.atput0(LINKED_LIST.FIRST_LINK, next);
        if (process == last) {
            list.atputNil0(LINKED_LIST.LAST_LINK);
        }
    }

    @Specialization(guards = "process != first")
    protected static final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final PointersObject first, final AbstractSqueakObject last) {
        PointersObject temp = first;
        Object next;
        while (true) {
            next = temp.at0(PROCESS.NEXT_LINK);
            if (next == process) {
                break;
            } else if (next == NilObject.SINGLETON) {
                throw new PrimitiveFailed(); // TODO: make this better.
            } else {
                temp = (PointersObject) next;
            }
        }
        next = process.at0(PROCESS.NEXT_LINK);
        temp.atput0(PROCESS.NEXT_LINK, next);
        if (process == last) {
            list.atput0(LINKED_LIST.LAST_LINK, temp);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final NilObject first, final AbstractSqueakObject last) {
        throw new PrimitiveFailed(); // TODO: make sure this is needed (and make it better).
    }
}

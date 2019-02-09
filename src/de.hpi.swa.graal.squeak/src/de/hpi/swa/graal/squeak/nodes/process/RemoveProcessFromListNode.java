package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class RemoveProcessFromListNode extends AbstractNodeWithImage {
    protected RemoveProcessFromListNode(final SqueakImageContext image) {
        super(image);
    }

    public static RemoveProcessFromListNode create(final SqueakImageContext image) {
        return RemoveProcessFromListNodeGen.create(image);
    }

    public final void executeRemove(final PointersObject process, final PointersObject list) {
        final Object first = list.at0(LINKED_LIST.FIRST_LINK);
        final Object last = list.at0(LINKED_LIST.LAST_LINK);
        executeRemove(process, list, first, last);
        process.atput0(PROCESS.NEXT_LINK, image.nil);
    }

    protected abstract void executeRemove(PointersObject process, PointersObject list, Object first, Object last);

    @Specialization(guards = "identical(process, first)")
    protected final void doRemoveEqual(final PointersObject process, final PointersObject list, @SuppressWarnings("unused") final AbstractSqueakObject first,
                    final AbstractSqueakObject last) {
        final Object next = process.at0(PROCESS.NEXT_LINK);
        list.atput0(LINKED_LIST.FIRST_LINK, next);
        if (process == last) {
            list.atput0(LINKED_LIST.LAST_LINK, image.nil);
        }
    }

    @Specialization(guards = "!identical(process, first)")
    protected final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final PointersObject first, final AbstractSqueakObject last) {
        PointersObject temp = first;
        Object next;
        while (true) {
            next = temp.at0(PROCESS.NEXT_LINK);
            if (next == process) {
                break;
            } else if (next == image.nil) {
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
    @Specialization(guards = "!identical(process, first)")
    protected static final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final NilObject first, final AbstractSqueakObject last) {
        throw new PrimitiveFailed(); // TODO: make sure this is needed (and make it better).
    }

    protected static final boolean identical(final PointersObject process, final AbstractSqueakObject first) {
        return process == first;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFail(final PointersObject process, final PointersObject list, final Object first, final Object last) {
        throw SqueakException.create("Should never happen");
    }
}

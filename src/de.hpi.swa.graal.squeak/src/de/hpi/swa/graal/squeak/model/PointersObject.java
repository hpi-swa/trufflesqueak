package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class PointersObject extends AbstractPointersObject {

    public PointersObject(final SqueakImageContext image) {
        super(image); // for special PointersObjects only
    }

    public PointersObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash, klass);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject sqClass, final Object[] pointers) {
        super(image, sqClass);
        setPointersUnsafe(pointers);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        this(image, classObject, ArrayUtils.withAll(size, image.nil));
    }

    private PointersObject(final PointersObject original) {
        super(original.image, original.getSqueakClass());
        setPointersUnsafe(original.getPointers().clone());
    }

    public Object at0(final long i) {
        return getPointer((int) i);
    }

    public void atput0(final long i, final Object obj) {
        assert obj != null; // null indicates a problem
        setPointer((int) i, obj);
    }

    public void become(final PointersObject other) {
        becomeOtherClass(other);
        final Object[] otherPointers = other.getPointers();
        other.setPointers(getPointers());
        setPointers(otherPointers);
    }

    public boolean isActiveProcess() {
        return this == image.getActiveProcess();
    }

    public boolean isEmptyList() {
        return at0(LINKED_LIST.FIRST_LINK) == image.nil;
    }

    public boolean isDisplay() {
        return this == image.specialObjectsArray.at0Object(SPECIAL_OBJECT.THE_DISPLAY);
    }

    public boolean isPoint() {
        return getSqueakClass() == image.pointClass;
    }

    public PointersObject removeFirstLinkOfList() {
        // Remove the first process from the given linked list.
        final PointersObject first = (PointersObject) at0(LINKED_LIST.FIRST_LINK);
        final Object last = at0(LINKED_LIST.LAST_LINK);
        if (first == last) {
            atput0(LINKED_LIST.FIRST_LINK, image.nil);
            atput0(LINKED_LIST.LAST_LINK, image.nil);
        } else {
            atput0(LINKED_LIST.FIRST_LINK, first.at0(PROCESS.NEXT_LINK));
        }
        first.atput0(PROCESS.NEXT_LINK, image.nil);
        return first;
    }

    public PointersObject shallowCopy() {
        return new PointersObject(this);
    }
}

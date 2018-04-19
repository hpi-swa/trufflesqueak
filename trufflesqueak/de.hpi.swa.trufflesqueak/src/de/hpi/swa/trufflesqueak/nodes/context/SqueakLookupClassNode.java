package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public abstract class SqueakLookupClassNode extends AbstractNodeWithImage {
    public static SqueakLookupClassNode create(final SqueakImageContext image) {
        return SqueakLookupClassNodeGen.create(image);
    }

    protected SqueakLookupClassNode(final SqueakImageContext image) {
        super(image);
    }

    public abstract ClassObject executeLookup(Object receiver);

    @Specialization(guards = "object == image.sqTrue")
    protected final ClassObject doTrue(@SuppressWarnings("unused") final boolean object) {
        return image.trueClass;
    }

    @Specialization(guards = "object != image.sqTrue")
    protected final ClassObject doFalse(@SuppressWarnings("unused") final boolean object) {
        return image.falseClass;
    }

    @Specialization(guards = "isLargeNegative(value)")
    protected final ClassObject doLargeNegative(@SuppressWarnings("unused") final long value) {
        return image.largeNegativeIntegerClass;
    }

    @Specialization(guards = "isLargePositive(value)")
    protected final ClassObject doLargePositive(@SuppressWarnings("unused") final long value) {
        return image.largePositiveIntegerClass;
    }

    @Specialization(guards = {"!isLargePositive(value)", "!isLargeNegative(value)"})
    protected final ClassObject doSmallInteger(@SuppressWarnings("unused") final long value) {
        return image.smallIntegerClass;
    }

    @Specialization
    protected final ClassObject squeakClass(@SuppressWarnings("unused") final char object) {
        return image.characterClass;
    }

    @Specialization
    protected final ClassObject squeakClass(@SuppressWarnings("unused") final double object) {
        return image.floatClass;
    }

    @Specialization
    protected final ClassObject squeakClass(@SuppressWarnings("unused") final BlockClosureObject ch) {
        return image.blockClosureClass;
    }

    @Specialization
    protected final ClassObject squeakClass(@SuppressWarnings("unused") final ContextObject ch) {
        return image.methodContextClass;
    }

    @Specialization
    protected final ClassObject nilClass(@SuppressWarnings("unused") final NilObject object) {
        return image.nilClass;
    }

    @Specialization
    protected static final ClassObject squeakClass(final BaseSqueakObject object) {
        return object.getSqClass();
    }

    protected static final boolean isLargeNegative(final long value) {
        return value < LargeIntegerObject.SMALLINTEGER32_MIN;
    }

    protected static final boolean isLargePositive(final long value) {
        return value > LargeIntegerObject.SMALLINTEGER32_MAX;
    }
}

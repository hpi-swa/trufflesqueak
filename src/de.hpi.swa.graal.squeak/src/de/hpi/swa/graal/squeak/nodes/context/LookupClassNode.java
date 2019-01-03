package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class LookupClassNode extends AbstractNodeWithImage {
    public static LookupClassNode create(final SqueakImageContext image) {
        return LookupClassNodeGen.create(image);
    }

    protected LookupClassNode(final SqueakImageContext image) {
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
    protected final ClassObject doChar(@SuppressWarnings("unused") final char value) {
        return image.characterClass;
    }

    @Specialization
    protected final ClassObject doChar(@SuppressWarnings("unused") final CharacterObject value) {
        return image.characterClass;
    }

    @Specialization
    protected final ClassObject doDouble(@SuppressWarnings("unused") final double value) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doSqueakObject(final AbstractSqueakObject value) {
        return value.getSqueakClass();
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)"})
    protected final ClassObject doTruffleObject(@SuppressWarnings("unused") final TruffleObject value) {
        return image.truffleObjectClass;
    }

    @Fallback
    protected static final ClassObject doFail(final Object value) {
        throw new SqueakException("Unexpected value: " + value);
    }

    protected final boolean isLargeNegative(final long value) {
        if (image.flags.is64bit()) {
            return value < LargeIntegerObject.SMALLINTEGER64_MIN;
        } else {
            return value < LargeIntegerObject.SMALLINTEGER32_MIN;
        }
    }

    protected final boolean isLargePositive(final long value) {
        if (image.flags.is64bit()) {
            return value > LargeIntegerObject.SMALLINTEGER64_MAX;
        } else {
            return value > LargeIntegerObject.SMALLINTEGER32_MAX;
        }
    }
}

package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;

public abstract class LookupClassNode extends AbstractNodeWithImage implements LookupClassNodeInterface {
    protected LookupClassNode(final SqueakImageContext image) {
        super(image);
    }

    public static LookupClassNode create(final SqueakImageContext image) {
        return LookupClassNodeGen.create(image);
    }

    @Override
    public abstract ClassObject executeLookup(Object receiver);

    @Specialization(guards = "value == image.sqTrue")
    protected final ClassObject doTrue(@SuppressWarnings("unused") final boolean value) {
        return image.trueClass;
    }

    @Specialization(guards = "value != image.sqTrue")
    protected final ClassObject doFalse(@SuppressWarnings("unused") final boolean value) {
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
    protected final ClassObject doDouble(@SuppressWarnings("unused") final double value) {
        assert image.flags.is64bit() : "Unexpected double value. 32-bit image does not support immediate floats.";
        return image.smallFloatClass;
    }

    @Specialization
    protected static final ClassObject doAbstractPointers(final AbstractPointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doArray(final ArrayObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected final ClassObject doClosure(@SuppressWarnings("unused") final BlockClosureObject value) {
        return image.blockClosureClass;
    }

    @Specialization
    protected final ClassObject doCharacter(@SuppressWarnings("unused") final CharacterObject value) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doClass(@SuppressWarnings("unused") final ClassObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doBlock(@SuppressWarnings("unused") final CompiledBlockObject value) {
        throw SqueakException.create("Should never happen?");
    }

    @Specialization
    protected final ClassObject doMethod(@SuppressWarnings("unused") final CompiledMethodObject value) {
        return image.compiledMethodClass;
    }

    @Specialization
    protected final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value) {
        return image.methodContextClass;
    }

    @Specialization
    protected static final ClassObject doEmpty(final EmptyObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doLargeInteger(final LargeIntegerObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doNative(final NativeObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected final ClassObject doNil(@SuppressWarnings("unused") final NilObject value) {
        return image.nilClass;
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)"})
    protected final ClassObject doTruffleObject(@SuppressWarnings("unused") final TruffleObject value) {
        assert image.supportsTruffleObject();
        return image.truffleObjectClass;
    }

    @Fallback
    protected static final ClassObject doFail(final Object value) {
        throw SqueakException.create("Unexpected value: " + value);
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

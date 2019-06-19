package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@GenerateUncached
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectClassNode extends AbstractNode {
    public static SqueakObjectClassNode create() {
        return SqueakObjectClassNodeGen.create();
    }

    public static SqueakObjectClassNode getUncached() {
        return SqueakObjectClassNodeGen.getUncached();
    }

    public abstract ClassObject executeLookup(SqueakImageContext image, Object receiver);

    @Specialization
    protected static final ClassObject doNil(final SqueakImageContext image, @SuppressWarnings("unused") final NilObject value) {
        return image.nilClass;
    }

    @Specialization(guards = "value == TRUE")
    protected static final ClassObject doTrue(final SqueakImageContext image, @SuppressWarnings("unused") final boolean value) {
        return image.trueClass;
    }

    @Specialization(guards = "value != TRUE")
    protected static final ClassObject doFalse(final SqueakImageContext image, @SuppressWarnings("unused") final boolean value) {
        return image.falseClass;
    }

    @Specialization
    protected static final ClassObject doSmallInteger(final SqueakImageContext image, @SuppressWarnings("unused") final long value) {
        return image.smallIntegerClass;
    }

    @Specialization
    protected static final ClassObject doChar(final SqueakImageContext image, @SuppressWarnings("unused") final char value) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doDouble(final SqueakImageContext image, @SuppressWarnings("unused") final double value) {
        return image.smallFloatClass;
    }

    @Specialization
    protected static final ClassObject doPointers(@SuppressWarnings("unused") final SqueakImageContext image, final PointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doWeakPointers(@SuppressWarnings("unused") final SqueakImageContext image, final WeakPointersObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doArray(@SuppressWarnings("unused") final SqueakImageContext image, final ArrayObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doClosure(final SqueakImageContext image, @SuppressWarnings("unused") final BlockClosureObject value) {
        return image.blockClosureClass;
    }

    @Specialization
    protected static final ClassObject doCharacter(final SqueakImageContext image, @SuppressWarnings("unused") final CharacterObject value) {
        return image.characterClass;
    }

    @Specialization
    protected static final ClassObject doClass(@SuppressWarnings("unused") final SqueakImageContext image, final ClassObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doMethod(final SqueakImageContext image, @SuppressWarnings("unused") final CompiledMethodObject value) {
        return image.compiledMethodClass;
    }

    @Specialization
    protected static final ClassObject doContext(final SqueakImageContext image, @SuppressWarnings("unused") final ContextObject value) {
        return image.methodContextClass;
    }

    @Specialization
    protected static final ClassObject doEmpty(@SuppressWarnings("unused") final SqueakImageContext image, final EmptyObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doFloat(final SqueakImageContext image, @SuppressWarnings("unused") final FloatObject value) {
        return image.floatClass;
    }

    @Specialization
    protected static final ClassObject doLargeInteger(@SuppressWarnings("unused") final SqueakImageContext image, final LargeIntegerObject value) {
        return value.getSqueakClass();
    }

    @Specialization
    protected static final ClassObject doNative(@SuppressWarnings("unused") final SqueakImageContext image, final NativeObject value) {
        return value.getSqueakClass();
    }

    @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
    protected static final ClassObject doTruffleObject(final SqueakImageContext image, @SuppressWarnings("unused") final Object value) {
        assert image.supportsTruffleObject();
        return image.truffleObjectClass;
    }
}

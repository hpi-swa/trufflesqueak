package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class NewObjectNode extends AbstractNodeWithImage {

    protected NewObjectNode(final SqueakImageContext image) {
        super(image);
    }

    public static NewObjectNode create(final SqueakImageContext image) {
        return NewObjectNodeGen.create(image);
    }

    public final Object execute(final ClassObject classObject) {
        return execute(classObject, 0);
    }

    public final Object execute(final ClassObject classObject, final int extraSize) {
        image.reportNewAllocationRequest();
        return image.reportNewAllocationResult(executeAllocation(classObject, extraSize));
    }

    protected abstract Object executeAllocation(ClassObject classObject, int extraSize);

    @Specialization(guards = "classObject.isZeroSized()")
    protected final Object doEmpty(final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        return new EmptyObject(image, classObject);
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "classObject.instancesAreClasses()"})
    protected final Object doClass(final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new ClassObject(image, classObject, classObject.getBasicInstanceSize());
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "!classObject.instancesAreClasses()"})
    protected final Object doClassPointers(final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new PointersObject(image, classObject, classObject.getBasicInstanceSize());
    }

    @Specialization(guards = "classObject.isIndexableWithNoInstVars()")
    protected final Object doIndexedPointers(final ClassObject classObject, final int extraSize) {
        if (ArrayObject.ENABLE_STORAGE_STRATEGIES) {
            return ArrayObject.createEmptyStrategy(image, classObject, classObject.getBasicInstanceSize() + extraSize);
        } else {
            return ArrayObject.createObjectStrategy(image, classObject, classObject.getBasicInstanceSize() + extraSize);
        }
    }

    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "classObject.isMethodContextClass()"})
    protected final Object doContext(final ClassObject classObject, final int extraSize) {
        return ContextObject.create(image, classObject.getBasicInstanceSize() + extraSize);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "classObject.isBlockClosureClass()"})
    protected final Object doBlockClosure(final ClassObject classObject, final int extraSize) {
        return new BlockClosureObject(image, extraSize);
    }

    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "!classObject.isMethodContextClass()", "!classObject.isBlockClosureClass()"})
    protected final Object doPointers(final ClassObject classObject, final int extraSize) {
        return new PointersObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "classObject.isWeak()")
    protected final Object doWeakPointers(final ClassObject classObject, final int extraSize) {
        return new WeakPointersObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "classObject.isEphemeronClassType()")
    protected final Object doEphemerons(final ClassObject classObject, final int extraSize) {
        return doWeakPointers(classObject, extraSize); // TODO: ephemerons
    }

    @Specialization(guards = "classObject.isLongs()")
    protected final Object doNativeLongs(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeLongs(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"classObject.isWords()", "classObject.isFloatClass()"})
    protected final Object doFloat(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() + extraSize == 2;
        return new FloatObject(image);
    }

    @Specialization(guards = {"classObject.isWords()", "!classObject.isFloatClass()"})
    protected final Object doNativeInts(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeInts(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "classObject.isShorts()")
    protected final Object doNativeShorts(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeShorts(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"classObject.isBytes()", "classObject.isLargePositiveIntegerClass() || classObject.isLargeNegativeIntegerClass()"})
    protected final Object doLargeIntegers(final ClassObject classObject, final int extraSize) {
        return new LargeIntegerObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"classObject.isBytes()", "!classObject.isLargePositiveIntegerClass()", "!classObject.isLargeNegativeIntegerClass()"})
    protected final Object doNativeBytes(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeBytes(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"classObject.isCompiledMethodClassType()"})
    protected final Object doCompiledMethod(final ClassObject classObject, final int extraSize) {
        return CompiledMethodObject.newOfSize(image, classObject.getBasicInstanceSize() + extraSize);
    }

    @Fallback
    protected static final Object doFail(final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        throw SqueakException.create("Cannot instantiate class:", classObject, "(Spec: " + classObject.getInstanceSpecification() + ")");
    }
}

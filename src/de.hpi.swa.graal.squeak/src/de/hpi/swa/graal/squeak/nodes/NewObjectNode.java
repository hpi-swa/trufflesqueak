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

    public static NewObjectNode create(final SqueakImageContext image) {
        return NewObjectNodeGen.create(image);
    }

    protected NewObjectNode(final SqueakImageContext image) {
        super(image);
    }

    public final Object executeNew(final ClassObject classObject) {
        return executeNew(classObject, 0);
    }

    public abstract Object executeNew(ClassObject classObject, int extraSize);

    @Specialization(guards = "classObject.getInstanceSpecification() == 0")
    protected final Object doEmpty(final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        return new EmptyObject(image, classObject);
    }

    @Specialization(guards = {"classObject.getInstanceSpecification() == 1", "classObject.instancesAreClasses()"})
    protected final Object doClass(final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new ClassObject(image, classObject, classObject.getBasicInstanceSize());
    }

    @Specialization(guards = {"classObject.getInstanceSpecification() == 1", "!classObject.instancesAreClasses()"})
    protected final Object doClassPointers(final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new PointersObject(image, classObject, classObject.getBasicInstanceSize());
    }

    @Specialization(guards = "classObject.getInstanceSpecification() == 2")
    protected final Object doIndexedPointers(final ClassObject classObject, final int extraSize) {
        return new ArrayObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"classObject.getInstanceSpecification() == 3", "classObject == image.methodContextClass"})
    protected final Object doContext(final ClassObject classObject, final int extraSize) {
        return ContextObject.create(image, classObject.getBasicInstanceSize() + extraSize);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObject.getInstanceSpecification() == 3", "classObject == image.blockClosureClass"})
    protected final Object doBlockClosure(final ClassObject classObject, final int extraSize) {
        return new BlockClosureObject(image); // TODO: verify this is actually used
    }

    @Specialization(guards = {"classObject.getInstanceSpecification() == 3", "classObject != image.methodContextClass", "classObject != image.blockClosureClass"})
    protected final Object doPointers(final ClassObject classObject, final int extraSize) {
        return new PointersObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "classObject.getInstanceSpecification() == 4")
    protected final Object doWeakPointers(final ClassObject classObject, final int extraSize) {
        return new WeakPointersObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "classObject.getInstanceSpecification() == 5")
    protected final Object doEphemerons(final ClassObject classObject, final int extraSize) {
        return doWeakPointers(classObject, extraSize); // TODO: ephemerons
    }

    @Specialization(guards = "classObject.getInstanceSpecification() == 9")
    protected final Object doNativeLongs(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeLongs(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"between(classObject.getInstanceSpecification(), 10, 11)", "classObject == image.floatClass"})
    protected final Object doFloat(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() + extraSize == 2;
        return new FloatObject(image);
    }

    @Specialization(guards = {"between(classObject.getInstanceSpecification(), 10, 11)", "classObject != image.floatClass"})
    protected final Object doNativeInts(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeInts(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = "between(classObject.getInstanceSpecification(), 12, 15)")
    protected final Object doNativeShorts(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeShorts(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"between(classObject.getInstanceSpecification(), 16, 23)", "classObject == image.largePositiveIntegerClass || classObject == image.largeNegativeIntegerClass"})
    protected final Object doLargeIntegers(final ClassObject classObject, final int extraSize) {
        return new LargeIntegerObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"between(classObject.getInstanceSpecification(), 16, 23)", "classObject != image.largePositiveIntegerClass", "classObject != image.largeNegativeIntegerClass"})
    protected final Object doNativeBytes(final ClassObject classObject, final int extraSize) {
        return NativeObject.newNativeBytes(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Specialization(guards = {"between(classObject.getInstanceSpecification(), 24, 31)"})
    protected final Object doCompiledMethod(final ClassObject classObject, final int extraSize) {
        return new CompiledMethodObject(image, classObject, classObject.getBasicInstanceSize() + extraSize);
    }

    @Fallback
    protected static final Object doFail(final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        throw new SqueakException("Cannot instantiate class:", classObject, "(Spec: " + classObject.getInstanceSpecification() + ")");
    }

    protected static final boolean between(final int value, final int start, final int stop) {
        return start <= value && value <= stop;
    }
}

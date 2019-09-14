package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.LookupMethodByStringNode;
import de.hpi.swa.graal.squeak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObjectWithImage {
    private ClassObject squeakClass;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image) {
        super(image);
        squeakClass = null;
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final ClassObject klass) {
        super(image);
        squeakClass = klass;
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final int hash) {
        super(image, hash);
        // TODO: Generate new hash if `0`. This might have something to do with compact classes?
        squeakClass = null;
    }

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash);
        squeakClass = klass;
    }

    public final void becomeOtherClass(final AbstractSqueakObjectWithClassAndHash other) {
        final ClassObject otherSqClass = other.squeakClass;
        other.setSqueakClass(squeakClass);
        setSqueakClass(otherSqClass);
    }

    @Override
    public final ClassObject getSqueakClass(final SqueakImageContext ignored) {
        return squeakClass;
    }

    public final ClassObject getSqueakClass() {
        return squeakClass;
    }

    public final String getSqueakClassName() {
        if (this instanceof ClassObject) {
            return getClassName() + " class";
        } else {
            return getSqueakClass().getClassName();
        }
    }

    public final boolean isMetaClass() {
        return this == image.metaClass;
    }

    @Override
    public final void setSqueakClass(final ClassObject newClass) {
        squeakClass = newClass;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName();
    }

    public final Object send(final String selector, final Object... arguments) {
        CompilerAsserts.neverPartOfCompilation("For testing or instrumentation only.");
        final Object methodObject = LookupMethodByStringNode.getUncached().executeLookup(getSqueakClass(), selector);
        if (methodObject instanceof CompiledMethodObject) {
            return DispatchUneagerlyNode.getUncached().executeDispatch((CompiledMethodObject) methodObject, ArrayUtils.copyWithFirst(arguments, this), NilObject.SINGLETON);
        } else {
            throw SqueakExceptions.SqueakException.create("CompiledMethodObject expected, got: " + methodObject);
        }
    }
}

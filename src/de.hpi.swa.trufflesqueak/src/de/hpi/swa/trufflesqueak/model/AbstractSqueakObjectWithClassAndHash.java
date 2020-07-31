/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public abstract class AbstractSqueakObjectWithClassAndHash extends AbstractSqueakObjectWithHash {
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

    protected AbstractSqueakObjectWithClassAndHash(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash);
        squeakClass = klass;
    }

    public AbstractSqueakObjectWithClassAndHash(final AbstractSqueakObjectWithClassAndHash original) {
        super(original);
        squeakClass = original.squeakClass;
    }

    public final void becomeOtherClass(final AbstractSqueakObjectWithClassAndHash other) {
        final ClassObject otherSqClass = other.squeakClass;
        other.setSqueakClass(squeakClass);
        setSqueakClass(otherSqClass);
    }

    @Override
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
        assert !(this instanceof AbstractPointersObject) || getSqueakClass() == null ||
                        ((AbstractPointersObject) this).getLayout().getSqueakClass() == newClass : "Layout should be migrated before class change";
        squeakClass = newClass;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode());
    }

    /**
     * Utility method to dispatch messages internally. The {@code selector} must resolve to a
     * {@link CompiledCodeObject}. Note that the priority of the current process is temporarily
     * raised to prevent the scheduler to switch control to another process.
     */
    @TruffleBoundary
    public final Object send(final String selector, final Object... arguments) {
        final Object methodObject = LookupMethodByStringNode.getUncached().executeLookup(getSqueakClass(), selector);
        final PointersObject activeProcess = (PointersObject) image.getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.ACTIVE_PROCESS);
        if (methodObject instanceof CompiledCodeObject) {
            final long currentPriority = (long) activeProcess.instVarAt0Slow(PROCESS.PRIORITY);
            activeProcess.instVarAtPut0Slow(PROCESS.PRIORITY, PROCESS_SCHEDULER.HIGHEST_PRIORITY);
            try {
                return DispatchUneagerlyNode.getUncached().executeDispatch((CompiledCodeObject) methodObject, ArrayUtils.copyWithFirst(arguments, this), NilObject.SINGLETON);
            } catch (final ProcessSwitch ps) {
                throw SqueakException.create("Unexpected process switch detected during internal send");
            } finally {
                activeProcess.instVarAtPut0Slow(PROCESS.PRIORITY, currentPriority);
            }
        } else {
            throw SqueakException.create("CompiledMethodObject expected, got:", methodObject);
        }
    }
}

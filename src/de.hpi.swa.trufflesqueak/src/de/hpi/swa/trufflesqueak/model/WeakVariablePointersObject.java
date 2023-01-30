/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class WeakVariablePointersObject extends AbstractVariablePointersObject {
    private ReferenceQueue<AbstractSqueakObject> weakPointersQueue;

    public WeakVariablePointersObject(final SqueakImageContext image, final long header, final ClassObject classObject) {
        super(header, classObject);
        weakPointersQueue = image.weakPointersQueue;
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout, variableSize);
        weakPointersQueue = image.weakPointersQueue;
    }

    private WeakVariablePointersObject(final WeakVariablePointersObject original) {
        super(original);
        weakPointersQueue = original.weakPointersQueue;
    }

    @Override
    protected void fillInVariablePart(final Object[] pointers, final int instSize) {
        super.fillInVariablePart(pointers, instSize);
        for (int i = 0; i < variablePart.length; i++) {
            final Object value = variablePart[i];
            if (value instanceof final AbstractSqueakObject o) {
                variablePart[i] = new WeakRef(o, weakPointersQueue);
            }
        }
    }

    public void become(final WeakVariablePointersObject other) {
        super.become(other);
        final ReferenceQueue<AbstractSqueakObject> otherWeakPointersQueue = other.weakPointersQueue;
        other.weakPointersQueue = weakPointersQueue;
        weakPointersQueue = otherWeakPointersQueue;
    }

    @Override
    public Object getFromVariablePart(final long index) {
        final Object value = super.getFromVariablePart(index);
        if (value instanceof final WeakRef o) {
            return NilObject.nullToNil(o.get());
        } else {
            assert value != null;
            return value;
        }
    }

    public Object getFromVariablePart(final long index, final ConditionProfile weakRefProfile) {
        final Object value = super.getFromVariablePart(index);
        if (weakRefProfile.profile(value instanceof WeakRef)) {
            return NilObject.nullToNil(((WeakRef) value).get());
        } else {
            assert value != null;
            return value;
        }
    }

    @Override
    public void putIntoVariablePart(final long index, final Object value) {
        putIntoVariablePart(index, value, ConditionProfile.getUncached());
    }

    public void putIntoVariablePart(final long index, final Object value, final ConditionProfile profile) {
        super.putIntoVariablePart(index, profile.profile(value instanceof AbstractSqueakObject) ? new WeakRef((AbstractSqueakObject) value, weakPointersQueue) : value);
    }

    @Override
    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        return layoutValuesPointTo(identityNode, thang) || variablePartPointsTo(thang);
    }

    private boolean variablePartPointsTo(final Object thang) {
        for (final Object value : variablePart) {
            if (value == thang || value instanceof final WeakRef o && o.get() == thang) {
                return true;
            }
        }
        return false;
    }

    public WeakVariablePointersObject shallowCopy() {
        return new WeakVariablePointersObject(this);
    }

    @Override
    protected void traceVariablePart(final ObjectTracer tracer) {
        /* Weak pointers excluded from tracing. */
    }

    @Override
    protected void traceVariablePart(final SqueakImageWriter tracer) {
        /* Weak pointers excluded from tracing. */
    }

    @Override
    protected void writeVariablePart(final SqueakImageWriter writer) {
        for (long i = 0; i < variablePart.length; i++) {
            /*
             * Since weak pointers are excluded from tracing, ignore (replace with nil) all objects
             * that have not been traced somewhere else.
             */
            writer.writeObjectIfTracedElseNil(getFromVariablePart(i));
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode()) + " of size " + variablePart.length;
    }

    /*
     * Final WeakReference subclass with boundaries because its methods should not be called on the
     * fast-path.
     */
    private static final class WeakRef extends WeakReference<AbstractSqueakObject> {
        @TruffleBoundary
        private WeakRef(final AbstractSqueakObject referent, final ReferenceQueue<? super AbstractSqueakObject> q) {
            super(referent, q);
        }

        @Override
        @TruffleBoundary
        public AbstractSqueakObject get() {
            return super.get();
        }
    }
}

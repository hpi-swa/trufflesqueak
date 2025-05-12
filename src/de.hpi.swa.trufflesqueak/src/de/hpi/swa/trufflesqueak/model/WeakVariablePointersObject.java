/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Deque;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
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
    protected void fillInVariablePart(final SqueakImageChunk chunk, final int instSize) {
        final int numVariableSlots = chunk.getWordSize() - instSize;
        variablePart = new Object[numVariableSlots];
        for (int i = 0; i < numVariableSlots; i++) {
            final Object value = chunk.getPointer(instSize + i);
            variablePart[i] = value instanceof final AbstractSqueakObject o ? new WeakRef(o, weakPointersQueue) : value;
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

    public Object getFromVariablePart(final long index, final InlinedConditionProfile weakRefProfile, final Node node) {
        final Object value = super.getFromVariablePart(index);
        if (weakRefProfile.profile(node, value instanceof WeakRef)) {
            return NilObject.nullToNil(((WeakRef) value).get());
        } else {
            assert value != null;
            return value;
        }
    }

    @Override
    public void putIntoVariablePart(final long index, final Object value) {
        putIntoVariablePart(index, value, InlinedConditionProfile.getUncached(), null);
    }

    public void putIntoVariablePart(final long index, final Object value, final InlinedConditionProfile profile, final Node node) {
        super.putIntoVariablePart(index, profile.profile(node, value instanceof AbstractSqueakObject) ? new WeakRef((AbstractSqueakObject) value, weakPointersQueue) : value);
    }

    @Override
    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Node inlineTarget, final Object thang) {
        return layoutValuesPointTo(identityNode, inlineTarget, thang) || variablePartPointsTo(thang);
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
    public void allInstances(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result) {
        layoutAllInstances(currentMarkingFlag, result);
    }

    @Override
    public void allInstancesOf(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result, final ClassObject targetClass) {
        layoutAllInstancesOf(currentMarkingFlag, result, targetClass);
    }

    @Override
    public void pointersBecomeOneWay(final boolean currentMarkingFlag, final Object[] from, final Object[] to) {
        layoutValuesBecomeOneWay(currentMarkingFlag, from, to);
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

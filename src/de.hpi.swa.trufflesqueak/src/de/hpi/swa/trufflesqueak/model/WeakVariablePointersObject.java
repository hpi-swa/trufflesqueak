/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.graalvm.collections.UnmodifiableEconomicMap;

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
    public WeakVariablePointersObject(final SqueakImageChunk chunk) {
        super(chunk);
    }

    public WeakVariablePointersObject(final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(classObject, layout, variableSize);
    }

    public WeakVariablePointersObject(final WeakVariablePointersObject original) {
        super(original);
    }

    @Override
    protected void fillInVariablePart(final SqueakImageChunk chunk, final int instSize) {
        final SqueakImageContext image = chunk.getImage();
        final int numVariableSlots = chunk.getWordSize() - instSize;
        variablePart = new Object[numVariableSlots];
        for (int i = 0; i < numVariableSlots; i++) {
            final Object value = chunk.getPointer(instSize + i);
            variablePart[i] = value instanceof final AbstractSqueakObject o ? new WeakRef(o, image.weakPointersQueue) : value;
        }
    }

    public Object getFromWeakVariablePart(final Node node, final long index, final InlinedConditionProfile weakRefProfile) {
        final Object value = getObjectFromVariablePart(index);
        if (weakRefProfile.profile(node, value instanceof WeakRef)) {
            return NilObject.nullToNil(((WeakRef) value).get());
        } else {
            assert value != null;
            return value;
        }
    }

    public void putIntoWeakVariablePart(final Node node, final long index, final Object value, final SqueakImageContext image, final InlinedConditionProfile profile) {
        putObjectFromVariablePart(index, profile.profile(node, value instanceof AbstractSqueakObject) ? new WeakRef((AbstractSqueakObject) value, image.weakPointersQueue) : value);
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

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        for (int i = 0; i < variablePart.length; i++) {
            if (variablePart[i] instanceof final WeakRef weakRef &&
                            weakRef.get() instanceof final AbstractSqueakObjectWithClassAndHash object &&
                            fromToMap.get(object) instanceof final AbstractSqueakObjectWithClassAndHash replacement) {
                variablePart[i] = new WeakRef(replacement, replacement.getSqueakClass().getImage().weakPointersQueue);
            }
        }
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
            writer.writeObjectIfTracedElseNil(getFromWeakVariablePart(null, i, InlinedConditionProfile.getUncached()));
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

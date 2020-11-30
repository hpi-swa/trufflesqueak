/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class WeakVariablePointersObject extends AbstractPointersObject {
    private static final WeakRef NIL_REF = new WeakRef(NilObject.SINGLETON);
    private WeakRef[] variablePart;
    private final ReferenceQueue<Object> weakPointersQueue;

    public WeakVariablePointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
        weakPointersQueue = image.weakPointersQueue;
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout);
        variablePart = new WeakRef[variableSize];
        Arrays.fill(variablePart, NIL_REF);
        weakPointersQueue = image.weakPointersQueue;
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final int variableSize) {
        super(image, classObject);
        variablePart = new WeakRef[variableSize];
        Arrays.fill(variablePart, NIL_REF);
        weakPointersQueue = image.weakPointersQueue;
    }

    private WeakVariablePointersObject(final WeakVariablePointersObject original) {
        super(original);
        variablePart = original.variablePart.clone();
        weakPointersQueue = original.weakPointersQueue;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointersObject = chunk.getPointers();
        fillInLayoutAndExtensions();
        final int instSize = getSqueakClass().getBasicInstanceSize();
        for (int i = 0; i < instSize; i++) {
            writeNode.execute(this, i, pointersObject[i]);
        }
        variablePart = new WeakRef[pointersObject.length - instSize];
        for (int i = instSize; i < pointersObject.length; i++) {
            putIntoVariablePartSlow(i - instSize, pointersObject[i]);
        }
        assert size() == pointersObject.length;
    }

    public void become(final WeakVariablePointersObject other) {
        becomeLayout(other);
        final WeakRef[] otherVariablePart = other.variablePart;
        other.variablePart = variablePart;
        variablePart = otherVariablePart;
    }

    @Override
    public int size() {
        return instsize() + variablePart.length;
    }

    public Object[] getVariablePart() {
        return variablePart;
    }

    public int getVariablePartSize() {
        return variablePart.length;
    }

    public Object getFromVariablePart(final int index) {
        return NilObject.nullToNil(UnsafeUtils.getWeakRef(variablePart, index).get());
    }

    public Object getFromVariablePart(final int index, final ConditionProfile nilProfile) {
        return NilObject.nullToNil(UnsafeUtils.getWeakRef(variablePart, index).get(), nilProfile);
    }

    private void putIntoVariablePartSlow(final int index, final Object value) {
        putIntoVariablePart(index, value, BranchProfile.getUncached(), ConditionProfile.getUncached());
    }

    public void putIntoVariablePart(final int index, final Object value, final BranchProfile nilProfile, final ConditionProfile primitiveProfile) {
        if (value == NilObject.SINGLETON) {
            nilProfile.enter();
            UnsafeUtils.putWeakRef(variablePart, index, NIL_REF);
        } else {
            UnsafeUtils.putWeakRef(variablePart, index, new WeakRef(value, primitiveProfile.profile(SqueakGuards.isUsedJavaPrimitive(value)) ? null : weakPointersQueue));
        }
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        return layoutValuesPointTo(identityNode, thang) || variablePartPointsTo(thang);
    }

    private boolean variablePartPointsTo(final Object thang) {
        for (final WeakRef weakRef : variablePart) {
            if (weakRef.get() == thang) {
                return true;
            }
        }
        return false;
    }

    public WeakVariablePointersObject shallowCopy() {
        return new WeakVariablePointersObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        layoutValuesBecomeOneWay(from, to, copyHash);
        final int variableSize = variablePart.length;
        if (variableSize > 0) {
            for (int i = 0; i < from.length; i++) {
                final Object fromPointer = from[i];
                for (int j = 0; j < variableSize; j++) {
                    final Object object = getFromVariablePart(j);
                    if (object == fromPointer) {
                        putIntoVariablePartSlow(j, to[i]);
                        copyHash(fromPointer, to[i], copyHash);
                    }
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
        /* Weak pointers excluded from tracing. */
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (super.writeHeaderAndLayoutObjects(writer)) {
            for (int i = 0; i < variablePart.length; i++) {
                /*
                 * Since weak pointers are excluded from tracing, ignore (replace with nil) all
                 * objects that have not been traced somewhere else.
                 */
                writer.writeObjectIfTracedElseNil(getFromVariablePart(i));
            }
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        final StringBuilder sb = new StringBuilder(64);
        if (variablePart.length > 0) {
            final Object referent = variablePart[0].get();
            sb.append('[').append(referent);
            if (variablePart[0].isEnqueued()) {
                sb.append(" (marked as garbage)");
            }
            if (variablePart.length > 1) {
                sb.append("...");
            }
            sb.append(']');
        }
        return sb.append(" a ").append(getSqueakClassName()).append(" @").append(Integer.toHexString(hashCode())).append(" of size ").append(variablePart.length).toString();
    }

    /*
     * Final subclass for TruffleSqueak to help Graal's inlining (and to avoid corresponding
     * performance warnings.
     */
    public static final class WeakRef extends WeakReference<Object> {
        public WeakRef(final Object referent) {
            super(referent);
        }

        public WeakRef(final Object referent, final ReferenceQueue<Object> q) {
            super(referent, q);
        }
    }
}

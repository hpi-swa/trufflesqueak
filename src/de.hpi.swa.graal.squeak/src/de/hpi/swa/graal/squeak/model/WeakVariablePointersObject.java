/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode.ObjectTracer;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class WeakVariablePointersObject extends AbstractPointersObject {
    private static final WeakReference<?> NIL_REFERENCE = new WeakReference<>(NilObject.SINGLETON);
    @CompilationFinal(dimensions = 0) private WeakReference<?>[] variablePart;

    public WeakVariablePointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final int variableSize) {
        super(image, classObject);
        variablePart = new WeakReference<?>[variableSize];
        Arrays.fill(variablePart, NIL_REFERENCE);
    }

    private WeakVariablePointersObject(final WeakVariablePointersObject original) {
        super(original);
        variablePart = original.variablePart.clone();
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointersObject = chunk.getPointers();
        initializeLayoutAndExtensionsUnsafe();
        final int instSize = getSqueakClass().getBasicInstanceSize();
        for (int i = 0; i < instSize; i++) {
            writeNode.execute(this, i, pointersObject[i]);
        }
        variablePart = new WeakReference<?>[pointersObject.length - instSize];
        for (int i = instSize; i < pointersObject.length; i++) {
            putIntoVariablePart(i - instSize, pointersObject[i]);
        }
        assert size() == pointersObject.length;
    }

    public void become(final WeakVariablePointersObject other) {
        becomeLayout(other);
        final Object[] otherVariablePart = other.variablePart;
        /*
         * Keep outer arrays and only copy contents as variablePart is marked
         * with @CompilationFinal(dimensions = 0).
         */
        System.arraycopy(variablePart, 0, other.variablePart, 0, variablePart.length);
        System.arraycopy(otherVariablePart, 0, variablePart, 0, otherVariablePart.length);
    }

    @Override
    public int size() {
        return instsize() + variablePart.length;
    }

    public void pointersBecomeOneWay(final UpdateSqueakObjectHashNode updateHashNode, final Object[] from, final Object[] to, final boolean copyHash) {
        layoutValuesBecomeOneWay(updateHashNode, from, to, copyHash);
        final int variableSize = variablePart.length;
        if (variableSize > 0) {
            for (int i = 0; i < from.length; i++) {
                final Object fromPointer = from[i];
                for (int j = 0; j < variableSize; j++) {
                    final Object object = getFromVariablePart(j);
                    if (object == fromPointer) {
                        putIntoVariablePart(j, to[i]);
                        updateHashNode.executeUpdate(fromPointer, to[i], copyHash);
                    }
                }
            }
        }
    }

    public Object[] getVariablePart() {
        return variablePart;
    }

    public Object getFromVariablePart(final int index) {
        return NilObject.nullToNil(variablePart[index].get());
    }

    public Object getFromVariablePart(final int index, final ConditionProfile nilProfile) {
        return NilObject.nullToNil(variablePart[index].get(), nilProfile);
    }

    private void putIntoVariablePart(final int index, final Object value) {
        putIntoVariablePart(index, value, BranchProfile.getUncached(), ConditionProfile.getUncached());
    }

    public void putIntoVariablePart(final int index, final Object value, final BranchProfile nilProfile, final ConditionProfile primitiveProfile) {
        if (value == NilObject.SINGLETON) {
            nilProfile.enter();
            UnsafeUtils.putWeakReference(variablePart, index, NIL_REFERENCE);
        } else {
            UnsafeUtils.putWeakReference(variablePart, index, new WeakReference<>(value, primitiveProfile.profile(SqueakGuards.isUsedJavaPrimitive(value)) ? null : image.weakPointersQueue));
        }
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final ConditionProfile isPrimitiveProfile, final Object thang) {
        return layoutValuesPointTo(identityNode, isPrimitiveProfile, thang) || variablePartPointsTo(thang);
    }

    private boolean variablePartPointsTo(final Object thang) {
        for (final WeakReference<?> weakRef : variablePart) {
            if (weakRef.get() == thang) {
                return true;
            }
        }
        return false;
    }

    public WeakVariablePointersObject shallowCopy() {
        return new WeakVariablePointersObject(this);
    }

    public void traceObjects(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
        /* Weak pointers excluded from tracing. */
    }

    @Override
    public void write(final SqueakImageWriter writerNode) {
        if (super.writeHeaderAndLayoutObjects(writerNode)) {
            for (int i = 0; i < variablePart.length; i++) {
                /*
                 * Since weak pointers are excluded from tracing, ignore (replace with nil) all
                 * objects that have not been traced somewhere else.
                 */
                writerNode.writeObjectIfTracedElseNil(getFromVariablePart(i));
            }
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        String prefix = "";
        if (variablePart.length > 0) {
            final Object referent = variablePart[0].get();
            prefix = "[" + referent;
            if (variablePart[0].isEnqueued()) {
                prefix += " (marked as garbage)";
            }
            if (variablePart.length > 1) {
                prefix += "...";
            }
            prefix += "]";
        }
        return prefix + " a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode()) + " of size " + variablePart.length;
    }

}

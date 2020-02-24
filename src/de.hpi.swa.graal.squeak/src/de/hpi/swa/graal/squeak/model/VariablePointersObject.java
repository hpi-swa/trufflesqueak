/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class VariablePointersObject extends AbstractPointersObject {
    @CompilationFinal(dimensions = 0) private Object[] variablePart;

    public VariablePointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    public VariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final int variableSize) {
        super(image, classObject);
        variablePart = ArrayUtils.withAll(variableSize, NilObject.SINGLETON);
    }

    private VariablePointersObject(final VariablePointersObject original) {
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
        variablePart = Arrays.copyOfRange(pointersObject, instSize, pointersObject.length);
        assert size() == pointersObject.length;
    }

    public void become(final VariablePointersObject other) {
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

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final ConditionProfile isPrimitiveProfile, final Object thang) {
        return layoutValuesPointTo(identityNode, isPrimitiveProfile, thang) || ArrayUtils.contains(variablePart, thang);
    }

    public Object[] getVariablePart() {
        return variablePart;
    }

    public Object getFromVariablePart(final int index) {
        return UnsafeUtils.getObject(variablePart, index);
    }

    public void putIntoVariablePart(final int index, final Object value) {
        UnsafeUtils.putObject(variablePart, index, value);
    }

    public VariablePointersObject shallowCopy() {
        return new VariablePointersObject(this);
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
        for (final Object object : variablePart) {
            tracer.addIfUnmarked(object);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writerNode) {
        super.trace(writerNode);
        for (final Object object : variablePart) {
            writerNode.traceIfNecessary(object);
        }
    }

    @Override
    public void write(final SqueakImageWriter writerNode) {
        if (super.writeHeaderAndLayoutObjects(writerNode)) {
            for (final Object object : variablePart) {
                writerNode.writeObject(object);
            }
        }
    }
}

/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class VariablePointersObject extends AbstractPointersObject {
    private Object[] variablePart;

    public VariablePointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
    }

    public VariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout);
        variablePart = ArrayUtils.withAll(variableSize, NilObject.SINGLETON);
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
        fillInLayoutAndExtensions();
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
        other.variablePart = variablePart;
        variablePart = otherVariablePart;
    }

    @Override
    public int size() {
        return instsize() + variablePart.length;
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        return layoutValuesPointTo(identityNode, thang) || ArrayUtils.contains(variablePart, thang);
    }

    public Object[] getVariablePart() {
        return variablePart;
    }

    public int getVariablePartSize() {
        return variablePart.length;
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
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        layoutValuesBecomeOneWay(from, to);
        final int variableSize = variablePart.length;
        if (variableSize > 0) {
            for (int i = 0; i < from.length; i++) {
                final Object fromPointer = from[i];
                for (int j = 0; j < variableSize; j++) {
                    final Object object = getFromVariablePart(j);
                    if (object == fromPointer) {
                        putIntoVariablePart(j, to[i]);
                    }
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
        for (final Object object : variablePart) {
            tracer.addIfUnmarked(object);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceAllIfNecessary(variablePart);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (super.writeHeaderAndLayoutObjects(writer)) {
            writer.writeObjects(variablePart);
        }
    }
}

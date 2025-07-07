/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public abstract class AbstractVariablePointersObject extends AbstractPointersObject {
    protected Object[] variablePart;

    public AbstractVariablePointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    public AbstractVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout);
        variablePart = ArrayUtils.withAll(variableSize, NilObject.SINGLETON);
    }

    protected AbstractVariablePointersObject(final AbstractVariablePointersObject original) {
        super(original);
        variablePart = original.variablePart.clone();
    }

    @Override
    protected void fillInVariablePart(final SqueakImageChunk chunk, final int instSize) {
        variablePart = chunk.getPointers(instSize);
    }

    public final void become(final AbstractVariablePointersObject other) {
        becomeLayout(other);
        final Object[] otherVariablePart = other.variablePart;
        other.variablePart = variablePart;
        variablePart = otherVariablePart;
    }

    @Override
    public final int size() {
        return instsize() + variablePart.length;
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Node inlineTarget, final Object thang) {
        return layoutValuesPointTo(identityNode, inlineTarget, thang) || ArrayUtils.contains(variablePart, thang);
    }

    public final Object[] getVariablePart() {
        return variablePart;
    }

    public final int getVariablePartSize() {
        return variablePart.length;
    }

    public Object getFromVariablePart(final long index) {
        return UnsafeUtils.getObject(variablePart, index);
    }

    public void putIntoVariablePart(final long index, final Object value) {
        UnsafeUtils.putObject(variablePart, index, value);
    }
}

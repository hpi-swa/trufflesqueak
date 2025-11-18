/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class VariablePointersObject extends AbstractVariablePointersObject {

    public VariablePointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    public VariablePointersObject(final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(classObject, layout, variableSize);
    }

    private VariablePointersObject(final VariablePointersObject original) {
        super(original);
    }

    public VariablePointersObject shallowCopy() {
        return new VariablePointersObject(this);
    }

    public Object getFromVariablePart(final long index) {
        return getObjectFromVariablePart(index);
    }

    public void putIntoVariablePart(final long index, final Object value) {
        putObjectFromVariablePart(index, value);
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        ArrayUtils.replaceAll(variablePart, fromToMap);
    }

    @Override
    protected void traceVariablePart(final ObjectTracer tracer) {
        tracer.addAllIfUnmarked(variablePart);
    }

    @Override
    protected void traceVariablePart(final SqueakImageWriter writer) {
        writer.traceAllIfNecessary(variablePart);
    }

    @Override
    protected void writeVariablePart(final SqueakImageWriter writer) {
        writer.writeObjects(variablePart);
    }
}

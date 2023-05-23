/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.object.Shape;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class VariablePointersObject extends AbstractVariablePointersObject {

    public VariablePointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    public VariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final Shape layout, final int variableSize) {
        super(image, classObject, layout, variableSize);
    }

    private VariablePointersObject(final VariablePointersObject original) {
        super(original);
    }

    public VariablePointersObject shallowCopy() {
        return new VariablePointersObject(this);
    }

    @Override
    protected void traceVariablePart(final ObjectTracer tracer) {
        for (final Object object : variablePart) {
            tracer.addIfUnmarked(object);
        }
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

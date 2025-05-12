/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Deque;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class VariablePointersObject extends AbstractVariablePointersObject {

    public VariablePointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    public VariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout, variableSize);
    }

    private VariablePointersObject(final VariablePointersObject original) {
        super(original);
    }

    public VariablePointersObject shallowCopy() {
        return new VariablePointersObject(this);
    }

    @Override
    public void allInstances(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result) {
        layoutAllInstances(currentMarkingFlag, result);
        allInstancesAll(variablePart, currentMarkingFlag, result);
    }

    @Override
    public void allInstancesOf(final boolean currentMarkingFlag, final Deque<AbstractSqueakObjectWithClassAndHash> result, final ClassObject targetClass) {
        layoutAllInstancesOf(currentMarkingFlag, result, targetClass);
        allInstancesOfAll(variablePart, currentMarkingFlag, result, targetClass);
    }

    @Override
    public void pointersBecomeOneWay(final boolean currentMarkingFlag, final Object[] from, final Object[] to) {
        layoutValuesBecomeOneWay(currentMarkingFlag, from, to);
        final int variableSize = variablePart.length;
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            final Object toPointer = to[i];
            for (int j = 0; j < variableSize; j++) {
                final Object part = variablePart[j];
                if (part == fromPointer) {
                    variablePart[j] = toPointer;
                } else {
                    pointersBecomeOneWay(part, currentMarkingFlag, from, to);
                }
            }
        }
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

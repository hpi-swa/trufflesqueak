/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class EmptyObject extends AbstractSqueakObjectWithClassAndHash {

    public EmptyObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    public EmptyObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    private EmptyObject(final EmptyObject original) {
        super(original);
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        // Nothing to do.
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    public void become(final EmptyObject other) {
        becomeOtherClass(other);
    }

    public EmptyObject shallowCopy() {
        return new EmptyObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        // Nothing to do.
    }

    @Override
    public void tracePointers(final ObjectTracer objectTracer) {
        // Nothing to trace.
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (writeHeader(writer)) {
            throw SqueakException.create("Empty objects should not have any slots:", this);
        }
    }
}

/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractPointersObject extends AbstractSqueakObjectWithClassAndHash {
    @DynamicField private long primitive1;
    @DynamicField private long primitive2;
    @DynamicField private long primitive3;

    @DynamicField private Object object1;
    @DynamicField private Object object2;
    @DynamicField private Object object3;

    protected AbstractPointersObject() {
        super(SqueakLanguage.POINTERS_SHAPE);
    }

    protected AbstractPointersObject(final SqueakImageContext image, final ClassObject classObject, final Shape shape) {
        super(image, classObject, shape);
        assert classObject.getRootShape() == shape;
    }

    protected AbstractPointersObject(final long header, final ClassObject classObject) {
        super(header, classObject, classObject.getRootShape());
    }

    protected AbstractPointersObject(final AbstractPointersObject original, final DynamicObjectLibrary lib) {
        super(original);
        copyLayoutValuesFrom(original, lib);
    }

    public final void copyLayoutValuesFrom(final AbstractPointersObject anotherObject, final DynamicObjectLibrary lib) {
        for (final var key : lib.getKeyArray(anotherObject)) {
            lib.put(this, key, lib.getOrDefault(anotherObject, key, NilObject.SINGLETON));
        }
    }

    @Override
    public final void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointers = chunk.getPointers();
        final int instSize = instsize();
        for (int i = 0; i < instSize; i++) {
            if (pointers[i] != NilObject.SINGLETON) {
                writeNode.execute(this, i, pointers[i]);
            }
        }
        fillInVariablePart(pointers, instSize);
        assert size() == pointers.length;
    }

    protected abstract void fillInVariablePart(Object[] pointers, int instSize);

    public final void changeClassTo(final DynamicObjectLibrary lib, final ClassObject newClass) {
        final Object[] keyArray = lib.getKeyArray(this);
        final Object[] values = new Object[keyArray.length];
        for (int i = 0; i < keyArray.length; i++) {
            values[i] = lib.getOrDefault(this, keyArray[i], NilObject.SINGLETON);
        }

        setSqueakClass(newClass);
        lib.resetShape(this, newClass.getRootShape());

        for (int i = 0; i < keyArray.length; i++) {
            lib.put(this, keyArray[i], values[i]);
        }
    }

    public final void becomeLayout(final AbstractPointersObject other) {
        assert getClass() == other.getClass();
        becomeOtherClass(other);

        CompilerDirectives.transferToInterpreterAndInvalidate();

        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var key : other.getShape().getKeys()) {
            final Object otherValue = lib.getOrDefault(other, key, NilObject.SINGLETON);
            lib.put(other, key, lib.getOrDefault(this, key, NilObject.SINGLETON));
            lib.put(this, key, otherValue);
        }
    }

    @Override
    public final int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    public final Object instVarAt0Slow(final long index) {
        CompilerAsserts.neverPartOfCompilation();
        return AbstractPointersObjectReadNode.getUncached().execute(this, index);
    }

    public final void instVarAtPut0Slow(final long index, final Object value) {
        CompilerAsserts.neverPartOfCompilation();
        AbstractPointersObjectWriteNode.getUncached().execute(this, index, value);
    }

    protected final boolean layoutValuesPointTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var key : getShape().getKeys()) {
            if (lib.getOrDefault(this, key, NilObject.SINGLETON) == thang) {
                return true;
            }
        }
        return false;
    }

    protected final void layoutValuesBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];

            final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
            for (final var key : getShape().getKeys()) {
                if (lib.getOrDefault(this, key, NilObject.SINGLETON) == fromPointer) {
                    lib.put(this, key, to[i]);
                }
            }
        }
    }

    @Override
    public final void tracePointers(final ObjectTracer tracer) {
        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var key : getShape().getKeys()) {
            tracer.addIfUnmarked(lib.getOrDefault(this, key, NilObject.SINGLETON));
        }
        traceVariablePart(tracer);
    }

    protected abstract void traceVariablePart(ObjectTracer tracer);

    @Override
    public final void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var key : getShape().getKeys()) {
            writer.traceIfNecessary(lib.getOrDefault(this, key, NilObject.SINGLETON));
        }
        traceVariablePart(writer);
    }

    protected abstract void traceVariablePart(SqueakImageWriter tracer);

    @Override
    public final void write(final SqueakImageWriter writer) {
        if (writeHeader(writer)) {
            final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
            for (int i = 0; i < instsize(); i++) {
                writer.writeObject(readNode.execute(this, i));
            }
            writeVariablePart(writer);
        }
    }

    protected abstract void writeVariablePart(SqueakImageWriter writer);
}

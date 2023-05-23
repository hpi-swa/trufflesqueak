/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public abstract class AbstractPointersObject extends AbstractSqueakObjectWithClassAndHash {
    /*
     * Field addresses for Unsafe access. `static final` allows SubstrateVM to intercept and
     * recalculate addresses before native image generation.
     */

// public long primitive0;
// public long primitive1;
// public long primitive2;

    public Object object1 = NilObject.SINGLETON;
    public Object object2 = NilObject.SINGLETON;

    protected AbstractPointersObject() {
        super();
    }

    protected AbstractPointersObject(final SqueakImageContext image, final ClassObject classObject, final Shape layout) {
        super(image, classObject, layout);
    }

    protected AbstractPointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    protected AbstractPointersObject(final AbstractPointersObject original) {
        super(original);

        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var p : original.getShape().getProperties()) {
            lib.put(this, p.getKey(), lib.getOrDefault(original, p.getKey(), NilObject.SINGLETON));
        }

// primitive0 = original.primitive0;
// primitive1 = original.primitive1;
// primitive2 = original.primitive2;

        object1 = original.object1;
        object2 = original.object2;

// if (original.primitiveExtension != null) {
// primitiveExtension = original.primitiveExtension.clone();
// }
// if (original.objectExtension != null) {
// objectExtension = original.objectExtension.clone();
// }
    }

    public final void copyLayoutValuesFrom(final AbstractPointersObject anotherObject) {
        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var p : anotherObject.getShape().getProperties()) {
            lib.put(this, p.getKey(), lib.getOrDefault(anotherObject, p.getKey(), NilObject.SINGLETON));
        }

// primitive0 = anotherObject.primitive0;
// primitive1 = anotherObject.primitive1;
// primitive2 = anotherObject.primitive2;
        object1 = anotherObject.object1;
        object2 = anotherObject.object2;
// if (anotherObject.primitiveExtension != null) {
// ArrayUtils.arraycopy(anotherObject.primitiveExtension, 0, primitiveExtension, 0,
// anotherObject.primitiveExtension.length);
// }
// if (anotherObject.objectExtension != null) {
// ArrayUtils.arraycopy(anotherObject.objectExtension, 0, objectExtension, 0,
// anotherObject.objectExtension.length);
// }
    }

    @Override
    public final void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointers = chunk.getPointers();
        final int instSize = instsize();
        for (int i = 0; i < instSize; i++) {
            writeNode.execute(this, i, pointers[i]);
        }
        fillInVariablePart(pointers, instSize);
        assert size() == pointers.length;
    }

    protected abstract void fillInVariablePart(Object[] pointers, int instSize);

    public final void changeClassTo(final ClassObject newClass) {
        setSqueakClass(newClass);
// migrateToLayout(newClass.getLayout());
    }

// @TruffleBoundary
// public final ObjectLayout updateLayout(final long index, final Object value) {
// assert !layout.getLocation(index).canStore(value);
// ObjectLayout latestLayout = getSqueakClass().getLayout();
// if (!latestLayout.getLocation(index).canStore(value)) {
// latestLayout = latestLayout.evolveLocation(index, value);
// } else {
// assert !layout.isValid() && layout != latestLayout : "Layout must have changed";
// }
// migrateToLayout(latestLayout);
// return getSqueakClass().getLayout(); /* Layout may have evolved again during migration. */
// }

// @TruffleBoundary
// private void migrateToLayout(final ObjectLayout targetLayout) {
// }

    public final void becomeLayout(final AbstractPointersObject other) {
        assert getClass() == other.getClass();
        becomeOtherClass(other);

        CompilerDirectives.transferToInterpreterAndInvalidate();

        final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        for (final var p : getShape().getProperties()) {
            final Object otherValue = lib.getOrDefault(other, p.getKey(), NilObject.SINGLETON);
            lib.put(other, p.getKey(), lib.getOrDefault(this, p.getKey(), NilObject.SINGLETON));
            lib.put(this, p.getKey(), otherValue);
        }

        // Copy all values.
// final ObjectLayout otherLayout = other.layout;

// final long otherPrimitive0 = other.primitive0;
// final long otherPrimitive1 = other.primitive1;
// final long otherPrimitive2 = other.primitive2;

        final Object otherObject1 = other.object1;
        final Object otherObject2 = other.object2;

// final long[] otherPrimitiveExtension = other.primitiveExtension;
// final Object[] otherObjectExtension = other.objectExtension;

        // Move content from this object to the other.
// other.layout = layout;

// other.primitive0 = primitive0;
// other.primitive1 = primitive1;
// other.primitive2 = primitive2;

        other.object1 = object1;
        other.object2 = object2;

// other.primitiveExtension = primitiveExtension;
// other.objectExtension = objectExtension;

        // Move copied content to this object.
// layout = otherLayout;

// primitive0 = otherPrimitive0;
// primitive1 = otherPrimitive1;
// primitive2 = otherPrimitive2;

        object1 = otherObject1;
        object2 = otherObject2;

// primitiveExtension = otherPrimitiveExtension;
// objectExtension = otherObjectExtension;
    }

    @Override
    public final int instsize() {
// assert getSqueakClass().getBasicInstanceSize() == getLayout().getInstSize();
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
// final boolean pointTo = object0 == thang || object1 == thang || object2 == thang ||
// objectExtension != null && ArrayUtils.contains(objectExtension, thang);
// if (pointTo) {
// return true;
// } else {
// return primitiveLocationsPointTo(identityNode, thang);
// }
        return false;
    }

    @TruffleBoundary
    private boolean primitiveLocationsPointTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        if (SqueakGuards.isUsedJavaPrimitive(thang)) {
            // TODO: This could be more efficient.
// for (final SlotLocation slotLocation : getLayout().getLocations()) {
// if (slotLocation.isPrimitive() && identityNode.execute(slotLocation.read(this), thang)) {
// return true;
// }
// }
        }
        return false;
    }

    protected final void layoutValuesBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];

            final DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
            for (final var p : getShape().getProperties()) {
                if (lib.getOrDefault(this, p.getKey(), NilObject.SINGLETON) == fromPointer) {
                    lib.put(this, p.getKey(), to[i]);
                }
            }

// if (object0 == fromPointer) {
// object0 = to[i];
// }
// if (object1 == fromPointer) {
// object1 = to[i];
// }
// if (object2 == fromPointer) {
// object2 = to[i];
// }
// if (objectExtension != null) {
// for (int j = 0; j < objectExtension.length; j++) {
// final Object object = objectExtension[j];
// if (object == fromPointer) {
// objectExtension[j] = to[i];
// }
// }
// }
        }
    }

    @Override
    public final void tracePointers(final ObjectTracer tracer) {
// tracer.addIfUnmarked(object0);
// tracer.addIfUnmarked(object1);
// tracer.addIfUnmarked(object2);
// if (objectExtension != null) {
// for (final Object object : objectExtension) {
// tracer.addIfUnmarked(object);
// }
// }
        traceVariablePart(tracer);
    }

    protected abstract void traceVariablePart(ObjectTracer tracer);

    @Override
    public final void trace(final SqueakImageWriter writer) {
        super.trace(writer);
// writer.traceIfNecessary(object0);
// writer.traceIfNecessary(object1);
// writer.traceIfNecessary(object2);
// if (objectExtension != null) {
// writer.traceAllIfNecessary(objectExtension);
// }
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

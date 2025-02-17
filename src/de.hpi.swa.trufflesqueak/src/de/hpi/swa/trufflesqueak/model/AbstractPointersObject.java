/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public abstract class AbstractPointersObject extends AbstractSqueakObjectWithClassAndHash {
    /*
     * Field addresses for Unsafe access. `static final` allows SubstrateVM to intercept and
     * recalculate addresses before native image generation.
     */
    public static final long PRIMITIVE_USED_MAP_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "primitiveUsedMap");
    public static final long PRIMITIVE_0_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "primitive0");
    public static final long PRIMITIVE_1_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "primitive1");
    public static final long PRIMITIVE_2_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "primitive2");
    public static final long OBJECT_0_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "object0");
    public static final long OBJECT_1_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "object1");
    public static final long OBJECT_2_ADDRESS = UnsafeUtils.getAddress(AbstractPointersObject.class, "object2");

    @CompilationFinal private ObjectLayout layout;

    public int primitiveUsedMap;
    public long primitive0;
    public long primitive1;
    public long primitive2;

    public Object object0 = NilObject.SINGLETON;
    public Object object1 = NilObject.SINGLETON;
    public Object object2 = NilObject.SINGLETON;

    public long[] primitiveExtension;
    public Object[] objectExtension;

    protected AbstractPointersObject() {
        super();
    }

    protected AbstractPointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout) {
        super(image, classObject);
        if (layout != null) {
            CompilerAsserts.partialEvaluationConstant(layout);
            this.layout = layout;
        } else {
            this.layout = classObject.getLayout();
        }
        assert classObject.getLayout() == this.layout : "Layout mismatch";
        primitiveExtension = this.layout.getFreshPrimitiveExtension();
        objectExtension = this.layout.getFreshObjectExtension();
    }

    protected AbstractPointersObject(final long header, final ClassObject classObject) {
        super(header, classObject);
    }

    protected AbstractPointersObject(final AbstractPointersObject original) {
        super(original);
        layout = original.layout;

        primitiveUsedMap = original.primitiveUsedMap;
        primitive0 = original.primitive0;
        primitive1 = original.primitive1;
        primitive2 = original.primitive2;

        object0 = original.object0;
        object1 = original.object1;
        object2 = original.object2;

        if (original.primitiveExtension != null) {
            primitiveExtension = original.primitiveExtension.clone();
        }
        if (original.objectExtension != null) {
            objectExtension = original.objectExtension.clone();
        }
    }

    public final void copyLayoutValuesFrom(final AbstractPointersObject anotherObject) {
        assert layout == anotherObject.layout;
        primitiveUsedMap = anotherObject.primitiveUsedMap;
        primitive0 = anotherObject.primitive0;
        primitive1 = anotherObject.primitive1;
        primitive2 = anotherObject.primitive2;
        object0 = anotherObject.object0;
        object1 = anotherObject.object1;
        object2 = anotherObject.object2;
        if (anotherObject.primitiveExtension != null) {
            UnsafeUtils.copyLongs(anotherObject.primitiveExtension, 0, primitiveExtension, 0, anotherObject.primitiveExtension.length);
        }
        if (anotherObject.objectExtension != null) {
            ArrayUtils.arraycopy(anotherObject.objectExtension, 0, objectExtension, 0, anotherObject.objectExtension.length);
        }
    }

    @Override
    public final void fillin(final SqueakImageChunk chunk) {
        layout = getSqueakClass().getLayout();
        primitiveExtension = layout.getFreshPrimitiveExtension();
        objectExtension = layout.getFreshObjectExtension();
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final int instSize = instsize();
        for (int i = 0; i < instSize; i++) {
            writeNode.execute(null, this, i, chunk.getPointer(i));
        }
        fillInVariablePart(chunk, instSize);
        assert size() == chunk.getWordSize();
    }

    protected abstract void fillInVariablePart(SqueakImageChunk chunk, int instSize);

    public final ObjectLayout getLayout() {
        if (layout == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            layout = getSqueakClass().getLayout();
        }
        return layout;
    }

    public final boolean matchesLayout(final ObjectLayout expectedLayout) {
        if (!getLayout().isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateLayout();
        }
        assert layout.isValid() : "Should only ever match valid layout (invalid expectedLayout will be replaced in PIC)";
        return layout == expectedLayout;
    }

    public final void changeClassTo(final ClassObject newClass) {
        setSqueakClass(newClass);
        migrateToLayout(newClass.getLayout());
    }

    @TruffleBoundary
    public final void updateLayout() {
        final ObjectLayout latestLayout = getSqueakClass().getLayout();
        assert !layout.isValid() && layout != latestLayout : "Layout upgrade requested, but layout is latest";
        migrateToLayout(latestLayout);
    }

    @TruffleBoundary
    public final ObjectLayout updateLayout(final long index, final Object value) {
        assert !layout.getLocation(index).canStore(value);
        ObjectLayout latestLayout = getSqueakClass().getLayout();
        if (!latestLayout.getLocation(index).canStore(value)) {
            latestLayout = latestLayout.evolveLocation(index, value);
        } else {
            assert !layout.isValid() && layout != latestLayout : "Layout must have changed";
        }
        migrateToLayout(latestLayout);
        return getSqueakClass().getLayout(); /* Layout may have evolved again during migration. */
    }

    @TruffleBoundary
    private void migrateToLayout(final ObjectLayout targetLayout) {
        assert targetLayout.isValid() : "Should not migrate to outdated layout";
        ObjectLayout newLayout = targetLayout;
        final ObjectLayout oldLayout = layout;
        assert oldLayout.getInstSize() == newLayout.getInstSize();
        final int instSize = oldLayout.getInstSize();
        final Object[] values = new Object[instSize];
        for (int i = 0; i < instSize; i++) {
            final SlotLocation oldLocation = oldLayout.getLocation(i);
            final SlotLocation newLocation = newLayout.getLocation(i);
            if (oldLocation.isSet(this)) {
                final Object oldValue = oldLocation.read(this);
                oldLocation.unset(this);
                if (!newLocation.canStore(oldValue)) {
                    newLayout = newLayout.evolveLocation(i, oldValue);
                }
                values[i] = oldValue;
            }
        }
        if (oldLayout.getNumPrimitiveExtension() != newLayout.getNumPrimitiveExtension()) {
            // primitiveExtension has grown ...
            if (primitiveExtension == null) {
                assert oldLayout.getNumPrimitiveExtension() == 0;
                // ... primitiveExtension now needed
                primitiveExtension = newLayout.getFreshPrimitiveExtension();
            } else {
                if (newLayout.getNumPrimitiveExtension() == 0) {
                    // ... primitiveExtension no longer needed
                    primitiveExtension = null;
                } else {
                    // ... resize primitiveExtension
                    primitiveExtension = Arrays.copyOf(primitiveExtension, newLayout.getNumPrimitiveExtension());
                }
            }
        }
        if (oldLayout.getNumObjectExtension() != newLayout.getNumObjectExtension()) {
            // objectExtension has grown ...
            if (objectExtension == null) {
                assert oldLayout.getNumObjectExtension() == 0;
                // ... objectExtension now needed
                objectExtension = newLayout.getFreshObjectExtension();
            } else {
                // ... resize objectExtension
                objectExtension = Arrays.copyOf(objectExtension, newLayout.getNumObjectExtension());
                for (int i = oldLayout.getNumObjectExtension(); i < newLayout.getNumObjectExtension(); i++) {
                    objectExtension[i] = NilObject.SINGLETON;
                }
            }
        }
        assert newLayout.getNumPrimitiveExtension() == 0 || newLayout.getNumPrimitiveExtension() == primitiveExtension.length;
        assert newLayout.getNumObjectExtension() == 0 || newLayout.getNumObjectExtension() == objectExtension.length;

        for (int i = 0; i < instSize; i++) {
            final Object value = values[i];
            if (value != null) {
                final SlotLocation newLocation = newLayout.getLocation(i);
                assert newLocation.canStore(value) : "Evolved new location must be able to store value";
                newLocation.writeMustSucceed(this, value);
            }
        }
        layout = newLayout;
        assert layout.getNumPrimitiveExtension() == 0 || layout.getNumPrimitiveExtension() == primitiveExtension.length;
        assert layout.getNumObjectExtension() == 0 || layout.getNumObjectExtension() == objectExtension.length;
    }

    public final void becomeLayout(final AbstractPointersObject other) {
        assert getClass() == other.getClass();
        becomeOtherClass(other);

        CompilerDirectives.transferToInterpreterAndInvalidate();

        // Copy all values.
        final ObjectLayout otherLayout = other.layout;

        final int otherPrimitiveUsedMap = other.primitiveUsedMap;
        final long otherPrimitive0 = other.primitive0;
        final long otherPrimitive1 = other.primitive1;
        final long otherPrimitive2 = other.primitive2;

        final Object otherObject0 = other.object0;
        final Object otherObject1 = other.object1;
        final Object otherObject2 = other.object2;

        final long[] otherPrimitiveExtension = other.primitiveExtension;
        final Object[] otherObjectExtension = other.objectExtension;

        // Move content from this object to the other.
        other.layout = layout;

        other.primitiveUsedMap = primitiveUsedMap;
        other.primitive0 = primitive0;
        other.primitive1 = primitive1;
        other.primitive2 = primitive2;

        other.object0 = object0;
        other.object1 = object1;
        other.object2 = object2;

        other.primitiveExtension = primitiveExtension;
        other.objectExtension = objectExtension;

        // Move copied content to this object.
        layout = otherLayout;

        primitiveUsedMap = otherPrimitiveUsedMap;
        primitive0 = otherPrimitive0;
        primitive1 = otherPrimitive1;
        primitive2 = otherPrimitive2;

        object0 = otherObject0;
        object1 = otherObject1;
        object2 = otherObject2;

        primitiveExtension = otherPrimitiveExtension;
        objectExtension = otherObjectExtension;
    }

    @Override
    public final int instsize() {
        assert getSqueakClass().getBasicInstanceSize() == getLayout().getInstSize();
        return getLayout().getInstSize();
    }

    public final Object instVarAt0Slow(final long index) {
        CompilerAsserts.neverPartOfCompilation();
        return AbstractPointersObjectReadNode.executeUncached(this, index);
    }

    public final void instVarAtPut0Slow(final long index, final Object value) {
        CompilerAsserts.neverPartOfCompilation();
        AbstractPointersObjectWriteNode.executeUncached(this, index, value);
    }

    protected final boolean layoutValuesPointTo(final SqueakObjectIdentityNode identityNode, final Node inlineTarget, final Object thang) {
        final boolean pointTo = object0 == thang || object1 == thang || object2 == thang || objectExtension != null && ArrayUtils.contains(objectExtension, thang);
        if (pointTo) {
            return true;
        } else {
            return primitiveLocationsPointTo(identityNode, inlineTarget, thang);
        }
    }

    @TruffleBoundary
    private boolean primitiveLocationsPointTo(final SqueakObjectIdentityNode identityNode, final Node inlineTarget, final Object thang) {
        if (SqueakGuards.isUsedJavaPrimitive(thang)) {
            // TODO: This could be more efficient.
            for (final SlotLocation slotLocation : getLayout().getLocations()) {
                if (slotLocation.isPrimitive() && identityNode.execute(inlineTarget, slotLocation.read(this), thang)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected final void layoutValuesBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (object0 == fromPointer) {
                object0 = to[i];
            }
            if (object1 == fromPointer) {
                object1 = to[i];
            }
            if (object2 == fromPointer) {
                object2 = to[i];
            }
            if (objectExtension != null) {
                for (int j = 0; j < objectExtension.length; j++) {
                    final Object object = objectExtension[j];
                    if (object == fromPointer) {
                        objectExtension[j] = to[i];
                    }
                }
            }
        }
    }

    @Override
    public final void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(object0);
        tracer.addIfUnmarked(object1);
        tracer.addIfUnmarked(object2);
        if (objectExtension != null) {
            for (final Object object : objectExtension) {
                tracer.addIfUnmarked(object);
            }
        }
        traceVariablePart(tracer);
    }

    protected abstract void traceVariablePart(ObjectTracer tracer);

    @Override
    public final void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(object0);
        writer.traceIfNecessary(object1);
        writer.traceIfNecessary(object2);
        if (objectExtension != null) {
            writer.traceAllIfNecessary(objectExtension);
        }
        traceVariablePart(writer);
    }

    protected abstract void traceVariablePart(SqueakImageWriter tracer);

    @Override
    public final void write(final SqueakImageWriter writer) {
        if (writeHeader(writer)) {
            final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
            for (int i = 0; i < instsize(); i++) {
                writer.writeObject(readNode.execute(null, this, i));
            }
            writeVariablePart(writer);
        }
    }

    protected abstract void writeVariablePart(SqueakImageWriter writer);
}

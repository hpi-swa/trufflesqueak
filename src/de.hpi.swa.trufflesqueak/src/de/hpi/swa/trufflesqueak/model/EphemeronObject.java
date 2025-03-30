/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class EphemeronObject extends AbstractSqueakObjectWithClassAndHash {

    private final SqueakImageContext image;

    @CompilerDirectives.CompilationFinal private Object key;
    @CompilerDirectives.CompilationFinal private Object value;

   private Object[] pointers;

    private EphemeronObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
        this.image = image;
        pointers = ArrayUtils.withAll(Math.max(classObject.getBasicInstanceSize() - ObjectLayouts.EPHEMERON.MIN_SIZE, 0), NilObject.SINGLETON);
    }

    private EphemeronObject(final EphemeronObject original) {
        super(original);
        image = original.image;

        key         = original.key;
        value       = original.value;
        pointers    = original.pointers.clone();
    }

    public static EphemeronObject create(final SqueakImageContext image, final ClassObject classObject) {
        final EphemeronObject result = new EphemeronObject(image, classObject);
        return result;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert chunk.getWordSize() >= ObjectLayouts.EPHEMERON.MIN_SIZE;

        key         = NilObject.nilToNull(chunk.getPointer(ObjectLayouts.EPHEMERON.KEY));
        value       = NilObject.nilToNull(chunk.getPointer(ObjectLayouts.EPHEMERON.VALUE));
        pointers    = chunk.getPointers(chunk.getWordSize() - ObjectLayouts.EPHEMERON.MIN_SIZE);
    }

    public static boolean isKeyIndex(final long index) { return index == ObjectLayouts.EPHEMERON.KEY; }

    public static boolean isValueIndex(final long index) { return index == ObjectLayouts.EPHEMERON.VALUE; }

    public static boolean isOtherIndex(final long index) {
        return index >= ObjectLayouts.EPHEMERON.MIN_SIZE;
    }

    public Object getKey() { return NilObject.nullToNil(key); }

    public void setKey(final Object newKey) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        key = newKey;
    }

    public Object getValue() { return NilObject.nullToNil(value); }

    public void setValue(final Object newValue) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        value = newValue;
    }

    public Object getOtherPointer(final int index) {
        return pointers[index - ObjectLayouts.EPHEMERON.MIN_SIZE];
    }

    public void setOtherPointer(final int index, final Object value) {
        pointers[index - ObjectLayouts.EPHEMERON.MIN_SIZE] = value;
    }

    public Object[] getOtherPointers() {
        return pointers;
    }

    public void setOtherPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    @Override
    public int instsize() { return ObjectLayouts.EPHEMERON.MIN_SIZE + pointers.length; }

    @Override
    public int size() { return instsize(); }

    public boolean pointsTo(final Object thang) {
        if (getKey() == thang) {
            return true;
        }
        if (getValue() == thang) {
            return true;
        }
        return ArrayUtils.contains(pointers, thang);
    }

    public void become(final EphemeronObject other) {
        becomeOtherClass(other);
        final Object otherKey        = other.key;
        final Object otherValue      = other.value;
        final Object[] otherPointers = other.pointers;

        // Move content from this object to the other.
        other.key       = key;
        other.value     = value;
        other.setOtherPointers(pointers);

        // Move copied content to this object.
        key         = otherKey;
        value       = otherValue;
        pointers    = otherPointers;
    }

    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            final Object toPointer = to[i];
            if (fromPointer != toPointer) {
                if (fromPointer == getKey()) {
                    setKey(toPointer);
                }
                if (fromPointer == getValue()) {
                    setValue(toPointer);
                }
            }
        }
        pointersBecomeOneWay(getOtherPointers(), from, to);
    }

    public EphemeronObject shallowCopy() {
        return new EphemeronObject(this);
    }

    @Override
    public final void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(getKey());
        tracer.addIfUnmarked(getValue());
        for (final Object value : getOtherPointers()) {
            tracer.addIfUnmarked(value);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(getKey());
        writer.traceIfNecessary(getValue());
        writer.traceAllIfNecessary(getOtherPointers());
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (writeHeader(writer)) {
            writer.writeObject(getKey());
            writer.writeObject(getValue());
            writer.writeObjects(getOtherPointers());
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode());
    }

}

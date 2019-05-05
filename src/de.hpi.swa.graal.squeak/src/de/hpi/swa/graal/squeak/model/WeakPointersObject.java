package de.hpi.swa.graal.squeak.model;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.nodes.accessing.WeakPointersObjectNodes.WeakPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class WeakPointersObject extends AbstractPointersObject {
    public static final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();

    public WeakPointersObject(final SqueakImageContext image, final long hash, final ClassObject sqClass) {
        super(image, hash, sqClass);
    }

    public WeakPointersObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(image, classObject);
        setPointers(ArrayUtils.withAll(size, NilObject.SINGLETON));
    }

    private WeakPointersObject(final WeakPointersObject original) {
        super(original.image, original.getSqueakClass());
        setPointersUnsafe(original.getPointers().clone());
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final Object[] pointers = chunk.getPointers();
        final int length = pointers.length;
        setPointers(new Object[length]);
        final WeakPointersObjectWriteNode writeNode = WeakPointersObjectWriteNode.getUncached();
        for (int i = 0; i < length; i++) {
            writeNode.execute(this, i, pointers[i]);
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "WeakPointersObject: " + getSqueakClass();
    }

    public void setWeakPointer(final int index, final Object value) {
        setPointer(index, new WeakReference<>(value, weakPointersQueue));
    }

    public WeakPointersObject shallowCopy() {
        return new WeakPointersObject(this);
    }
}

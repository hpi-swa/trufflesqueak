package de.hpi.swa.graal.squeak.model;

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.nodes.accessing.WeakPointersObjectNodes.WeakPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class WeakPointersObject extends AbstractPointersObject {
    public WeakPointersObject(final SqueakImageContext image, final long hash, final ClassObject sqClass) {
        super(image, hash, sqClass);
    }

    public WeakPointersObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(image, classObject);
        setPointersUnsafe(ArrayUtils.withAll(size, NilObject.SINGLETON));
    }

    private WeakPointersObject(final WeakPointersObject original) {
        super(original);
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
        setPointer(index, new WeakReference<>(value, image.weakPointersQueue));
    }

    public WeakPointersObject shallowCopy() {
        return new WeakPointersObject(this);
    }
}

package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.AbstractImageChunk;

public abstract class AbstractPointersObject extends SqueakObject {
    @CompilationFinal(dimensions = 1) protected Object[] pointers;

    public AbstractPointersObject(final SqueakImageContext img) {
        super(img);
    }

    public AbstractPointersObject(final SqueakImageContext img, final ClassObject klass) {
        super(img, klass);
    }

    public AbstractPointersObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        this(img, sqClass);
        pointers = ptrs;
    }

    public AbstractPointersObject(final SqueakImageContext img, final ClassObject classObject, final int size) {
        this(img, classObject, ArrayUtils.withAll(size, img.nil));
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    @Override
    public Object at0(final long i) {
        return pointers[(int) i];
    }

    @Override
    public void atput0(final long i, final Object obj) {
        assert obj != null; // null indicates a problem
        pointers[(int) i] = obj;
    }

    @Override
    public boolean become(final BaseSqueakObject other) {
        if (!(other instanceof AbstractPointersObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers2 = ((PointersObject) other).getPointers();
        ((PointersObject) other).pointers = this.getPointers();
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // TODO: super.pointersBecomeOneWay(from, to); ?
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                        ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
    }

    @Override
    public final int size() {
        return pointers.length;
    }

    @Override
    public final int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    public Object[] getPointers() {
        return pointers;
    }
}

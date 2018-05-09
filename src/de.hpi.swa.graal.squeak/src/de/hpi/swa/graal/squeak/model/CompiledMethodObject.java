package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class CompiledMethodObject extends CompiledCodeObject {
    public CompiledMethodObject(final SqueakImageContext img) {
        super(img);
    }

    public CompiledMethodObject(final SqueakImageContext img, final byte[] bc, final Object[] lits) {
        this(img);
        literals = lits;
        decodeHeader();
        bytes = bc;
    }

    public CompiledMethodObject(final SqueakImageContext img, final ClassObject klass, final int size) {
        super(img, klass);
        bytes = new byte[size];
    }

    private CompiledMethodObject(final CompiledMethodObject compiledMethodObject) {
        super(compiledMethodObject);
    }

    @Override
    public NativeObject getCompiledInSelector() {
        if (literals.length > 1) {
            final Object lit = literals[literals.length - 2];
            if (lit == null) {
                return null;
            } else if (lit instanceof NativeObject) {
                return (NativeObject) lit;
            } else if ((lit instanceof PointersObject) && ((PointersObject) lit).size() >= 2) {
                final Object secondValue = ((PointersObject) lit).at0(1);
                if (secondValue instanceof NativeObject) {
                    return (NativeObject) secondValue;
                }
            }
        }
        return null;
    }

    @Override
    public ClassObject getCompiledInClass() {
        if (literals.length == 0) {
            return null;
        }
        Object baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject && ((PointersObject) baseSqueakObject).size() == 2) {
            baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
        }
        if (baseSqueakObject instanceof ClassObject) {
            return (ClassObject) baseSqueakObject;
        }
        return null;
    }

    public void setCompiledInClass(final ClassObject newClass) {
        if (literals.length == 0) {
            return;
        }
        final Object baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject && ((PointersObject) baseSqueakObject).size() == 2) {
            ((PointersObject) baseSqueakObject).atput0(1, newClass);
            return;
        }
        if (baseSqueakObject instanceof ClassObject) {
            literals[literals.length - 1] = newClass;
        }
    }

    @Override
    public CompiledMethodObject getMethod() {
        return this;
    }

    public void setHeader(final long header) {
        literals = new Object[]{header};
        decodeHeader();
        literals = new Object[1 + numLiterals];
        literals[0] = header;
        for (int i = 1; i < literals.length; i++) {
            literals[i] = image.nil;
        }
    }

    public AbstractSqueakObject shallowCopy() {
        return new CompiledMethodObject(this);
    }

    @Override
    public int getOffset() {
        return 0; // methods always start at the beginning
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        super.pointersBecomeOneWay(from, to, copyHash);
        final ClassObject oldClass = getCompiledInClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                final ClassObject newClass = (ClassObject) to[i];  // must be a ClassObject
                setCompiledInClass(newClass);
                if (copyHash) {
                    newClass.setSqueakHash(oldClass.squeakHash());
                }
                // TODO: flush method caches
            }
        }
    }
}

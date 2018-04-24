package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.util.AbstractImageChunk;

public abstract class SqueakObject extends BaseSqueakObject {
    @CompilationFinal private long hash;
    @CompilationFinal private ClassObject sqClass;

    public SqueakObject(final SqueakImageContext img) {
        this(img, null);
    }

    public SqueakObject(final SqueakImageContext img, final ClassObject klass) {
        super(img);
        hash = 0;
        sqClass = klass;
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        hash = chunk.getHash();
        sqClass = chunk.getSqClass();
    }

    @Override
    public final ClassObject getSqClass() {
        return sqClass;
    }

    @Override
    public void setSqClass(final ClassObject newCls) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        sqClass = newCls;
    }

    @Override
    public boolean become(final BaseSqueakObject other) {
        if (this == other || !(other instanceof SqueakObject)) {
            return false;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final ClassObject otherSqClass = ((SqueakObject) other).sqClass;
        ((SqueakObject) other).sqClass = this.sqClass;
        this.sqClass = otherSqClass;
        return true;
    }

    @Override
    public long squeakHash() {
        if (hash == 0) {
            setSqueakHash(super.squeakHash());
        }
        return hash;
    }

    public void setSqueakHash(final long hash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.hash = hash;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        final ClassObject oldClass = getSqClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                final ClassObject newClass = (ClassObject) to[i]; // must be a ClassObject
                setSqClass(newClass);
                newClass.setSqueakHash(oldClass.squeakHash());
            }
        }
    }
}

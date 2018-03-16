package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public abstract class SqueakObject extends BaseSqueakObject {
    @CompilationFinal private long hash;
    @CompilationFinal private ClassObject sqClass;

    public SqueakObject(SqueakImageContext img) {
        this(img, null);
    }

    public SqueakObject(SqueakImageContext img, ClassObject klass) {
        super(img);
        hash = 0;
        sqClass = klass;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        hash = chunk.getHash();
        sqClass = chunk.getSqClass();
    }

    @Override
    public final ClassObject getSqClass() {
        return sqClass;
    }

    @Override
    public void setSqClass(ClassObject newCls) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        sqClass = newCls;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (this == other || !(other instanceof SqueakObject)) {
            return false;
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ClassObject otherSqClass = ((SqueakObject) other).sqClass;
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

    public void setSqueakHash(long hash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.hash = hash;
    }

    @Override
    public void pointersBecomeOneWay(Object[] from, Object[] to, boolean copyHash) {
        ClassObject oldClass = getSqClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                ClassObject newClass = (ClassObject) to[i]; // must be a ClassObject
                setSqClass(newClass);
                newClass.setSqueakHash(oldClass.squeakHash());
            }
        }
    }
}

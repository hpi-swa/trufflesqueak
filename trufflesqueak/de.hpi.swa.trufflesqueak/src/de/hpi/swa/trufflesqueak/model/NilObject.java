package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class NilObject extends BaseSqueakObject {

    public NilObject(SqueakImageContext img) {
        super(img);
    }

    @Override
    public String toString() {
        return "nil";
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
    }

    @Override
    public ClassObject getSqClass() {
        return null;
    }

    @Override
    public Object at0(int l) {
        return null;
    }

    @Override
    public void atput0(int idx, Object object) {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int instsize() {
        return 0;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return this;
    }
}

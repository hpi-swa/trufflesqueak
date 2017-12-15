package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public final class FrameMarker extends BaseSqueakObject {

    public FrameMarker(SqueakImageContext img) {
        super(img);
    }

    public FrameMarker() {
        this(null);
    }

    @Override
    public void fillin(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ClassObject getSqClass() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object at0(int l) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void atput0(int idx, Object object) {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
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
    public String toString() {
        return "aFrameMarker";
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
    }
}

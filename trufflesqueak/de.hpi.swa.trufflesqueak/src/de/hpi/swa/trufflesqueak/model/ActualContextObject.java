package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public interface ActualContextObject {
    public Object at0(int l);

    public void atput0(int index, Object value);

    public void atContextPut0(int index, Object value) throws NonVirtualContextModification;

    public void fillin(SqueakImageChunk chunk);

    public int size();

    public Object getFrameMarker();

    public BaseSqueakObject shallowCopy();
}
